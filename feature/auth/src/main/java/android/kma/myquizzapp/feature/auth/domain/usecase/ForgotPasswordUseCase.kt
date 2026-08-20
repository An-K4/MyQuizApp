package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import timber.log.Timber
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
        Timber.d("Forgot Pass: UseCase invoke() called with email: $email")
        val result = authRepository.forgotPassword(email)
        when (result) {
            is Result.Success -> Timber.d("Forgot Pass: UseCase - Repository returned SUCCESS")
            is Result.Error -> Timber.e("Forgot Pass: UseCase - Repository returned ERROR: ${result.error}")
        }
        return result
    }
}
