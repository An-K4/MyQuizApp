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

    /**
     * `game:next` — MỘT event, HAI Ý NGHĨA tùy `current_phase`:
     * đang `question_active` thì chốt câu sớm, đang `showing_results` thì sang câu
     * mới, đang `countdown` thì bị 409. Ngoài ra backend từ chối hẳn
     * (`GAME_ADVANCE_NOT_ALLOWED`) khi `timing.autoAdvance = true`.
     */
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

    /** Snapshot toàn phòng. Trả lời cho `lobby:join` và `player:sync`. */
    const val GAME_STATE = "game:state"

    const val GAME_COUNTDOWN = "game:countdown"
    const val GAME_ENDED = "game:ended"

    /**
     * Bản câu hỏi CHO CẢ PHÒNG, đã cắt đáp án.
     *
     * Host cũng nhận event này vì vẫn nằm trong room chung — màn host phải Bỏ QUA
     * nó và chỉ dùng [HOST_QUESTION], nếu không bản không có đáp án sẽ ghi đè.
     */
    const val QUESTION_STARTED = "question:started"

    const val QUESTION_LOCKED = "question:locked"
    const val QUESTION_RESULTS = "question:results"
    const val QUESTION_AWAITING_NEXT = "question:awaiting_next"
    const val QUESTION_TIMEOUT = "question:timeout"

    /** Bản đếm cho người chơi. Host KHÔNG nhận (backend `.except(hostRoom)`). */
    const val ANSWER_RECEIVED = "answer:received"

    /** Câu hỏi kèm đáp án đúng, chỉ phát vào host room. */
    const val HOST_QUESTION = "host:question"

    const val HOST_ANSWER_RECEIVED = "host:answer-received"

    /** Chỉ có ở phòng nhịp tự do — ngoài phạm vi N21 (host-paced). */
    const val HOST_PLAYER_PROGRESS = "host:player-progress"

    const val PLAYER_FINISHED = "player:finished"
    const val PLAYER_ELIMINATED = "player:eliminated"

    /** Bảng xếp hạng cho người chơi. Host KHÔNG nhận. */
    const val LEADERBOARD_UPDATED = "leaderboard:updated"

    /** Bảng theo dõi đầy đủ của host, luôn được gửi bất kể `flow.showLeaderboard`. */
    const val LEADERBOARD_HOST = "leaderboard:host"

    /**
     * Toàn bộ event server có thể gửi trên `/game`.
     *
     * N21 map thêm phần gameplay của host; những event còn lại (self-paced,
     * người chơi) vẫn được đăng ký listener để phát ra GameEvent.Unhandled — có
     * log thấy backend đang gửi gì khi debug, thay vì im lặng bỏ qua.
     */
    val SERVER_EVENTS = listOf(
        LOBBY_UPDATED,
        ERROR,
        GAME_STATE,
        GAME_COUNTDOWN,
        GAME_STARTED,
        GAME_ENDED,
        QUESTION_STARTED,
        QUESTION_LOCKED,
        QUESTION_RESULTS,
        QUESTION_AWAITING_NEXT,
        QUESTION_TIMEOUT,
        ANSWER_RECEIVED,
        HOST_QUESTION,
        HOST_ANSWER_RECEIVED,
        HOST_PLAYER_PROGRESS,
        PLAYER_FINISHED,
        PLAYER_ELIMINATED,
        LEADERBOARD_UPDATED,
        LEADERBOARD_HOST
    )
}
