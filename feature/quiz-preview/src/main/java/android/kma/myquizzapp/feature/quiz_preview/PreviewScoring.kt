package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal data class PreviewEvaluation(
    val isCorrect: Boolean?,
    val submittedLabel: String?,
    val correctLabel: String?
)

internal fun evaluatePreviewAnswer(
    question: Question,
    selectedOptionIds: Set<Long>,
    textAnswer: String,
    answered: Boolean
): PreviewEvaluation {
    return when (question.questionType) {
        QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT -> {
            val correctIds = (question.correctAnswer as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }
                ?.toSet()
                .orEmpty()
            val options = question.answerOptions.orEmpty()
            val submitted = options.filter { it.id in selectedOptionIds }.joinToString(" · ") { it.optionText }
            val correct = options.filter { it.id in correctIds }.joinToString(" · ") { it.optionText }
            val validKey = correctIds.isNotEmpty()
            val matches = when (question.questionType) {
                QuestionType.MULTIPLE_CHOICE -> selectedOptionIds.size == 1 && selectedOptionIds.first() in correctIds
                QuestionType.MULTIPLE_SELECT -> selectedOptionIds == correctIds
                else -> false
            }
            PreviewEvaluation(
                isCorrect = if (!validKey || !answered) if (validKey) false else null else matches,
                submittedLabel = submitted.ifBlank { null },
                correctLabel = correct.ifBlank { null }
            )
        }
        QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER -> {
            val correct = (question.correctAnswer as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val submitted = textAnswer.trim()
            val validKey = correct.isNotEmpty()
            PreviewEvaluation(
                isCorrect = if (!validKey || !answered) if (validKey) false else null
                else submitted.equals(correct, ignoreCase = true),
                submittedLabel = submitted.ifBlank { null },
                correctLabel = correct.ifBlank { null }
            )
        }
    }
}

/** Mirrors backend engine/scoring.ts with classic defaults. */
internal fun computePreviewScore(
    isCorrect: Boolean,
    timeTakenSeconds: Double,
    timeLimitSeconds: Int,
    isLate: Boolean
): Int {
    if (!isCorrect) return 0
    val base = 1_000
    if (isLate) return (base * 0.9).roundToInt()
    if (timeLimitSeconds <= 0) return base
    val clamped = timeTakenSeconds.coerceIn(0.0, timeLimitSeconds.toDouble())
    val speedBonus = ((timeLimitSeconds - clamped) / timeLimitSeconds) * base * 0.5
    return (base + speedBonus).roundToInt()
}
