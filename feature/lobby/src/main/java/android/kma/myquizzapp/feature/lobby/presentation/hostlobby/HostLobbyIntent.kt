package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

/**
 * Intent của màn lobby host.
 *
 * Phạm vi N18 cố tình hẹp: chỉ kết nối — xem danh sách — thoát. Các lệnh điều
 * khiển trận (bắt đầu/tạm dừng/kết thúc) tầng repository đã có sẵn nhưng KHÔNG
 * khai báo intent ở đây, vì chưa có nút thật nào trên UI — theo quy ước đã chốt:
 * mọi Intent phải có trigger UI thật, không để intent chết.
 */
sealed interface HostLobbyIntent {
    /** Thử kết nối lại sau khi socket.io đã cạn số lần tự retry. */
    data object Retry : HostLobbyIntent

    /** Người dùng bấm thoát phòng. */
    data object LeaveRoom : HostLobbyIntent

    /** Snackbar lỗi đã hiển xong — xóa để không hiện lại khi recompose. */
    data object ErrorShown : HostLobbyIntent
}
