package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.PresignResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO cho POST /storage/presign.
 *
 * Khác với các DTO quiz (snake_case), field ở đây backend trả camelCase thật
 * (storage.service.ts trả { uploadUrl, publicUrl, key }, xác nhận qua
 * frontend/src/api/storage.api.js destructure đúng { uploadUrl, publicUrl }).
 *
 * LƯU Ý (đã sửa 24/08 sau lỗi thực tế, refactor 27/08): StorageApiService
 * dùng shared PreserveCaseJson KHÔNG có namingStrategy — vì
 * JsonNamingStrategy.SnakeCase của Json chung app vẫn tự đổi tên dù property
 * đã có @SerialName tường minh (VD "contentType" -> "content_type"), khiến
 * request/response method này luôn sai hình dạng nếu dùng Json chung. Các
 * @SerialName dưới đây giờ chỉ là an toàn/tài liệu (tên Kotlin đã khớp thẳng
 * JSON), không còn là cơ chế chính để tránh namingStrategy.
 */
@Serializable
data class PresignResponseDto(
    @SerialName("presignedUrl")
    val presignedUrl: PresignResultDto
)

@Serializable
data class PresignResultDto(
    @SerialName("uploadUrl")
    val uploadUrl: String,

    @SerialName("publicUrl")
    val publicUrl: String,

    // "key" không đổi hình dạng qua SnakeCase (chỉ 1 từ) nên không cần @SerialName,
    // nhưng field này hiện không được client dùng tới (xem PresignResult.kt).
    val key: String
)

fun PresignResultDto.toDomain(): PresignResult = PresignResult(
    uploadUrl = uploadUrl,
    publicUrl = publicUrl,
    key = key
)

/**
 * Request body cho POST /storage/presign — khớp presignUploadSchema
 * (storage.schema.ts): { contentType, folder, fileSize }, cũng camelCase thật
 * (frontend gửi y nguyên các field này), nên cần @SerialName tương tự.
 */
@Serializable
data class PresignUploadRequestDto(
    @SerialName("contentType")
    val contentType: String,

    val folder: String,

    @SerialName("fileSize")
    val fileSize: Long
)
