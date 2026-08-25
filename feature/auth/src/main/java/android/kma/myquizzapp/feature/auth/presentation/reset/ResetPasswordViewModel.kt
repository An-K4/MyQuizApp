package android.kma.myquizzapp.feature.auth.presentation.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.CompleteResetUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.GetResetTicketUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.VerifyResetTokenUseCase
import android.kma.myquizzapp.feature.auth.presentation.validation.AuthValidator
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel cho Reset Password screen (MVI pattern) — N16.5.
 *
 * Màn này chỉ làm việc với TICKET (bước 3 của luồng reset). 2 cách vào màn:
 *  1. Từ màn OTP: nav args có sẵn ticket + email (đã verify) → peek ticket để
 *     hiện email từ server + bắt sớm trường hợp ticket hết hạn.
 *  2. Từ deep link email (?token=): verify token đổi lấy ticket trước đã.
 */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val verifyResetTokenUseCase: VerifyResetTokenUseCase,
    private val getResetTicketUseCase: GetResetTicketUseCase,
    private val completeResetUseCase: CompleteResetUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticketArg: String? = savedStateHandle.get<String>("ticket")?.takeIf { it.isNotBlank() }
    private val tokenArg: String? = savedStateHandle.get<String>("token")?.takeIf { it.isNotBlank() }
    private val emailArg: String? = savedStateHandle.get<String>("email")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ResetPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        when {
            ticketArg != null -> {
                _uiState.update { it.copy(ticket = ticketArg, email = emailArg.orEmpty()) }
                peekTicket(ticketArg)
            }
            tokenArg != null -> verifyToken(tokenArg)
            else -> _uiState.update {
                it.copy(ticketError = "Link đặt lại mật khẩu không hợp lệ, vui lòng thực hiện lại từ đầu.")
            }
        }
    }

    fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.PasswordChanged -> _uiState.update {
                it.copy(newPassword = intent.value, passwordError = null)
            }
            is ResetPasswordIntent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = intent.value, confirmPasswordError = null)
            }
            ResetPasswordIntent.Submit -> submit()
            ResetPasswordIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(ResetPasswordEffect.NavigateToLogin)
            }
        }
    }

    // Peek GET /users/password-reset/ticket: lấy email + expiresAt từ server.
    // Lỗi mạng thì bỏ qua (ticket vẫn thử submit được); lỗi nghiệp vụ
    // (RESET_TICKET_INVALID...) → chặn form ngay, đỡ phải gõ pass xong mới vỡ.
    private fun peekTicket(ticket: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingTicket = true) }
            when (val result = getResetTicketUseCase(ticket)) {
                is Result.Success -> _uiState.update {
                    it.copy(email = result.data.email, ticketExpiresAt = result.data.expiresAt)
                }
                is Result.Error -> if (result.error != AppError.Network) {
                    _uiState.update { it.copy(ticketError = result.error.toUserMessage()) }
                }
            }
            _uiState.update { it.copy(isCheckingTicket = false) }
        }
    }

    // Nhánh deep link: token trong email → verify để đổi lấy ticket.
    private fun verifyToken(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingTicket = true) }
            when (val result = verifyResetTokenUseCase(token)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        ticket = result.data.ticket,
                        email = result.data.email,
                        ticketExpiresAt = result.data.expiresAt
                    )
                }
                is Result.Error -> {
                    Timber.e("Reset: verify token FAILED - ${result.error}")
                    _uiState.update { it.copy(ticketError = result.error.toUserMessage()) }
                }
            }
            _uiState.update { it.copy(isCheckingTicket = false) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.ticket.isBlank()) return  // ticketError đang hiện — form đã bị chặn

        // Validate password
        val passwordError = AuthValidator.registerPasswordError(state.newPassword)
        if (passwordError != null) {
            _uiState.update { it.copy(passwordError = passwordError) }
            return
        }

        // Validate confirm password
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Mật khẩu xác nhận không khớp") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = completeResetUseCase(state.ticket, state.newPassword)) {
                is Result.Success -> {
                    _effect.send(ResetPasswordEffect.ShowMessage("Đặt lại mật khẩu thành công! Vui lòng đăng nhập."))
                    _effect.send(ResetPasswordEffect.NavigateToLogin)
                }
                is Result.Error -> {
                    // RESET_TICKET_INVALID (quá 10 phút) / RESET_PASSWORD_REUSED —
                    // đã có message tiếng Việt trong AppErrorExt.
                    _effect.send(ResetPasswordEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
