package android.kma.myquizzapp.feature.auth.presentation.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.ResetPasswordUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.ResetPasswordWithOtpUseCase
import android.kma.myquizzapp.feature.auth.presentation.validation.AuthValidator
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resetPasswordWithOtpUseCase: ResetPasswordWithOtpUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Extract parameters from navigation arguments
    private val tokenFromDeepLink: String? = savedStateHandle.get<String>("token")
    private val emailFromNavigation: String? = savedStateHandle.get<String>("email")
    private val otpFromNavigation: String? = savedStateHandle.get<String>("otp")

    data class UiState(
        val isTokenFlow: Boolean = true, // true = token flow (deep link), false = OTP flow (manual)
        val token: String = "",
        val email: String = "",
        val otp: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val emailError: String? = null,
        val otpError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class TokenChanged(val value: String) : Intent
        data class EmailChanged(val value: String) : Intent
        data class OtpChanged(val value: String) : Intent
        data class PasswordChanged(val value: String) : Intent
        data class ConfirmPasswordChanged(val value: String) : Intent
        data object ToggleFlow : Intent // Switch between token flow and OTP flow
        data object Submit : Intent
        data object NavigateBack : Intent
    }

    sealed interface Effect {
        data object NavigateToLogin : Effect
        data class ShowMessage(val message: String) : Effect
    }

    private val _uiState = MutableStateFlow(
        UiState(
            isTokenFlow = tokenFromDeepLink != null,
            token = tokenFromDeepLink ?: "",
            email = emailFromNavigation ?: "",
            otp = otpFromNavigation ?: ""
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.TokenChanged -> _uiState.update { 
                it.copy(token = intent.value, emailError = null) 
            }
            is Intent.EmailChanged -> _uiState.update { 
                it.copy(email = intent.value, emailError = null) 
            }
            is Intent.OtpChanged -> _uiState.update { 
                it.copy(otp = intent.value, otpError = null) 
            }
            is Intent.PasswordChanged -> _uiState.update { 
                it.copy(newPassword = intent.value, passwordError = null) 
            }
            is Intent.ConfirmPasswordChanged -> _uiState.update { 
                it.copy(confirmPassword = intent.value, confirmPasswordError = null) 
            }
            Intent.ToggleFlow -> _uiState.update { 
                it.copy(isTokenFlow = !it.isTokenFlow) 
            }
            Intent.Submit -> submit()
            Intent.NavigateBack -> viewModelScope.launch { 
                _effect.send(Effect.NavigateToLogin) 
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

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

        if (state.isTokenFlow) {
            submitWithToken(state.token, state.newPassword)
        } else {
            submitWithOtp(state.email, state.otp, state.newPassword)
        }
    }

    private fun submitWithToken(token: String, newPassword: String) {
        if (token.isBlank()) {
            _uiState.update { it.copy(emailError = "Token không hợp lệ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = resetPasswordUseCase(token, newPassword)) {
                is Result.Success -> {
                    _effect.send(Effect.ShowMessage("Đặt lại mật khẩu thành công! Vui lòng đăng nhập."))
                    _effect.send(Effect.NavigateToLogin)
                }
                is Result.Error -> {
                    _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun submitWithOtp(email: String, otp: String, newPassword: String) {
        // Validate email
        val emailError = AuthValidator.emailError(email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        // Validate OTP (must be 6 digits)
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otpError = "OTP phải là 6 chữ số") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = resetPasswordWithOtpUseCase(email, otp, newPassword)) {
                is Result.Success -> {
                    _effect.send(Effect.ShowMessage("Đặt lại mật khẩu thành công! Vui lòng đăng nhập."))
                    _effect.send(Effect.NavigateToLogin)
                }
                is Result.Error -> {
                    _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
