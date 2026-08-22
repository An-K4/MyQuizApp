package android.kma.myquizzapp.feature.auth.presentation.otp

/**
 * User intents for Otp Verification screen (MVI pattern).
 */
sealed interface OtpVerificationIntent {
    data class OtpChanged(val value: String) : OtpVerificationIntent
    data object Verify : OtpVerificationIntent
    data object ResendCode : OtpVerificationIntent
    data object NavigateBack : OtpVerificationIntent
}
