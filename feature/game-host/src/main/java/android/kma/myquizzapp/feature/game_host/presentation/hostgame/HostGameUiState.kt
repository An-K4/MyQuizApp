package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus

/**
 * Trạng thái kết nối socket của màn điều khiển trận.
 *
 * Khai báo lại ở đây thay vì dùng enum của feature:lobby: hai feature không được
 * phụ thuộc nhau (quy ước kiến trúc của dự án).
 */
enum class HostGameConnection { CONNECTING, CONNECTED, RECONNECTING }

/**
 * State của màn điều khiển trận (HOST, chỉ mode host-paced ở N21).
 *
 * Bốn quyết định đáng ghi lại:
 *
 * 1. Mọi mốc thời gian giữ dạng epoch millis ĐÃ HIỆU CHỊNH theo đồng hồ server
 *    ([serverOffsetMs]). Backend gửi `endsAt` tuyệt đối kèm `serverTime`, nên đúng
 *    là phải bù lệch rồi mới đếm; tin đồng hồ máy thì máy lệch giờ hiện sai hết.
 *
 * 2. [correctAnswers] có thể rỗng giữa một câu đang mở — [hasAnswerKey] phân biệt
 *    "không có đáp án" với "đáp án rỗng". Xảy ra khi host reconnect giữa câu:
 *    `game:state` dùng bản công khai và backend không phát lại `host:question`.
 *
 * 3. Nút chuyển câu CHỈ tồn tại khi [autoAdvance] = false. Backend từ chối `game:next`
 *    bằng 409 `GAME_ADVANCE_NOT_ALLOWED` khi phòng tự chuyển câu, mà classic mặc
 *    định `autoAdvance = true` — hiện nút đó là mời host bấm để nhận lỗi.
 *
 * 4. Đáp án đúng ẨN sau nút "Xem đáp án" ([isAnswerRevealed]) trong khi câu hỏi luôn
 *    hiện. Màn host hay được chiếu lên máy chiếu nên không được để đáp án lọt ra
 *    mặc định; nhưng ẩn cả câu hỏi như bản web thì host mất ngữ cảnh điều phối.
 */
data class HostGameUiState(
    val connection: HostGameConnection = HostGameConnection.CONNECTING,
    val sessionStatus: SessionStatus? = null,
    val phase: GamePhase = GamePhase.UNKNOWN,
    val mode: GameMode? = null,

    /** Lấy từ `config.timing.autoAdvance`. Mặc định true đúng như backend. */
    val autoAdvance: Boolean = true,

    val index: Int = 0,
    val totalQuestions: Int = 0,
    val question: PublicQuestion? = null,

    val correctAnswers: List<String> = emptyList(),
    val hasAnswerKey: Boolean = false,
    val isAnswerRevealed: Boolean = false,

    /** Hạn của câu đang mở, epoch millis theo đồng hồ server. Null = không giới hạn. */
    val deadlineEpochMs: Long? = null,

    /** Mốc câu đầu tiên nổ ra khi đang ở phase đếm ngược. */
    val countdownTargetEpochMs: Long? = null,

    val serverOffsetMs: Long = 0L,

    val answeredCount: Int = 0,
    val activePlayers: Int = 0,

    val leaderboard: HostLeaderboard = HostLeaderboard(),
    val lockReason: QuestionLockReason? = null,
    val results: QuestionResults? = null,
    val ended: GameEnded? = null,

    val isSendingCommand: Boolean = false,
    val isEndDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val notice: String? = null
) {

    val isFinished: Boolean
        get() = ended != null ||
            phase == GamePhase.FINISHED ||
            sessionStatus == SessionStatus.FINISHED ||
            sessionStatus == SessionStatus.CANCELLED

    val isPaused: Boolean get() = sessionStatus == SessionStatus.PAUSED

    val hasSnapshot: Boolean get() = question != null || ended != null

    private val isOnline: Boolean get() = connection == HostGameConnection.CONNECTED

    /** Nhãn đổi theo phase: đang mở thì chốt câu, đã đóng thì sang câu mới. */
    val advanceLabel: String
        get() = if (phase == GamePhase.QUESTION_ACTIVE) "Chốt câu" else "Câu tiếp"

    /** Chỉ render nút chuyển câu khi phòng KHÔNG tự chuyển — xem ghi chú (3). */
    val isManualAdvanceVisible: Boolean get() = !autoAdvance && !isFinished

    /**
     * `game:next` bị backend trả 409 khi đang đếm ngược, nên chặn sẵn ở client.
     */
    val canAdvance: Boolean
        get() = isManualAdvanceVisible && isOnline && !isSendingCommand && !isPaused &&
            (
                phase == GamePhase.QUESTION_ACTIVE ||
                    phase == GamePhase.QUESTION_LOCKED ||
                    phase == GamePhase.SHOWING_RESULTS
                )

    /** `game:pause` chỉ hợp lệ khi trận active, `game:resume` chỉ khi đang tạm dừng. */
    val canPauseOrResume: Boolean
        get() = isOnline && !isSendingCommand && !isFinished &&
            (sessionStatus == SessionStatus.ACTIVE || isPaused)

    val pauseLabel: String get() = if (isPaused) "Tiếp tục" else "Tạm dừng"

    /** `game:end` từ lobby trả GAME_NOT_STARTED, từ trận đã xong trả GAME_NOT_ACTIVE. */
    val canEndGame: Boolean
        get() = isOnline && !isSendingCommand && !isFinished &&
            sessionStatus != null && sessionStatus != SessionStatus.LOBBY

    /** Để UI biết có nên cho bấm "Xem đáp án" hay hiện lời giải thích vì sao không có. */
    val canRevealAnswer: Boolean get() = hasAnswerKey && question != null

    val questionNumberLabel: String
        get() {
            val total = if (totalQuestions > 0) totalQuestions else question?.total ?: 0
            return if (total > 0) "Câu ${index + 1}/$total" else "Câu ${index + 1}"
        }

    val answeredLabel: String get() = "Đã trả lời $answeredCount/$activePlayers"
}
