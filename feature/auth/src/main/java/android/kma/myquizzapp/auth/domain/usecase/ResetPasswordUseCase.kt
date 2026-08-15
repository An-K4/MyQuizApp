package android.kma.myquizzapp.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Resets user password using token from deep link.
 * This is the primary flow when user clicks the reset link in email.
 * Token is extracted from deep link: https://myquizz.dpdns.org/reset-password?token=...
 * Token is valid for 5 minutes (stored in Redis).
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> =
        authRepository.resetPasswordWithToken(token, newPassword)
}
