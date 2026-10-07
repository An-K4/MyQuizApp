package android.kma.myquizzapp.presentation.profile

import kotlinx.coroutines.flow.collect

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.domain.security.ChangePasswordUseCase
import android.kma.myquizzapp.domain.security.DeactivateAccountUseCase
import android.kma.myquizzapp.domain.security.ObserveSecurityAccountUseCase
import android.kma.myquizzapp.domain.security.PasswordErrors
import android.kma.myquizzapp.domain.security.SecurityMutationResult
import android.kma.myquizzapp.domain.security.SecurityValidation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Presentation orchestrates intents/state/effects only; all repository access stays in use cases. */
@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val observeAccount: ObserveSecurityAccountUseCase,
    private val changePassword: ChangePasswordUseCase,
    private val deactivateAccount: DeactivateAccountUseCase
) : ViewModel() {
    private val state = MutableStateFlow(AccountSecurityUiState(account = observeAccount.current()))
    val uiState = state.asStateFlow()
    private val effects = Channel<AccountSecurityEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()
    private var cleanupToken: SessionUserToken? = null

    init {
        viewModelScope.launch {
            observeAccount().collect { account ->
                val before = state.value
                if (before.account.token != account.token) {
                    // Erase secrets immediately; let the guarded use case classify an in-flight result.
                    state.value = AccountSecurityUiState(account = account, busy = before.busy)
                    if (before.busy == null && !account.loading && account.token == null)
                        effects.send(AccountSecurityEffect.SessionEnded("Phiên đăng nhập đã kết thúc"))
                } else state.update { it.copy(account = account) }
            }
        }
    }

    fun onIntent(intent: AccountSecurityIntent) {
        val current = observeAccount.current()
        if (current.token != state.value.account.token) {
            state.value = AccountSecurityUiState(account = current, busy = state.value.busy)
        }
        when (intent) {
            is AccountSecurityIntent.OldPassword -> edit { copy(oldPassword = intent.value, passwordErrors = PasswordErrors(), message = null) }
            is AccountSecurityIntent.NewPassword -> edit { copy(newPassword = intent.value, passwordErrors = PasswordErrors(), message = null) }
            is AccountSecurityIntent.ConfirmPassword -> edit { copy(confirmPassword = intent.value, passwordErrors = PasswordErrors(), message = null) }
            is AccountSecurityIntent.DeactivationPassword -> edit { copy(deactivationPassword = intent.value, deactivationError = null, message = null) }
            AccountSecurityIntent.ChangePassword -> submitPassword()
            AccountSecurityIntent.RequestDeactivation -> requestDeactivation()
            AccountSecurityIntent.CancelDeactivation -> if (state.value.busy == null) state.update { it.copy(showConfirmation = false) }
            AccountSecurityIntent.ConfirmDeactivation -> submitDeactivation()
            AccountSecurityIntent.RetryLocalCleanup -> retryCleanup()
            AccountSecurityIntent.Back -> if (state.value.busy == null && !state.value.cleanupPending) {
                eraseSecrets()
                viewModelScope.launch { effects.send(AccountSecurityEffect.Back) }
            }
        }
    }

    private fun edit(change: AccountSecurityUiState.() -> AccountSecurityUiState) {
        if (state.value.canEdit && !state.value.showConfirmation) state.update { it.change() }
    }

    private fun submitPassword() {
        val s = state.value
        val token = s.account.token ?: return
        if (!s.canEdit || s.showConfirmation) return
        val errors = SecurityValidation.change(s.oldPassword, s.newPassword, s.confirmPassword)
        state.update { it.copy(passwordErrors = errors, message = null) }
        if (errors.hasErrors) return
        state.update { it.copy(busy = SecurityAction.CHANGE_PASSWORD) }
        viewModelScope.launch {
            handle(changePassword(s.oldPassword, s.newPassword, s.confirmPassword, token), SecurityAction.CHANGE_PASSWORD, token)
        }
    }

    private fun requestDeactivation() {
        if (!state.value.canEdit) return
        val error = SecurityValidation.deactivate(state.value.deactivationPassword)
        state.update { it.copy(deactivationError = error, showConfirmation = error == null, message = null) }
    }

    private fun submitDeactivation() {
        val s = state.value
        val token = s.account.token ?: return
        if (!s.canEdit || !s.showConfirmation) return
        state.update { it.copy(busy = SecurityAction.DEACTIVATE, showConfirmation = false) }
        viewModelScope.launch {
            handle(deactivateAccount(s.deactivationPassword, true, token), SecurityAction.DEACTIVATE, token)
        }
    }

    private fun retryCleanup() {
        val token = cleanupToken ?: return
        if (!state.value.cleanupPending || state.value.busy != null) return
        state.update { it.copy(busy = SecurityAction.DEACTIVATE) }
        viewModelScope.launch { handle(deactivateAccount.finishLocal(token), SecurityAction.DEACTIVATE, token) }
    }

    private suspend fun handle(result: SecurityMutationResult, action: SecurityAction, token: SessionUserToken) {
        when (result) {
            SecurityMutationResult.Completed -> {
                eraseSecrets()
                if (action == SecurityAction.CHANGE_PASSWORD) {
                    if (observeAccount.current().token == token) effects.send(AccountSecurityEffect.Message("Đã đổi mật khẩu. Phiên hiện tại vẫn được giữ."))
                } else if (observeAccount.current().token == null) effects.send(AccountSecurityEffect.SessionEnded("Tài khoản đã được vô hiệu hóa"))
            }
            SecurityMutationResult.SessionEnded -> {
                eraseSecrets()
                if (observeAccount.current().token == null) effects.send(AccountSecurityEffect.SessionEnded("Phiên đăng nhập đã kết thúc"))
            }
            SecurityMutationResult.SessionChanged -> {
                cleanupToken = null
                state.value = AccountSecurityUiState(account = observeAccount.current())
                if (observeAccount.current().token == null) effects.send(AccountSecurityEffect.SessionEnded("Phiên đăng nhập đã kết thúc"))
                else effects.send(AccountSecurityEffect.Message("Phiên đăng nhập đã thay đổi. Kết quả cũ không được áp dụng."))
            }
            SecurityMutationResult.Uncertain -> {
                eraseSecrets()
                state.update { it.copy(uncertain = action, message = "Chưa xác định được kết quả. Ứng dụng không tự gửi lại thao tác. Hãy quay lại và kiểm tra bằng đăng nhập trước khi thử tiếp.") }
            }
            SecurityMutationResult.CleanupPending -> {
                eraseSecrets()
                cleanupToken = token
                state.update { it.copy(cleanupPending = true, message = "Cần dọn phiên trên thiết bị. Nút bên dưới chỉ dọn phiên, không gửi lại yêu cầu vô hiệu hóa.") }
            }
            is SecurityMutationResult.Rejected -> state.update { it.copy(busy = null, message = result.error.toUserMessage()) }
        }
    }

    private fun eraseSecrets() {
        state.update { it.copy(oldPassword = "", newPassword = "", confirmPassword = "", deactivationPassword = "", busy = null, showConfirmation = false) }
    }

    override fun onCleared() {
        eraseSecrets()
        cleanupToken = null
        super.onCleared()
    }
}
