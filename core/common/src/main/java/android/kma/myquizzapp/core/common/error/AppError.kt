package android.kma.myquizzapp.core.common.error

sealed interface AppError {
    data object Network : AppError       // IOException / mất mạng
    data object Unauthorized : AppError  // 401 — cookie chết cả sau refresh, bắt login lại
    data object Forbidden : AppError     // 403 — VD tài khoản bị deactivated
    data object Gone : AppError          // phòng game bị xóa giữa chừng, điều hướng về Home
    data object NotFound : AppError      // 404 — sai mã phòng, quiz không tồn tại
    data class Server(val httpCode: Int) : AppError
    // N16.5: lỗi nghiệp vụ từ backend chỉ mang code (shared/errors/codes.ts) —
    // client tự map sang tiếng Việt, không còn message/details từ server.
    data class Api(val code: String) : AppError
    data class Unknown(val cause: Throwable?) : AppError
}