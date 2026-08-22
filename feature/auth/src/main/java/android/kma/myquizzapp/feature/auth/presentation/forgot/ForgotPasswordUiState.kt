package android.kma.myquizzapp.feature.auth.presentation.forgot

/**
 * UI state for Forgot Password screen (MVI pattern).
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
)
