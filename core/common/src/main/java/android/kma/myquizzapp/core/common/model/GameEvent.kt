package android.kma.myquizzapp.core.common.model

/**
 * Sự kiện realtime của namespace `/game` sau khi đã map sang domain.
 *
 * N18 chỉ phủ phần lobby (connect + lobby:join + lobby:updated + error). Các event
 * gameplay (question:*, leaderboard:*, game:*) sẽ được thêm dần ở N19+; tạm thời
 * chúng rơi vào [Unhandled] để log mà không làm sập luồng.
 *
 * Nguyên tắc: KHÔNG để JsonElement/JSONObject lọt lên domain. Mọi payload phải
 * được GameEventMapper (core:network) parse thành model typed trước khi phát ra.
 */
sealed interface GameEvent {

    /** Handshake thành công. Repository sẽ tự re-emit `lobby:join` sau event này. */
    data object Connected : GameEvent

    /** Mất kết nối. [reason] quyết định có auto-reconnect hay điều hướng ra ngoài. */
    data class Disconnected(val reason: DisconnectReason) : GameEvent

    /** `lobby:updated` — nguồn sự thật duy nhất cho danh sách người chơi trong lobby. */
    data class LobbyUpdated(val lobby: LobbyState) : GameEvent

    /**
     * `error` từ server, hoặc handshake bị từ chối (`connect_error`).
     *
     * Backend chỉ gửi CODE, không gửi câu văn (xem shared/errors/codes.ts) — client
     * tự sở hữu wording qua AppError.Api(code).toUserMessage().
     *
     * @param event tên client event gây lỗi; null khi lỗi đến từ handshake.
     */
    data class Failed(val event: String?, val code: String) : GameEvent

    /** Event backend gửi mà N18 chưa xử lý — chỉ để log, không phải lỗi. */
    data class Unhandled(val event: String) : GameEvent
}

/**
 * Lý do socket ngắt, quy về 3 nhóm mà UI phản ứng khác nhau.
 *
 * Socket.IO trả reason dạng string; chỉ `io server disconnect` là do server chủ
 * động đá ra — client KHÔNG được auto-reconnect trong trường hợp đó.
 */
enum class DisconnectReason {
    /** Server chủ động ngắt (kick, phòng đóng). Không tự kết nối lại. */
    SERVER_DISCONNECT,

    /** Rớt mạng / transport lỗi. Socket.IO sẽ tự thử lại. */
    TRANSPORT,

    /** Client tự ngắt (thoát màn hình). Trường hợp bình thường. */
    CLIENT
}

/**
 * Trạng thái lobby dựng từ payload `lobby:updated`.
 *
 * @param serverTime đồng hồ server lúc build message (ISO-8601). Client giữ offset
 *   thay vì tin đồng hồ máy mình — cần cho countdown ở N20+.
 */
data class LobbyState(
    val sessionStatus: SessionStatus,
    val config: GameConfig,
    val players: List<LobbyPlayer>,
    val serverTime: String? = null
)
