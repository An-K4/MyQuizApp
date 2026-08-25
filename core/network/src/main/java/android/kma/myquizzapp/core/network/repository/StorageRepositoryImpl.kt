package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.PresignResult
import android.kma.myquizzapp.core.common.repository.StorageRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.StorageApiService
import android.kma.myquizzapp.core.network.di.RawUploadOkHttpClient
import android.kma.myquizzapp.core.network.dto.PresignUploadRequestDto
import android.kma.myquizzapp.core.network.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation của StorageRepository (N15).
 *
 * presignUpload dùng storageApi (Retrofit, client đã auth — endpoint /storage/presign
 * cần cookie đăng nhập). uploadBytes dùng rawUploadClient (KHÔNG auth, KHÔNG cookie —
 * xem RawUploadOkHttpClient để biết lý do phải tách riêng).
 */
class StorageRepositoryImpl @Inject constructor(
    private val storageApi: StorageApiService,
    @RawUploadOkHttpClient private val rawUploadClient: OkHttpClient
) : StorageRepository {

    override suspend fun presignUpload(
        contentType: String,
        folder: String,
        fileSize: Long
    ): Result<PresignResult> =
        storageApi.presignUpload(
            PresignUploadRequestDto(contentType = contentType, folder = folder, fileSize = fileSize)
        ).map { it.presignedUrl.toDomain() }

    override suspend fun uploadBytes(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(uploadUrl)
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .build()

        try {
            rawUploadClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.Success(Unit)
                } else {
                    // VD 403 SignatureDoesNotMatch (hết hạn 5 phút / sai Content-Type).
                    // N16.5: AppError.Api giờ chỉ mang code của backend — lỗi S3 thô
                    // không có envelope, dùng Server(httpCode) thay thế.
                    Result.Error(AppError.Server(response.code))
                }
            }
        } catch (e: IOException) {
            Result.Error(AppError.Network)
        }
    }
}
