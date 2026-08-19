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

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val fullname: String = "",
        val phone: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val fullnameError: String? = null,
        val phoneError: String? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class EmailChanged(val value: String) : Intent
        data class PasswordChanged(val value: String) : Intent
        data class FullnameChanged(val value: String) : Intent
        data class PhoneChanged(val value: String) : Intent
        data object Submit : Intent
    }

    sealed interface Effect {
        data object NavigateToHostHome : Effect
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
            is Intent.PasswordChanged -> _uiState.update {
                it.copy(password = intent.value, passwordError = null)
            }
            is Intent.FullnameChanged -> _uiState.update {
                it.copy(fullname = intent.value, fullnameError = null)
            }
            is Intent.PhoneChanged -> _uiState.update {
                it.copy(phone = intent.value, phoneError = null)
            }
            Intent.Submit -> submit()
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
                is Result.Success -> _effect.send(Effect.NavigateToHostHome)
                is Result.Error -> _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
            }
        }
    }
}
