package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Correct keys always green; only the user's wrong selections are red. */
internal fun previewOptionFeedback(
    question: Question,
    optionId: Long,
    selectedIds: Set<Long>,
    result: PreviewQuestionResult?,
): AnswerOptionFeedback {
    if (result == null || result.question.id != question.id || result.isCorrect == null) {
        return AnswerOptionFeedback.None
    }
    val correctIds = (question.correctAnswer as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }?.toSet().orEmpty()
    return when {
        optionId in correctIds -> AnswerOptionFeedback.Correct
        result.isAnswered && optionId in selectedIds -> AnswerOptionFeedback.Incorrect
        else -> AnswerOptionFeedback.None
    }
}

internal fun previewTimeFraction(
    question: Question?, remainingSeconds: Int, remainingMillis: Long? = null,
): Float {
    val limit = question?.timeLimit?.takeIf { it > 0 } ?: return 0f
    val left = remainingMillis ?: remainingSeconds.toLong() * 1_000L
    return (left.toFloat() / (limit * 1_000L)).coerceIn(0f, 1f)
}

internal fun previewTimeIsCritical(
    question: Question?, remainingSeconds: Int, remainingMillis: Long? = null,
): Boolean {
    if (question == null || question.timeLimit <= 0) return false
    val left = (remainingMillis ?: remainingSeconds.toLong() * 1_000L).coerceAtLeast(0L)
    // Critical at 30% remaining (milliseconds), including subsecond thresholds.
    return left <= question.timeLimit.toLong() * 300L
}

internal fun previewPrimaryLabel(state: QuizPreviewUiState): String = when {
    state.phase == PreviewPhase.QUESTION &&
        state.currentQuestion?.questionType != android.kma.myquizzapp.core.common.model.QuestionType.MULTIPLE_CHOICE ->
        "Gửi đáp án"
    state.phase == PreviewPhase.FEEDBACK &&
        state.currentIndex + 1 >= state.quiz.orEmptyQuestionCount() -> "Xem tổng kết"
    else -> "Câu tiếp theo"
}

private fun android.kma.myquizzapp.core.common.model.Quiz?.orEmptyQuestionCount(): Int =
    this?.questions?.size ?: 0
