package android.kma.myquizzapp.feature.game_player.presentation

import java.time.Instant

internal enum class ClassicResultKind { CORRECT, INCORRECT, UNANSWERED, UNKNOWN, HIDDEN }

internal fun classicResultKind(state: GameUiState): ClassicResultKind = when {
    state.showCorrectAnswer == false -> ClassicResultKind.HIDDEN
    state.visiblePersonalResult != null -> state.visiblePersonalResult!!.let {
        when {
            !it.answered -> ClassicResultKind.UNANSWERED
            it.isCorrect == true -> ClassicResultKind.CORRECT
            else -> ClassicResultKind.INCORRECT
        }
    }
    state.showCorrectAnswer == null || state.results == null ||
        (state.phase as? GamePhaseUi.Results)?.restoredWithoutDetails == true ->
        ClassicResultKind.UNKNOWN
    state.acceptedAnswerKeys == null && state.hasAnswerAttempt -> ClassicResultKind.UNKNOWN
    state.acceptedAnswerKeys.isNullOrEmpty() -> ClassicResultKind.UNANSWERED
    state.outcome == QuestionOutcome.CORRECT -> ClassicResultKind.CORRECT
    state.outcome == QuestionOutcome.INCORRECT -> ClassicResultKind.INCORRECT
    else -> ClassicResultKind.UNKNOWN
}

internal fun classicResultTitle(state: GameUiState): String = when (classicResultKind(state)) {
    ClassicResultKind.CORRECT -> "Chính xác!"
    ClassicResultKind.INCORRECT -> "Chưa chính xác"
    ClassicResultKind.UNANSWERED -> "Bạn chưa trả lời câu này"
    ClassicResultKind.UNKNOWN -> "Chưa đủ dữ liệu kết quả"
    ClassicResultKind.HIDDEN -> "Câu hỏi đã kết thúc"
}

internal fun classicResultDescription(state: GameUiState): String = when (classicResultKind(state)) {
    ClassicResultKind.CORRECT -> "Bạn đã trả lời đúng câu hỏi này."
    ClassicResultKind.INCORRECT -> "Câu trả lời đã gửi chưa khớp đáp án được chấp nhận."
    ClassicResultKind.UNANSWERED -> "Không có câu trả lời được xác nhận cho câu hỏi này."
    ClassicResultKind.HIDDEN -> "Phòng chơi không công bố đáp án và kết quả đúng/sai."
    ClassicResultKind.UNKNOWN -> when {
        (state.phase as? GamePhaseUi.Results)?.restoredWithoutDetails == true ->
            "Đã đồng bộ phiên chơi, nhưng máy chủ không gửi lại chi tiết kết quả câu này."
        state.hasAnswerAttempt && state.acceptedAnswerKeys == null ->
            "Chưa xác nhận được máy chủ đã ghi nhận câu trả lời. Không tự suy đoán đúng/sai."
        else -> "Chưa nhận đủ dữ liệu để hiển thị kết quả đúng/sai."
    }
}

/** endsAt is the current server phase deadline, including the refreshed deadline after resume. */
internal fun classicResultsWaitText(state: GameUiState, nowMillis: Long): String {
    if (state.connection == GameConnection.RECONNECT_FAILED) return "Chưa kết nối lại được — chờ đồng bộ phiên chơi."
    if (state.connection != GameConnection.CONNECTED || state.isSynchronizing) return "Đang kết nối lại và đồng bộ phiên chơi…"
    if (state.isPaused) return "Trận đang tạm dừng — chờ Host tiếp tục."
    if (!state.autoAdvance) return "Chờ Host chuyển câu."
    val deadline = state.endsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: return "Đang chờ máy chủ chuyển tiếp…"
    val left = (deadline - nowMillis - state.serverOffsetMs).coerceAtLeast(0L)
    if (left == 0L) return "Đang chờ máy chủ chuyển tiếp…"
    val seconds = (left + 999L) / 1_000L
    return if (state.question?.let { it.index + 1 == it.total } == true)
        "Tổng kết trận sẽ bắt đầu sau: ${seconds}s"
    else "Câu tiếp theo sẽ bắt đầu sau: ${seconds}s"
}

internal fun classicResultsFooter(state: GameUiState): String = when {
    state.connection != GameConnection.CONNECTED || state.isSynchronizing -> "Đang kết nối lại và đồng bộ phiên chơi…"
    state.isPaused -> "Trận đang tạm dừng — chờ Host tiếp tục."
    state.skipsClassicSummary -> "Câu hỏi đã kết thúc. Chờ máy chủ chuyển tiếp…"
    state.revealsClassicFeedback -> "Đã có kết quả. Đang chuyển sang tổng kết câu hỏi…"
    else -> "Câu hỏi đã kết thúc. Chờ máy chủ chuyển tiếp…"
}
