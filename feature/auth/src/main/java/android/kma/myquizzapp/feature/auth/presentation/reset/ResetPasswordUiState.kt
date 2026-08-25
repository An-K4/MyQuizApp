package android.kma.myquizzapp.feature.auth.presentation.reset

/**
 * UI state for Reset Password screen (MVI pattern).
 *
 * N16.5: màn này chỉ làm việc với TICKET (cấp sau khi verify OTP/token ở bước 2) —
 * không còn dual-flow token/OTP như bản cũ.
 */
data class ResetPasswordUiState(
    val ticket: String = "",
    // Email + hạn ticket lấy từ peek (GET /users/password-reset/ticket) hoặc từ
    // response verify — chỉ để HIỂN THỊ cho user biết đang đổi pass cho tài khoản nào.
    val email: String = "",
    val ticketExpiresAt: String = "",
    // Đang verify token (deep link) hoặc peek ticket lúc mở màn.
    val isCheckingTicket: Boolean = false,
    // Ticket hết hạn/không hợp lệ → hiện lỗi toàn màn, chặn submit.
    val ticketError: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
)
