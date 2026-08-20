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

@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Email passed from ForgotPasswordScreen
    private val email: String = savedStateHandle.get<String>("email") ?: ""

    data class UiState(
        val email: String = "",
        val otp: String = "",
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class OtpChanged(val value: String) : Intent
        data object Verify : Intent
        data object ResendCode : Intent
        data object NavigateBack : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class NavigateToResetPassword(val email: String, val otp: String) : Effect
        data class ShowMessage(val message: String) : Effect
    }

    private val _uiState = MutableStateFlow(UiState(email = email))
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.OtpChanged -> {
                // Only allow 6 digits
                if (intent.value.length <= 6 && intent.value.all { it.isDigit() }) {
                    _uiState.update { it.copy(otp = intent.value) }
                }
            }
            Intent.Verify -> verify()
            Intent.ResendCode -> resendCode()
            Intent.NavigateBack -> viewModelScope.launch {
                _effect.send(Effect.NavigateBack)
            }
        }
    }

    private fun verify() {
        val otp = _uiState.value.otp

        if (otp.length != 6) {
            viewModelScope.launch {
                _effect.send(Effect.ShowMessage("Vui lòng nhập đủ 6 chữ số"))
            }
            return
        }

        // Navigate to ResetPasswordScreen with email + OTP
        // The actual OTP verification will happen when user submits new password
        viewModelScope.launch {
            Timber.d("OTP Verify: Navigating to ResetPassword with email=$email, otp=$otp")
            _effect.send(Effect.NavigateToResetPassword(email = email, otp = otp))
        }
    }

    private fun resendCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("OTP Verify: Resending code to $email")

            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    Timber.d("OTP Verify: Resend SUCCESS")
                    _effect.send(Effect.ShowMessage("Đã gửi lại mã OTP đến email của bạn"))
                }
                is Result.Error -> {
                    Timber.e("OTP Verify: Resend FAILED - ${result.error}")
                    _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
