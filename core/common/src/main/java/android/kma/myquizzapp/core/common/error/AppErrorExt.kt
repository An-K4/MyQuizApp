package android.kma.myquizzapp.core.common.error

// Mapper lỗi → message người dùng
fun AppError.toUserMessage(): String = when (this) {
    AppError.Network -> "Không có kết nối mạng"
    AppError.Unauthorized -> "Email hoặc mật khẩu không đúng"
    AppError.Forbidden -> "Tài khoản đã bị vô hiệu hóa"
    AppError.NotFound -> "Không tìm thấy"
    AppError.Gone -> "Tài nguyên không còn tồn tại"
    is AppError.Server -> "Lỗi server (HTTP $httpCode)"
    is AppError.Api -> message  // Dùng message từ backend envelope
    is AppError.Unknown -> cause?.message ?: "Lỗi không xác định"
}