package android.kma.myquizzapp.feature.auth.presentation.reset

/**
 * User intents for Reset Password screen (MVI pattern).
 *
 * N16.5: chỉ còn nhập pass — ticket/token được xử lý tự động lúc mở màn (init).
 */
sealed interface ResetPasswordIntent {
    data class PasswordChanged(val value: String) : ResetPasswordIntent
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordIntent
    data object Submit : ResetPasswordIntent
    data object NavigateBack : ResetPasswordIntent
}
