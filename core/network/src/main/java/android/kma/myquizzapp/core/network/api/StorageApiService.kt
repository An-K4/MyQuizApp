package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.PresignResponseDto
import android.kma.myquizzapp.core.network.dto.PresignUploadRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service cho storage presign endpoint.
 *
 * Base URL: /v1/storage
 */
interface StorageApiService {

    /**
     * POST /v1/storage/presign
     *
     * Auth required: cookie authentication — userId dùng để tạo object key
     * {folder}/{userId}/{uuid} (storage.service.ts). Endpoint này chỉ trả về
     * URL, KHÔNG nhận bytes ảnh.
     */
    @POST("storage/presign")
    suspend fun presignUpload(@Body body: PresignUploadRequestDto): Result<PresignResponseDto>
}
