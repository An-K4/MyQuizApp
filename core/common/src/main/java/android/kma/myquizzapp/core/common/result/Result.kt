package android.kma.myquizzapp.core.common.result

import android.kma.myquizzapp.core.common.error.AppError

/**
 * Thông tin cursor-pagination đi kèm một Result.Success, dùng cho các endpoint
 * listing (VD: GET /quizzes/me) — backend trả cursor trong `meta.pagination`,
 * KHÔNG phải trong `data` (xem ApiEnvelope.Meta ở core:network). ResultCall
 * (ApiCallResult.kt) đọc meta.pagination và gắn vào đây để PagingSource dùng
 * mà không phải sửa lại toàn bộ cơ chế Result<T> hiện có.
 */
data class PageInfo(
    val nextCursor: String?,
    val hasMore: Boolean,
    val total: Int? = null
)

sealed interface Result<out T> {
    data class Success<T>(val data: T, val page: PageInfo? = null) : Result<T>
    data class Error(val error: AppError) : Result<Nothing>
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data), page)
    is Result.Error -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Error) action(error)
    return this
}