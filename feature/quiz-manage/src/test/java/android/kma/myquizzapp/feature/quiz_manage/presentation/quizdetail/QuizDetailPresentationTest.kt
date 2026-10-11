package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.model.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class QuizDetailPresentationTest {
    private fun question(id: Long = 1L, seconds: Int = 30) = Question(
        id = id, quizId = 1L, questionType = QuestionType.MULTIPLE_CHOICE,
        questionText = "Câu hỏi", timeLimit = seconds,
        answerOptions = listOf(AnswerOption(11L, "Lựa chọn A"), AnswerOption(12L, "Lựa chọn B")),
    )
    private fun quiz(questions: List<Question>) = Quiz(
        id = 1L, quizOwner = 99L, quizName = "Quiz", quizLanguage = "vi", isPublic = true,
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z", questions = questions,
    )

    @Test
    fun `same duration is shown`() {
        assertEquals(30, quiz(listOf(question(), question(2L))).sharedQuestionTimeSeconds())
    }

    @Test
    fun `mixed durations omit the shared duration`() {
        assertNull(quiz(listOf(question(), question(2L, 45))).sharedQuestionTimeSeconds())
    }

    @Test
    fun `empty questions have no shared duration`() {
        assertNull(quiz(emptyList()).sharedQuestionTimeSeconds())
    }

    @Test
    fun `one question has its real duration`() {
        assertEquals(45, quiz(listOf(question(seconds = 45))).sharedQuestionTimeSeconds())
    }

    @Test
    fun `missing question count uses actual question list`() {
        assertEquals(2, quiz(listOf(question(), question(2L))).displayQuestionCount())
    }

    @Test
    fun `provided question count is preserved`() {
        assertEquals(10, quiz(listOf(question())).copy(questionCount = 10).displayQuestionCount())
    }

    @Test
    fun `missing or blank owner uses neutral label not numeric owner ID`() {
        val source = quiz(emptyList())
        assertEquals("Tác giả không còn khả dụng", source.displayAuthorName())
        assertEquals("Tác giả không còn khả dụng", source.copy(owner = QuizOwner(99L, "  ")).displayAuthorName())
        assertEquals("Hoàng Long", source.copy(owner = QuizOwner(99L, " Hoàng Long ")).displayAuthorName())
    }

    @Test
    fun `answer key hint and explanation never affect question preview projection`() {
        val source = question()
        val withSecrets = source.copy(
            correctAnswer = JsonArray(listOf(JsonPrimitive(11L))),
            questionHint = "Gợi ý có thể tiết lộ đáp án", explanation = "Đáp án là A",
        )
        assertEquals(source.toQuestionPreviewUiModel(), withSecrets.toQuestionPreviewUiModel())
        assertEquals(listOf("Lựa chọn A", "Lựa chọn B"), withSecrets.toQuestionPreviewUiModel().optionTexts)
        // The typed projection has no correctAnswer/hint/explanation fields to forward into the card.
    }

    @Test
    fun `date is formatted in requested timezone`() {
        assertEquals("11/10/2026", formatQuizCreatedDate("2026-10-10T23:30:00Z", ZoneId.of("Asia/Ho_Chi_Minh")))
    }

    @Test
    fun `malformed date is omitted`() {
        assertNull(formatQuizCreatedDate("invalid-date", ZoneId.of("UTC")))
    }
}
