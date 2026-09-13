package android.kma.myquizzapp.core.common.model

/**
 * Model domain cho giai đoạn CHƠI của host (N21) — bám đúng schema realtime của
 * backend ở `docs/components/socket.doc.ts`.
 *
 * Hai quy ước xuyên suốt file này, cả hai đều là hệ quả của cách backend thiết kế:
 *
 * 1. Mọi mốc thời gian giữ nguyên chuỗi ISO-8601 của server ([HostQuestion.endsAt],
 *    [QuestionResults.nextQuestionAt], [GameCountdown.startsAt]). KHÔNG quy về mili
 *    giây theo đồng hồ máy ngay tại tầng model: mọi payload đều kèm `serverTime` và
 *    client phải tính lệch đồng hồ rồi mới đếm ngược, nếu tin đồng hồ máy thì máy
 *    lệch giờ sẽ hiện sai toàn bộ.
 *
 * 2. Đáp án đúng và id lựa chọn đều là [String]. Backend khai báo id lựa chọn là
 *    kiểu tự do ("can be 0, so never test it for truthiness") và `correct_answer`
 *    có thể là một id, một mảng id, hoặc chuỗi tự luận. Quy hết về chuỗi khiến việc
 *    so khớp chỉ có một luật duy nhất, giống hệt cách web đang làm (trim + lowercase).
 */

/**
 * Giai đoạn của trận, lấy từ `current_phase`.
 *
 * [UNKNOWN] tồn tại để backend thêm phase mới không làm sập màn hình đang chạy —
 * cùng triết lý với việc giữ `status` của người chơi ở dạng String.
 */
enum class GamePhase {
    COUNTDOWN,
    QUESTION_ACTIVE,
    QUESTION_LOCKED,
    SHOWING_RESULTS,
    FINISHED,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): GamePhase = when (raw) {
            "countdown" -> COUNTDOWN
            "question_active" -> QUESTION_ACTIVE
            "question_locked" -> QUESTION_LOCKED
            "showing_results" -> SHOWING_RESULTS
            "finished" -> FINISHED
            else -> UNKNOWN
        }
    }
}

/**
 * Một lựa chọn trả lời trong lúc CHƠI. [id] là thứ backend dùng để chấm, [text] chỉ để hiển thị.
 *
 * Khác hẳn [AnswerOption] của luồng soạn quiz (`Quiz.kt`): bên đó `id` là khoá chính
 * kiểu [Long] trong CSDL, còn ở đây `id` là chuỗi tự do do backend realtime khai
 * ("can be 0, so never test it for truthiness"). Gộp hai model này
 * làm một sẽ bắt một trong hai phía phải ép kiểu sai lệch.
 */
data class PublicAnswerOption(
    val id: String,
    val text: String? = null
)

/**
 * Câu hỏi ở dạng công khai (`PublicQuestion`) — bản đã bị cắt `correct_answer`.
 *
 * Thứ tự [answerOptions] là thứ tự server gửi: khi `flow.shuffleOptions` bật, việc
 * trộn đã làm ở server và việc chấm dựa trên [PublicAnswerOption.id], nên client TUYỆT ĐỐI
 * không được sắp xếp lại.
 */
data class PublicQuestion(
    val index: Int,
    val total: Int,
    val id: Long,
    val questionType: String,
    val questionText: String,
    val questionImage: String? = null,
    val questionHint: String? = null,
    val answerOptions: List<PublicAnswerOption> = emptyList()
)

/**
 * `host:question` — câu hỏi kèm ĐÁP ÁN ĐÚNG, chỉ phát vào host room.
 *
 * Máy người chơi nhận cùng câu hỏi qua `question:started` nhưng đã bị cắt đáp án.
 * Host cũng nằm trong room chung nên vẫn nhận `question:started`; phải bỏ qua bản
 * đó, nếu không sẽ render hai lần và bản sau (không có đáp án) ghi đè bản có đáp án.
 *
 * @param correctAnswers rỗng khi backend không tiết lộ đáp án cho câu này.
 * @param timeLimitSeconds null khi câu hỏi không giới hạn thời gian.
 * @param endsAt null khi không có hạn — đừng đếm ngược từ [timeLimitSeconds].
 */
data class HostQuestion(
    val question: PublicQuestion,
    val correctAnswers: List<String> = emptyList(),
    val timeLimitSeconds: Int? = null,
    val endsAt: String? = null,
    val totalQuestions: Int = 0
)

/** `game:countdown` — đồng hồ trước câu đầu tiên. */
data class GameCountdown(
    val seconds: Int,
    val startsAt: String? = null
)

/** Lý do câu hỏi bị đóng (`question:locked`). */
enum class QuestionLockReason {
    /** Hết thời gian. */
    TIME_UP,

    /** Mọi người chơi còn hoạt động đều đã trả lời. */
    ALL_ANSWERED,

    UNKNOWN;

    companion object {
        fun fromRaw(raw: String?): QuestionLockReason = when (raw) {
            "time_up" -> TIME_UP
            "all_answered" -> ALL_ANSWERED
            else -> UNKNOWN
        }
    }
}

