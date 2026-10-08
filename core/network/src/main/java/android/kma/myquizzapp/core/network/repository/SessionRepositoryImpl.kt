package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.error.isSessionTerminal
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject
import javax.inject.Singleton
import android.kma.myquizzapp.core.common.cookie.CookieStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
class SessionRepositoryImpl internal constructor(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
    private val cookieStore: CookieStore? = null
) : SessionRepository {
    @Inject constructor(authRepository: AuthRepository, cookieStore: CookieStore) : this(
        authRepository, CoroutineScope(SupervisorJob() + Dispatchers.Default), cookieStore
    )

    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val state: StateFlow<SessionState> = _state.asStateFlow()
    private val stateLock = Any()
    private var generation = 0L
    private var revision = 0L
    private val inFlightLock = Mutex()
    private data class RefreshJob(val revision: Long, val job: Deferred<Unit>)
    private var inFlight: RefreshJob? = null

    override suspend fun refresh() {
        val job = inFlightLock.withLock {
            val expectedRevision = synchronized(stateLock) { revision }
            inFlight?.takeIf { it.revision == expectedRevision && it.job.isActive }?.job
                ?: scope.async { fetchAndApply(expectedRevision) }.also {
                    inFlight = RefreshJob(expectedRevision, it)
                }
        }
        try {
            job.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Session refresh failed")
        }
    }

    private suspend fun fetchAndApply(expectedRevision: Long) {
        val result = authRepository.getCurrentUser()
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

    override fun captureUserSession(): SessionUserToken? = synchronized(stateLock) {
        _state.value.userOrNull?.let { SessionUserToken(it.id, generation) }
    }

    override fun snapshot(): android.kma.myquizzapp.core.common.model.SessionSnapshot = synchronized(stateLock) {
        android.kma.myquizzapp.core.common.model.SessionSnapshot(_state.value, generation)
    }

    private fun matches(token: SessionUserToken): Boolean =
        token.generation == generation && token.userId == _state.value.userOrNull?.id

    override fun applyUserUpdate(token: SessionUserToken, user: User): Boolean = synchronized(stateLock) {
        if (!matches(token) || user.id != token.userId) return@synchronized false
        revision++
        _state.value = SessionState.LoggedIn(user)
        true
    }

    override fun applyAvatarUpdate(token: SessionUserToken, avatarUrl: String): Boolean = synchronized(stateLock) {
        if (!matches(token)) return@synchronized false
        val current = _state.value.userOrNull ?: return@synchronized false
        revision++
        _state.value = SessionState.LoggedIn(current.copy(avatar = avatarUrl))
        true
    }

    override suspend fun clearSession(token: SessionUserToken): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        synchronized(stateLock) {
            if (!matches(token)) return@synchronized false
            val store = cookieStore ?: return@synchronized false
            // One critical section: no newer published login can be cleared by a stale response.
            runBlocking { store.clear() }
            generation++
            revision++
            _state.value = SessionState.Guest
            true
        }
    }

    override fun invalidateSession(token: SessionUserToken): Boolean = synchronized(stateLock) {
        if (!matches(token)) return@synchronized false
        generation++
        revision++
        _state.value = SessionState.Guest
        true
    }
}
