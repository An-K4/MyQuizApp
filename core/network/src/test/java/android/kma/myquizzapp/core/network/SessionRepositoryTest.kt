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
