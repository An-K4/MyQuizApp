package android.kma.myquizzapp.core.common.model

/** Đáp án typed ở domain; chỉ tầng network mới chuyển thành JSON socket. */
sealed interface PlayerAnswer {
    data class SingleChoice(val optionId: String) : PlayerAnswer
    data class MultipleSelect(val optionIds: List<String>) : PlayerAnswer
    data class Text(val value: String) : PlayerAnswer
}

/** ACK thật của `question:answer`.
 *
 * Với host-paced, các field kết quả cá nhân đều null: server chỉ xác nhận đã nhận
 * đáp án. Self-paced có thể reveal chúng khi cấu hình cho phép.
 */
data class AnswerAck(
    val accepted: Boolean,
    val isLate: Boolean = false,
    val lives: Int? = null,
    val eliminated: Boolean = false,
    val serverTime: String? = null,
    val isCorrect: Boolean? = null,
    val scoreEarned: Int? = null,
    val totalScore: Int? = null,
    val streak: Int? = null,
    val correctAnswers: List<String> = emptyList()
)

/** `question:started` bản công khai dành cho player. */
data class PlayerQuestionStarted(
    val question: PublicQuestion,
    val timeLimitSeconds: Int? = null,
    val endsAt: String? = null,
    val matchEndsAt: String? = null,
    val allowAnswerLate: Boolean = false,
    val remainingSeconds: Int? = null,
    val lives: Int? = null
)

/** Một câu đã được server ghi nhận trong `game:state.player.answered_questions`. */
data class AnsweredQuestionSnapshot(
    val questionId: Long,
    val questionIndex: Int,
    val answerKeys: List<String> = emptyList(),
    val isLate: Boolean = false,
    val answeredAt: String? = null
)

/** Phần player của `game:state`; dùng để giải quyết reconnect/ACK không chắc chắn. */
data class PlayerStateSnapshot(
    val id: Long,
    val playerName: String,
    val status: String,
    val lives: Int? = null,
    val currentQuestionIndex: Int = 0,
    val answeredQuestions: List<AnsweredQuestionSnapshot> = emptyList()
) {
    fun answerFor(index: Int): AnsweredQuestionSnapshot? =
        answeredQuestions.firstOrNull { it.questionIndex == index }
}
