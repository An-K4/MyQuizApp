package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * N16.5 — bước 2 (nhánh OTP): verify mã 6 số NGAY tại màn OTP.
 * Thành công → ResetTicket (sống 10 phút, RESET_TICKET_TTL) để màn Reset đổi mật
 * khẩu. Sai/hết hạn/nhập sai quá 5 lần → RESET_OTP_INVALID / RESET_OTP_EXPIRED /
 * RESET_OTP_ATTEMPTS (đã map tiếng Việt trong AppErrorExt).
 */
class VerifyResetOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String): Result<ResetTicket> =
        authRepository.verifyResetWithOtp(email, otp)
}
