package android.kma.myquizzapp.core.common.validator

import android.kma.myquizzapp.core.common.model.QuestionType

/** Pure input: no Android Uri or feature/presentation models cross this boundary. */
data class QuestionValidationInput(
    val localId: String,
    val type: QuestionType,
    val text: String,
    val timeLimit: String,
    val options: List<String>,
    val correctIndexes: Set<Int>,
    val correctText: String,
    val hint: String,
    val explanation: String,
)

data class QuizEditorErrors(
    val name: String? = null,
    val description: String? = null,
    val language: String? = null,
    val category: String? = null,
    val questions: Map<String, List<String>> = emptyMap(),
    val general: List<String> = emptyList(),
) {
    val messages: List<String>
        get() = listOfNotNull(name, description, language, category) + general + questions.values.flatten()
    val hasErrors: Boolean get() = messages.isNotEmpty()
}

/** Shared create/edit rules, validated after the same trimming used by the wire mapper. */
object QuizEditorValidator {
    fun validate(
        name: String,
        description: String,
        language: String,
        category: String,
        questions: List<QuestionValidationInput>,
    ): QuizEditorErrors = QuizEditorErrors(
        name = (QuizNameValidator.validate(name.trim()) as? ValidationResult.Error)?.message,
        description = (QuizDescriptionValidator.validate(description.trim()) as? ValidationResult.Error)?.message,
        language = if (language.isBlank()) "Ngôn ngữ không được để trống." else null,
        category = if (category.trim().length > 50) "Chủ đề tối đa 50 ký tự." else null,
        general = if (questions.isEmpty()) listOf("Quiz phải có ít nhất 1 câu hỏi.") else emptyList(),
        questions = buildMap {
            questions.forEachIndexed { index, question ->
                val errors = validateQuestion(question).map { "Câu ${index + 1}: $it" }
                if (errors.isNotEmpty()) put(question.localId, errors)
            }
        },
    )

    private fun validateQuestion(q: QuestionValidationInput): List<String> = buildList {
        val text = q.text.trim()
        if (text.isEmpty()) add("thiếu nội dung câu hỏi.")
        else if (text.length > 200) add("nội dung tối đa 200 ký tự.")
        val seconds = q.timeLimit.trim().toIntOrNull()
        if (seconds == null || seconds !in 5..600) add("thời gian phải từ 5 đến 600 giây.")
        if (q.hint.trim().length > 255) add("gợi ý tối đa 255 ký tự.")
        if (q.explanation.trim().length > 255) add("giải thích tối đa 255 ký tự.")
        val choice = q.type == QuestionType.MULTIPLE_CHOICE || q.type == QuestionType.MULTIPLE_SELECT
        if (choice) {
            if (q.options.size !in 2..4) add("cần từ 2 đến 4 lựa chọn.")
            q.options.forEachIndexed { index, option ->
                if (option.isBlank()) add("đáp án ${'A' + index} không được để trống.")
                else if (option.trim().length > 100) add("đáp án ${'A' + index} tối đa 100 ký tự.")
            }
            if (q.correctIndexes.isEmpty()) add("chọn ít nhất 1 đáp án đúng.")
            if (q.type == QuestionType.MULTIPLE_CHOICE && q.correctIndexes.size > 1) {
                add("câu một lựa chọn chỉ có 1 đáp án đúng.")
            }
            if (q.correctIndexes.any { it !in q.options.indices || q.options[it].isBlank() }) {
                add("đáp án đúng phải trỏ tới một lựa chọn có nội dung.")
            }
        } else if (q.correctText.isBlank()) add("thiếu đáp án đúng.")
    }
}
