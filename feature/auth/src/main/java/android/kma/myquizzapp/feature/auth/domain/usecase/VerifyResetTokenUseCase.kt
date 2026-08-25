package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * N16.5 — bước 2 (nhánh deep link): verify token trong link email để đổi lấy ticket.
 * Token tự mang theo địa chỉ email nên body chỉ cần { token } (verifyResetSchema).
 * Link hỏng/hết hạn → RESET_LINK_INVALID.
 */
class VerifyResetTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String): Result<ResetTicket> =
        authRepository.verifyResetWithToken(token)
}
