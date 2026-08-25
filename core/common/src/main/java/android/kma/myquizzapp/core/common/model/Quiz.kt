package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Full quiz detail với questions array.
 * 
 * Dùng cho endpoint: GET /quizzes/id/:quizId
 * Để lấy lightweight listing, dùng [QuizCard] thay thế.
 */
@Serializable
data class Quiz(
    val id: Long,
    
    @SerialName("quiz_owner")
    val quizOwner: Long,
    
    /**
     * Nested owner object với fullname và avatar.
     * Null nếu owner bị soft-deleted.
     * Optional vì một số responses không join owner (e.g., RETURNING * sau create).
     */
    val owner: QuizOwner? = null,
    
    @SerialName("quiz_name")
    val quizName: String,
    
    @SerialName("quiz_description")
    val quizDescription: String? = null,
    
    @SerialName("quiz_language")
    val quizLanguage: String,
    
    @SerialName("quiz_image")
    val quizImage: String? = null,
    
    @SerialName("quiz_category")
    val quizCategory: String? = null,
    
    @SerialName("is_public")
    val isPublic: Boolean,
    
    /**
     * Optional counters - absent on rows từ RETURNING * (create/update).
     */
    @SerialName("question_count")
    val questionCount: Int? = null,
    
    @SerialName("play_count")
    val playCount: Int? = null,
    
    /**
     * Soft delete timestamp. Null nếu quiz active.
     */
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("updated_at")
    val updatedAt: String,
    
    /**
     * Full questions array với answer options và correct answers.
     * Empty list nếu quiz chưa có câu hỏi.
     */
    val questions: List<Question> = emptyList()
)

@Serializable
data class Question(
    val id: Long,
    val quizId: Long,
    val questionType: QuestionType,
    val questionText: String,
    val timeLimit: Int,                 // giây; server default 30
    val questionHint: String? = null,   // chỉ hiện khi config.flow.showHint = true
    val explanation: String? = null,    // dùng cho review mode (N29)
    val questionImage: String? = null,
    val answerOptions: List<AnswerOption>? = null,  // null với short/long answer

    /**
     * Đáp án đúng — CHỈ dùng cho luồng chủ quiz chỉnh sửa (N16): GET quiz detail
     * trả field này (backend getQuizById select cả correct_answer) để pre-fill editor.
     * Gameplay không dùng model này — câu hỏi lúc chơi đi qua socket payload đã bị
     * server cắt đáp án. JsonElement vì wire là union: number[] (choice) | string (text).
     */
    @SerialName("correct_answer")
    val correctAnswer: JsonElement? = null
)

@Serializable
data class AnswerOption(
    val id: Long,
    val optionText: String
)

@Serializable
enum class QuestionType {
    @SerialName("multiple_choice") MULTIPLE_CHOICE,   // chọn 1
    @SerialName("multiple_select") MULTIPLE_SELECT,   // chọn nhiều — correct_answer là number[]
    @SerialName("short_answer") SHORT_ANSWER,         // nhập text — correct_answer là string
    @SerialName("long_answer") LONG_ANSWER
}