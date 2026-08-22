package android.kma.myquizzapp.feature.auth.presentation.register

/**
 * UI state for Register screen (MVI pattern).
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val fullname: String = "",
    val phone: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val fullnameError: String? = null,
    val phoneError: String? = null,
    val isLoading: Boolean = false,
)
