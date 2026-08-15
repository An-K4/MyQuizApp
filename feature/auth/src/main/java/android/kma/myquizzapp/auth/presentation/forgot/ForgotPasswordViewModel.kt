package android.kma.myquizzapp.auth.presentation.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.auth.domain.usecase.ForgotPasswordUseCase
import android.kma.myquizzapp.auth.presentation.validation.AuthValidator
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

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val emailError: String? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class EmailChanged(val value: String) : Intent
        data object Submit : Intent
        data object NavigateBack : Intent
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class NavigateToOtpVerification(val email: String) : Effect
        data class ShowMessage(val message: String) : Effect
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.EmailChanged -> _uiState.update { 
                it.copy(email = intent.value, emailError = null) 
            }
            Intent.Submit -> submit()
            Intent.NavigateBack -> viewModelScope.launch { 
                _effect.send(Effect.NavigateBack) 
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
                    _effect.send(Effect.ShowMessage(
                        "Đã gửi mã OTP đến email của bạn. Vui lòng kiểm tra."
                    ))
                    _effect.send(Effect.NavigateToOtpVerification(email))
                }
                is Result.Error -> {
                    Timber.e("Forgot Pass: API call FAILED - error: ${result.error}")
                    Timber.e("Forgot Pass: Error message: ${result.error.toUserMessage()}")
                    _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
            Timber.d("Forgot Pass: Request completed, isLoading set to false")
        }
    }
}
