package android.kma.myquizzapp.feature.auth.presentation.otp

/**
 * One-shot side effects for Otp Verification screen (MVI pattern).
 */
sealed interface OtpVerificationEffect {
    data object NavigateBack : OtpVerificationEffect
    // N16.5: verify OTP thành công → sang màn Reset với TICKET (không phải OTP nữa).
    data class NavigateToResetPassword(val ticket: String, val email: String) : OtpVerificationEffect
    data class ShowMessage(val message: String) : OtpVerificationEffect
}
