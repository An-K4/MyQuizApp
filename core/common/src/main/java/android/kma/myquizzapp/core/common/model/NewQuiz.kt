package android.kma.myquizzapp.core.common.model

/**
 * Domain model cho câu trả lời đúng khi tạo/sửa câu hỏi.
 *
 * Khớp `correct_answer` trong createQuestionSchema (quiz.schema.ts) — zod union:
 * - number[] (>=1 phần tử, index vào answerOptions) cho multiple_choice/multiple_select
 * - string cho short_answer/long_answer
 *
 * Domain model [Question] (Quiz.kt) không dùng kiểu [CorrectAnswer] vì gameplay không
 * được thấy đáp án (anti-cheat). Từ N16, Question có thêm correctAnswer dạng JsonElement
 * thô — chỉ để pre-fill màn Sửa quiz của chủ quiz, không dùng khi chơi.
 */
sealed interface CorrectAnswer {
    data class Indexes(val values: List<Int>) : CorrectAnswer
    data class Text(val value: String) : CorrectAnswer
}

/**
 * Một câu hỏi khi tạo quiz mới — khớp createQuestionSchema (quiz.schema.ts).
 * answerOptions chỉ dùng cho multiple_choice/multiple_select (2-4 lựa chọn).
 */
data class NewQuestion(
    val questionType: QuestionType,
    val questionText: String,
    val timeLimit: Int = 30,
    val questionImage: String? = null,
    val questionHint: String? = null,
    val explanation: String? = null,
    val answerOptions: List<String>? = null,
    val correctAnswer: CorrectAnswer
)

/**
 * Quiz mới khi tạo — khớp createQuizSchema (quiz.schema.ts).
 * Cần ít nhất 1 câu hỏi (backend validate lại lần nữa, đây chỉ là validate phía client).
 */
data class NewQuiz(
    val quizName: String,
    val quizDescription: String? = null,
    val quizLanguage: String,
    val quizImage: String? = null,
    val quizCategory: String? = null,
    val isPublic: Boolean,
    val questions: List<NewQuestion>
)
