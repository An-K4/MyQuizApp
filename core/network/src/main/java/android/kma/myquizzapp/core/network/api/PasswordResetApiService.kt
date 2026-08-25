package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.CompleteResetRequest
import android.kma.myquizzapp.core.network.dto.ForgotPasswordDataDto
import android.kma.myquizzapp.core.network.dto.ForgotPasswordRequest
import android.kma.myquizzapp.core.network.dto.MessageDto
import android.kma.myquizzapp.core.network.dto.ResetTicketDto
import android.kma.myquizzapp.core.network.dto.ResetTicketStatusDto
import android.kma.myquizzapp.core.network.dto.VerifyResetOtpRequest
import android.kma.myquizzapp.core.network.dto.VerifyResetTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * N16.5 — 4 endpoint reset password THẬT (user.route.ts). Tách khỏi AuthApiService
 * vì module user của backend dùng camelCase thật trên wire → service này gắn vào
 * Retrofit với Json riêng KHÔNG namingStrategy (xem NetworkModule + Qualifiers.kt).
 *
 * 2 endpoint users/reset-password-token & users/reset-password từng khai báo ở
 * AuthApiService KHÔNG tồn tại trên backend (404 chắc chắn).
 *
 * Luồng 3 bước (comment user.route.ts): xin mã → chứng minh đọc được email → đổi
 * pass bằng ticket. Chỉ bước cuối đụng password, và nó không hề thấy OTP/token.
 */
interface PasswordResetApiService {

    /**
     * Bước 1 — gửi OTP 6 số + link (kèm token) về email. OTP/link sống 2 phút
     * (RESET_TTL); 2 lần gửi phải cách nhau ≥60s (RESET_RESEND_TTL), vượt → RATE_LIMITED.
     */
    @POST("users/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Result<ForgotPasswordDataDto>

    /**
     * Bước 2 (nhánh OTP) — verify mã 6 số. Sai 5 lần là OTP bị hủy
     * (RESET_MAX_ATTEMPTS). Thành công trả ticket sống 10 phút (RESET_TICKET_TTL).
     */
    @POST("users/password-reset/verify")
    suspend fun verifyResetWithOtp(@Body body: VerifyResetOtpRequest): Result<ResetTicketDto>

    /** Bước 2 (nhánh token từ deep link email) — token tự mang theo địa chỉ email. */
    @POST("users/password-reset/verify")
    suspend fun verifyResetWithToken(@Body body: VerifyResetTokenRequest): Result<ResetTicketDto>

    /** Bước 2.5 — peek: hỏi thăm ticket (email + expiresAt) trước khi render form đổi pass. */
    @GET("users/password-reset/ticket")
    suspend fun getResetTicket(@Query("ticket") ticket: String): Result<ResetTicketStatusDto>

    /** Bước 3 — đổi mật khẩu bằng ticket. Ticket dùng xong là chết. */
    @POST("users/password-reset/complete")
    suspend fun completeReset(@Body body: CompleteResetRequest): Result<MessageDto>
}
