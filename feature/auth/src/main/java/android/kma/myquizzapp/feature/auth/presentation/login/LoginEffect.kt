package android.kma.myquizzapp.feature.auth.presentation.login

/**
 * One-shot side effects for Login screen (MVI pattern).
 */
sealed interface LoginEffect {
    data object NavigateToHostHome : LoginEffect
    data object NavigateToGuestHome : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
    data class ShowMessage(val message: String) : LoginEffect
    // TODO(phase 2): đổi String → UiText (core:ui) khi cần localize
}
