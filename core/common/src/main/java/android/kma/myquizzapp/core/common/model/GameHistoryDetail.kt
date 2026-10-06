package android.kma.myquizzapp.core.common.model

/** Chi tiết một trận đã đóng, lấy từ snapshot server thay vì dữ liệu quiz hiện tại. */
data class GameHistorySummary(
    val sessionId: Long,
    val sessionName: String,
    val gameMode: GameMode,
    val sessionStatus: SessionStatus,
    val config: GameConfig,
    val totalPlayers: Int,
    val totalQuestions: Int,
    val finishedAt: String? = null,
    val hostName: String? = null,
    val hostAvatar: String? = null,
    val quiz: GameHistoryQuiz? = null,
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val perQuestion: List<QuestionStat> = emptyList(),
    val viewer: GameHistoryViewer,
    /** Thành tích riêng vẫn được giữ khi danh sách xếp hạng bị ẩn khỏi player. */
    val viewerResult: LeaderboardRow? = null,
    /** Defense-in-depth: backend hiện vẫn serialize bảng dù player bị cấu hình ẩn. */
    val leaderboardHiddenByConfig: Boolean = false
)

data class GameHistoryQuiz(
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val image: String? = null,
    val category: String? = null,
    val language: String? = null
)

data class GameHistoryViewer(
    val isHost: Boolean,
    val playerId: Long? = null
)
