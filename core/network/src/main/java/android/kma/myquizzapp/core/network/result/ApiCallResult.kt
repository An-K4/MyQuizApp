package android.kma.myquizzapp.core.network.result

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.result.PageInfo
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import kotlinx.serialization.json.Json
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// ---------- Factory: quyết định method nào được "bọc" ----------
class ResultCallAdapterFactory(
    private val json: Json
) : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Với `suspend fun foo(): Result<Quiz>`, Retrofit hỏi adapter cho kiểu Call<Result<Quiz>>
        if (getRawType(returnType) != Call::class.java) return null
        val resultType = getParameterUpperBound(0, returnType as ParameterizedType)
        if (getRawType(resultType) != Result::class.java) return null
        check(resultType is ParameterizedType) { "Result phải có generic, VD: Result<Quiz>" }

        val dataType = getParameterUpperBound(0, resultType)          // Quiz
        val envelopeType = ParameterizedTypeImpl(ApiEnvelope::class.java, arrayOf(dataType))
        return ResultCallAdapter<Any?>(envelopeType, json)
    }
}

private class ResultCallAdapter<T>(
    private val envelopeType: Type,
    private val json: Json
) : CallAdapter<ApiEnvelope<T>, Call<Result<T>>> {

    override fun responseType(): Type = envelopeType  // Retrofit dùng converter cho ApiEnvelope<T>

    override fun adapt(call: Call<ApiEnvelope<T>>): Call<Result<T>> = ResultCall(call, json)
}

// ---------- Call bọc: biến Response<ApiEnvelope<T>> thành Result<T> ----------
private class ResultCall<T>(
    private val delegate: Call<ApiEnvelope<T>>,
    private val json: Json
) : Call<Result<T>> {

    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(object : Callback<ApiEnvelope<T>> {
            override fun onResponse(call: Call<ApiEnvelope<T>>, response: Response<ApiEnvelope<T>>) {
                // Luôn trả onResponse — lỗi nằm trong Result, không ném exception lên Retrofit
                callback.onResponse(this@ResultCall, Response.success(response.toResult()))
            }

            override fun onFailure(call: Call<ApiEnvelope<T>>, t: Throwable) {
                callback.onResponse(this@ResultCall, Response.success(Result.Error(t.toAppError())))
            }
        })
    }

    private fun Response<ApiEnvelope<T>>.toResult(): Result<T> {
        if (!isSuccessful) return Result.Error(mapHttpError(code(), errorBody()?.string()))
        val envelope = body() ?: return Result.Error(AppError.Unknown(null))
        // meta.pagination (cursor listing, VD GET /quizzes/me) không nằm trong `data` —
        // đọc riêng ở đây và gắn vào Result.Success.page để PagingSource dùng được mà
        // không phải thay đổi shape của data theo từng endpoint.
        val page = envelope.meta?.pagination?.let {
            PageInfo(nextCursor = it.nextCursor, hasMore = it.hasMore, total = it.total)
        }
        return when {
            envelope.success && envelope.data != null -> Result.Success(envelope.data, page)
            envelope.success ->
                @Suppress("UNCHECKED_CAST") Result.Success(Unit as T, page) // VD logout trả data: null → khai báo Result<Unit>
            else -> Result.Error(
                AppError.Api(envelope.error?.message ?: "Unknown error")
            )
        }
    }

    private fun mapHttpError(code: Int, rawBody: String?): AppError = when (code) {
        401 -> AppError.Unauthorized
        403 -> AppError.Forbidden
        404 -> AppError.NotFound
        410 -> AppError.Gone                       // phòng bị xóa → về Home (N37)
        in 500..599 -> AppError.Server(code)
        else -> AppError.Api(parseErrorMessage(rawBody) ?: "HTTP $code")
    }

    private fun parseErrorMessage(raw: String?): String? = raw?.let {
        runCatching { json.decodeFromString<ApiEnvelope<Unit>>(it).error?.message }.getOrNull()
    }

    private fun Throwable.toAppError(): AppError = when (this) {
        is IOException -> AppError.Network         // mất mạng, DNS, timeout
        else -> AppError.Unknown(this)
    }

    // --- phần còn lại chỉ là ủy quyền cho delegate ---
    override fun execute(): Response<Result<T>> =
        Response.success(runCatching { delegate.execute().toResult() }
            .getOrElse { Result.Error(it.toAppError()) })

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone(), json)
    override fun isExecuted(): Boolean = delegate.isExecuted
    override fun cancel() = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}

// ---------- Helper: Retrofit không public Utils, tự implement ParameterizedType ----------
internal class ParameterizedTypeImpl(
    private val raw: Class<*>,
    private val args: Array<Type>
) : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> = args
    override fun getRawType(): Type = raw
    override fun getOwnerType(): Type? = null
}