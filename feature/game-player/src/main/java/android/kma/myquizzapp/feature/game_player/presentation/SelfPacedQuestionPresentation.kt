package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import java.time.Instant

/** Display policy only. Server ACKs/events remain the authority for grading and transitions. */
internal val GameUiState.usesSelfPacedLayout: Boolean
    get() = isSelfPaced && mode in setOf(GameMode.SOLO, GameMode.SURVIVAL, GameMode.MARATHON, GameMode.PRACTICE)

internal val GameUiState.selfPacedIsPractice: Boolean get() = mode == GameMode.PRACTICE
internal val GameUiState.selfPacedIsOnline: Boolean
    get() = connection == GameConnection.CONNECTED && !isSynchronizing
internal val GameUiState.selfPacedCanAnswer: Boolean
    get() = selfPacedIsOnline && !isPaused && !matchTimeExpired && isAnswerInputEnabled &&
        phase == GamePhaseUi.Question && !isSubmitting && !isConfirming
internal val GameUiState.selfPacedShowsSubmitAction: Boolean
    get() = isSelfPaced && question?.questionType in setOf("multiple_select", "short_answer", "long_answer") &&
        (phase == GamePhaseUi.Question || phase == GamePhaseUi.Submitted)

internal val GameUiState.selfPacedCanRequestNext: Boolean
    get() = shouldShowNextAction && selfPacedIsOnline && !isPaused && !matchTimeExpired && !isRequestingNext

internal fun selfPacedModeLabel(state: GameUiState): String = when (state.mode) {
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
    else -> "Đang chơi"
}

internal val GameUiState.selfPacedResultMatchesQuestion: Boolean
    get() {
        val current = question ?: return false
        val result = results ?: return false
        return (phase is GamePhaseUi.Results || phase == GamePhaseUi.Eliminated) &&
            result.index == current.index && (result.questionId == null || result.questionId == current.id) &&
            (phase as? GamePhaseUi.Results)?.restoredWithoutDetails != true
    }

internal val GameUiState.selfPacedRevealsResult: Boolean
    get() = selfPacedResultMatchesQuestion && showCorrectAnswer == true &&
        outcome in setOf(QuestionOutcome.CORRECT, QuestionOutcome.INCORRECT)

/** Pending drafts may be displayed; results use accepted evidence only, never a draft. */
internal fun selfPacedSelectedKeys(state: GameUiState): List<String> =
    if (state.phase == GamePhaseUi.Question || state.phase == GamePhaseUi.Submitted) state.submittedAnswerKeys
    else state.acceptedAnswerKeys.orEmpty()

internal fun selfPacedProgressLabel(state: GameUiState): String {
    if (state.isPersonallyDone) return "Lượt chơi đã kết thúc"
    val question = state.question ?: return "Đang đồng bộ câu hỏi…"
    if (state.mode == GameMode.MARATHON) {
        val answered = question.index + if (state.selfPacedResultMatchesQuestion ||
            state.acceptedAnswerKeys != null) 1 else 0
        return "Đã làm ${answered.coerceAtLeast(0)} câu"
    }
    return "Câu ${question.index + 1}/${question.total}"
}

internal fun selfPacedScoreDeltaLabel(state: GameUiState): String? {
    if (state.selfPacedIsPractice || !state.selfPacedRevealsResult) return null
    return state.scoreEarned?.let { "${if (it > 0) "+" else ""}$it điểm" }
}

internal fun selfPacedStreakLabel(state: GameUiState): String? {
    if (!state.selfPacedRevealsResult || state.outcome != QuestionOutcome.CORRECT) return null
    return state.streak?.takeIf { it > 0 }?.let { "Chuỗi $it câu đúng liên tiếp!" }
}

internal fun selfPacedOutcomeTitle(state: GameUiState): String = when {
    (state.phase as? GamePhaseUi.Results)?.restoredWithoutDetails == true -> "Đã ghi nhận kết quả"
    !state.selfPacedRevealsResult -> "Đã ghi nhận kết quả"
    state.timedOut -> "Đã hết thời gian câu này"
    state.outcome == QuestionOutcome.CORRECT -> "Chính xác!"
    else -> "Chưa chính xác"
}

internal fun selfPacedCompletionTitle(state: GameUiState): String = when {
    state.phase == GamePhaseUi.Eliminated -> "Bạn đã hết mạng"
    state.matchTimeExpired -> "Đã hết thời gian"
    state.selfPacedIsPractice -> "Đã hoàn thành luyện tập"
    else -> "Bạn đã hoàn thành lượt chơi"
}

internal fun selfPacedFooterText(state: GameUiState): String = when {
    state.connection == GameConnection.RECONNECT_FAILED -> "Chưa kết nối lại được. Hãy thử lại."
    !state.selfPacedIsOnline -> "Đang kết nối và đồng bộ trạng thái…"
    state.isPaused -> "Trận đang tạm dừng — chờ Host tiếp tục."
    state.phase == GamePhaseUi.Finished -> "Đang mở kết quả chung cuộc…"
    state.isPersonallyDone -> "Đang chờ trận đấu kết thúc."
    state.matchTimeExpired -> "Đã hết thời gian. Đang chờ máy chủ xác nhận…"
    state.isRequestingNext -> "Đang tải câu tiếp theo…"
    state.phase is GamePhaseUi.Results -> when {
        state.shouldShowNextAction -> "Sẵn sàng? Chuyển sang câu tiếp theo."
        state.autoAdvance -> "Đang chờ máy chủ chuyển câu…"
        else -> "Đang chờ máy chủ xác nhận hoàn thành…"
    }
    state.isConfirming -> "Đang xác nhận câu trả lời với máy chủ…"
    state.isSubmitting -> "Đang gửi đáp án…"
    state.phase == GamePhaseUi.Submitted -> "Đã gửi đáp án. Đang chờ kết quả…"
    state.phase == GamePhaseUi.Locked -> "Câu hỏi đã đóng. Đang chờ kết quả…"
    !state.isAnswerInputEnabled -> "Đã hết thời gian trả lời. Đang chờ máy chủ…"
    state.question?.questionType == "multiple_choice" -> "Chạm một đáp án để gửi ngay."
    state.question?.questionType == "multiple_select" -> "Chọn các đáp án rồi bấm Gửi đáp án."
    else -> "Nhập câu trả lời rồi bấm Gửi đáp án."
}

internal data class SelfPacedTime(
    val question: ClassicQuestionTime = ClassicQuestionTime(),
    val matchSeconds: Int? = null,
    val matchExpired: Boolean = false,
)

/** No synthetic duration/deadline, no local advance or grading. Pause/reconnect hide stale clocks. */
internal fun selfPacedTime(state: GameUiState, nowMillis: Long): SelfPacedTime {
    if (!state.selfPacedIsOnline || state.isPaused || state.isPersonallyDone || state.phase == GamePhaseUi.Finished)
        return SelfPacedTime()
    val questionTime = if (state.phase == GamePhaseUi.Question) classicQuestionTime(state, nowMillis)
        else ClassicQuestionTime()
    val matchDeadline = state.matchEndsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val matchLeft = matchDeadline?.let { (it - nowMillis - state.serverOffsetMs).coerceAtLeast(0L) }
    return SelfPacedTime(questionTime, matchLeft?.let { ((it + 999L) / 1_000L).toInt() }, matchLeft == 0L)
}

internal fun selfPacedClockLabel(seconds: Int): String =
    "${seconds.coerceAtLeast(0) / 60}:${(seconds.coerceAtLeast(0) % 60).toString().padStart(2, '0')}"
