package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizDraft
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.toNewQuestion
import javax.inject.Inject

/**
 * Use case tạo quiz với upload ảnh (cover + question images).
 * 
 * Orchestrates business logic cho việc tạo quiz:
 * 1. Upload ảnh cover (nếu có) lên storage
 * 2. Upload ảnh từng câu hỏi (nếu có)
 * 3. Build NewQuiz object với các URL đã upload
 * 4. Gọi CreateQuizUseCase để lưu vào database
 * 
 * Error handling:
 * - Nếu BẤT KỲ ảnh nào upload lỗi → dừng ngay, KHÔNG tạo quiz
 *   (tránh tạo quiz thiếu ảnh)
 * - Chỉ khi tất cả ảnh upload thành công mới gọi createQuiz
 * 
 * Design rationale:
 * - Tách orchestration logic ra khỏi ViewModel (Clean Architecture)
 * - ViewModel chỉ làm validation + state management
 * - UseCase làm business logic + coordination giữa các UseCase khác
 * - Dễ test, dễ tái sử dụng cho màn EditQuiz (UpdateQuizWithAssetsUseCase)
 * 
 * @param draft Quiz đang soạn từ UI (đã validated)
 * @return Result<Quiz> - Quiz vừa tạo (có id từ server) hoặc Error
 */
class CreateQuizWithAssetsUseCase @Inject constructor(
    private val uploadImageUseCase: UploadImageUseCase,
    private val createQuizUseCase: CreateQuizUseCase
) {
    suspend operator fun invoke(draft: QuizDraft): Result<Quiz> {
        // 1) Upload cover image nếu có
        val coverImageUrl: String? = draft.coverImageUri?.let { uri ->
            when (val result = uploadImageUseCase(uri, folder = "quizzes")) {
                is Result.Success -> result.data
                is Result.Error -> return result // Early return on error
            }
        }

        // 2) Upload question images (nếu có)
        // Map: localId -> uploadedUrl
        val questionImageUrls = mutableMapOf<String, String>()
        for (question in draft.questions) {
            val uri = question.imageUri ?: continue // Skip if no image
            when (val result = uploadImageUseCase(uri, folder = "questions")) {
                is Result.Success -> questionImageUrls[question.localId] = result.data
                is Result.Error -> return result // Early return on error
            }
        }

        // 3) Build NewQuiz object với URLs đã upload
        val newQuiz = NewQuiz(
            quizName = draft.quizName.trim(),
            quizDescription = draft.quizDescription?.trim()?.ifBlank { null },
            quizLanguage = draft.quizLanguage,
            quizImage = coverImageUrl,
            quizCategory = draft.quizCategory?.trim()?.ifBlank { null },
            isPublic = draft.isPublic,
            questions = draft.questions.map { question ->
                // toNewQuestion() xử lý conversion từ draft sang NewQuestion
                question.toNewQuestion(questionImageUrls[question.localId])
            }
        )

        // 4) Tạo quiz với tất cả ảnh đã upload thành công
        return createQuizUseCase(newQuiz)
    }
}
