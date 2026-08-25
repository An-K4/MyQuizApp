package android.kma.myquizzapp.core.common.model

/**
 * N16.5 — ticket reset password mà backend cấp sau khi verify OTP/token thành công
 * (user.schema.ts ResetTicket). "Giấy thông hành" chứng minh user đã đọc được email;
 * sống 10 phút (RESET_TICKET_TTL) và là thứ duy nhất màn Reset cần để đổi mật khẩu.
 */
data class ResetTicket(
    val ticket: String,
    val expiresAt: String,
    val email: String
)

/** Kết quả peek GET /users/password-reset/ticket (ResetTicketStatus). */
data class ResetTicketStatus(
    val email: String,
    val expiresAt: String
)
