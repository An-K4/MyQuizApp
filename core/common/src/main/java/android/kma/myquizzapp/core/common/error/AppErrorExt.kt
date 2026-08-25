package android.kma.myquizzapp.core.common.error

// Mapper lỗi → message người dùng.
// N16.5: AppError.Api giờ mang CODE — envelope lỗi thật của backend chỉ có {code}
// (response.ts fail(), danh sách đầy đủ ở shared/errors/codes.ts). Backend cố ý
// không trả message để client tự sở hữu wording (đa ngôn ngữ, tránh leak nội bộ).
fun AppError.toUserMessage(): String = when (this) {
    AppError.Network -> "Không có kết nối mạng"
    AppError.Unauthorized -> "Email hoặc mật khẩu không đúng"
    AppError.Forbidden -> "Tài khoản đã bị vô hiệu hóa"
    AppError.NotFound -> "Không tìm thấy"
    AppError.Gone -> "Tài nguyên không còn tồn tại"
    is AppError.Server -> "Lỗi server (HTTP $httpCode)"
    is AppError.Api -> apiCodeToMessage(code)
    is AppError.Unknown -> cause?.message ?: "Lỗi không xác định"
}

/**
 * Map error code của backend (codes.ts) → tiếng Việt.
 * Code lạ (backend thêm mới mà app chưa kịp map) → fallback chung cuối hàm.
 */
