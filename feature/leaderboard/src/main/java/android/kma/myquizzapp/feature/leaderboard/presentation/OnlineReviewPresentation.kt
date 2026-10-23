package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import java.text.NumberFormat
import java.util.Locale

internal enum class OnlineReviewTone(val label: String) {
    Correct("Đúng"), Incorrect("Sai"), Skipped("Bỏ qua"), Neutral("Chưa có dữ liệu"),
}

internal data class OnlineReviewOptionUi(
    val label: String,
    val text: String,
    val selected: Boolean,
    val tone: OnlineReviewTone,
) {
    // readOnly answer cards deliberately have no selection/radio semantics.
    // Keep the exact submitted set visible in words, including a partially correct set.
    val displayText: String get() = if (selected) "$text\nBạn đã chọn" else text
}

internal data class OnlineReviewResponseUi(val label: String, val text: String, val tone: OnlineReviewTone)

internal data class OnlineReviewCardUi(
    val heading: String,
    val questionText: String,
    val tone: OnlineReviewTone,
    val options: List<OnlineReviewOptionUi>,
    val responses: List<OnlineReviewResponseUi>,
    val notices: List<String>,
    val explanation: String?,
    val scoreText: String?,
    val timeText: String?,
    val isLate: Boolean,
)

/** Presentation only: no question-type inference, local grading or invented answer content. */
internal fun GameReviewItem.toOnlineReviewCardUi(mode: GameMode?): OnlineReviewCardUi {
    val tone = when {
        !answered -> OnlineReviewTone.Skipped
        isCorrect -> OnlineReviewTone.Correct
        else -> OnlineReviewTone.Incorrect
    }
    val correctKeys = correctAnswers.map(String::reviewKey).filter(String::isNotEmpty).toSet()
    val selectedKeys = if (answered) yourAnswers.map(String::reviewKey).filter(String::isNotEmpty).toSet()
        else emptySet()
    val optionKeys = answerOptions.map { it.id.reviewKey() }.filter(String::isNotEmpty).toSet()
    val options = answerOptions.mapIndexed { index, option ->
        val key = option.id.reviewKey()
        val selected = key.isNotEmpty() && key in selectedKeys
        OnlineReviewOptionUi(
            label = if (index < 26) ('A'.code + index).toChar().toString() else "${index + 1}",
            text = option.text?.takeIf(String::isNotBlank)
                ?: option.id.takeIf(String::isNotBlank)?.let { "Đáp án $it" }
                ?: "Nội dung đáp án không còn khả dụng",
            selected = selected,
            tone = when {
                key.isNotEmpty() && key in correctKeys -> OnlineReviewTone.Correct
                selected && correctKeys.isNotEmpty() -> OnlineReviewTone.Incorrect
                else -> OnlineReviewTone.Neutral
            },
        )
    }
    val responses = if (options.isEmpty()) buildList {
        add(OnlineReviewResponseUi(
            label = "Câu trả lời của bạn",
            text = if (!answered) "Chưa trả lời"
                else yourAnswers.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.joinToString("\n")
                    ?: "Không có dữ liệu câu trả lời",
            tone = if (answered && yourAnswers.any(String::isNotBlank)) tone else OnlineReviewTone.Neutral,
        ))
        correctAnswers.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let {
            add(OnlineReviewResponseUi("Đáp án đúng", it.joinToString("\n"), OnlineReviewTone.Correct))
        }
    } else emptyList()
    val notices = buildList {
        if (correctKeys.isEmpty()) add("Chưa có dữ liệu đáp án đúng để đối chiếu.")
        if (options.isNotEmpty()) {
            val unmatchedSelected = if (answered) yourAnswers.filter {
                it.isNotBlank() && it.reviewKey() !in optionKeys
            } else emptyList()
            if (unmatchedSelected.isNotEmpty()) {
                add("Lựa chọn đã gửi không còn trong danh sách: ${unmatchedSelected.joinToString(", ")}")
            } else if (answered && selectedKeys.isEmpty()) {
                add("Không có dữ liệu lựa chọn đã gửi.")
            }
            val unmatchedCorrect = correctAnswers.filter { it.isNotBlank() && it.reviewKey() !in optionKeys }
            if (unmatchedCorrect.isNotEmpty()) {
                add("Đáp án đúng không còn trong danh sách: ${unmatchedCorrect.joinToString(", ")}")
            }
        }
    }
    val locale = Locale.forLanguageTag("vi-VN")
    return OnlineReviewCardUi(
        heading = "Câu ${questionIndex + 1}",
        questionText = questionText?.takeIf(String::isNotBlank) ?: "Nội dung câu hỏi không còn khả dụng",
        tone = tone,
        options = options,
        responses = responses,
        notices = notices,
        explanation = explanation?.takeIf(String::isNotBlank),
        scoreText = if (mode == GameMode.PRACTICE) null
            else "${NumberFormat.getIntegerInstance(locale).format(scoreEarned)} điểm",
        timeText = timeTakenSeconds?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            "${String.format(locale, "%.2f", it)} giây"
        },
        isLate = isLate,
    )
}

internal fun onlineReviewSummary(review: GameReview, mode: GameMode?): String = buildList {
    if (review.correctAnswersCount >= 0) {
        // Marathon repeats its question bank: never present attempts as x/bank-size.
        val finiteTotal = mode != GameMode.MARATHON && review.totalQuestions > 0 &&
            review.correctAnswersCount <= review.totalQuestions
        add(if (finiteTotal) "${review.correctAnswersCount}/${review.totalQuestions} câu đúng"
            else "${review.correctAnswersCount} câu đúng")
    }
    if (review.answeredCount >= 0) add("${review.answeredCount} lượt trả lời")
}.joinToString(" • ").ifBlank { "Chưa có dữ liệu tổng kết câu trả lời." }

private fun String.reviewKey(): String = trim().lowercase(Locale.ROOT)
