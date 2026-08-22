package android.kma.myquizzapp.feature.auth.presentation.otp

/**
 * One-shot side effects for Otp Verification screen (MVI pattern).
 */
sealed interface OtpVerificationEffect {
    data object NavigateBack : OtpVerificationEffect
    data class NavigateToResetPassword(val email: String, val otp: String) : OtpVerificationEffect
    data class ShowMessage(val message: String) : OtpVerificationEffect
}
