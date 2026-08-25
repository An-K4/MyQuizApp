package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.CorrectAnswer
import android.kma.myquizzapp.core.common.model.NewQuestion
import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.QuizPatch
import android.kma.myquizzapp.core.common.model.QuizSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Wrapper cho response của GET /quizzes/me.
 *
 * Backend (listing.controller.ts) trả `success(res, { quizzes: page.items }, ...)`
 * giống QuizListDto, nhưng /quizzes/me trả đầy đủ is_public/updated_at (QuizSummary
 * ở listing.type.ts) vì chỉ owner mới xem được trạng thái public/private + thời gian
 * sửa cuối của chính họ — QuizCardDto (home/search) không có 2 field này.
 */
@Serializable
data class QuizSummaryListDto(val quizzes: List<QuizSummaryDto>)

@Serializable
data class QuizSummaryDto(
    val id: Long,

    @SerialName("quiz_owner")
    val quizOwner: Long,

    val owner: QuizOwnerDto? = null,

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

    @SerialName("is_public")
    val isPublic: Boolean,

    @SerialName("question_count")
    val questionCount: Int,

    @SerialName("play_count")
    val playCount: Int,

    @SerialName("completion_rate")
    val completionRate: Double,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)

fun QuizSummaryDto.toDomain(): QuizSummary = QuizSummary(
    id = id,
    quizOwner = quizOwner,
    owner = owner?.toDomain(),
    quizName = quizName,
    quizDescription = quizDescription,
    quizImage = quizImage,
    quizCategory = quizCategory,
    quizLanguage = quizLanguage,
    isPublic = isPublic,
    questionCount = questionCount,
    playCount = playCount,
    completionRate = completionRate,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ===== POST /quizzes request body — khớp createQuizSchema (quiz.schema.ts) =====

@Serializable
data class CreateQuizRequestDto(
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

    val questions: List<CreateQuestionRequestDto>
)

@Serializable
data class CreateQuestionRequestDto(
    @SerialName("question_type")
    val questionType: QuestionType,

    @SerialName("question_text")
    val questionText: String,

    @SerialName("time_limit")
    val timeLimit: Int,

    @SerialName("question_image")
    val questionImage: String? = null,

    @SerialName("question_hint")
    val questionHint: String? = null,

    val explanation: String? = null,

    @SerialName("answer_options")
    val answerOptions: List<String>? = null,

    // correct_answer: number[] (multiple_choice/multiple_select) | string (short/long answer)
    // — zod union ở backend. Dùng JsonElement để tự serialize đúng hình dạng theo loại câu hỏi
    // thay vì cần 2 DTO riêng.
    @SerialName("correct_answer")
    val correctAnswer: JsonElement
)

fun NewQuiz.toRequestDto(): CreateQuizRequestDto = CreateQuizRequestDto(
    quizName = quizName,
    quizDescription = quizDescription,
    quizLanguage = quizLanguage,
    quizImage = quizImage,
    quizCategory = quizCategory,
    isPublic = isPublic,
    questions = questions.map { it.toRequestDto() }
)

fun NewQuestion.toRequestDto(): CreateQuestionRequestDto = CreateQuestionRequestDto(
    questionType = questionType,
    questionText = questionText,
    timeLimit = timeLimit,
    questionImage = questionImage,
    questionHint = questionHint,
    explanation = explanation,
    answerOptions = answerOptions,
    correctAnswer = when (val ca = correctAnswer) {
        is CorrectAnswer.Indexes -> JsonArray(ca.values.map { JsonPrimitive(it) })
        is CorrectAnswer.Text -> JsonPrimitive(ca.value)
    }
)

// ===== PATCH /quizzes/id/:quizId request body (N16) — khớp updateQuizSchema =====
// (= createQuizSchema.partial(): mọi field optional; field null bị omit khỏi JSON
// nhờ explicitNulls=false trong NetworkModule, backend giữ nguyên giá trị cũ.)

@Serializable
data class UpdateQuizRequestDto(
    @SerialName("quiz_name")
    val quizName: String? = null,

    @SerialName("quiz_description")
    val quizDescription: String? = null,

    @SerialName("quiz_language")
    val quizLanguage: String? = null,

    @SerialName("quiz_image")
    val quizImage: String? = null,

    @SerialName("quiz_category")
    val quizCategory: String? = null,

    @SerialName("is_public")
    val isPublic: Boolean? = null,

    // Gửi = thay thế toàn bộ câu hỏi (replaceQuizQuestions); null = không đụng tới.
    val questions: List<CreateQuestionRequestDto>? = null
)

fun QuizPatch.toRequestDto(): UpdateQuizRequestDto = UpdateQuizRequestDto(
    quizName = quizName,
    quizDescription = quizDescription,
    quizLanguage = quizLanguage,
    quizImage = quizImage,
    quizCategory = quizCategory,
    isPublic = isPublic,
    questions = questions?.map { it.toRequestDto() }
)
