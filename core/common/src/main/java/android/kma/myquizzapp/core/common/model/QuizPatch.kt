package android.kma.myquizzapp.core.common.model

/**
 * Patch cập nhật quiz (N16) — khớp updateQuizSchema = createQuizSchema.partial()
 * (quiz.schema.ts): mọi field đều optional.
 *
 * Semantics quan trọng của backend (quiz.service.ts + quiz.repository.ts):
 * - Field metadata nào = null sẽ KHÔNG được gửi lên (Json explicitNulls=false) và
 *   backend giữ nguyên giá trị cũ — không có cách nào "clear" 1 field về rỗng.
 * - `questions` != null → THAY THẾ toàn bộ danh sách câu hỏi (soft-delete câu cũ +
 *   insert câu mới trong 1 transaction). Gửi list rỗng bị backend từ chối
 *   (400 QUIZ_NO_QUESTIONS) — client phải validate trước.
 */
data class QuizPatch(
    val quizName: String? = null,
    val quizDescription: String? = null,
    val quizLanguage: String? = null,
    val quizImage: String? = null,
    val quizCategory: String? = null,
    val isPublic: Boolean? = null,
    val questions: List<NewQuestion>? = null
)
