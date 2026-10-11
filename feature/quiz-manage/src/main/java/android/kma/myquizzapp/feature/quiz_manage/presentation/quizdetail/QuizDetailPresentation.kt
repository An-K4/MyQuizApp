package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.Quiz
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Deliberately excludes correctAnswer, hints and explanations from the read-only card API. */
internal data class QuestionPreviewUiModel(
    val id: Long,
    val text: String,
    val type: QuestionType,
    val timeLimitSeconds: Int,
    val imageUrl: String?,
    val optionTexts: List<String>,
)

internal fun Question.toQuestionPreviewUiModel() = QuestionPreviewUiModel(
    id = id,
    text = questionText,
    type = questionType,
    timeLimitSeconds = timeLimit,
    imageUrl = questionImage,
    optionTexts = answerOptions.orEmpty().map { it.optionText },
)

/** No questions or mixed durations means NO shared-duration cell, not a placeholder cell. */
internal fun Quiz.sharedQuestionTimeSeconds(): Int? = questions.firstOrNull()?.timeLimit
    ?.takeIf { time -> questions.all { it.timeLimit == time } }

internal fun Quiz.displayQuestionCount(): Int = questionCount ?: questions.size

internal fun Quiz.displayAuthorName(): String = owner?.fullname?.trim()?.takeIf { it.isNotEmpty() }
    ?: "Tác giả không còn khả dụng"

/** Absolute date from server data; never invent a relative date or show a raw timestamp. */
internal fun formatQuizCreatedDate(createdAt: String, zoneId: ZoneId = ZoneId.systemDefault()): String? =
    runCatching {
        Instant.parse(createdAt).atZone(zoneId).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrNull()
