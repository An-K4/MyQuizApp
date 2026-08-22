package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorBody? = null,
    val meta: Meta? = null
)

@Serializable
data class ApiErrorBody(
    val message: String,
    val details: JsonElement? = null   // backend gửi details kiểu gì cũng có — JsonElement cho an toàn
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