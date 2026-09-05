package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus

/**
 * MVI contract màn phòng chờ của NGƯỜI CHƠI.
 *
 * Dùng chung [ConnectionStatus] với HostLobby thay vì định nghĩa enum thứ hai giống
 * hệt: bản chất trạng thái socket của hai màn là một. Khi nào có màn thứ ba dùng
 * tới thì chuyển enum này lên package dùng chung.
 *
 * Khác HostLobby ở hai điểm:
 * - Có [myPlayerId] để tô đậm dòng của chính mình trong danh sách.
 * - Không có nút bắt đầu trận; người chơi chỉ chờ host bấm.
 */
data class PlayerLobbyUiState(
    val players: List<LobbyPlayer> = emptyList(),
    val sessionStatus: SessionStatus? = null,
    val connection: ConnectionStatus = ConnectionStatus.CONNECTING,
    val myPlayerId: Long = 0L,
    val errorMessage: String? = null
) {
    /** Đã nhận được snapshot lobby đầu tiên chưa. */
    val hasLobbySnapshot: Boolean get() = sessionStatus != null

    val playerCount: Int get() = players.size

    /** Có mode tính mạng (survival) hay không — quyết định có hiện số mạng không. */
    val showLives: Boolean get() = players.any { it.lives != null }
}

sealed interface PlayerLobbyIntent {
    data object Retry : PlayerLobbyIntent
    data object LeaveRoom : PlayerLobbyIntent
    data object ErrorShown : PlayerLobbyIntent
}

sealed interface PlayerLobbyEffect {
    /** Rời màn lobby; [message] là lý do cần báo lại cho màn trước (null = tự thoát). */
    data class ExitLobby(val message: String?) : PlayerLobbyEffect
}
