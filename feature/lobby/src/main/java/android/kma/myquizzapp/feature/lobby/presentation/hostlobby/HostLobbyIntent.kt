package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.GameConfigKey

/**
 * Intent của màn lobby host.
 *
 * Quy ước đã chốt: mọi Intent phải có trigger UI thật, không để intent chết.
 * Vì vậy các lệnh điều khiển trận mà repository đã có (`game:next`, `game:pause`,
 * `game:resume`, `game:end`) vẫn CHƯA xuất hiện ở đây — chúng thuộc màn chơi của
 * host, không thuộc phòng chờ.
 */
sealed interface HostLobbyIntent {
    /** Thống kết nối lại sau khi socket.io đã cạn số lần tự retry. */
    data object Retry : HostLobbyIntent

    /** Người dùng bấm thoát phòng. */
    data object LeaveRoom : HostLobbyIntent

    /** Snackbar lỗi đã hiển xong — xóa để không hiện lại khi recompose. */
    data object ErrorShown : HostLobbyIntent

    /** Mở bảng sửa cấu hình; đây cũng là lúc mới đi tải mode + descriptor. */
    data object OpenConfigSheet : HostLobbyIntent

    /** Đóng bảng sửa cấu hình, bỏ mọi thay đổi chưa lưu. */
    data object DismissConfigSheet : HostLobbyIntent

    /** Tải lại mode + descriptor sau khi lần tải trước thất bại. */
    data object RetryLoadSpec : HostLobbyIntent

    data class ToggleChanged(val key: GameConfigKey, val checked: Boolean) : HostLobbyIntent

    /**
     * Ô số thay đổi. [value] là chuỗi thô, chưa lọc — chuỗi rỗng là trạng thái
     * hợp lệ (một số field cho phép null) nên không ép sang Int? ở đây.
     */
    data class NumberChanged(val key: GameConfigKey, val value: String) : HostLobbyIntent

    data class ChoiceChanged(val key: GameConfigKey, val value: String) : HostLobbyIntent

    /** Gửi patch cấu hình lên server. */
    data object SaveConfig : HostLobbyIntent

    /** Thông báo kết quả sửa cấu hình đã hiển xong. */
    data object ConfigNoticeShown : HostLobbyIntent

    /**
     * Bắt đầu trận (`game:start`).
     *
     * Lệnh này KHÔNG có ack: thành công thì biết qua `game:started`, thất bại thì
     * qua event `error`. UI phải tự đặt hạn chờ.
     */
    data object StartGame : HostLobbyIntent
}
