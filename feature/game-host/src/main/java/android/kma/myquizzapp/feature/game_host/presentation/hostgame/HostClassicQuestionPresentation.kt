package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.ShowLeaderboard

internal data class HostQuestionProgress(val answered: Int, val total: Int) {
    val pending: Int get() = total - answered
    val fraction: Float get() = answered.toFloat() / total
    val percent: Int get() = (answered * 100.0 / total).toInt()
}

internal fun hostQuestionProgress(state: HostGameUiState): HostQuestionProgress? {
    if (!state.hasAnswerProgress || !state.isStateSynchronized ||
        state.connection != HostGameConnection.CONNECTED || state.activePlayers <= 0 ||
        state.answeredCount !in 0..state.activePlayers
    ) return null
    return HostQuestionProgress(state.answeredCount, state.activePlayers)
}

internal fun hostCurrentQuestion(state: HostGameUiState): PublicQuestion? =
    state.question?.takeIf { it.index == state.index }

internal fun hostCurrentResults(state: HostGameUiState): QuestionResults? {
    if (!state.isShowingQuestionResults) return null
    val question = hostCurrentQuestion(state)
    return state.results?.takeIf { it.index == state.index &&
        (it.questionId == null || it.questionId == question?.id) }
}

internal fun hostIsChoiceQuestion(state: HostGameUiState): Boolean {
    val question = hostCurrentQuestion(state) ?: return false
    return question.questionType !in setOf("short_answer", "long_answer") && question.answerOptions.isNotEmpty()
}

internal fun hostShowsResultsLeaderboard(state: HostGameUiState): Boolean =
    state.isClassic && state.isShowingQuestionResults && state.showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS

internal fun hostOptionIsCorrect(state: HostGameUiState, optionId: String): Boolean =
    state.hasAnswerKey && state.isAnswerRevealed && hostCurrentQuestion(state)?.answerOptions?.any { it.id == optionId } == true &&
        optionId in state.correctAnswers

/** A column represents one option's selections, not a mutually exclusive share of people. */
internal data class HostResultColumn(
    val optionId: String, val label: String, val text: String, val selections: Int, val isCorrect: Boolean,
)

/** Null means unavailable/withheld, never an all-zero distribution fabricated from a missing payload. */
internal fun hostResultColumns(state: HostGameUiState): List<HostResultColumn>? {
    if (state.showCorrectAnswer != true || !hostIsChoiceQuestion(state)) return null
    val question = hostCurrentQuestion(state) ?: return null
    val stats = hostCurrentResults(state)?.stats ?: return null
    val optionIds = question.answerOptions.map { it.id }.toSet()
    if (optionIds.size != question.answerOptions.size || stats.total < 0 ||
        stats.distribution.values.any { it < 0 || it > stats.total }) return null
    if (stats.total > 0 && stats.distribution.isEmpty()) return null
    val multiple = question.questionType == "multiple_select"
    // String(array) in the current backend loses boundaries for IDs containing commas.
    // Numeric/ordinary IDs are recoverable; ambiguous IDs must not produce guessed columns.
    if (multiple && optionIds.any { ',' in it || it.isEmpty() }) return null
    val counts = optionIds.associateWith { 0L }.toMutableMap()
    for ((bucket, count) in stats.distribution) {
        val selected = when {
            bucket in optionIds -> listOf(bucket)
            bucket == "null" -> emptyList() // Cached timeout answer, not an option.
            multiple && bucket.isEmpty() -> emptyList() // String([]).
            multiple -> bucket.split(',').takeIf { ids -> ids.all { it in optionIds } } ?: return null
            else -> return null
        }
        // One submission contributes at most once to an option, even if its array repeats an ID.
        for (id in selected.distinct()) counts[id] = counts.getValue(id) + count.toLong()
    }
    if (counts.values.any { it > stats.total }) return null
    return question.answerOptions.mapIndexed { index, option ->
        HostResultColumn(option.id, hostOptionLetter(index), option.text ?: option.id,
            counts.getValue(option.id).toInt(), hostOptionIsCorrect(state, option.id))
    }
}

internal fun hostDistributionUnavailableLabel(state: HostGameUiState): String = when {
    state.showCorrectAnswer == false -> "Không có dữ liệu phân bố theo cấu hình phòng."
    state.showCorrectAnswer == null -> "Đang đồng bộ cấu hình hiển thị kết quả…"
    hostCurrentResults(state) == null -> "Chưa nhận được thống kê của câu này. Không suy đoán từ bảng tích lũy."
    else -> "Chưa đủ dữ liệu phân bố để vẽ biểu đồ."
}

