package android.kma.myquizzapp.core.common.error

/** So khớp code backend mà không làm mất type an toàn của [AppError]. */
fun AppError.hasApiCode(vararg codes: String): Boolean =
    this is AppError.Api && code in codes

/** Lỗi xác nhận phiên không còn hợp lệ và cần đưa người dùng về trạng thái guest. */
val AppError.isAuthenticationRequired: Boolean
    get() = this is AppError.Unauthorized || hasApiCode(
        "AUTH_TOKEN_MISSING",
        "AUTH_TOKEN_INVALID",
        "AUTH_REFRESH_INVALID",
        "UNAUTHORIZED",
        "QUIZ_AUTH_REQUIRED",
        "GAME_AUTH_REQUIRED"
    )

/** Lỗi kết thúc phiên tài khoản; lỗi mạng và lỗi server tuyệt đối không thuộc nhóm này. */
val AppError.isSessionTerminal: Boolean
    get() = isAuthenticationRequired || hasApiCode("USER_DEACTIVATED")

/**
 * Tài nguyên chính của một màn không còn tồn tại.
 * Chỉ ViewModel của màn biết code nào là terminal; không suy luận chỉ từ HTTP 410.
 */
fun AppError.isMissingResource(vararg resourceCodes: String): Boolean =
    this is AppError.NotFound ||
        this is AppError.Gone ||
        (this is AppError.Api && (code == "GONE" || code in resourceCodes))
