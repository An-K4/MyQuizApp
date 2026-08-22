package android.kma.myquizzapp.feature.auth.presentation.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.ForgotPasswordUseCase
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
 * ViewModel cho Otp Verification screen (MVI pattern).
 *
 * UiState/Intent/Effect được tách thành file riêng (OtpVerificationUiState.kt,
 * OtpVerificationIntent.kt, OtpVerificationEffect.kt) theo cùng convention với
 * home/search/quiz-manage.
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Email passed from ForgotPasswordScreen
    private val email: String = savedStateHandle.get<String>("email") ?: ""

    private val _uiState = MutableStateFlow(OtpVerificationUiState(email = email))
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<OtpVerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: OtpVerificationIntent) {
        when (intent) {
            is OtpVerificationIntent.OtpChanged -> {
                // Only allow 6 digits
                if (intent.value.length <= 6 && intent.value.all { it.isDigit() }) {
                    _uiState.update { it.copy(otp = intent.value) }
                }
            }
            OtpVerificationIntent.Verify -> verify()
            OtpVerificationIntent.ResendCode -> resendCode()
            OtpVerificationIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(OtpVerificationEffect.NavigateBack)
            }
        }
    }

    private fun verify() {
        val otp = _uiState.value.otp

        if (otp.length != 6) {
            viewModelScope.launch {
                _effect.send(OtpVerificationEffect.ShowMessage("Vui lòng nhập đủ 6 chữ số"))
            }
            return
        }

        // Navigate to ResetPasswordScreen with email + OTP
        // The actual OTP verification will happen when user submits new password
        viewModelScope.launch {
            Timber.d("OTP Verify: Navigating to ResetPassword with email=$email, otp=$otp")
            _effect.send(OtpVerificationEffect.NavigateToResetPassword(email = email, otp = otp))
        }
    }

    private fun resendCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("OTP Verify: Resending code to $email")

            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    Timber.d("OTP Verify: Resend SUCCESS")
                    _effect.send(OtpVerificationEffect.ShowMessage("Đã gửi lại mã OTP đến email của bạn"))
                }
                is Result.Error -> {
                    Timber.e("OTP Verify: Resend FAILED - ${result.error}")
                    _effect.send(OtpVerificationEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
