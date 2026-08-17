package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight quiz card dùng cho listing, home feed, và search results.
 * 
 * KHÔNG chứa questions array - chỉ có question_count summary.
 * Dùng cho endpoints: /quizzes/home, /quizzes/feed, /quizzes/search, /quizzes/me
 * 
 * Để lấy full quiz detail với questions, dùng model [Quiz] qua endpoint
 * GET /quizzes/id/:quizId
 */
@Serializable
data class QuizCard(
    val id: Long,
    
    @SerialName("quiz_name")
    val quizName: String,
    
    @SerialName("quiz_description")
    val quizDescription: String? = null,
    
    @SerialName("quiz_image")
    val quizImage: String? = null,
    
    @SerialName("quiz_category")
    val quizCategory: String? = null,
    
    @SerialName("quiz_language")
    val quizLanguage: String,
    
    /**
     * ID của quiz owner.
     */
    @SerialName("quiz_owner")
    val quizOwnerId: Long,
    
    /**
     * Nested owner object với fullname và avatar.
     * Null nếu owner bị soft-deleted.
     */
    val owner: QuizOwner? = null,
    
    /**
     * Số lượng câu hỏi (summary field, không load full questions).
     */
    @SerialName("question_count")
    val questionCount: Int,
    
    /**
     * Số lần quiz này được chơi.
     */
    @SerialName("play_count")
    val playCount: Int,
    
    /**
     * Tỷ lệ hoàn thành (0.0 - 1.0).
     * Ví dụ: 0.85 = 85% người chơi hoàn thành quiz.
     */
    @SerialName("completion_rate")
    val completionRate: Double,
    
    @SerialName("created_at")
    val createdAt: String
)
