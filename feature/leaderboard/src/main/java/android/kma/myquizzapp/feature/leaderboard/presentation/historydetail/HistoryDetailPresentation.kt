package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.SessionStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class HistoryAnswerTone { Neutral, Correct, Incorrect }

internal data class HistoryAnswerUi(
    val text: String,
    val tone: HistoryAnswerTone,
    val optionLabel: String? = null,
    val caption: String? = null
)

internal data class HistoryReviewUi(
    val tone: HistoryAnswerTone,
    val statusText: String,
    val answers: List<HistoryAnswerUi>,
    val notice: String? = null
)

/** Presentation only: the server's answered/isCorrect flags own the overall result. */
internal fun GameReviewItem.toHistoryReviewUi(): HistoryReviewUi {
    val tone = when {
        !answered -> HistoryAnswerTone.Neutral
        isCorrect -> HistoryAnswerTone.Correct
        else -> HistoryAnswerTone.Incorrect
    }
    val status = when (tone) {
        HistoryAnswerTone.Neutral -> "Chưa trả lời"
        HistoryAnswerTone.Correct -> "Bạn trả lời đúng"
        HistoryAnswerTone.Incorrect -> "Bạn trả lời sai"
    }
    val score = (if (scoreEarned > 0) "+" else "") + formatHistoryNumber(scoreEarned)
    var notice: String? = null
    val rows = if (answerOptions.isNotEmpty()) {
        val mine = if (answered) yourAnswers.map { it.optionKey() }.toSet() else emptySet()
        val correct = correctAnswers.map { it.optionKey() }.toSet()
        val known = answerOptions.map { it.id.optionKey() }.toSet()
        if (correct.isEmpty()) notice = "Đáp án đối chiếu chưa khả dụng."
        if ((mine + correct).any { it !in known }) {
            notice = "Một phần nội dung lựa chọn không còn khả dụng."
        }
        // Keep snapshot order/labels. A correct choice stays green even in an incomplete set.
        answerOptions.mapIndexedNotNull { index, option ->
            val key = option.id.optionKey()
            val selected = key in mine
            val isAnswer = key in correct
            if (!isAnswer && !selected) return@mapIndexedNotNull null
            HistoryAnswerUi(
                text = option.text?.takeIf(String::isNotBlank) ?: "Nội dung đáp án không còn khả dụng",
                tone = when {
                    isAnswer -> HistoryAnswerTone.Correct
                    correct.isEmpty() -> HistoryAnswerTone.Neutral
                    else -> HistoryAnswerTone.Incorrect
                },
                optionLabel = if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString(),
                caption = if (selected) "Bạn đã chọn" else "Đáp án đúng"
            )
        }
    } else {
        val mine = yourAnswers.filter(String::isNotBlank).joinToString("\n")
        val correct = correctAnswers.filter(String::isNotBlank).joinToString("\n")
        if (correct.isBlank() && (!answered || !isCorrect)) {
            notice = "Đáp án đối chiếu chưa khả dụng."
        }
        buildList {
            if (answered && isCorrect) {
                when {
                    mine.isNotBlank() -> add(HistoryAnswerUi(mine, HistoryAnswerTone.Correct, caption = "Bạn trả lời"))
                    correct.isNotBlank() -> add(HistoryAnswerUi(correct, HistoryAnswerTone.Correct, caption = "Đáp án đúng"))
                    else -> notice = "Nội dung câu trả lời không còn khả dụng."
                }
            } else {
                if (answered) {
                    add(HistoryAnswerUi(
                        text = mine.ifBlank { "Không có nội dung trả lời" },
                        tone = HistoryAnswerTone.Incorrect,
                        caption = "Bạn trả lời"
                    ))
                }
                if (correct.isNotBlank()) {
                    add(HistoryAnswerUi(correct, HistoryAnswerTone.Correct, caption = "Đáp án đúng"))
                }
            }
        }
    }
    return HistoryReviewUi(tone, "$status (${score}đ)", rows, notice)
}

internal data class HistoryAccuracyUi(val text: String, val progress: Float?)

/** The denominator is submitted answers, not room attendance. No fabricated 0% for no data. */
internal fun historyAccuracy(correct: Int, answers: Int): HistoryAccuracyUi {
    if (answers == 0 && correct == 0) return HistoryAccuracyUi("Chưa có lượt trả lời", null)
    if (answers <= 0 || correct < 0 || correct > answers) {
        return HistoryAccuracyUi("Thống kê chưa khả dụng", null)
    }
    val percent = correct.toLong() * 100 / answers
    return HistoryAccuracyUi("$correct/$answers trả lời đúng – $percent%", correct.toFloat() / answers)
}

internal fun GameHistorySummary.historyRoomLabel(): String? =
    sessionCode?.trim()?.takeIf(String::isNotEmpty)?.let { "Mã phòng: $it" }
        ?: sessionName.trim().takeIf(String::isNotEmpty)?.let { "Phòng: $it" }

internal fun SessionStatus.historyStatusLabel(): String? = if (this == SessionStatus.CANCELLED) "Đã hủy" else null

internal fun GameMode.historyModeLabel(): String = when (this) {
    GameMode.CLASSIC -> "Cổ điển"
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
}

internal fun formatHistoryNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

internal fun formatHistoryTime(raw: String?): String? = raw?.takeIf(String::isNotBlank)?.let {
    runCatching {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Instant.parse(it).atZone(ZoneId.systemDefault()))
    }.getOrDefault(it)
}

internal fun historyPlayerInitials(name: String): String = name.trim().split(Regex("\\s+"))
    .filter(String::isNotEmpty).take(2).map { it.first() }.joinToString("").uppercase(Locale.getDefault())

private fun String.optionKey(): String = trim().lowercase(Locale.ROOT)
