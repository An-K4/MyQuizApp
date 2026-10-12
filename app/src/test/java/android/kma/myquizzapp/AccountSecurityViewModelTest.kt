package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.AuthProvider
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.security.*
import android.kma.myquizzapp.presentation.profile.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()
    private fun vm(users: SecurityTestUsers, session: ProfileTestSession) = AccountSecurityViewModel(
        ObserveSecurityAccountUseCase(session), ChangePasswordUseCase(users, session), DeactivateAccountUseCase(users, session)
    )
    private fun fill(vm: AccountSecurityViewModel) {
        vm.onIntent(AccountSecurityIntent.OldPassword("oldpass1"))
        vm.onIntent(AccountSecurityIntent.NewPassword("newpass1"))
        vm.onIntent(AccountSecurityIntent.ConfirmPassword("newpass1"))
    }
    @Test fun `duplicate submit sends one request and success clears every password`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val session = ProfileTestSession(); val vm = vm(users, session)
        val gate = CompletableDeferred<Result<Unit>>(); users.changeHandler = { gate.await() }
        runCurrent(); fill(vm)
        vm.onIntent(AccountSecurityIntent.ChangePassword); vm.onIntent(AccountSecurityIntent.ChangePassword)
        runCurrent(); assertEquals(1, users.changeCalls)
        gate.complete(Result.Success(Unit)); runCurrent()
        assertEquals("", vm.uiState.value.oldPassword); assertEquals("", vm.uiState.value.newPassword)
        assertNotNull(session.captureUserSession())
    }
    @Test fun `deactivation password is validated inside confirmation dialog`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val session = ProfileTestSession(); val vm = vm(users, session)
        runCurrent()
        vm.onIntent(AccountSecurityIntent.RequestDeactivation)
        assertTrue(vm.uiState.value.showConfirmation)

        vm.onIntent(AccountSecurityIntent.ConfirmDeactivation)
        assertNotNull(vm.uiState.value.deactivationError)
        assertEquals(0, users.deactivateCalls)

        vm.onIntent(AccountSecurityIntent.DeactivationPassword("oldpass1"))
        vm.onIntent(AccountSecurityIntent.ConfirmDeactivation)
        runCurrent()
        assertEquals(1, users.deactivateCalls)
        assertNull(session.captureUserSession())
        assertEquals("", vm.uiState.value.deactivationPassword)
    }

    @Test fun `rejected deactivation keeps dialog open with password error`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val session = ProfileTestSession(); val vm = vm(users, session)
        users.deactivateHandler = { Result.Error(AppError.Api("USER_PASSWORD_INCORRECT")) }
        runCurrent()
        vm.onIntent(AccountSecurityIntent.RequestDeactivation)
        vm.onIntent(AccountSecurityIntent.DeactivationPassword("oldpass1"))
        vm.onIntent(AccountSecurityIntent.ConfirmDeactivation)
        runCurrent()
        assertTrue(vm.uiState.value.showConfirmation)
        assertNotNull(vm.uiState.value.deactivationError)
        assertNull(vm.uiState.value.message)
        assertEquals(1, users.deactivateCalls)
        assertNotNull(session.captureUserSession())
    }

    @Test fun `cancel deactivation confirmation clears password and sends no delete`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val vm = vm(users, ProfileTestSession()); runCurrent()
        vm.onIntent(AccountSecurityIntent.RequestDeactivation)
        vm.onIntent(AccountSecurityIntent.DeactivationPassword("oldpass1"))
        vm.onIntent(AccountSecurityIntent.CancelDeactivation)
        assertFalse(vm.uiState.value.showConfirmation)
        assertEquals("", vm.uiState.value.deactivationPassword)
        vm.onIntent(AccountSecurityIntent.ConfirmDeactivation); runCurrent()
        assertEquals(0, users.deactivateCalls)
    }
    @Test fun `ambiguous error erases secrets and blocks resubmission`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val vm = vm(users, ProfileTestSession())
        users.changeHandler = { Result.Error(AppError.Network) }
        runCurrent(); fill(vm); vm.onIntent(AccountSecurityIntent.ChangePassword); runCurrent()
        assertEquals(SecurityAction.CHANGE_PASSWORD, vm.uiState.value.uncertain)
        assertEquals("", vm.uiState.value.oldPassword)
        vm.onIntent(AccountSecurityIntent.ChangePassword); runCurrent()
        assertEquals(1, users.changeCalls)
    }
    @Test fun `session switch erases secrets and google account cannot submit`() = runTest(dispatcher) {
        val users = SecurityTestUsers(); val session = ProfileTestSession(); val vm = vm(users, session)
        runCurrent(); fill(vm)
        session.onSignedOut(); session.onAuthenticated(profileUser().copy(authProvider = AuthProvider.GOOGLE)); runCurrent()
        assertEquals("", vm.uiState.value.oldPassword)
        assertFalse(vm.uiState.value.canEdit)
        vm.onIntent(AccountSecurityIntent.ChangePassword); runCurrent()
        assertEquals(0, users.changeCalls)
    }
    @Test fun `state and intents redact secrets from toString`() {
        val state = AccountSecurityUiState(oldPassword = "never-print-this")
        assertFalse(state.toString().contains("never-print-this"))
        assertFalse(AccountSecurityIntent.OldPassword("never-print-this").toString().contains("never-print-this"))
    }
}
