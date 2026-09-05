package android.kma.myquizzapp.core.common.model

/**
 * Thông tin phòng đọc được TRƯỚC khi join, lấy từ `GET /v1/games/{code}`.
 *
 * Endpoint này PUBLIC (không cần đăng nhập) nên guest cũng tra được — đó là điều
 * kiện để client biết phòng có cho guest vào hay không mà chưa cần gọi join.
 *
 * Đây là bản rút gọn của [GameSession] (server trả về nguyên session): chỉ giữ
 * những field màn "Nhập mã phòng" thực sự dùng để quyết định cho đi tiếp hay chặn.
 * Cố tình KHÔNG dùng thẳng GameSession ở tầng UI để sau này server đổi/ thêm field
 * gameplay thì màn join không phải sửa theo.
 */
data class RoomLookup(
    val gameId: Long,
    val sessionCode: String,
    val sessionName: String,
    val mode: GameMode,
    val status: SessionStatus,
    /** `config.lobby.allowGuests` — host quyết định khi tạo phòng. */
    val allowGuests: Boolean,
    /** `config.lobby.allowLateJoin` — cho vào khi trận đã bắt đầu. */
    val allowLateJoin: Boolean,
    val maxPlayers: Int,
    val totalPlayers: Int,
    val questionCount: Int,
    /** `config.flow.lives` — chỉ có nghĩa ở mode survival; null = không giới hạn. */
    val lives: Int? = null
) {
    /**
     * Phòng còn nhận người vào hay không.
     *
     * Trạng thái `lobby` thì luôn được. Trận đang chạy (`active`/`paused`) chỉ được
     * khi host bật allowLateJoin. Trận đã kết thúc/huỷ thì không.
     *
     * Đây là kiểm tra ở CLIENT để báo lỗi sớm cho người dùng; server vẫn là nơi
     * quyết định cuối cùng (join sẽ trả về code lỗi tương ứng).
     */
    val isOpenForJoin: Boolean
        get() = when (status) {
            SessionStatus.LOBBY -> true
            SessionStatus.ACTIVE, SessionStatus.PAUSED -> allowLateJoin
            SessionStatus.FINISHED, SessionStatus.CANCELLED -> false
        }

    /** Đã đủ người theo `maxPlayers`. */
    val isFull: Boolean get() = totalPlayers >= maxPlayers
}
