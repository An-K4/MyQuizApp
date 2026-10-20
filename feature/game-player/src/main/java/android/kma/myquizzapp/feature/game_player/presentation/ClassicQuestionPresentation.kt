package android.kma.myquizzapp.feature.game_player.presentation

import java.time.Instant

/** Display-only clock values. A missing duration is not a fresh 100% timer. */
internal data class ClassicQuestionTime(
    val seconds: Int? = null,
    val fraction: Float? = null,
    val critical: Boolean = false,
    val expired: Boolean = false,
)

internal fun classicQuestionTime(state: GameUiState, nowMillis: Long): ClassicQuestionTime {
    val deadline = state.endsAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    val left = when {
        state.isPaused -> state.remainingSeconds?.toLong()?.times(1_000L)
            ?: return ClassicQuestionTime()
        deadline != null -> deadline - (nowMillis + state.serverOffsetMs)
        else -> return ClassicQuestionTime()
    }.coerceAtLeast(0L)
    val limit = state.timeLimitSeconds?.takeIf { it > 0 }?.toLong()?.times(1_000L)
    return ClassicQuestionTime(
        seconds = ((left + 999L) / 1_000L).toInt(),
        fraction = limit?.let { (left.toFloat() / it).coerceIn(0f, 1f) },
        critical = limit != null && left <= limit * 3L / 10L,
        expired = deadline != null && left == 0L && !state.isPaused,
    )
}

internal fun classicQuestionStatus(state: GameUiState): String = when {
    state.connection == GameConnection.RECONNECT_FAILED -> "Chưa kết nối lại được. Đáp án đang khóa."
    state.connection != GameConnection.CONNECTED -> "Đang kết nối và đồng bộ câu hỏi…"
    state.isPaused -> "Trận đang tạm dừng — chờ Host tiếp tục."
    state.phase == GamePhaseUi.Locked -> "Câu hỏi đã đóng. Đang chờ kết quả…"
    state.isConfirming -> "Đang xác nhận câu trả lời với máy chủ…"
    state.isSubmitting -> "Đang gửi đáp án…"
    state.phase == GamePhaseUi.Submitted -> "Đã gửi đáp án. Đang chờ kết quả…"
    !state.isAnswerInputEnabled -> "Đã hết thời gian trả lời. Đang chờ máy chủ…"
    state.question?.questionType == "multiple_choice" -> "Chạm một đáp án để gửi ngay."
    state.question?.questionType == "multiple_select" -> "Chọn các đáp án rồi bấm Gửi đáp án."
    else -> "Nhập câu trả lời rồi bấm Gửi đáp án."
}
