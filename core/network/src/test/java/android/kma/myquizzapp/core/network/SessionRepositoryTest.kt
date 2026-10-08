package android.kma.myquizzapp.core.network

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.model.ResetTicketStatus
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.repository.SessionRepositoryImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryTest {
    @Test fun `refresh started before logout cannot restore old user`() = runTest {
        val answer = CompletableDeferred<Result<User>>()
        val auth = RefreshAuth { answer.await() }
        val session = SessionRepositoryImpl(auth, backgroundScope)
        session.onAuthenticated(user())
        val request = launch { session.refresh() }
        runCurrent()
        session.onSignedOut()
        answer.complete(Result.Success(user()))
        request.join()
        assertEquals(SessionState.Guest, session.state.value)
    }

    @Test fun `refresh started before confirmed profile update cannot overwrite it`() = runTest {
        val answer = CompletableDeferred<Result<User>>()
        val session = SessionRepositoryImpl(RefreshAuth { answer.await() }, backgroundScope)
        session.onAuthenticated(user())
        val token = checkNotNull(session.captureUserSession())
        val request = launch { session.refresh() }
        runCurrent()
        assertTrue(session.applyUserUpdate(token, user().copy(fullname = "New Name")))
        answer.complete(Result.Success(user()))
        request.join()
        assertEquals("New Name", session.state.value.userOrNull?.fullname)
    }

    @Test fun `same account login has a new generation and rejects stale mutation`() = runTest {
        val session = SessionRepositoryImpl(RefreshAuth { Result.Success(user()) }, backgroundScope)
        session.onAuthenticated(user())
        val old = checkNotNull(session.captureUserSession())
        session.onSignedOut()
        session.onAuthenticated(user())
        assertFalse(session.applyAvatarUpdate(old, "https://old-response"))
        assertFalse(session.applyUserUpdate(old, user().copy(fullname = "Stale")))
        assertFalse(session.invalidateSession(old))
        assertEquals("User Name", session.state.value.userOrNull?.fullname)
    }

    @Test fun `avatar merge preserves already saved profile fields`() = runTest {
        val session = SessionRepositoryImpl(RefreshAuth { Result.Success(user()) }, backgroundScope)
        session.onAuthenticated(user())
        val token = checkNotNull(session.captureUserSession())
        session.applyUserUpdate(token, user().copy(fullname = "New Name", description = "New bio"))
        assertTrue(session.applyAvatarUpdate(token, "https://new-image"))
        assertEquals("New Name", session.state.value.userOrNull?.fullname)
        assertEquals("New bio", session.state.value.userOrNull?.description)
        assertEquals("https://new-image", session.state.value.userOrNull?.avatar)
    }

    @Test fun `refresh dedupes concurrent callers and network failure preserves login`() = runTest {
        val answer = CompletableDeferred<Result<User>>()
        val auth = RefreshAuth { answer.await() }
        val session = SessionRepositoryImpl(auth, backgroundScope)
        session.onAuthenticated(user())
        val a = launch { session.refresh() }
        val b = launch { session.refresh() }
        runCurrent()
        assertEquals(1, auth.calls)
        answer.complete(Result.Error(AppError.Network))
        a.join(); b.join()
        assertEquals(user(), session.state.value.userOrNull)
    }

    @Test fun `stale terminal response must not sign out new login`() = runTest {
        val answer = CompletableDeferred<Result<User>>()
        val session = SessionRepositoryImpl(RefreshAuth { answer.await() }, backgroundScope)
        session.onAuthenticated(user())
        val request = launch { session.refresh() }
        runCurrent()
        session.onAuthenticated(user().copy(id = 9, fullname = "Another user"))
        answer.complete(Result.Error(AppError.Unauthorized))
        request.join()
        assertEquals(9L, session.state.value.userOrNull?.id)
    }


    @Test fun `retiring current session clears cookies and prevents stale refresh resurrection`() = runTest {
        val answer = CompletableDeferred<Result<User>>()
        val cookies = RetirementCookies()
        val session = SessionRepositoryImpl(RefreshAuth { answer.await() }, backgroundScope, cookies)
        session.onAuthenticated(user())
        val token = checkNotNull(session.captureUserSession())
        val refresh = launch { session.refresh() }; runCurrent()
        assertTrue(session.clearSession(token))
        assertEquals(1, cookies.clears)
        answer.complete(Result.Success(user())); refresh.join()
        assertEquals(SessionState.Guest, session.state.value)
    }

    @Test fun `stale retirement never clears cookies of a newer login`() = runTest {
        val cookies = RetirementCookies()
        val session = SessionRepositoryImpl(RefreshAuth { Result.Success(user()) }, backgroundScope, cookies)
        session.onAuthenticated(user())
        val old = checkNotNull(session.captureUserSession())
        session.onAuthenticated(user())
        assertFalse(session.clearSession(old))
        assertEquals(0, cookies.clears)
        assertNotNull(session.captureUserSession())
    }

    @Test fun `snapshot changes lifetime for guest and repeated same account login`() = runTest {
        val session = SessionRepositoryImpl(RefreshAuth { Result.Success(user()) }, backgroundScope)
        val unknown = session.snapshot()
        assertFalse(unknown.identity.resolved)
        session.onAuthenticated(user())
        val first = session.snapshot()
        session.onAuthenticated(user())
        val second = session.snapshot()
        assertEquals(first.state, second.state)
        assertNotEquals(first.identity, second.identity)
        session.onSignedOut()
        val guest = session.snapshot()
        assertTrue(guest.identity.resolved)
        assertNull(guest.identity.userId)
        assertNotEquals(second.generation, guest.generation)
    }

    @Test fun `profile publication changes snapshot state but not identity`() = runTest {
        val session = SessionRepositoryImpl(RefreshAuth { Result.Success(user()) }, backgroundScope)
        session.onAuthenticated(user())
        val before = session.snapshot()
        assertTrue(session.applyUserUpdate(checkNotNull(session.captureUserSession()), user().copy(fullname = "Changed")))
        val after = session.snapshot()
        assertEquals(before.identity, after.identity)
        assertEquals("Changed", after.state.userOrNull?.fullname)
    }

    private fun user() = User(7, "User Name", "user@example.com", createdAt = "2026-10-01", updatedAt = "2026-10-06")
}

