package android.kma.myquizzapp.core.common.model

/** Bảng xem lại câu trả lời của chính Player sau khi trận đã kết thúc. */
data class GameReview(
    val playerScore: Int,
    val correctAnswersCount: Int,
    val totalQuestions: Int,
    val answeredCount: Int,
    val items: List<GameReviewItem>,
    val serverTime: String? = null
)

/** Một lượt trả lời trong answer sheet; Marathon có thể lặp cùng câu ở index lớn hơn tổng câu. */
data class GameReviewItem(
    val questionIndex: Int,
    val questionId: Long? = null,
    val questionText: String? = null,
    val questionImage: String? = null,
    val answerOptions: List<PublicAnswerOption> = emptyList(),
    val explanation: String? = null,
    val answered: Boolean,
    val yourAnswers: List<String> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val isCorrect: Boolean,
    val isLate: Boolean,
    val scoreEarned: Int,
    val timeTakenSeconds: Double? = null
)
