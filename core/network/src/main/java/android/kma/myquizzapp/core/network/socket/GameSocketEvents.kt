package android.kma.myquizzapp.core.network.socket

/**
 * Tên event của namespace `/game`, copy nguyên văn từ backend
 * (docs/socket.channels.ts). Đặt một chỗ để không rải string literal khắp code —
 * sai một ký tự trong tên event là lỗi im lặng, không compile error nào bắt được.
 */
internal object GameSocketEvents {

    /** Namespace, nối vào sau SOCKET_URL (SOCKET_URL không chứa path). */
    const val NAMESPACE = "/game"

    // ----- Client → server -----
    const val LOBBY_JOIN = "lobby:join"
    const val LOBBY_LEAVE = "lobby:leave"

    /**
     * `lobby:config-update` — event duy nhất của host CÓ ack.
     *
     * Payload nhận cả `{ config: {...} }` và object config trần (`raw.config ?? raw`
     * trong `game.socket.ts`); ta gửi dạng bọc `config` cho rõ ràng.
     */
    const val LOBBY_CONFIG_UPDATE = "lobby:config-update"

    const val GAME_START = "game:start"
    const val GAME_NEXT = "game:next"
    const val GAME_PAUSE = "game:pause"
    const val GAME_RESUME = "game:resume"
    const val GAME_END = "game:end"
    const val QUESTION_ANSWER = "question:answer"
    const val QUESTION_NEXT = "question:next"
    const val PLAYER_SYNC = "player:sync"

    // ----- Server → client -----
    const val LOBBY_UPDATED = "lobby:updated"
    const val ERROR = "error"

    /** Xác nhận trận đã bắt đầu — `game:start` không có ack nên phải dựa vào event này. */
    const val GAME_STARTED = "game:started"

    /**
     * Toàn bộ event server có thể gửi trên `/game`.
     *
     * N18 chỉ map thật [LOBBY_UPDATED] và [ERROR]; số còn lại vẫn được đăng ký
     * listener để phát ra GameEvent.Unhandled — có log thấy backend đang gửi gì
     * khi debug reconnect, thay vì im lặng bỏ qua.
     */
    val SERVER_EVENTS = listOf(
        LOBBY_UPDATED,
        ERROR,
        "game:state",
        "game:countdown",
        GAME_STARTED,
        "game:ended",
        "question:started",
        "question:locked",
        "question:results",
        "question:awaiting_next",
        "question:timeout",
        "answer:received",
        "host:question",
        "host:answer-received",
        "host:player-progress",
        "player:finished",
        "player:eliminated",
        "leaderboard:updated",
        "leaderboard:host"
    )
}
