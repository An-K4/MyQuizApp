package com.example.myquizzapp.core.common.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizSummary(
    val id: Long,
    val quizOwner: Long,
    val owner: QuizOwner? = null,        // null khi tác giả bị soft-delete
    val quizName: String,
    val quizDescription: String? = null,
    val quizImage: String? = null,
    val quizCategory: String? = null,
    val quizLanguage: String,
    val isPublic: Boolean,
    val questionCount: Int,
    val playCount: Int,
    val completionRate: Double,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class QuizOwner(
    val id: Long,
    val fullname: String,
    val avatar: String? = null
)