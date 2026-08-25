package android.kma.myquizzapp.feature.auth.presentation.otp

/** N16.5: backend bắt 2 lần gửi OTP cách nhau tối thiểu 60s (RESET_RESEND_TTL, user.schema.ts). */
const val RESEND_COOLDOWN_SECONDS = 60

/**
 * UI state for Otp Verification screen (MVI pattern).
 */
data class OtpVerificationUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    // N16.5: nút "Gửi lại" bị khóa khi resendSecondsLeft > 0. Vào màn đã đếm ngược
    // sẵn vì email OTP vừa được gửi ở màn Forgot — enable ngay sẽ ăn RATE_LIMITED.
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
)
