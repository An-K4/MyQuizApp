package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorBody? = null,
    val meta: Meta? = null
)

// N16.5: envelope lỗi thật của backend CHỈ có code (response.ts fail()) — cố ý
// không có message/details để tránh leak nội bộ + đa ngôn ngữ. Client tự map
// code → chuỗi tiếng Việt (xem AppErrorExt.toUserMessage ở core:common).
@Serializable
data class ApiErrorBody(
    val code: String
)

@Serializable
data class Meta(
    val timestamp: String? = null,
    val pagination: PaginationMetaDto? = null
)

/**
 * Cursor pagination meta cho các endpoint listing (VD: GET /quizzes/me).
 *
 * Backend gắn field này vào `meta.pagination`, KHÔNG phải trong `data`
 * (xem response.ts success()/paginationMeta() ở backend). ResultCall
 * (ApiCallResult.kt) đọc field này và gắn vào Result.Success.page.
 */
@Serializable
data class PaginationMetaDto(
    val limit: Int,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val total: Int? = null
)