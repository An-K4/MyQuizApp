package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.cookie.CredentialStorageException
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AuthStorageFailureTest {
    @Test fun loginReportsFailedCleanupWithoutCallingNetwork() = runBlocking {
        val session = FakeSession().apply { failBegin = true }
        val auth = FakeAuth()
        assertEquals(Result.Error(AppError.LocalStorage), LoginUseCase(auth, session)("a", "b"))
        assertEquals(0, auth.calls)
    }

    @Test fun failedAbortReturnsLocalStorageError() = runBlocking {
        val session = FakeSession().apply { failAbort = true }
        val result = LoginUseCase(FakeAuth(), session)("a", "b")
        assertEquals(Result.Error(AppError.LocalStorage), result)
        assertEquals(1, session.aborts)
    }

    @Test fun failedLogoutCleanupDoesNotReportSuccess() = runBlocking {
        val session = FakeSession().apply { failLogout = true }
        assertEquals(Result.Error(AppError.LocalStorage), LogoutUseCase(FakeAuth(), session)())
        assertEquals(1, session.signouts)
    }

    @Test fun networkLogoutFailureIsReportedEvenWhenLocalCleanupSucceeds() = runBlocking {
        val session = FakeSession()
        val auth = FakeAuth().apply { logoutResult = Result.Error(AppError.Network) }
        assertEquals(Result.Error(AppError.Network), LogoutUseCase(auth, session)())
        assertEquals(1, session.signouts)
    }

    @Test fun cancellationStillPropagatesWhenCleanupFails() = runBlocking {
        val session = FakeSession().apply { failAbort = true }
        val auth = FakeAuth().apply { cancelLogin = true }
        try {
            LoginUseCase(auth, session)("a", "b")
            fail("cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(1, session.aborts)
    }
}

private fun storageFailure() = CredentialStorageException(IOException("fixture"))
private class FakeSession : SessionRepository {
    override val state = MutableStateFlow<SessionState>(SessionState.Unknown)
    var failBegin = false
    var failAbort = false
    var failLogout = false
    var aborts = 0
    var signouts = 0
    override suspend fun refresh() = Unit
    override fun onAuthenticated(user: User) = Unit
    override fun onSignedOut() = Unit
    override suspend fun beginAuthentication(): Long { if (failBegin) throw storageFailure(); return 1 }
    override suspend fun abortAuthentication(epoch: Long): Boolean { aborts++; if (failAbort) throw storageFailure(); return true }
    override fun currentCredentialEpoch() = 1L
    override suspend fun signOutIfCurrent(epoch: Long): Boolean { signouts++; if (failLogout) throw storageFailure(); return true }
}
private class FakeAuth : AuthRepository {
    var calls = 0
    var cancelLogin = false
    var logoutResult: Result<Unit> = Result.Success(Unit)
    override suspend fun loginAtEpoch(email: String, password: String, epoch: Long): Result<User> {
        calls++
        if (cancelLogin) throw CancellationException("fixture")
        return Result.Error(AppError.Network)
    }
    override suspend fun logoutAtEpoch(epoch: Long): Result<Unit> = logoutResult
    override suspend fun login(email: String, password: String): Result<User> = error("unused")
    override suspend fun register(email: String, password: String, fullname: String, phone: String?): Result<User> = error("unused")
    override suspend fun loginWithGoogle(idToken: String): Result<User> = error("unused")
    override suspend fun getCurrentUser(): Result<User> = error("unused")
    override suspend fun logout(): Result<Unit> = error("unused")
    override suspend fun isAuthenticated() = false
    override suspend fun forgotPassword(email: String): Result<Unit> = error("unused")
    override suspend fun verifyResetWithOtp(email: String, otp: String): Result<ResetTicket> = error("unused")
    override suspend fun verifyResetWithToken(token: String): Result<ResetTicket> = error("unused")
    override suspend fun getResetTicket(ticket: String): Result<ResetTicketStatus> = error("unused")
    override suspend fun completeReset(ticket: String, newPassword: String): Result<Unit> = error("unused")
}
