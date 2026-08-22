package android.kma.myquizzapp.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.RegisterUseCase
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
 * ViewModel cho Register screen (MVI pattern).
 *
 * UiState/Intent/Effect được tách thành file riêng (RegisterUiState.kt, RegisterIntent.kt,
 * RegisterEffect.kt) theo cùng convention với home/search/quiz-manage.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.EmailChanged -> _uiState.update {
                it.copy(email = intent.value, emailError = null)
            }
            is RegisterIntent.PasswordChanged -> _uiState.update {
                it.copy(password = intent.value, passwordError = null)
            }
            is RegisterIntent.FullnameChanged -> _uiState.update {
                it.copy(fullname = intent.value, fullnameError = null)
            }
            is RegisterIntent.PhoneChanged -> _uiState.update {
                it.copy(phone = intent.value, phoneError = null)
            }
            RegisterIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val s = uiState.value
        // Validate client-side TRƯỚC — khớp registerSchema backend
        val emailError = AuthValidator.emailError(s.email)
        val passwordError = AuthValidator.registerPasswordError(s.password)
        val fullnameError = AuthValidator.fullnameError(s.fullname)
        val phoneError = AuthValidator.phoneError(s.phone)

        if (emailError != null || passwordError != null ||
            fullnameError != null || phoneError != null
        ) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    fullnameError = fullnameError,
                    phoneError = phoneError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // RegisterUseCase đã tự động login sau khi register thành công
            val result = registerUseCase(
                email = s.email.trim(),
                password = s.password,
                fullname = s.fullname.trim(),
                phone = s.phone.trim().takeIf { it.isNotBlank() }
            )
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Result.Success -> _effect.send(RegisterEffect.NavigateToHostHome)
                is Result.Error -> _effect.send(RegisterEffect.ShowMessage(result.error.toUserMessage()))
            }
        }
    }
}
