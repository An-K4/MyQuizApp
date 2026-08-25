package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.model.CorrectAnswer
import android.kma.myquizzapp.core.common.model.NewQuestion
import android.kma.myquizzapp.core.common.model.QuestionType
import android.net.Uri
import java.util.UUID

/**
 * Model UI-local cho 1 câu hỏi đang soạn trong editor quiz (Tạo: N13-15, Sửa: N16).
 *
 * Không dùng trực tiếp NewQuestion (core:common) vì UI cần giữ trạng thái
 * "đang soạn" (options dạng list String rời có thể rỗng, correctIndexes dạng
 * Set để dễ toggle check-box) trước khi map sang NewQuestion lúc submit.
 * localId dùng để identify câu hỏi trong list khi sửa/xóa (không phải id server).
 *
 * N16: chuyển từ package createquiz sang components để màn Sửa quiz dùng chung.
 */
data class QuestionDraft(
    val localId: String = UUID.randomUUID().toString(),
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val questionText: String = "",
    // Thời gian (giây) dạng text thô — cho phép rỗng; rỗng = mặc định 30s lúc submit
    // (xem toNewQuestion). Nhập thì phải 5-600s (validate ở ViewModel).
    val timeLimit: String = "30",
    val questionHint: String = "",
    val explanation: String = "",
    // Chỉ dùng cho MULTIPLE_CHOICE / MULTIPLE_SELECT (2-4 items theo answer_options schema).
    val options: List<String> = listOf("", ""),
    // Index (trong options) được chọn là đúng — MULTIPLE_CHOICE chỉ cho phép 1,
    // MULTIPLE_SELECT cho phép nhiều.
    val correctIndexes: Set<Int> = emptySet(),
    // Chỉ dùng cho SHORT_ANSWER / LONG_ANSWER.
    val correctText: String = "",
    // Ảnh câu hỏi MỚI chọn (local, chưa upload) — chỉ upload thật lúc submit.
    val imageUri: Uri? = null,
    // URL ảnh đã lưu trên server (chỉ màn Sửa quiz) — giữ nguyên nếu user không đổi ảnh.
    val existingImageUrl: String? = null
) {
    val isChoiceType: Boolean
        get() = questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT
}

/**
 * Map draft sang NewQuestion để gửi backend — dùng chung cho Tạo quiz và Sửa quiz.
 *
 * @param imageUrl publicUrl của ảnh vừa upload lúc submit (null nếu câu này không
 * chọn ảnh mới). Màn Sửa truyền thêm existingImageUrl làm fallback khi user giữ
 * nguyên ảnh cũ; màn Tạo luôn truyền URL vừa upload hoặc null.
 */
fun QuestionDraft.toNewQuestion(imageUrl: String?): NewQuestion = NewQuestion(
    questionType = questionType,
    questionText = questionText.trim(),
    timeLimit = timeLimit.trim().toIntOrNull() ?: 30,  // để trống = mặc định 30s
    questionImage = imageUrl,
    questionHint = questionHint.trim().ifBlank { null },
    explanation = explanation.trim().ifBlank { null },
    answerOptions = if (isChoiceType) options.filter { it.isNotBlank() } else null,
    correctAnswer = if (isChoiceType) {
        CorrectAnswer.Indexes(correctIndexes.sorted())
    } else {
        CorrectAnswer.Text(correctText.trim())
    }
)
