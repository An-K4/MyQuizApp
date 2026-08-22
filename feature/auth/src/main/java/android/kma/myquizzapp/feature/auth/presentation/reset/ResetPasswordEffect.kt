package android.kma.myquizzapp.feature.auth.presentation.reset

/**
 * One-shot side effects for Reset Password screen (MVI pattern).
 */
sealed interface ResetPasswordEffect {
    data object NavigateToLogin : ResetPasswordEffect
    data class ShowMessage(val message: String) : ResetPasswordEffect
}
