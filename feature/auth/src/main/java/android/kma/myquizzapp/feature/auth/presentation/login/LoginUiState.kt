package android.kma.myquizzapp.feature.auth.presentation.login

/**
 * UI state for Login screen (MVI pattern).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)
