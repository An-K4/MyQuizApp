package com.example.myquizzapp.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myquizzapp.auth.domain.usecase.LoginUseCase
import com.example.myquizzapp.auth.domain.usecase.LoginWithGoogleUseCase
import com.example.myquizzapp.auth.presentation.validation.AuthValidator
import com.example.myquizzapp.core.common.error.AppError
import com.example.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class EmailChanged(val value: String) : Intent
        data class PasswordChanged(val value: String) : Intent
        data object Submit : Intent
        data class GoogleTokenReceived(val idToken: String) : Intent // N9
    }

    sealed interface Effect {
        data object NavigateToHostHome : Effect
        data class ShowMessage(val message: String) : Effect
        // TODO(phase 2): đổi String → UiText (core:ui) khi cần localize
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    // One-shot events → Channel, KHÔNG StateFlow (xem phần giải thích MVI ở trên)
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: Intent) {
        when (intent) {
            is Intent.EmailChanged -> _uiState.update { it.copy(email = intent.value, emailError = null) }
            is Intent.PasswordChanged -> _uiState.update { it.copy(password = intent.value, passwordError = null) }
            Intent.Submit -> submit()
            is Intent.GoogleTokenReceived -> loginWithGoogle(intent.idToken)
        }
    }

    private fun submit() {
        val s = uiState.value
        // Validate client-side TRƯỚC — tiết kiệm 1 round-trip; server vẫn validate lại
        // (server-authoritative), nên rule ở đây chỉ để UX, khớp auth.schema.ts.
        val emailError = AuthValidator.emailError(s.email)
        val passwordError = AuthValidator.loginPasswordError(s.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = loginUseCase(s.email.trim(), s.password)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Result.Success -> _effect.send(Effect.NavigateToHostHome)
                is Result.Error -> _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
            }
        }
    }

    private fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = loginWithGoogleUseCase(idToken)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Result.Success -> _effect.send(Effect.NavigateToHostHome)
                is Result.Error -> _effect.send(Effect.ShowMessage(result.error.toUserMessage()))
            }
        }
    }
}

// Mapper lỗi → message người dùng
private fun AppError.toUserMessage(): String = when (this) {
    AppError.Network -> "Không có kết nối mạng"
    AppError.Unauthorized -> "Email hoặc mật khẩu không đúng"
    AppError.Forbidden -> "Tài khoản đã bị vô hiệu hóa"
    AppError.NotFound -> "Không tìm thấy"
    AppError.Gone -> "Tài nguyên không còn tồn tại"
    is AppError.Server -> "Lỗi server (HTTP $httpCode)"
    is AppError.Api -> message  // Dùng message từ backend envelope
    is AppError.Unknown -> cause?.message ?: "Lỗi không xác định"
}