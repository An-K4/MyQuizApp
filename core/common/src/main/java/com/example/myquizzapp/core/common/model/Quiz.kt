package com.example.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val id: Long,
    val quizOwner: Long,
    val quizName: String,
    val quizDescription: String? = null,
    val quizLanguage: String,
    val quizImage: String? = null,
    val quizCategory: String? = null,
    val isPublic: Boolean,
    val createdAt: String,
    val updatedAt: String,
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
    val answerOptions: List<AnswerOption>? = null  // null với short/long answer
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