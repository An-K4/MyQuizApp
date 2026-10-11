package android.kma.myquizzapp.core.common.validator

import android.kma.myquizzapp.core.common.model.QuestionType
import org.junit.Assert.*
import org.junit.Test

class QuizEditorValidatorTest {
    private val question = QuestionValidationInput("q1", QuestionType.MULTIPLE_CHOICE,
        "Câu hỏi", "30", listOf(" A ", "B"), setOf(0), "", "", "")
    private fun validate(q: QuestionValidationInput = question, name: String = "Quiz mẫu",
        description: String = "", language: String = "vi", category: String = "") =
        QuizEditorValidator.validate(name, description, language, category, listOf(q))

    @Test fun `valid input and free language category are accepted`() {
        assertFalse(validate(language = "Tiếng Việt", category = "Chủ đề tự nhập").hasErrors)
    }
    @Test fun `name is validated after trim`() { assertNotNull(validate(name = " a ").name) }
    @Test fun `blank language is rejected`() { assertNotNull(validate(language = "  ").language) }
    @Test fun `category boundaries are enforced after trim`() {
        assertNull(validate(category = " " + "x".repeat(50) + " ").category)
        assertNotNull(validate(category = "x".repeat(51)).category)
    }
    @Test fun `description accepts 500 but rejects 501`() {
        assertNull(validate(description = "x".repeat(500)).description)
        assertNotNull(validate(description = "x".repeat(501)).description)
    }
    @Test fun `quiz needs at least one question`() {
        assertTrue(QuizEditorValidator.validate("Quiz mẫu", "", "vi", "", emptyList()).general.isNotEmpty())
    }
    @Test fun `question text has a 200 character limit`() {
        assertFalse(validate(question.copy(text = "x".repeat(200))).hasErrors)
        assertTrue(validate(question.copy(text = "x".repeat(201))).hasErrors)
        assertTrue(validate(question.copy(text = " ")).hasErrors)
    }
    @Test fun `time accepts inclusive bounds and rejects empty zero out of range or non integer`() {
        listOf("5", "600").forEach { assertFalse(validate(question.copy(timeLimit = it)).hasErrors) }
        listOf("", "0", "4", "601", "1.5", "abc").forEach { assertTrue(validate(question.copy(timeLimit = it)).hasErrors) }
    }
    @Test fun `all visible options must be filled to keep correct indexes stable`() {
        assertTrue(validate(question.copy(options = listOf("A", "", "B"), correctIndexes = setOf(2))).hasErrors)
    }
    @Test fun `options need two to four rows`() {
        assertTrue(validate(question.copy(options = listOf("A"))).hasErrors)
        assertFalse(validate(question.copy(options = listOf("A", "B", "C", "D"))).hasErrors)
        assertTrue(validate(question.copy(options = listOf("A", "B", "C", "D", "E"))).hasErrors)
    }
    @Test fun `option has a 100 character limit`() {
        assertFalse(validate(question.copy(options = listOf("x".repeat(100), "B"))).hasErrors)
        assertTrue(validate(question.copy(options = listOf("x".repeat(101), "B"))).hasErrors)
    }
    @Test fun `correct indexes cannot be empty negative or outside the options`() {
        listOf(emptySet(), setOf(-1), setOf(2)).forEach { assertTrue(validate(question.copy(correctIndexes = it)).hasErrors) }
    }
    @Test fun `single allows one correct answer while multiple allows more`() {
        assertTrue(validate(question.copy(correctIndexes = setOf(0, 1))).hasErrors)
        assertFalse(validate(question.copy(type = QuestionType.MULTIPLE_SELECT, correctIndexes = setOf(0, 1))).hasErrors)
    }
    @Test fun `short and long answer require nonblank text but ignore choice state`() {
        listOf(QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER).forEach { type ->
            assertTrue(validate(question.copy(type = type, correctText = " ")).hasErrors)
            assertFalse(validate(question.copy(type = type, correctText = " Đáp án ", options = emptyList())).hasErrors)
        }
    }
    @Test fun `hint and explanation have 255 character limits`() {
        assertFalse(validate(question.copy(hint = "x".repeat(255), explanation = "x".repeat(255))).hasErrors)
        assertTrue(validate(question.copy(hint = "x".repeat(256))).hasErrors)
        assertTrue(validate(question.copy(explanation = "x".repeat(256))).hasErrors)
    }
    @Test fun `question errors retain local id and visible number`() {
        val errors = validate(question.copy(text = ""))
        assertTrue(errors.questions.containsKey("q1"))
        assertTrue(errors.questions.getValue("q1").first().startsWith("Câu 1:"))
    }
}
