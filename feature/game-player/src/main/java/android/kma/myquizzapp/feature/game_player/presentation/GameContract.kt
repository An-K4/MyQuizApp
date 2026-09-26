package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.QuestionResults

internal data class QuestionFeedback(
    val results: QuestionResults,
    val outcome: QuestionOutcome
)

internal fun resolveQuestionFeedback(
    showCorrectAnswer: Boolean?,
    questionType: String,
    submitted: List<String>,
    results: QuestionResults
): QuestionFeedback {
    if (showCorrectAnswer != true) {
        return QuestionFeedback(
            results = results.copy(
                correctAnswers = emptyList(),
                stats = results.stats.copy(distribution = emptyMap())
            ),
            outcome = QuestionOutcome.HIDDEN
        )
    }
    return QuestionFeedback(
        results = results,
        outcome = evaluateOutcome(questionType, submitted, results.correctAnswers)
    )
}

internal fun resolveSelfPacedFeedback(
    showCorrectAnswer: Boolean?,
    questionIndex: Int,
    ack: AnswerAck
): QuestionFeedback {
    val canReveal = showCorrectAnswer == true && ack.isCorrect != null
    return QuestionFeedback(
        results = QuestionResults(
            index = questionIndex,
            correctAnswers = if (canReveal) ack.correctAnswers else emptyList()
        ),
        outcome = when {
            !canReveal -> QuestionOutcome.HIDDEN
            ack.isCorrect == true -> QuestionOutcome.CORRECT
            else -> QuestionOutcome.INCORRECT
        }
    )
}

internal fun evaluateOutcome(
    questionType: String,
    submitted: List<String>,
    correct: List<String>
): QuestionOutcome {
    if (correct.isEmpty()) return QuestionOutcome.HIDDEN
    val got = submitted.normalizeAnswerKeys()
    val want = correct.normalizeAnswerKeys()
    val matches = when (questionType) {
        "multiple_choice" -> got.size == 1 && got.first() in want
        "multiple_select" -> got == want
        "short_answer", "long_answer" -> got.size == 1 && got.first() in want
        else -> false
    }
    return if (matches) QuestionOutcome.CORRECT else QuestionOutcome.INCORRECT
}

private fun List<String>.normalizeAnswerKeys(): List<String> =
    map(String::trim).map(String::lowercase).filter(String::isNotEmpty).sorted()
