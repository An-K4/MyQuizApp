package android.kma.myquizzapp.feature.auth.presentation.forgot

/**
 * User intents for Forgot Password screen (MVI pattern).
 */
sealed interface ForgotPasswordIntent {
    data class EmailChanged(val value: String) : ForgotPasswordIntent
    data object Submit : ForgotPasswordIntent
    data object NavigateBack : ForgotPasswordIntent
}
