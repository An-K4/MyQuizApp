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

/**
 * ViewModel cho Reset Password screen (MVI pattern).
 *
 * UiState/Intent/Effect được tách thành file riêng (ResetPasswordUiState.kt,
 * ResetPasswordIntent.kt, ResetPasswordEffect.kt) theo cùng convention với
 * home/search/quiz-manage.
 */
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

    private val _uiState = MutableStateFlow(
        ResetPasswordUiState(
            isTokenFlow = tokenFromDeepLink != null,
            token = tokenFromDeepLink ?: "",
            email = emailFromNavigation ?: "",
            otp = otpFromNavigation ?: ""
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ResetPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ResetPasswordIntent) {
        when (intent) {
            is ResetPasswordIntent.TokenChanged -> _uiState.update { 
                it.copy(token = intent.value, emailError = null) 
            }
            is ResetPasswordIntent.EmailChanged -> _uiState.update { 
                it.copy(email = intent.value, emailError = null) 
            }
            is ResetPasswordIntent.OtpChanged -> _uiState.update { 
                it.copy(otp = intent.value, otpError = null) 
            }
            is ResetPasswordIntent.PasswordChanged -> _uiState.update { 
                it.copy(newPassword = intent.value, passwordError = null) 
            }
            is ResetPasswordIntent.ConfirmPasswordChanged -> _uiState.update { 
                it.copy(confirmPassword = intent.value, confirmPasswordError = null) 
            }
            ResetPasswordIntent.ToggleFlow -> _uiState.update { 
                it.copy(isTokenFlow = !it.isTokenFlow) 
            }
            ResetPasswordIntent.Submit -> submit()
            ResetPasswordIntent.NavigateBack -> viewModelScope.launch { 
                _effect.send(ResetPasswordEffect.NavigateToLogin) 
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
                    _effect.send(ResetPasswordEffect.ShowMessage("Đặt lại mật khẩu thành công! Vui lòng đăng nhập."))
                    _effect.send(ResetPasswordEffect.NavigateToLogin)
                }
                is Result.Error -> {
                    _effect.send(ResetPasswordEffect.ShowMessage(result.error.toUserMessage()))
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
                    _effect.send(ResetPasswordEffect.ShowMessage("Đặt lại mật khẩu thành công! Vui lòng đăng nhập."))
                    _effect.send(ResetPasswordEffect.NavigateToLogin)
                }
                is Result.Error -> {
                    _effect.send(ResetPasswordEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