private fun apiCodeToMessage(code: String): String = when (code) {
    // --- Auth ---
    "AUTH_INVALID_CREDENTIALS" -> "Email hoặc mật khẩu không đúng"
    "AUTH_EMAIL_TAKEN" -> "Email này đã được đăng ký"
    "AUTH_PHONE_TAKEN" -> "Số điện thoại này đã được đăng ký"
    "AUTH_TOKEN_MISSING", "AUTH_TOKEN_INVALID", "AUTH_REFRESH_INVALID" ->
        "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
    "AUTH_GOOGLE_FAILED" -> "Đăng nhập Google thất bại, vui lòng thử lại"
    "AUTH_GOOGLE_EMAIL_UNVERIFIED" -> "Tài khoản Google chưa xác minh email"
    "AUTH_GOOGLE_ONLY" -> "Tài khoản này đăng nhập bằng Google, không có mật khẩu"

    // --- User ---
    "USER_NOT_FOUND" -> "Không tìm thấy người dùng"
    "USER_EMAIL_NOT_FOUND" -> "Email này chưa đăng ký tài khoản"
    "USER_DEACTIVATED" -> "Tài khoản đã bị vô hiệu hóa"
    "USER_PASSWORD_INCORRECT" -> "Mật khẩu hiện tại không đúng"
    "USER_NO_FIELDS_TO_UPDATE" -> "Không có thông tin nào để cập nhật"

    // --- Reset password (N16.5) ---
    "RESET_OTP_INVALID" -> "Mã OTP không đúng"
    "RESET_OTP_EXPIRED" -> "Mã OTP đã hết hạn, vui lòng gửi lại mã mới"
    "RESET_OTP_ATTEMPTS" -> "Bạn đã nhập sai quá nhiều lần, vui lòng gửi lại mã mới"
    "RESET_LINK_INVALID" -> "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"
    "RESET_TICKET_INVALID" -> "Phiên đặt lại mật khẩu đã hết hạn, vui lòng thực hiện lại từ đầu"
    "RESET_PASSWORD_REUSED" -> "Mật khẩu mới không được trùng mật khẩu cũ"

    // --- Quiz ---
    "QUIZ_NOT_FOUND" -> "Quiz không tồn tại hoặc đã bị xóa"
    "QUIZ_NO_QUESTIONS" -> "Quiz chưa có câu hỏi nào"
    "QUIZ_CURSOR_INVALID" -> "Danh sách đã hết hạn, vui lòng tải lại"
    "QUIZ_AUTH_REQUIRED" -> "Bạn cần đăng nhập để thực hiện thao tác này"

    // --- Game (map sẵn cho các N tuần 4+) ---
    "GAME_ROOM_NOT_FOUND" -> "Không tìm thấy phòng chơi"
    "GAME_NOT_HOST" -> "Chỉ chủ phòng mới thực hiện được thao tác này"
    "GAME_LOBBY_ONLY" -> "Phòng đã bắt đầu, không thể thao tác"
    "GAME_ALREADY_STARTED" -> "Trò chơi đã bắt đầu"
    "GAME_ROOM_FULL" -> "Phòng đã đầy"
    "GAME_GUESTS_NOT_ALLOWED" -> "Phòng này không cho khách tham gia"
    "GAME_HOST_CANNOT_JOIN" -> "Chủ phòng không thể tham gia như người chơi"
    "GAME_PLAYER_NOT_FOUND" -> "Không tìm thấy người chơi"
    "GAME_MODE_UNSUPPORTED" -> "Chế độ chơi không được hỗ trợ"
    "GAME_TOKEN_INVALID", "GAME_TOKEN_WRONG_ROOM" -> "Phiên chơi không hợp lệ"
    "GAME_REVIEW_DISABLED" -> "Phòng này không cho xem lại đáp án"
    "GAME_STILL_RUNNING" -> "Trò chơi vẫn đang diễn ra"
    "GAME_FORBIDDEN" -> "Bạn không có quyền xem nội dung này"
    "GAME_AUTH_REQUIRED" -> "Bạn cần đăng nhập để xem nội dung này"
    "GAME_CURSOR_INVALID" -> "Danh sách đã hết hạn, vui lòng tải lại"
    "GAME_PLAYER_ONLY" -> "Chỉ người chơi mới thực hiện được thao tác này"
    "GAME_NOT_STARTED" -> "Trò chơi chưa bắt đầu"
    "GAME_NOT_ACTIVE" -> "Trò chơi không còn hoạt động"
    "GAME_NOT_PAUSED" -> "Trò chơi không ở trạng thái tạm dừng"
    "GAME_PACING_MISMATCH" -> "Thao tác không hợp lệ với chế độ chơi"
    "GAME_ADVANCE_NOT_ALLOWED" -> "Chưa thể chuyển câu hỏi"
    "GAME_PLAYER_INACTIVE" -> "Người chơi không còn hoạt động"
    "GAME_QUESTION_NOT_FOUND" -> "Không tìm thấy câu hỏi"
    "GAME_QUESTION_LOCKED" -> "Câu hỏi đã bị khóa"
    "GAME_ANSWER_REQUIRED" -> "Bạn cần chọn đáp án"
    "GAME_ANSWER_DUPLICATE" -> "Bạn đã trả lời câu này rồi"
    "GAME_ANSWER_TOO_LATE" -> "Đã hết thời gian trả lời"

    // --- Upload ---
    "FILE_TOO_LARGE" -> "File vượt quá dung lượng cho phép"
    "FILE_TYPE_UNSUPPORTED" -> "Định dạng file không được hỗ trợ"
    "FILE_FIELD_INVALID" -> "File tải lên không hợp lệ"

    // --- Cross-cutting ---
    "VALIDATION_ERROR" -> "Dữ liệu không hợp lệ, vui lòng kiểm tra lại"
    "RATE_LIMITED" -> "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút"
    "SERVICE_UNAVAILABLE" -> "Dịch vụ tạm thời gián đoạn, vui lòng thử lại sau"
    "SERVER_ERROR" -> "Lỗi server, vui lòng thử lại sau"
    "BAD_REQUEST" -> "Yêu cầu không hợp lệ"
    "UNAUTHORIZED" -> "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
    "FORBIDDEN" -> "Bạn không có quyền thực hiện thao tác này"
    "NOT_FOUND" -> "Không tìm thấy"
    "CONFLICT" -> "Dữ liệu bị xung đột, vui lòng tải lại"
    "GONE" -> "Tài nguyên không còn tồn tại"

    else -> "Đã có lỗi xảy ra, vui lòng thử lại"
}
