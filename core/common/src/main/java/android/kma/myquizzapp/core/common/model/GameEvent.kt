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

    /**
     * `game:started` — server đã chuyển session sang active và broadcast cho cả room.
     *
     * Đây là XÁC NHẬN DUY NHẤT rằng `game:start` thành công: `onStart` ở backend
     * không có ack, lỗi thì về qua [Failed] với event = `game:start`. Vì vậy host
     * phải neo việc điều hướng vào event này, không phải vào lúc bấm nút.
     *
     * @param config config đầy đủ lúc bắt đầu (đã normalize) — có thể khác với giá
     *   trị host vừa chỉnh nếu backend tự điều chỉnh.
     */
    data class GameStarted(
        val mode: GameMode,
        val config: GameConfig,
        val totalQuestions: Int,
        val serverTime: String? = null
    ) : GameEvent

    /**
     * `game:countdown` — đồng hồ trước câu đầu tiên.
     *
     * Đến cả khi host vừa kết nối vào giữa lúc đang đếm, nên phải dùng
     * [GameCountdown.startsAt] (mốc tuyệt đối) chứ không tự đếm từ
     * [GameCountdown.seconds] tại thời điểm nhận.
     */
    data class Countdown(
        val countdown: GameCountdown,
        val serverTime: String? = null
    ) : GameEvent

    /**
     * `host:question` — câu hỏi kèm ĐÁP ÁN ĐÚNG, chỉ host room nhận.
     *
     * Host cũng nhận `question:started` (bản đã cắt đáp án) vì ở trong room chung.
     * Màn host phải bỏ qua bản đó, nếu không sẽ render trùng và mất khoá đáp án.
     */
    data class HostQuestionReceived(
        val hostQuestion: HostQuestion,
        val serverTime: String? = null
    ) : GameEvent

    /** `question:locked` — câu hỏi đã đóng, ngay sau đó sẽ có [QuestionResultsReceived]. */
    data class QuestionLocked(
        val index: Int,
        val reason: QuestionLockReason,
        val serverTime: String? = null
    ) : GameEvent

    /** `question:results` — thời điểm đầu tiên được phép công bố đáp án. */
    data class QuestionResultsReceived(
        val results: QuestionResults,
        val serverTime: String? = null
    ) : GameEvent

    /**
     * `host:answer-received` — một người chơi cụ thể vừa trả lời.
     *
     * Chỉ host room nhận; bản dùng chung `answer:received` bị backend loại trừ host.
     */
    data class HostAnswerReceivedEvent(
        val answer: HostAnswerReceived,
        val serverTime: String? = null
    ) : GameEvent

    /** `leaderboard:host` — bảng theo dõi đầy đủ, luôn được gửi cho host. */
    data class HostLeaderboardUpdated(
        val leaderboard: HostLeaderboard,
        val serverTime: String? = null
    ) : GameEvent

    /**
     * `game:state` — snapshot toàn phòng, dùng để dựng lại màn sau reconnect.
     *
     * BẪY: [GameSnapshot.question] là bản công khai nên KHÔNG có đáp án đúng, và
     * backend không phát lại `host:question`. Reconnect vào giữa một câu đang mở
     * thì host mất khoá đáp án cho tới câu kế tiếp.
     */
    data class StateSnapshot(
        val snapshot: GameSnapshot,
        val serverTime: String? = null
    ) : GameEvent

    /**
     * `game:ended` — trận kết thúc, payload đã mang sẵn bảng xếp hạng cuối và
     * thống kê từng câu nên KHÔNG cần gọi thêm REST `GET /games/:id/results`.
     */
    data class GameEndedEvent(
        val ended: GameEnded,
        val serverTime: String? = null
    ) : GameEvent

    /** `player:eliminated` — một người chơi hết mạng (chỉ mode sinh tồn). */
    data class PlayerEliminated(
        val player: EliminatedPlayer,
        val serverTime: String? = null
    ) : GameEvent

    /** Event backend gửi mà tầng hiện tại chưa xử lý — chỉ để log, không phải lỗi. */
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
