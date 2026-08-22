package android.kma.myquizzapp.feature.auth.presentation.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.ForgotPasswordUseCase
import android.kma.myquizzapp.feature.auth.presentation.validation.AuthValidator
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho Forgot Password screen (MVI pattern).
 *
 * UiState/Intent/Effect được tách thành file riêng (ForgotPasswordUiState.kt,
 * ForgotPasswordIntent.kt, ForgotPasswordEffect.kt) theo cùng convention với
 * home/search/quiz-manage.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> _uiState.update { 
                it.copy(email = intent.value, emailError = null) 
            }
            ForgotPasswordIntent.Submit -> submit()
            ForgotPasswordIntent.NavigateBack -> viewModelScope.launch { 
                _effect.send(ForgotPasswordEffect.NavigateBack) 
            }
        }
    }

    private fun submit() {
        val email = _uiState.value.email
        Timber.d("Forgot Pass: submit() called with email: $email")

        // Validation
        val emailError = AuthValidator.emailError(email)
        if (emailError != null) {
            Timber.w("Forgot Pass: Validation failed - $emailError")
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("Forgot Pass: Starting API call to forgotPasswordUseCase")

            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    Timber.d("Forgot Pass: API call SUCCESS - email sent successfully")
                    _effect.send(ForgotPasswordEffect.ShowMessage(
                        "Đã gửi mã OTP đến email của bạn. Vui lòng kiểm tra."
                    ))
                    _effect.send(ForgotPasswordEffect.NavigateToOtpVerification(email))
                }
                is Result.Error -> {
                    Timber.e("Forgot Pass: API call FAILED - error: ${result.error}")
                    Timber.e("Forgot Pass: Error message: ${result.error.toUserMessage()}")
                    _effect.send(ForgotPasswordEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
            Timber.d("Forgot Pass: Request completed, isLoading set to false")
        }
    }
}
