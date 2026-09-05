package android.kma.myquizzapp.core.common.model

/**
 * Kết quả `POST /v1/games/{code}/join` (HTTP 201).
 *
 * QUAN TRỌNG — khác host: response của join trả `socketToken` PHẲNG (chuỗi ngay
 * trong `data`), còn host-token trả lồng `hostToken.socketToken`. Hai endpoint
 * không cùng hình dạng, đừng copy DTO của nhau (xem điểm lệch tài liệu N19).
 */
data class JoinRoomResult(
    val player: JoinedPlayer,
    /** JWT ngắn hạn để mở socket `/game`; mang sẵn psid + gsid + role. */
    val socketToken: String
)

/**
 * Bản ghi người chơi vừa được tạo trong phòng.
 *
 * Chỉ giữ những field màn lobby cần: id để biết "tôi là ai" trong danh sách,
 * tên/avatar để hiển thị, lives cho mode survival. Server trả về nhiều field hơn
 * (điểm, streak, đáp án...) nhưng lúc mới join tất cả đều rỗng nên không mang lên.
 */
data class JoinedPlayer(
    /** `player.id` — id của DÒNG người chơi trong phiên, không phải user id. */
    val id: Long,
    val playerName: String,
    val playerAvatar: String? = null,
    val lives: Int? = null
)
