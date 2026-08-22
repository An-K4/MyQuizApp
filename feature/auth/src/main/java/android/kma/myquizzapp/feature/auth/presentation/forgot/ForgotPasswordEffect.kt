package android.kma.myquizzapp.feature.auth.presentation.forgot

/**
 * One-shot side effects for Forgot Password screen (MVI pattern).
 */
sealed interface ForgotPasswordEffect {
    data object NavigateBack : ForgotPasswordEffect
    data class NavigateToOtpVerification(val email: String) : ForgotPasswordEffect
    data class ShowMessage(val message: String) : ForgotPasswordEffect
}
