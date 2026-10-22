package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import java.text.NumberFormat
import java.util.Locale

internal val HostGameUiState.usesSelfPacedMonitorLayout: Boolean
    get() = isSelfPaced && !isFinished && phase in setOf(
        GamePhase.QUESTION_ACTIVE, GamePhase.QUESTION_LOCKED, GamePhase.SHOWING_RESULTS)
internal val HostGameUiState.hasFreshMonitoringData: Boolean
    get() = connection == HostGameConnection.CONNECTED && isStateSynchronized && hasMonitoringData
internal val HostGameUiState.monitorShowsScores: Boolean get() = mode != GameMode.PRACTICE
internal val HostGameUiState.monitorShowsEliminated: Boolean
    get() = mode != GameMode.PRACTICE && (mode == GameMode.SURVIVAL || leaderboard.rows.any {
        it.status == "eliminated" || (mode == GameMode.MARATHON && it.lives != null)
    })

internal fun hostMonitorModeLabel(state: HostGameUiState): String = when (state.mode) {
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
    else -> "Tiến độ riêng"
}

internal enum class HostMonitorStatus(val label: String) {
    PLAYING("Đang chơi"), FINISHED("Đã hoàn thành"), DISCONNECTED("Mất kết nối"),
    ELIMINATED("Đã bị loại"), UNKNOWN("Chưa rõ trạng thái")
}
internal fun hostMonitorStatus(status: String): HostMonitorStatus = when (status) {
    "connected" -> HostMonitorStatus.PLAYING
    "finished" -> HostMonitorStatus.FINISHED
    "disconnected" -> HostMonitorStatus.DISCONNECTED
    "eliminated" -> HostMonitorStatus.ELIMINATED
    else -> HostMonitorStatus.UNKNOWN
}

internal data class HostMonitorMetric(val label: String, val value: Int?, val status: HostMonitorStatus)
internal fun hostMonitorMetrics(state: HostGameUiState): List<HostMonitorMetric> {
    val fresh = state.hasFreshMonitoringData
    return buildList {
        add(HostMonitorMetric("Đang chơi", state.playingPlayers.takeIf { fresh }, HostMonitorStatus.PLAYING))
        add(HostMonitorMetric("Hoàn thành", state.finishedPlayers.takeIf { fresh }, HostMonitorStatus.FINISHED))
        add(HostMonitorMetric("Mất kết nối", state.disconnectedPlayers.takeIf { fresh }, HostMonitorStatus.DISCONNECTED))
        if (state.monitorShowsEliminated)
            add(HostMonitorMetric("Bị loại", state.eliminatedPlayers.takeIf { fresh }, HostMonitorStatus.ELIMINATED))
    }
}

internal data class HostMonitorProgress(val label: String, val fraction: Float? = null)
internal fun hostMonitorProgress(row: HostLeaderboardRow, state: HostGameUiState): HostMonitorProgress {
    val answered = row.answeredCount
    if (answered < 0) return HostMonitorProgress("Chưa có tiến độ")
    // Marathon can loop the bank. Never show a finite completion percentage or 11/10.
    if (state.isMarathon) return HostMonitorProgress("Đã làm $answered câu")
    val total = row.totalQuestions.takeIf { it > 0 } ?: state.totalQuestions
    if (total <= 0 || answered > total) return HostMonitorProgress("Đã làm $answered câu")
    // Finished/eliminated does not imply every question was answered.
    return HostMonitorProgress("Đã làm $answered/$total câu", answered.toFloat() / total)
}

/** A monitor is not a leaderboard. Do not shuffle cards after every answer, including zero-score Practice. */
internal fun hostMonitorRows(state: HostGameUiState): List<HostLeaderboardRow> =
    state.leaderboard.rows.sortedBy { it.id }

internal fun hostMonitorInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    fun first(word: String) = String(Character.toChars(word.codePointAt(0)))
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> first(words.first())
        else -> first(words.first()) + first(words.last())
    }.uppercase(Locale.ROOT)
}
internal fun hostMonitorScoreLabel(score: Int): String =
    "${NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(score)} điểm"

internal fun hostMonitorNotice(state: HostGameUiState): String? = when {
    state.connection == HostGameConnection.RECONNECT_FAILED -> "Không thể kết nối lại. Dữ liệu bên dưới chưa được cập nhật."
    state.connection == HostGameConnection.CONNECTING -> "Đang kết nối với máy chủ…"
    state.connection != HostGameConnection.CONNECTED -> "Mất kết nối. Đang hiển thị dữ liệu lần đồng bộ trước."
    !state.isStateSynchronized -> "Đang đồng bộ trạng thái trận. Các thao tác điều khiển đang khóa."
    !state.hasMonitoringData -> "Đang đồng bộ danh sách người chơi…"
    state.isPaused -> "Trận đang tạm dừng."
    else -> null
}
