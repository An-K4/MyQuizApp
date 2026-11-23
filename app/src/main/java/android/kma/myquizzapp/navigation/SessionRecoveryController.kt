package android.kma.myquizzapp.navigation

import android.kma.myquizzapp.core.common.model.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/** UI progress only: this never changes the authoritative session or clears cookies. */
enum class SessionRecoveryState { Checking, Offline, Unavailable, Resolved }

class SessionRecoveryController(
    private val session: StateFlow<SessionState>,
    private val online: StateFlow<Boolean>,
    private val refresh: suspend () -> Unit,
    private val scope: CoroutineScope,
    private val timeoutMs: Long = 10_000L
) {
    private val attemptLock = Mutex()
    private val _state = MutableStateFlow(
        if (session.value !is SessionState.Unknown) SessionRecoveryState.Resolved
        else if (!online.value) SessionRecoveryState.Offline
        else SessionRecoveryState.Checking
    )
    val state: StateFlow<SessionRecoveryState> = _state.asStateFlow()

    init {
        scope.launch {
            session.collect { value ->
                if (value !is SessionState.Unknown) _state.value = SessionRecoveryState.Resolved
            }
        }
        scope.launch {
            online.collectLatest { connected ->
                if (session.value !is SessionState.Unknown) {
                    _state.value = SessionRecoveryState.Resolved
                } else if (!connected) {
                    _state.value = SessionRecoveryState.Offline
                } else {
                    attempt()
                }
            }
        }
    }

    fun retry() { scope.launch { attempt() } }

    private suspend fun attempt() {
        if (!attemptLock.tryLock()) return
        try {
            if (session.value !is SessionState.Unknown) {
                _state.value = SessionRecoveryState.Resolved
                return
            }
            if (!online.value) {
                _state.value = SessionRecoveryState.Offline
                return
            }
            _state.value = SessionRecoveryState.Checking
            try {
                withTimeoutOrNull(timeoutMs) {
                    // The first call can join a stale in-flight offline request.
                    // Retry at most once, with a delay, only while still unresolved.
                    repeat(2) { attempt ->
                        if (!online.value || session.value !is SessionState.Unknown) return@withTimeoutOrNull
                        refresh()
                        if (attempt == 0 && online.value && session.value is SessionState.Unknown) delay(500)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A failed request does not prove that the user is signed out.
            }
            _state.value = when {
                session.value !is SessionState.Unknown -> SessionRecoveryState.Resolved
                !online.value -> SessionRecoveryState.Offline
                else -> SessionRecoveryState.Unavailable
            }
        } finally {
            attemptLock.unlock()
        }
    }
}
