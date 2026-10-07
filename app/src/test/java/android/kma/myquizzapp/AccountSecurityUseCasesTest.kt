package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.AuthProvider
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.UserProfilePatch
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.security.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AccountSecurityUseCasesTest {
    @Test fun `validation requires eight characters difference and confirmation without trimming`() {
        assertTrue(SecurityValidation.change("short", "abcdefgh", "abcdefgh").hasErrors)
        assertTrue(SecurityValidation.change("abcdefgh", "abcdefgh", "abcdefgh").hasErrors)
        assertTrue(SecurityValidation.change("abcdefgh", "ijklmnop", "different").hasErrors)
        assertFalse(SecurityValidation.change(" oldpass", " newpass", " newpass").hasErrors)
        assertNotNull(SecurityValidation.deactivate("short"))
    }
    @Test fun `changing password keeps session and opaque password bytes`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        val token = checkNotNull(session.captureUserSession())
        assertEquals(SecurityMutationResult.Completed, ChangePasswordUseCase(users, session)(" oldpass", " newpass", " newpass", token))
        assertEquals(" oldpass" to " newpass", users.lastPasswords)
        assertEquals(token, session.captureUserSession())
    }
    @Test fun `invalid input never calls network`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        ChangePasswordUseCase(users, session)("short", "newpass1", "newpass1", checkNotNull(session.captureUserSession()))
        assertEquals(0, users.changeCalls)
    }
    @Test fun `google only rejects both operations without network`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        session.onAuthenticated(profileUser().copy(authProvider = AuthProvider.GOOGLE))
        val token = checkNotNull(session.captureUserSession())
        assertTrue(ChangePasswordUseCase(users, session)("oldpass1", "newpass1", "newpass1", token) is SecurityMutationResult.Rejected)
        assertTrue(DeactivateAccountUseCase(users, session)("oldpass1", true, token) is SecurityMutationResult.Rejected)
        assertEquals(0, users.changeCalls + users.deactivateCalls)
    }
    @Test fun `deactivation requires explicit confirmation`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        DeactivateAccountUseCase(users, session)("oldpass1", false, checkNotNull(session.captureUserSession()))
        assertEquals(0, users.deactivateCalls)
    }
    @Test fun `confirmed deactivation clears local session once`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        assertEquals(SecurityMutationResult.Completed, DeactivateAccountUseCase(users, session)("oldpass1", true, checkNotNull(session.captureUserSession())))
        assertEquals(SessionState.Guest, session.state.value)
        assertEquals(1, users.deactivateCalls)
    }
    @Test fun `incorrect password does not end the session`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        users.deactivateHandler = { Result.Error(AppError.Api("USER_PASSWORD_INCORRECT")) }
        assertTrue(DeactivateAccountUseCase(users, session)("oldpass1", true, checkNotNull(session.captureUserSession())) is SecurityMutationResult.Rejected)
        assertNotNull(session.state.value.userOrNull)
    }
    @Test fun `network and post write server errors are uncertain never replayed`() = runTest {
        for (error in listOf(AppError.Network, AppError.Server(500), AppError.Api("SERVER_ERROR"))) {
            val users = SecurityTestUsers(); val session = ProfileTestSession()
            users.deactivateHandler = { Result.Error(error) }
            assertEquals(SecurityMutationResult.Uncertain, DeactivateAccountUseCase(users, session)("oldpass1", true, checkNotNull(session.captureUserSession())))
            assertEquals(1, users.deactivateCalls)
            assertNotNull(session.captureUserSession())
        }
    }
    @Test fun `old response cannot retire a newer same account login`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        val gate = CompletableDeferred<Result<Unit>>()
        users.deactivateHandler = { gate.await() }
        val old = checkNotNull(session.captureUserSession())
        val request = launch {
            assertEquals(SecurityMutationResult.SessionChanged, DeactivateAccountUseCase(users, session)("oldpass1", true, old))
        }
        kotlinx.coroutines.yield()
        session.onSignedOut(); session.onAuthenticated(profileUser())
        gate.complete(Result.Success(Unit)); request.join()
        assertNotNull(session.captureUserSession())
    }
    @Test fun `terminal response clears session but does not claim deactivation success`() = runTest {
        val users = SecurityTestUsers(); val session = ProfileTestSession()
        users.deactivateHandler = { Result.Error(AppError.Api("USER_NOT_FOUND")) }
        assertEquals(SecurityMutationResult.SessionEnded, DeactivateAccountUseCase(users, session)("oldpass1", true, checkNotNull(session.captureUserSession())))
    }
}

internal class SecurityTestUsers : UserRepository {
    var changeCalls = 0; var deactivateCalls = 0
    var lastPasswords: Pair<String, String>? = null
    var changeHandler: suspend () -> Result<Unit> = { Result.Success(Unit) }
    var deactivateHandler: suspend () -> Result<Unit> = { Result.Success(Unit) }
    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        changeCalls++; lastPasswords = oldPassword to newPassword; return changeHandler()
    }
    override suspend fun deactivateAccount(password: String): Result<Unit> { deactivateCalls++; return deactivateHandler() }
    override suspend fun updateProfile(patch: UserProfilePatch): Result<User> = Result.Success(profileUser())
    override suspend fun updateAvatar(fileUrl: String): Result<String> = Result.Success(fileUrl)
    override suspend fun getCurrentUser(): Result<User> = Result.Success(profileUser())
}
