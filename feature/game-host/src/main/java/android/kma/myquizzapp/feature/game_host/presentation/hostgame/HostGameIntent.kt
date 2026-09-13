package android.kma.myquizzapp.feature.game_host.presentation.hostgame

/**
 * Hành vi người dùng trên màn điều khiển trận.
 *
 * Mỗi Intent ở đây đều phải có một điểm bấm thật trên UI (bài học mục 3.5 của
 * AGENTS.md): thêm Intent mà không nối nút thì code chỉ là đồ trang trí.
 */
sealed interface HostGameIntent {

    /** Mở lại kết nối sau khi mất mạng. */
    data object Retry : HostGameIntent

    /** Bật/tắt phần đáp án đúng — thao tác thuần cục bộ, không gọi mạng. */
    data object ToggleAnswerKey : HostGameIntent

    /**
     * Nút chính: `game:next`. MỘT event nhưng HAI ý nghĩa tùy phase — đang mở thì
     * chốt câu sớm, đang hiện kết quả thì sang câu mới.
     */
    data object AdvanceQuestion : HostGameIntent

    /** Một nút cho cả `game:pause` và `game:resume`, chọn theo trạng thái phiên. */
    data object PauseOrResume : HostGameIntent

    /** Mở hộp xác nhận kết thúc trận (không gửi gì lên server). */
    data object RequestEndGame : HostGameIntent

    /** Đã xác nhận: gửi `game:end`. */
    data object ConfirmEndGame : HostGameIntent

    data object DismissEndDialog : HostGameIntent

    /** Rời màn điều khiển mà KHÔNG kết thúc trận. */
    data object LeaveGame : HostGameIntent

    data object ErrorShown : HostGameIntent

    data object NoticeShown : HostGameIntent
}
