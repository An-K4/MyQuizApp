package android.kma.myquizzapp.feature.auth.presentation.login

/**
 * User intents for Login screen (MVI pattern).
 */
sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
    data class GoogleTokenReceived(val idToken: String) : LoginIntent // N9
    data object PlayAsGuest : LoginIntent
    data object GoToForgotPassword : LoginIntent
}
