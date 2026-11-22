package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Sends a password reset email with OTP and token to the user.
 * Backend generates both 6-digit OTP and reset token, stores them in Redis (5 min TTL),
 * and sends email with:
 * - Deep link: https://myquizz.dpdns.org/reset-password?token=...
 * - OTP code for manual entry
 */
class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.forgotPassword(email)
    }
}
