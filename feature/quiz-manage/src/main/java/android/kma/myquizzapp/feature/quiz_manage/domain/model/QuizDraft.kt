package android.kma.myquizzapp.feature.quiz_manage.domain.model

import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
import android.net.Uri

/**
 * Transfer object từ presentation layer sang domain layer khi tạo/sửa quiz.
 * 
 * Đây là model trung gian giữa UI state (CreateQuizUiState/EditQuizUiState) 
 * và domain logic (CreateQuizWithAssetsUseCase/UpdateQuizWithAssetsUseCase).
 * Giúp tách biệt presentation concerns (UI validation, local state) khỏi 
 * business logic (upload assets, build NewQuiz/QuizPatch).
 * 
 * Tái sử dụng QuestionDraft từ presentation/components vì đó đã là model
 * "đang soạn" phù hợp, không cần duplicate.
 * 
 * @property quizName Tên quiz (đã validated, trim, min 3 chars)
 * @property quizDescription Mô tả quiz (nullable, trim)
 * @property quizLanguage Ngôn ngữ quiz (vi/en)
 * @property quizCategory Danh mục quiz (nullable, trim)
 * @property isPublic Công khai hay riêng tư
 * @property coverImageUri URI ảnh cover LOCAL chưa upload (null = không có ảnh mới)
 * @property existingCoverUrl URL ảnh cover cũ từ server (dùng cho EditQuiz, null cho CreateQuiz)
 * @property questions Danh sách câu hỏi đang soạn (min 1 câu đã validated)
 */
data class QuizDraft(
    val quizName: String,
    val quizDescription: String?,
    val quizLanguage: String,
    val quizCategory: String?,
    val isPublic: Boolean,
    val coverImageUri: Uri?,
    val existingCoverUrl: String? = null,
    val questions: List<QuestionDraft>
)
