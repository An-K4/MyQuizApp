package android.kma.myquizzapp.feature.auth.presentation.reset

/**
 * User intents for Reset Password screen (MVI pattern).
 */
sealed interface ResetPasswordIntent {
    data class TokenChanged(val value: String) : ResetPasswordIntent
    data class EmailChanged(val value: String) : ResetPasswordIntent
    data class OtpChanged(val value: String) : ResetPasswordIntent
    data class PasswordChanged(val value: String) : ResetPasswordIntent
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordIntent
    data object ToggleFlow : ResetPasswordIntent // Switch between token flow and OTP flow
    data object Submit : ResetPasswordIntent
    data object NavigateBack : ResetPasswordIntent
}
