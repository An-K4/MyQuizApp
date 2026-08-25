package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.model.ResetTicketStatus
import kotlinx.serialization.Serializable

// N16.5 — DTO cho luồng reset password 3 bước (user.route.ts / user.schema.ts).
// ⚠️ Module user của backend dùng camelCase THẬT trên wire (resetTime, expiresAt,
// newPassword...) — KHÔNG snake_case như module quiz. Các DTO này chỉ đi qua
// PasswordResetApiService với Json riêng KHÔNG namingStrategy — @SerialName không
// thoát được JsonNamingStrategy.SnakeCase của Json chung (bài học N15).

/** Response data của POST /users/forgot-password: { resetTime, expiresAt } (ResetSchedule). */
@Serializable
data class ForgotPasswordDataDto(
    val resetTime: String,
    val expiresAt: String
)

/** verifyResetSchema nhánh OTP: strictObject { email, otp } — gửi kèm token sẽ bị 400. */
@Serializable
data class VerifyResetOtpRequest(val email: String, val otp: String)

/** verifyResetSchema nhánh token (deep link trong email): strictObject { token }. */
@Serializable
data class VerifyResetTokenRequest(val token: String)

/** Response data của POST /users/password-reset/verify: { ticket, expiresAt, email } (ResetTicket). */
@Serializable
data class ResetTicketDto(
    val ticket: String,
    val expiresAt: String,
    val email: String
) {
    fun toDomain(): ResetTicket = ResetTicket(ticket = ticket, expiresAt = expiresAt, email = email)
}

/** Response data của GET /users/password-reset/ticket: { email, expiresAt } (ResetTicketStatus). */
@Serializable
data class ResetTicketStatusDto(
    val email: String,
    val expiresAt: String
) {
    fun toDomain(): ResetTicketStatus = ResetTicketStatus(email = email, expiresAt = expiresAt)
}

/** completeResetSchema: strictObject { ticket, newPassword } — newPassword min 8. */
@Serializable
data class CompleteResetRequest(val ticket: String, val newPassword: String)

/** Response data dạng { message } — VD completeReset trả { message: "Password reset successfully" }. */
@Serializable
data class MessageDto(val message: String)