internal data class HostQuestionTime(val seconds: Int? = null, val label: String)

/** Display only: reaching zero never changes the server-owned phase. */
internal fun hostQuestionTime(state: HostGameUiState, nowMillis: Long): HostQuestionTime {
    if (state.connection != HostGameConnection.CONNECTED || !state.isStateSynchronized)
        return HostQuestionTime(label = "Chờ đồng bộ thời gian")
    if (state.isPaused) return HostQuestionTime(label = "Thời gian đang tạm dừng")
    if (state.isShowingQuestionResults) {
        if (!state.autoAdvance) return HostQuestionTime(label = "Chờ Host chuyển câu")
        // Results events seed this deadline; snapshots replace it after pause/resume.
        // Do not fall back to the pre-pause nextQuestionAt cached in the result payload.
        val target = state.deadlineEpochMs
            ?: return HostQuestionTime(label = "Đang chờ máy chủ chuyển câu…")
        return countdownTo(target, state.serverOffsetMs, nowMillis, "Câu tiếp theo sẽ bắt đầu", "Đang chờ máy chủ chuyển câu…")
    }
    if (state.phase == GamePhase.QUESTION_LOCKED) return HostQuestionTime(label = "Câu hỏi đã đóng")
    if (!state.isQuestionClockKnown) return HostQuestionTime(label = "Chờ đồng bộ thời gian")
    val deadline = state.deadlineEpochMs ?: return HostQuestionTime(label = "Không giới hạn thời gian")
    return countdownTo(deadline, state.serverOffsetMs, nowMillis, "Thời gian còn lại", "Đang chờ máy chủ đóng câu…")
}

private fun countdownTo(target: Long, offset: Long, now: Long, label: String, zeroLabel: String): HostQuestionTime {
    val left = (target - (now + offset)).coerceAtLeast(0L)
    return HostQuestionTime(((left + 999L) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        if (left == 0L) zeroLabel else label)
}

internal fun hostQuestionStatus(state: HostGameUiState): String = when {
    state.connection == HostGameConnection.RECONNECT_FAILED -> "Chưa kết nối lại được. Các lệnh điều khiển đang khóa."
    state.connection != HostGameConnection.CONNECTED || !state.isStateSynchronized ->
        "Đang kết nối và đồng bộ trạng thái trận…"
    state.isSendingCommand -> "Đã gửi lệnh. Đang chờ máy chủ xác nhận…"
    state.isPaused -> "Trận đang tạm dừng. Bấm Tiếp tục để chơi tiếp."
    state.isShowingQuestionResults -> if (state.autoAdvance) "Đang hiện kết quả. Chờ máy chủ chuyển câu…"
        else "Đang hiện kết quả. Bấm Câu tiếp theo khi bạn sẵn sàng."
    state.phase == GamePhase.QUESTION_LOCKED -> when (state.lockReason) {
        QuestionLockReason.TIME_UP -> "Đã hết giờ. Đang chờ kết quả từ máy chủ…"
        // The same backend reason is also used by a manual close.
        else -> "Câu hỏi đã đóng. Đang chờ kết quả từ máy chủ…"
    }
    state.autoAdvance -> "Kết quả sẽ hiện khi hết giờ hoặc mọi người đã trả lời. Trận sẽ tự chuyển câu."
    else -> "Câu tự đóng khi hết giờ hoặc mọi người đã trả lời. Bạn có thể chốt sớm."
}

internal fun hostQuestionKey(state: HostGameUiState): String = "${hostCurrentQuestion(state)?.id}:${state.index}"

/** Letters are display-only. Actual option IDs determine which answers are correct. */
internal fun hostAnswerKeyLines(state: HostGameUiState): List<String> {
    if (!state.hasAnswerKey || !state.isAnswerRevealed) return emptyList()
    val question = hostCurrentQuestion(state) ?: return emptyList()
    return state.correctAnswers.map { key ->
        val index = question.answerOptions.indexOfFirst { it.id == key }
        if (index < 0) key else "${hostOptionLetter(index)}. ${question.answerOptions[index].text ?: key}"
    }
}

internal fun hostOptionLetter(index: Int): String =
    if (index in 0..25) ('A'.code + index).toChar().toString() else (index + 1).toString()
