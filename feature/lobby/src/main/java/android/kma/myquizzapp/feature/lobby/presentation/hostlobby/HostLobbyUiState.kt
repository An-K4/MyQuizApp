package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.gameconfig.RoomConfigForm

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
    val errorMessage: String? = null,

    /** Config hiện tại của phòng, lấy từ `lobby:updated` hoặc ack sửa config. */
    val config: GameConfig? = null,

    /** Chế độ chơi của phòng — chỉ có sau khi tra `GET /:code`. */
    val mode: GameMode? = null,

    /** Đặc tả editable/locked của [mode], chỉ có sau khi gọi `GET /game-modes`. */
    val descriptor: GameModeDescriptor? = null,

    /** Form đang sửa; null khi chưa dựng được (thiếu config hoặc descriptor). */
    val configForm: RoomConfigForm? = null,
    val invalidConfigKeys: Set<GameConfigKey> = emptySet(),
    val isConfigSheetOpen: Boolean = false,

    /** Đang tải [mode] + [descriptor] (hai request song song). */
    val isLoadingSpec: Boolean = false,

    /** Đang chờ ack của `lobby:config-update`. */
    val isSavingConfig: Boolean = false,

    /**
     * Thông báo kết quả sửa config (đã lưu / không có gì thay đổi / bị bỏ qua).
     *
     * Tách khỏi [errorMessage] vì đây là kết quả bình thường của một thao tác
     * thành công, không phải lỗi; gộp chung sẽ khiến "đã lưu" hiện lên như lỗi.
     */
    val configNotice: String? = null,

    /**
     * Đã gửi `game:start` và đang chờ `game:started`.
     *
     * Lệnh này không có ack nên đây là trạng thái "chưa biết kết quả", có hạn
     * chờ riêng ở ViewModel để không treo vĩnh viễn.
     */
    val isStarting: Boolean = false
) {
    /** Đã nhận được snapshot lobby đầu tiên chưa. */
    val hasLobbySnapshot: Boolean get() = sessionStatus != null

    val playerCount: Int get() = players.size

    /**
     * Chỉ được sửa config khi phòng còn ở lobby.
     *
     * Backend chặn bằng 409 `GAME_LOBBY_ONLY` trong `writeConfig`, nhưng chặn ở
     * client vẫn cần: nút mờ đi ngay khi trận bắt đầu thì host không gửi một
     * thao tác chắc chắn thất bại rồi nhận thông báo lỗi khó hiểu.
     */
    val canEditConfig: Boolean
        get() = sessionStatus == SessionStatus.LOBBY

    /** Các control trong form có đang cho tương tác không. */
    val isConfigFormEnabled: Boolean
        get() = canEditConfig && !isSavingConfig && !isLoadingSpec

    /** Link web để chia sẻ phòng cho người chơi không dùng app. */
    val shareLink: String
        get() = "$WEB_ORIGIN/join?code=$sessionCode"

    /**
     * Cho bấm "Bắt đầu" hay chưa.
     *
     * Phải đang ở lobby, socket phải đang sống (gửi khi mất kết nối thì lệnh rơi
     * vào khoảng không vì `game:start` không có ack), và không đang chờ dở.
     */
    val canStartGame: Boolean
        get() = canEditConfig &&
            connection == ConnectionStatus.CONNECTED &&
            !isStarting &&
            !isSavingConfig
}

/** Origin của web app — dùng dụng link chia sẻ phòng. */
const val WEB_ORIGIN = "https://myquizz.dpdns.org"

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
