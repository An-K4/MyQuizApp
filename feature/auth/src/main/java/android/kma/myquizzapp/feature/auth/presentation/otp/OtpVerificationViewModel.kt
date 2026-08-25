package android.kma.myquizzapp.feature.auth.presentation.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.auth.domain.usecase.ForgotPasswordUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.VerifyResetOtpUseCase
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * N16.5: bấm "Xác nhận" giờ VERIFY OTP THẬT qua POST /users/password-reset/verify
 * (bản cũ chỉ navigate, OTP sai chỉ lộ ra lúc submit password — và endpoint lúc đó
 * còn không tồn tại). Verify thành công nhận ticket (sống 10 phút) → sang màn Reset.
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val verifyResetOtpUseCase: VerifyResetOtpUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Email passed from ForgotPasswordScreen
    private val email: String = savedStateHandle.get<String>("email") ?: ""

    private val _uiState = MutableStateFlow(OtpVerificationUiState(email = email))
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<OtpVerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var resendCountdownJob: Job? = null

    init {
        // Vào màn là email OTP vừa được gửi ở màn Forgot → khóa nút "Gửi lại" 60s
        // ngay (RESET_RESEND_TTL); enable sớm chỉ tổ ăn RATE_LIMITED vô nghĩa.
        startResendCountdown()
    }

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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("OTP Verify: verifying OTP for $email")

            when (val result = verifyResetOtpUseCase(email, otp)) {
                is Result.Success -> {
                    // Verify xong OTP "chết" — màn sau chỉ dùng ticket.
                    Timber.d("OTP Verify: SUCCESS, got ticket")
                    _effect.send(
                        OtpVerificationEffect.NavigateToResetPassword(
                            ticket = result.data.ticket,
                            email = result.data.email
                        )
                    )
                }
                is Result.Error -> {
                    // RESET_OTP_INVALID / RESET_OTP_EXPIRED / RESET_OTP_ATTEMPTS —
                    // toUserMessage đã map sẵn tiếng Việt (AppErrorExt).
                    Timber.e("OTP Verify: FAILED - ${result.error}")
                    _effect.send(OtpVerificationEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun resendCode() {
        // Nút đã disable lúc đang đếm ngược — chặn thêm ở đây cho chắc.
        if (_uiState.value.resendSecondsLeft > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("OTP Verify: Resending code to $email")

            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    Timber.d("OTP Verify: Resend SUCCESS")
                    _effect.send(OtpVerificationEffect.ShowMessage("Đã gửi lại mã OTP đến email của bạn"))
                    startResendCountdown()
                }
                is Result.Error -> {
                    Timber.e("OTP Verify: Resend FAILED - ${result.error}")
                    _effect.send(OtpVerificationEffect.ShowMessage(result.error.toUserMessage()))
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun startResendCountdown() {
        resendCountdownJob?.cancel()
        resendCountdownJob = viewModelScope.launch {
            _uiState.update { it.copy(resendSecondsLeft = RESEND_COOLDOWN_SECONDS) }
            while (_uiState.value.resendSecondsLeft > 0) {
                delay(1_000)
                _uiState.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }
}
