package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.PresignResult
import android.kma.myquizzapp.core.common.result.Result

/**
 * Repository cho luồng upload ảnh 2 bước (N15): xin URL đã ký từ backend, rồi
 * PUT bytes ảnh trực tiếp lên storage (S3-compatible), không qua backend.
 *
 * Interface đặt tại core:common để feature module (quiz-manage) không phụ thuộc
 * trực tiếp vào core:network. Implementation: StorageRepositoryImpl (core:network).
 */
interface StorageRepository {

    /**
     * Bước 1: xin backend 1 URL upload có chữ ký (hết hạn 5 phút) + URL public
     * tương ứng, dùng để lưu vào quiz_image/question_image.
     *
     * Endpoint: POST /v1/storage/presign (authRequired).
     * folder phải thuộc ALLOWED_FOLDERS backend: "avatars" | "quizzes" | "questions" | "uploads".
     */
    suspend fun presignUpload(contentType: String, folder: String, fileSize: Long): Result<PresignResult>

    /**
     * Bước 2: PUT bytes ảnh trực tiếp lên uploadUrl (bên thứ 3, KHÔNG cookie/auth
     * của app — xem StorageRepositoryImpl để biết lý do dùng client riêng).
     */
    suspend fun uploadBytes(uploadUrl: String, contentType: String, bytes: ByteArray): Result<Unit>
}
