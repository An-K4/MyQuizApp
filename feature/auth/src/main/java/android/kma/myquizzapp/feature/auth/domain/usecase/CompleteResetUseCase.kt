package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * N16.5 — bước 3: đổi mật khẩu bằng ticket. Đây là bước duy nhất đụng vào password
 * và nó không hề thấy OTP/token (comment trong user.route.ts). Ticket dùng xong là
 * chết; quá 10 phút chưa submit → RESET_TICKET_INVALID, phải làm lại từ đầu.
 */
class CompleteResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(ticket: String, newPassword: String): Result<Unit> =
        authRepository.completeReset(ticket, newPassword)
}