private class RefreshAuth(private val fetch: suspend () -> Result<User>) : AuthRepository {
    var calls = 0
    override suspend fun getCurrentUser(): Result<User> { calls++; return fetch() }
    override suspend fun login(email: String, password: String): Result<User> = unused()
    override suspend fun register(email: String, password: String, fullname: String, phone: String?): Result<User> = unused()
    override suspend fun loginWithGoogle(idToken: String): Result<User> = unused()
    override suspend fun logout(): Result<Unit> = unused()
    override suspend fun isAuthenticated(): Boolean = false
    override suspend fun forgotPassword(email: String): Result<Unit> = unused()
    override suspend fun verifyResetWithOtp(email: String, otp: String): Result<ResetTicket> = unused()
    override suspend fun verifyResetWithToken(token: String): Result<ResetTicket> = unused()
    override suspend fun getResetTicket(ticket: String): Result<ResetTicketStatus> = unused()
    override suspend fun completeReset(ticket: String, newPassword: String): Result<Unit> = unused()
    private fun <T> unused(): Result<T> = error("Not used by session tests")
}

private class RetirementCookies : android.kma.myquizzapp.core.common.cookie.CookieStore {
    @Volatile var clears = 0
    override suspend fun loadForHost(host: String) = emptyList<android.kma.myquizzapp.core.common.cookie.StoredCookie>()
    override suspend fun saveAll(host: String, cookies: List<android.kma.myquizzapp.core.common.cookie.StoredCookie>) = Unit
    override suspend fun clear() { clears++ }
}

