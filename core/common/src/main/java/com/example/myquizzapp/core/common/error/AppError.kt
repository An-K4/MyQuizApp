package com.example.myquizzapp.core.common.error

sealed interface AppError {
    data object Network : AppError       // IOException / mất mạng
    data object Unauthorized : AppError  // 401 — cookie chết cả sau refresh, bắt login lại
    data object Forbidden : AppError     // 403 — VD tài khoản bị deactivated
    data object Gone : AppError          // phòng game bị xóa giữa chừng, điều hướng về Home
    data object NotFound : AppError      // 404 — sai mã phòng, quiz không tồn tại
    data class Server(val httpCode: Int) : AppError
    data class Api(val code: String?, override val message: String?) : AppError, Exception()
    data class Unknown(val cause: Throwable?) : AppError
}