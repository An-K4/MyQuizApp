package android.kma.myquizzapp.feature.auth.presentation.otp

/**
 * UI state for Otp Verification screen (MVI pattern).
 */
data class OtpVerificationUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
)
