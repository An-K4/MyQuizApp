package android.kma.myquizzapp.feature.auth.presentation.reset

/**
 * UI state for Reset Password screen (MVI pattern).
 */
data class ResetPasswordUiState(
    val isTokenFlow: Boolean = true, // true = token flow (deep link), false = OTP flow (manual)
    val token: String = "",
    val email: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val otpError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
)
