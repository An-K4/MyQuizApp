package android.kma.myquizzapp.core.network.di

import javax.inject.Qualifier

/**
 * Qualifier cho OkHttpClient KHÔNG gắn CookieJar/TokenAuthenticator — dùng để
 * PUT bytes ảnh trực tiếp lên uploadUrl (S3-compatible storage, bên thứ 3).
 *
 * KHÔNG dùng chung OkHttpClient đã auth (provideOkHttpClient) vì:
 * - Tránh rò rỉ cookie session của app sang domain của bên lưu trữ thứ 3.
 * - Tránh TokenAuthenticator hiểu nhầm lỗi 403 (presigned URL hết hạn/sai chữ ký)
 *   thành lỗi auth của app rồi tự logout/refresh token vô nghĩa.
 * - Tránh các interceptor mặc định (header Content-Type json, logging cố đọc
 *   body binary) làm sai lệch header đã được ký (SigV4) trong uploadUrl.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RawUploadOkHttpClient

/**
 * Qualifier cho Json/Retrofit dùng chung bởi các backend endpoint cần giữ nguyên
 * tên field đã resolve, không áp dụng JsonNamingStrategy.SnakeCase của Json mặc định.
 *
 * Dùng cho payload camelCase hoặc mixed naming. Với mixed naming, các field
 * snake_case ở lớp ngoài phải khai báo bằng @SerialName, còn nested camelCase
 * (ví dụ GameConfig) được giữ nguyên. Không dùng @SerialName để cố "thoát" khỏi
 * namingStrategy vì namingStrategy vẫn biến đổi cả tên đã được override.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PreserveCaseJson

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PreserveCaseRetrofit