/**
 * Phân bố câu trả lời của một câu hỏi.
 *
 * CẢNH BÁO: [distribution] CHỈ có dữ liệu khi `flow.showCorrectAnswer = true`.
 * Trường hợp còn lại backend chỉ gửi `{ total }` — và bóp cả với host, không riêng
 * người chơi. Muốn luôn có số liệu thì phải tự đếm từ `host:answer-received`.
 *
 * @param distribution khóa là [PublicAnswerOption.id] dạng chuỗi, giá trị là số lượt chọn.
 */
data class AnswerStats(
    val total: Int = 0,
    val distribution: Map<String, Int> = emptyMap()
)

/**
 * `question:results` — thời điểm đầu tiên được phép công bố đáp án.
 *
 * @param nextQuestionAt null khi host tự bấm chuyển câu (`autoAdvance = false`).
 */
data class QuestionResults(
    val index: Int,
    val questionId: Long? = null,
    val correctAnswers: List<String> = emptyList(),
    val stats: AnswerStats = AnswerStats(),
    val nextQuestionAt: String? = null
)

/**
 * `host:answer-received` — một người chơi cụ thể vừa trả lời.
 *
 * Chỉ host room nhận được event này: bản dùng chung `answer:received` bị backend
 * loại trừ host (`.except(hostRoom)`) và cũng không kèm tên người chơi hay đúng/sai.
 *
 * @param index số hiệu câu hỏi — phải đối chiếu với câu đang mở rồi mới cộng dồn,
 *   vì gói tin của câu trước có thể về muộn.
 */
data class HostAnswerReceived(
    val index: Int,
    val answered: Int,
    val activePlayers: Int,
    val playerId: Long,
    val playerName: String,
    val isCorrect: Boolean
)

/**
 * Một dòng trong bảng theo dõi của host (`HostLeaderboardRow`).
 *
 * @param currentQuestionIndex chỉ có nghĩa khi phòng chạy nhịp tự do (self-paced).
 */
data class HostLeaderboardRow(
    val rank: Int,
    val id: Long,
    val playerName: String,
    val playerScore: Int,
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unansweredCount: Int = 0,
    val totalQuestions: Int = 0,
    val currentQuestionIndex: Int = 0,
    val streak: Int = 0,
    val lives: Int? = null,
    val status: String = "connected"
)

/**
 * `leaderboard:host` — bảng theo dõi đầy đủ.
 *
 * Luôn được gửi bất kể `flow.showLeaderboard` đặt gì: host luôn thấy mọi thứ.
 */
data class HostLeaderboard(
    val rows: List<HostLeaderboardRow> = emptyList(),
    val totalQuestions: Int = 0,
    val answeredTotal: Int = 0
)

/** Một dòng bảng xếp hạng công khai (`LeaderboardRow`). */
data class LeaderboardRow(
    val rank: Int,
    val id: Long,
    val playerName: String,
    val playerScore: Int
)

/** Thống kê một câu hỏi trong tổng kết trận (`QuestionStat`). */
data class QuestionStat(
    val questionId: Long,
    val questionIndex: Int,
    val answerCount: Int,
    val correctCount: Int
)

/**
 * `game:state` — ảnh chụp toàn bộ phòng, dùng để dựng lại màn hình sau reconnect.
 *
 * BẪY quan trọng cho host: [question] dùng bản `publicQuestion` nên KHÔNG có đáp án
 * đúng, và backend không phát lại `host:question` khi host join lại. Reconnect vào
 * giữa một câu đang mở thì host mất khoá đáp án cho tới câu kế tiếp — UI phải nói rõ
 * điều đó thay vì hiện một ô trống trông như lỗi.
 */
data class GameSnapshot(
    val sessionStatus: SessionStatus? = null,
    val phase: GamePhase = GamePhase.UNKNOWN,
    val mode: GameMode? = null,
    val config: GameConfig? = null,
    val index: Int = 0,
    val totalQuestions: Int = 0,
    val question: PublicQuestion? = null,
    val countdownStartsAt: String? = null,
    val endsAt: String? = null,
    val remainingSeconds: Int? = null,
    val leaderboard: List<LeaderboardRow> = emptyList()
)

/**
 * `game:ended` — trận kết thúc, kèm sẵn toàn bộ số liệu tổng kết.
 *
 * Vì payload này đã mang [leaderboard] đầy đủ và [perQuestion], màn kết quả KHÔNG
 * cần gọi thêm REST `GET /games/:id/results`. Bản của host luôn có bảng đầy đủ.
 *
 * @param reviewEnabled phản chiếu `flow.reviewMode` — quyết định có được gọi
 *   `GET /games/:id/review` để xem lại từng câu hay không.
 */
data class GameEnded(
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val perQuestion: List<QuestionStat> = emptyList(),
    val reviewEnabled: Boolean = false
)

/** `player:eliminated` — một người chơi hết mạng (chỉ mode sinh tồn). */
data class EliminatedPlayer(
    val id: Long,
    val playerName: String
)
