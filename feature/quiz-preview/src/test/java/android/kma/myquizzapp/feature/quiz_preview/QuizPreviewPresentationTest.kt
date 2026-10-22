package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class QuizPreviewPresentationTest {
    @Test
    fun `before submission no answer key feedback is revealed`() {
        assertEquals(AnswerOptionFeedback.None, previewOptionFeedback(question(), 1, emptySet(), null))
    }

    @Test
    fun `correct answer is green even when user selected another answer`() {
        val q = question()
        assertEquals(AnswerOptionFeedback.Correct, previewOptionFeedback(q, 1, setOf(0), result(q, false)))
        assertEquals(AnswerOptionFeedback.Incorrect, previewOptionFeedback(q, 0, setOf(0), result(q, false)))
        assertEquals(AnswerOptionFeedback.None, previewOptionFeedback(q, 2, setOf(0), result(q, false)))
    }

    @Test
    fun `multiple choice feedback handles correct selected missed and extra wrong options`() {
        val q = question().copy(questionType = QuestionType.MULTIPLE_SELECT,
            correctAnswer = JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
        val r = result(q, false)
        assertEquals(AnswerOptionFeedback.Correct, previewOptionFeedback(q, 1, setOf(0, 1), r))
        assertEquals(AnswerOptionFeedback.Correct, previewOptionFeedback(q, 2, setOf(0, 1), r))
        assertEquals(AnswerOptionFeedback.Incorrect, previewOptionFeedback(q, 0, setOf(0, 1), r))
    }

    @Test
    fun `skip reveals correct answers but does not mark abandoned selection red`() {
        val q = question()
        val r = result(q, false).copy(isAnswered = false)
        assertEquals(AnswerOptionFeedback.Correct, previewOptionFeedback(q, 1, setOf(0), r))
        assertEquals(AnswerOptionFeedback.None, previewOptionFeedback(q, 0, setOf(0), r))
    }

    @Test
    fun `missing key or result from another question stays neutral`() {
        val q = question()
        assertEquals(AnswerOptionFeedback.None, previewOptionFeedback(q, 1, emptySet(), result(q, null)))
        assertEquals(AnswerOptionFeedback.None, previewOptionFeedback(q, 1, emptySet(), result(q.copy(id = 2), true)))
    }

    @Test
    fun `timer starts full clamps bounds and is absent for untimed question`() {
        assertEquals(1f, previewTimeFraction(question(), 30), 0f)
        assertEquals(1f, previewTimeFraction(question(), 40), 0f)
        assertEquals(0f, previewTimeFraction(question(), -1), 0f)
        assertEquals(0f, previewTimeFraction(question().copy(timeLimit = 0), 30), 0f)
        assertFalse(previewTimeIsCritical(question().copy(timeLimit = 0), 0))
    }

    @Test
    fun `timer becomes critical at exactly thirty percent including subsecond limits`() {
        assertFalse(previewTimeIsCritical(question(), 10))
        assertTrue(previewTimeIsCritical(question(), 9))
        assertTrue(previewTimeIsCritical(question(), 0))
        val q = question().copy(timeLimit = 7)
        assertFalse(previewTimeIsCritical(q, 3, 2_101))
        assertTrue(previewTimeIsCritical(q, 3, 2_100))
        assertEquals(0.3f, previewTimeFraction(q, 3, 2_100), 0.0001f)
    }

    @Test
    fun `primary action labels cover single multi text feedback and final question`() {
        val q = question()
        fun state(type: QuestionType, phase: PreviewPhase, total: Int = 2) = QuizPreviewUiState(
            isLoading = false, quiz = Quiz(1, 10, quizName = "Test", quizLanguage = "vi", isPublic = true,
                createdAt = "", updatedAt = "", questions = List(total) { q.copy(id = it + 1L, questionType = type) }),
            phase = phase,
        )
        assertEquals("Câu tiếp theo", previewPrimaryLabel(state(QuestionType.MULTIPLE_CHOICE, PreviewPhase.QUESTION)))
        for (type in listOf(QuestionType.MULTIPLE_SELECT, QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER)) {
            assertEquals("Gửi đáp án", previewPrimaryLabel(state(type, PreviewPhase.QUESTION)))
            assertEquals("Câu tiếp theo", previewPrimaryLabel(state(type, PreviewPhase.FEEDBACK)))
        }
        assertEquals("Xem tổng kết", previewPrimaryLabel(state(QuestionType.MULTIPLE_CHOICE, PreviewPhase.FEEDBACK, 1)))
    }

    @Test
    fun `summary duration formats minutes and clamps negative input`() {
        assertEquals("0:00", previewElapsedLabel(-1))
        assertEquals("0:09", previewElapsedLabel(9))
        assertEquals("2:05", previewElapsedLabel(125))
        assertEquals("60:00", previewElapsedLabel(3600))
    }

    @Test
    fun `review priority preserves question identity and stable order`() {
        val correct = result(question().copy(id = 1), true)
        val wrong = result(question().copy(id = 2), false)
        val skipped = result(question().copy(id = 3), false).copy(isAnswered = false)
        val ungradable = result(question().copy(id = 4), null)
        val anotherWrong = wrong.copy(question = question().copy(id = 5))
        assertEquals(listOf(2L, 5L, 3L, 4L, 1L),
            previewOrderedResults(listOf(correct, wrong, skipped, ungradable, anotherWrong)).map { it.question.id })
    }

    @Test
    fun `summary distinguishes skip from ungradable wrong and correct`() {
        assertEquals("Bỏ qua", previewResultStatus(result(question(), null).copy(isAnswered = false)))
        assertEquals("Không thể chấm", previewResultStatus(result(question(), null)))
        assertEquals("Sai", previewResultStatus(result(question(), false)))
        assertEquals("Đúng · +950 điểm", previewResultStatus(result(question(), true).copy(scoreEarned = 950)))
    }

    @Test
    fun `summary accuracy keeps total question denominator including ungradable and skipped`() {
        val q = question()
        val state = QuizPreviewUiState(quiz = Quiz(1, 10, quizName = "Test", quizLanguage = "vi", isPublic = true,
            createdAt = "", updatedAt = "", questions = List(4) { q.copy(id = it + 1L) }),
            results = listOf(result(q, true), result(q.copy(id = 2), false),
                result(q.copy(id = 3), null), result(q.copy(id = 4), false).copy(isAnswered = false)))
        assertEquals(1, state.correctCount)
        assertEquals(3, state.answeredCount)
        assertEquals(25, state.accuracy)
    }

    private fun question() = Question(1, 1, QuestionType.MULTIPLE_CHOICE, "Chọn một", 30,
        answerOptions = listOf(AnswerOption(0, "Sai"), AnswerOption(1, "Đúng"), AnswerOption(2, "Khác")),
        correctAnswer = JsonArray(listOf(JsonPrimitive(1))))

    private fun result(q: Question, correct: Boolean?) = PreviewQuestionResult(
        question = q, submittedAnswer = "Sai", correctAnswer = "Đúng", isCorrect = correct,
        isAnswered = true, isLate = false, scoreEarned = 0, timeTakenSeconds = 1.0,
    )
}
