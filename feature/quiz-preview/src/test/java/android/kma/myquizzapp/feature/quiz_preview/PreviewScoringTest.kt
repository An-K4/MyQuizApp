package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.AnswerOption
import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewScoringTest {
    @Test
    fun `single choice grades by option id`() {
        val question = choiceQuestion(QuestionType.MULTIPLE_CHOICE, listOf(1L))

        val result = evaluatePreviewAnswer(question, setOf(1L), "", answered = true)

        assertEquals(true, result.isCorrect)
        assertEquals("Một", result.correctLabel)
    }

    @Test
    fun `multiple select requires exact set regardless of order`() {
        val question = choiceQuestion(QuestionType.MULTIPLE_SELECT, listOf(0L, 2L))

        assertEquals(
            true,
            evaluatePreviewAnswer(question, setOf(2L, 0L), "", answered = true).isCorrect
        )
        assertEquals(
            false,
            evaluatePreviewAnswer(question, setOf(0L), "", answered = true).isCorrect
        )
    }

    @Test
    fun `typed answer ignores surrounding whitespace and case`() {
        val question = textQuestion("Hà Nội")

        val result = evaluatePreviewAnswer(question, emptySet(), "  hà nội ", answered = true)

        assertEquals(true, result.isCorrect)
    }

    @Test
    fun `missing answer key is ungradable`() {
        val question = textQuestion("").copy(correctAnswer = null)

        val result = evaluatePreviewAnswer(question, emptySet(), "abc", answered = true)

        assertNull(result.isCorrect)
    }

    @Test
    fun `classic score includes speed bonus`() {
        assertEquals(1_250, computePreviewScore(true, 15.0, 30, false))
        assertEquals(0, computePreviewScore(false, 1.0, 30, false))
    }

    @Test
    fun `late correct answer follows backend base-only penalty`() {
        assertEquals(900, computePreviewScore(true, 45.0, 30, true))
    }

    private fun choiceQuestion(type: QuestionType, correct: List<Long>) = Question(
        id = 1,
        quizId = 1,
        questionType = type,
        questionText = "Chọn đáp án",
        timeLimit = 30,
        answerOptions = listOf(
            AnswerOption(0, "Không"),
            AnswerOption(1, "Một"),
            AnswerOption(2, "Hai")
        ),
        correctAnswer = JsonArray(correct.map(::JsonPrimitive))
    )

    private fun textQuestion(correct: String) = Question(
        id = 2,
        quizId = 1,
        questionType = QuestionType.SHORT_ANSWER,
        questionText = "Thủ đô?",
        timeLimit = 30,
        correctAnswer = JsonPrimitive(correct)
    )
}
