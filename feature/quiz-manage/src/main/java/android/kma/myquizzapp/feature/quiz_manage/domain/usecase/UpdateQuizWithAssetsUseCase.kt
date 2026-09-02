package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizPatch
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizDraft
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.toNewQuestion
import javax.inject.Inject

/**
 * Use case sửa quiz với upload ảnh mới (cover + question images).
 * 
 * Orchestrates business logic cho việc sửa quiz:
 * 1. Upload ảnh cover MỚI (nếu user chọn ảnh mới), giữ URL cũ nếu không đổi
 * 2. Upload ảnh MỚI từng câu hỏi (nếu có), giữ URL cũ nếu không đổi
 * 3. Build QuizPatch object với các URL (mới hoặc cũ)
 * 4. Gọi UpdateQuizUseCase để PATCH quiz
 * 
 * Design rationale:
 * - Tương tự CreateQuizWithAssetsUseCase nhưng cho edit use case
 * - Giữ nguyên ảnh cũ (existingCoverUrl/existingImageUrl) khi user không đổi
 * - Backend PATCH không clear được metadata field về null (nếu bỏ qua field
 *   trong payload thì giữ nguyên giá trị cũ) — chỉ có questions được replace
 *   toàn bộ (backend replaceQuizQuestions)
 * 
 * Error handling:
 * - Nếu BẤT KỲ ảnh mới nào upload lỗi → dừng ngay, KHÔNG gọi PATCH
 * - Chỉ khi tất cả ảnh mới upload thành công mới gọi updateQuiz
 * 
 * @param quizId ID quiz cần sửa
 * @param draft Quiz draft từ UI (đã validated)
 * @return Result<Quiz> - Quiz sau khi update hoặc Error
 */
class UpdateQuizWithAssetsUseCase @Inject constructor(
    private val uploadImageUseCase: UploadImageUseCase,
    private val updateQuizUseCase: UpdateQuizUseCase
) {
    suspend operator fun invoke(quizId: Long, draft: QuizDraft): Result<Quiz> {
        // 1) Upload cover image MỚI hoặc giữ URL cũ
        val coverImageUrl: String? = when {
            // User chọn ảnh mới → upload
            draft.coverImageUri != null -> {
                when (val result = uploadImageUseCase(draft.coverImageUri, folder = "quizzes")) {
                    is Result.Success -> result.data
                    is Result.Error -> return result // Early return on error
                }
            }
            // User không đổi ảnh → giữ nguyên URL cũ
            else -> draft.existingCoverUrl
        }

        // 2) Upload question images MỚI (nếu có)
        // Map: localId -> uploadedUrl (hoặc existingUrl)
        val questionImageUrls = mutableMapOf<String, String?>()
        for (question in draft.questions) {
            val imageUrl = when {
                // User chọn ảnh mới cho câu hỏi này → upload
                question.imageUri != null -> {
                    when (val result = uploadImageUseCase(question.imageUri, folder = "questions")) {
                        is Result.Success -> result.data
                        is Result.Error -> return result // Early return on error
                    }
                }
                // User không đổi ảnh → giữ nguyên URL cũ
                else -> question.existingImageUrl
            }
            questionImageUrls[question.localId] = imageUrl
        }

        // 3) Build QuizPatch object với URLs (mới hoặc cũ)
        val patch = QuizPatch(
            quizName = draft.quizName.trim(),
            quizDescription = draft.quizDescription?.trim()?.ifBlank { null },
            quizLanguage = draft.quizLanguage,
            quizImage = coverImageUrl,
            quizCategory = draft.quizCategory?.trim()?.ifBlank { null },
            isPublic = draft.isPublic,
            questions = draft.questions.map { question ->
                // toNewQuestion() xử lý conversion từ draft sang NewQuestion
                // Truyền URL đã resolve (new or existing)
                question.toNewQuestion(questionImageUrls[question.localId])
            }
        )

        // 4) PATCH quiz với tất cả ảnh đã xử lý xong
        return updateQuizUseCase(quizId, patch)
    }
}
