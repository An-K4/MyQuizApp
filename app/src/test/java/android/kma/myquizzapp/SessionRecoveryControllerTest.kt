package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.navigation.SessionRecoveryController
import android.kma.myquizzapp.navigation.SessionRecoveryState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRecoveryControllerTest {
    private val user = User(7, "Test User", "fake@example.com", createdAt = "2026-01-01", updatedAt = "2026-01-01")

    @Test
    fun `offline unknown shows offline without converting to guest`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(false)
        var calls = 0
        val controller = SessionRecoveryController(session, network, { calls++ }, backgroundScope)
        runCurrent()
        assertEquals(SessionRecoveryState.Offline, controller.state.value)
        assertEquals(SessionState.Unknown, session.value)
        assertEquals(0, calls)
        controller.retry()
        runCurrent()
        assertEquals(0, calls)
    }

    @Test
    fun `network returning refreshes once and publishes resolved user`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(false)
        var calls = 0
        val controller = SessionRecoveryController(session, network, {
            calls++
            session.value = SessionState.LoggedIn(user)
        }, backgroundScope)
        runCurrent()
        network.value = true
        runCurrent()
        assertEquals(1, calls)
        assertEquals(SessionState.LoggedIn(user), session.value)
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
    }

    @Test
    fun `failed refresh offers retry and does not manufacture guest`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(true)
        var calls = 0
        val controller = SessionRecoveryController(session, network, {
            calls++
            if (calls == 3) session.value = SessionState.LoggedIn(user)
        }, backgroundScope)
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(SessionRecoveryState.Unavailable, controller.state.value)
        assertEquals(SessionState.Unknown, session.value)
        controller.retry()
        runCurrent()
        assertEquals(3, calls)
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
    }

    @Test
    fun `slow unresolved request stops loading after bounded timeout`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(true)
        var calls = 0
        val controller = SessionRecoveryController(session, network, { calls++; awaitCancellation() }, backgroundScope, timeoutMs = 1000)
        runCurrent()
        assertEquals(SessionRecoveryState.Checking, controller.state.value)
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, calls)
        assertEquals(SessionRecoveryState.Unavailable, controller.state.value)
        assertEquals(SessionState.Unknown, session.value)
    }

    @Test
    fun `network loss cancels wait and reconnect tries once more`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(true)
        var calls = 0
        val controller = SessionRecoveryController(session, network, {
            calls++
            if (calls == 1) awaitCancellation() else session.value = SessionState.LoggedIn(user)
        }, backgroundScope)
        runCurrent()
        network.value = false
        runCurrent()
        assertEquals(SessionRecoveryState.Offline, controller.state.value)
        network.value = true
        runCurrent()
        assertEquals(2, calls)
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
    }

    @Test
    fun `repeated manual retries do not start overlapping requests`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(true)
        val result = CompletableDeferred<Unit>()
        var calls = 0
        val controller = SessionRecoveryController(session, network, {
            calls++
            result.await()
            session.value = SessionState.LoggedIn(user)
        }, backgroundScope)
        runCurrent()
        controller.retry()
        controller.retry()
        runCurrent()
        assertEquals(1, calls)
        result.complete(Unit)
        runCurrent()
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
    }

    @Test
    fun `reconnect retries after joining an unresolved older request`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(false)
        val oldRequest = CompletableDeferred<Unit>()
        var calls = 0
        val controller = SessionRecoveryController(session, network, {
            calls++
            if (calls == 1) oldRequest.await() else session.value = SessionState.LoggedIn(user)
        }, backgroundScope)
        runCurrent()
        network.value = true
        runCurrent()
        assertEquals(1, calls)
        oldRequest.complete(Unit)
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(SessionState.LoggedIn(user), session.value)
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
    }

    @Test
    fun `external login or logout during refresh prevents an extra attempt`() = runTest {
        for (resolved in listOf(SessionState.Guest, SessionState.LoggedIn(user))) {
            val session = MutableStateFlow<SessionState>(SessionState.Unknown)
            val network = MutableStateFlow(true)
            val request = CompletableDeferred<Unit>()
            var calls = 0
            val controller = SessionRecoveryController(session, network, {
                calls++
                request.await()
            }, backgroundScope)
            runCurrent()
            session.value = resolved
            runCurrent()
            request.complete(Unit)
            advanceTimeBy(500)
            runCurrent()
            assertEquals(1, calls)
            assertEquals(resolved, session.value)
            assertEquals(SessionRecoveryState.Resolved, controller.state.value)
        }
    }

    @Test
    fun `refresh exception offers retry without changing unknown session`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.Unknown)
        val network = MutableStateFlow(true)
        val controller = SessionRecoveryController(session, network, {
            throw IllegalStateException("fixture")
        }, backgroundScope)
        runCurrent()
        assertEquals(SessionRecoveryState.Unavailable, controller.state.value)
        assertEquals(SessionState.Unknown, session.value)
    }

    @Test
    fun `resolved guest or logged in user does not refresh on connectivity changes`() = runTest {
        val session = MutableStateFlow<SessionState>(SessionState.LoggedIn(user))
        val network = MutableStateFlow(false)
        var calls = 0
        val controller = SessionRecoveryController(session, network, { calls++ }, backgroundScope)
        runCurrent()
        network.value = true
        runCurrent()
        assertEquals(0, calls)
        assertEquals(SessionRecoveryState.Resolved, controller.state.value)
        session.value = SessionState.Guest
        network.value = false
        runCurrent()
        network.value = true
        runCurrent()
        assertEquals(0, calls)
        assertEquals(SessionState.Guest, session.value)
    }
}
