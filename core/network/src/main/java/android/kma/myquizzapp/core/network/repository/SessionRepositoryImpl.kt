package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.CredentialStorageException
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.isSessionTerminal
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.cookie.CredentialEpoch
import android.kma.myquizzapp.core.network.cookie.CredentialLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl internal constructor(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
    private val cookieStore: CookieStore? = null,
    private val lifecycle: CredentialLifecycle? = cookieStore?.let { CredentialLifecycle(it) }
) : SessionRepository {
    @Inject
    constructor(authRepository: AuthRepository, cookieStore: CookieStore, lifecycle: CredentialLifecycle) : this(
        authRepository, CoroutineScope(SupervisorJob() + Dispatchers.Default), cookieStore, lifecycle
    )

    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val state: StateFlow<SessionState> = _state.asStateFlow()
    private val stateLock = Any()
    private var generation = 0L
    private var revision = 0L
    // Only for the existing in-process retry button; not persisted or used after restart.
    private data class CleanupRetry(val token: SessionUserToken, val epoch: Long, val revision: Long)
    private var cleanupRetry: CleanupRetry? = null
    private val inFlightLock = Mutex()

    private data class RefreshJob(val revision: Long, val job: Deferred<Unit>)

    private var inFlight: RefreshJob? = null

    override suspend fun refresh() {
        val job = inFlightLock.withLock {
            val captured = lifecycle?.snapshot { epoch ->
                synchronized(stateLock) { revision to epoch.value }
            } ?: synchronized(stateLock) { revision to null }
            val expectedRevision = captured.first
            inFlight?.takeIf { it.revision == expectedRevision && it.job.isActive }?.job
                ?: scope.async { fetchAndApply(expectedRevision, captured.second) }.also {
                    inFlight = RefreshJob(expectedRevision, it)
                }
        }
        try {
            job.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e("Session refresh failed")
        }
    }

    private suspend fun fetchAndApply(expectedRevision: Long, epoch: Long?) {
        val result = if (epoch == null) authRepository.getCurrentUser()
            else authRepository.getCurrentUserAtEpoch(epoch)
        if (result is Result.Error && lifecycle != null && epoch != null &&
            (result.error.isSessionTerminal ||
                (result.error == AppError.LocalStorage && !lifecycle.isCurrent(CredentialEpoch(epoch))))
        ) {
            lifecycle.snapshot { current ->
                synchronized(stateLock) {
                    if (revision == expectedRevision) {
                        generation++
                        revision++
                        _state.value = SessionState.Guest
                        // Publish Guest even if persistent storage fails. The authenticator
                        // may already have retired this epoch.
                        if (current.value == epoch) {
                            try {
                                lifecycle.retireAndClear(current)
                            } catch (e: Exception) {
                                // The coordinator blocks cookie use after a failed clear.
                                Timber.e("Failed to clear session credentials")
                            }
                        }
                    }
                }
            }
            return
        }
        synchronized(stateLock) {
            // Logout/login or a confirmed mutation after this GET started wins.
            if (revision != expectedRevision) return
            when (result) {
                is Result.Success -> {
                    if (_state.value.userOrNull?.id != result.data.id) generation++
                    revision++
                    _state.value = SessionState.LoggedIn(result.data)
                }

                is Result.Error -> if (result.error.isSessionTerminal) {
                    generation++
                    revision++
                    _state.value = SessionState.Guest
                }
            }
        }
    }

    override fun onAuthenticated(user: User) = synchronized(stateLock) {
        generation++ // A new login of the SAME account is a different lifetime.
        revision++
        _state.value = SessionState.LoggedIn(user)
    }

    override fun onSignedOut() = synchronized(stateLock) {
        generation++
        revision++
        _state.value = SessionState.Guest
    }

    override suspend fun beginAuthentication(): Long? {
        var started: Long? = null
        try {
            return withContext(Dispatchers.IO + NonCancellable) {
                lifecycle?.beginFreshAuthentication {
                    synchronized(stateLock) {
                        generation++
                        revision++
                        _state.value = SessionState.Guest
                    }
                }?.value.also { started = it }
            }
        } catch (e: CancellationException) {
            started?.let { abortAuthentication(it) }
            throw e
        }
    }

    override fun currentCredentialEpoch(): Long? = lifecycle?.current()?.value

    override fun completeAuthentication(epoch: Long, user: User): Boolean =
        lifecycle?.ifCurrent(CredentialEpoch(epoch)) {
            if (!lifecycle.finishAuthentication(CredentialEpoch(epoch))) return@ifCurrent false
            synchronized(stateLock) {
                generation++
                revision++
                _state.value = SessionState.LoggedIn(user)
                true
            }
        } ?: false

    override suspend fun abortAuthentication(epoch: Long): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        lifecycle?.abortAuthentication(CredentialEpoch(epoch)) {
            synchronized(stateLock) {
                generation++
                revision++
                _state.value = SessionState.Guest
            }
        } ?: false
    }

    override suspend fun signOutIfCurrent(epoch: Long): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        val coordinator = lifecycle ?: return@withContext false
        coordinator.ifCurrent(CredentialEpoch(epoch)) {
            synchronized(stateLock) {
                generation++
                revision++
                _state.value = SessionState.Guest
                coordinator.retireAndClear(CredentialEpoch(epoch))
                true
            }
        } ?: false
    }

    override fun captureUserSession(): SessionUserToken? = synchronized(stateLock) {
        _state.value.userOrNull?.let { SessionUserToken(it.id, generation) }
    }

    override fun snapshot(): android.kma.myquizzapp.core.common.model.SessionSnapshot =
        synchronized(stateLock) {
            android.kma.myquizzapp.core.common.model.SessionSnapshot(_state.value, generation)
        }

    private fun matches(token: SessionUserToken): Boolean =
        token.generation == generation && token.userId == _state.value.userOrNull?.id

    override fun applyUserUpdate(token: SessionUserToken, user: User): Boolean =
        synchronized(stateLock) {
            if (!matches(token) || user.id != token.userId) return@synchronized false
            revision++
            _state.value = SessionState.LoggedIn(user)
            true
        }

    override fun applyAvatarUpdate(token: SessionUserToken, avatarUrl: String): Boolean =
        synchronized(stateLock) {
            if (!matches(token)) return@synchronized false
            val current = _state.value.userOrNull ?: return@synchronized false
            revision++
            _state.value = SessionState.LoggedIn(current.copy(avatar = avatarUrl))
            true
        }

    override suspend fun clearSession(token: SessionUserToken): Boolean =
        withContext(Dispatchers.IO + NonCancellable) {
            val coordinator = lifecycle ?: return@withContext false
            coordinator.snapshot { epoch ->
                synchronized(stateLock) {
                    val retry = cleanupRetry == CleanupRetry(token, epoch.value, revision) &&
                        _state.value == SessionState.Guest
                    if (!matches(token) && !retry) return@synchronized false
                    if (!retry) {
                        generation++
                        revision++
                        _state.value = SessionState.Guest
                    }
                    try {
                        coordinator.retireAndClear(epoch)
                        cleanupRetry = null
                        true
                    } catch (e: CredentialStorageException) {
                        cleanupRetry = CleanupRetry(token, coordinator.current().value, revision)
                        throw e
                    }
                }
            }
        }

    override suspend fun invalidateSession(token: SessionUserToken): Boolean {
        if (lifecycle != null) return clearSession(token)
        return synchronized(stateLock) {
            if (!matches(token)) return@synchronized false
            generation++
            revision++
            _state.value = SessionState.Guest
            true
        }
    }
}
