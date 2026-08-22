package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.core.common.model.QuestionType
import java.util.UUID

/**
 * Model UI-local cho 1 câu hỏi đang soạn trong màn Tạo quiz.
 *
 * Không dùng trực tiếp NewQuestion (core:common) vì UI cần giữ trạng thái
 * “đang soạn” (options dạng list String rời có thể rỗng, correctIndexes dạng
 * Set để dễ toggle check-box) trước khi map sang NewQuestion lúc submit.
 * localId dùng để identify câu hỏi trong list khi sửa/xóa (không phải id server).
 */
data class QuestionDraft(
    val localId: String = UUID.randomUUID().toString(),
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val questionText: String = "",
    val timeLimit: Int = 30,
    val questionHint: String = "",
    val explanation: String = "",
    // Chỉ dùng cho MULTIPLE_CHOICE / MULTIPLE_SELECT (2-4 items theo answer_options schema).
    val options: List<String> = listOf("", ""),
    // Index (trong options) được chọn là đúng — MULTIPLE_CHOICE chỉ cho phép 1,
    // MULTIPLE_SELECT cho phép nhiều.
    val correctIndexes: Set<Int> = emptySet(),
    // Chỉ dùng cho SHORT_ANSWER / LONG_ANSWER.
    val correctText: String = ""
) {
    val isChoiceType: Boolean
        get() = questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.MULTIPLE_SELECT
}
