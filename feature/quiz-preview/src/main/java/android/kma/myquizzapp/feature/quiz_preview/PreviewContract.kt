package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.Quiz
import kotlin.math.roundToInt

enum class PreviewPhase { COUNTDOWN, QUESTION, FEEDBACK, FINISHED }

data class PreviewQuestionResult(
    val question: Question,
    val submittedAnswer: String?,
    val correctAnswer: String?,
    val isCorrect: Boolean?,
    val isAnswered: Boolean,
    val isLate: Boolean,
    val scoreEarned: Int,
    val timeTakenSeconds: Double
)

data class QuizPreviewUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quiz: Quiz? = null,
    val isOwner: Boolean = false,
    val phase: PreviewPhase = PreviewPhase.COUNTDOWN,
    val currentIndex: Int = 0,
    val countdownSeconds: Int = 3,
    val remainingSeconds: Int = 0,
    val isLate: Boolean = false,
    val selectedOptionIds: Set<Long> = emptySet(),
    val textAnswer: String = "",
    val currentResult: PreviewQuestionResult? = null,
    val results: List<PreviewQuestionResult> = emptyList(),
    val totalScore: Int = 0,
    val elapsedSeconds: Int = 0
) {
    val currentQuestion: Question?
        get() = quiz?.questions?.getOrNull(currentIndex)

    val canSubmit: Boolean
        get() = phase == PreviewPhase.QUESTION && when (currentQuestion?.questionType) {
            QuestionType.MULTIPLE_SELECT -> selectedOptionIds.isNotEmpty()
            QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER -> textAnswer.isNotBlank()
            else -> false
        }

    val correctCount: Int get() = results.count { it.isCorrect == true }
    val answeredCount: Int get() = results.count(PreviewQuestionResult::isAnswered)
    val accuracy: Int
        get() = quiz?.questions?.size?.takeIf { it > 0 }
            ?.let { total -> (correctCount * 100.0 / total).roundToInt() } ?: 0
}

sealed interface QuizPreviewIntent {
    data object Retry : QuizPreviewIntent
    data class SelectOption(val optionId: Long) : QuizPreviewIntent
    data class ToggleOption(val optionId: Long) : QuizPreviewIntent
    data class ChangeText(val value: String) : QuizPreviewIntent
    data object Submit : QuizPreviewIntent
    data object Skip : QuizPreviewIntent
    data object Next : QuizPreviewIntent
    data object Restart : QuizPreviewIntent
    data object Exit : QuizPreviewIntent
    data object EditQuiz : QuizPreviewIntent
}

sealed interface QuizPreviewEffect {
    data object Exit : QuizPreviewEffect
    data class EditQuiz(val quizId: Long) : QuizPreviewEffect
}
