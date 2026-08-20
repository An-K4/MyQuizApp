package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Resets user password using email + 6-digit OTP + new password.
 * This is the fallback flow when user cannot click deep link (e.g., email client doesn't support links).
 * User manually enters email, 6-digit OTP from email, and new password.
 * OTP is valid for 5 minutes (stored in Redis).
 */
class ResetPasswordWithOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String, newPassword: String): Result<Unit> =
        authRepository.resetPasswordWithOtp(email, otp, newPassword)
}
