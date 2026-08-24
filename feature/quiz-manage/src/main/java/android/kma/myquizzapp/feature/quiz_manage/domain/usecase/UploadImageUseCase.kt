package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.repository.StorageRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.util.ImageCompressor
import android.net.Uri
import javax.inject.Inject

/**
 * Upload 1 ảnh (cover quiz hoặc ảnh câu hỏi) — N15.
 *
 * Luồng đúng theo backend (storage module): nén ảnh phía client -> POST
 * /storage/presign (xin uploadUrl + publicUrl) -> PUT bytes lên uploadUrl ->
 * trả publicUrl để gắn vào quiz_image/question_image.
 *
 * folder phải là "quizzes" (cover) hoặc "questions" (ảnh câu hỏi) — khớp
 * ALLOWED_FOLDERS (storage.schema.ts).
 *
 * Theo quyết định phạm vi N15: KHÔNG gọi use case này ngay khi người dùng chọn
 * ảnh — chỉ gọi lúc bấm "Tạo quiz" (xem CreateQuizViewModel.submit), để tránh
 * tạo object rác trên storage nếu người dùng bỏ ngang việc tạo quiz.
 */
class UploadImageUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val imageCompressor: ImageCompressor
) {
    companion object {
        private const val CONTENT_TYPE = "image/jpeg"
    }

    suspend operator fun invoke(uri: Uri, folder: String): Result<String> {
        val bytes = imageCompressor.compress(uri)
            ?: return Result.Error(AppError.Api("Không đọc được ảnh đã chọn."))

        val presigned = when (val result = storageRepository.presignUpload(
            contentType = CONTENT_TYPE,
            folder = folder,
            fileSize = bytes.size.toLong()
        )) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        return when (val uploadResult = storageRepository.uploadBytes(
            uploadUrl = presigned.uploadUrl,
            contentType = CONTENT_TYPE,
            bytes = bytes
        )) {
            is Result.Success -> Result.Success(presigned.publicUrl)
            is Result.Error -> uploadResult
        }
    }
}
