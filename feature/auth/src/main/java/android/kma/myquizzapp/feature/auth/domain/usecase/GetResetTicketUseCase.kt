package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.model.ResetTicketStatus
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * N16.5 — bước 2.5 (peek): hỏi thăm ticket trước khi render form đổi pass.
 * Trả email + expiresAt để hiển thị; ticket hỏng/hết hạn → RESET_TICKET_INVALID
 * (bắt sớm, đỡ để user gõ mật khẩu xong submit mới vỡ).
 */
class GetResetTicketUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(ticket: String): Result<ResetTicketStatus> =
        authRepository.getResetTicket(ticket)
}
