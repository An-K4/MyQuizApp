package android.kma.myquizzapp.core.common.model

/** Hai truy vấn lịch sử độc lập; cursor của role này không dùng được cho role kia. */
enum class GameHistoryRole(val apiValue: String) {
    PLAYED("played"),
    HOSTED("hosted")
}

/** Một phiên đã đóng lấy từ snapshot server, không phụ thuộc Room cache cục bộ. */
data class GameHistoryItem(
    val sessionId: Long,
    val sessionName: String,
    val gameMode: GameMode,
    val sessionStatus: SessionStatus,
    val totalPlayers: Int,
    val totalQuestions: Int,
    val endedAt: String,
    val quizId: Long?,
    val quizName: String?,
    val quizImage: String?,
    val hostName: String?,
    val hostAvatar: String?,
    val playerScore: Int?,
    val correctAnswersCount: Int?,
    val rank: Int?
)
