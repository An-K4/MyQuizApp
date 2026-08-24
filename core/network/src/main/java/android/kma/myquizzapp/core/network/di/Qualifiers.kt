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
 * Qualifier cho Json/Retrofit riêng của StorageApiService — KHÔNG set
 * namingStrategy như Json chung của app (provideJson trong NetworkModule).
 *
 * Lý do (phát hiện qua log thực tế 24/08): JsonNamingStrategy.SnakeCase áp
 * dụng lên tên property ĐÃ RESOLVE, kể cả khi property đã có @SerialName
 * tường minh (VD "contentType" vẫn bị đổi tiếp thành "content_type"), nên
 * không thể dùng @SerialName để "thoát" khỏi namingStrategy chung. Vì
 * storage.schema.ts (backend) dùng camelCase thật cho request/response của
 * /storage/presign, phải tách hẳn 1 Json không namingStrategy cho service này.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class StorageJson

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class StorageRetrofit
