package android.kma.myquizzapp.feature.auth.presentation.register

/**
 * User intents for Register screen (MVI pattern).
 */
sealed interface RegisterIntent {
    data class EmailChanged(val value: String) : RegisterIntent
    data class PasswordChanged(val value: String) : RegisterIntent
    data class FullnameChanged(val value: String) : RegisterIntent
    data class PhoneChanged(val value: String) : RegisterIntent
    data object Submit : RegisterIntent
}
