package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus

/**
 * Trạng thái màn lobby của HOST (MVI).
 *
 * [connection] tách riêng khỏi [errorMessage] có lý do: mất mạng tạm thời là trạng
 * thái đang diễn ra (hiện banner "Đang kết nối lại..." và tự hồi phục), còn
 * errorMessage là một sự kiện đã xảy ra và cần người dùng đọc rồi bỏ qua. Nhộp
 * chung một field thì banner reconnect sẽ bị snackbar đè mất.
 *
 * Danh sách [players] luôn là toàn bộ snapshot từ server (`lobby:updated` gửi cả
 * danh sách, không gửi delta) nên chỉ cần gán đè, không tự cộng/trừ ở client.
 */
data class HostLobbyUiState(
    val sessionCode: String = "",
    val players: List<LobbyPlayer> = emptyList(),
    val sessionStatus: SessionStatus? = null,
    val connection: ConnectionStatus = ConnectionStatus.CONNECTING,
    val errorMessage: String? = null
) {
    /** Đã nhận được snapshot lobby đầu tiên chưa. */
    val hasLobbySnapshot: Boolean get() = sessionStatus != null

    val playerCount: Int get() = players.size
}

/**
 * Trạng thái kết nối socket, hiển thị trực tiếp cho người dùng.
 *
 * Không dùng boolean isConnected vì ba trạng thái "chưa từng kết nối", "đang kết
 * nối lại" và "đã kết nối" cần ba cách hiển thị khác nhau.
 */
enum class ConnectionStatus {
    /** Lần kết nối đầu tiên, chưa có dự liệu để hiển thị. */
    CONNECTING,

    /** Đã kết nối và đã join phòng. */
    CONNECTED,

    /** Mất kết nối tạm thời, socket.io đang tự thử lại — vẫn giữ dự liệu cũ. */
    RECONNECTING
}
