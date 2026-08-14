package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameSession(
    val id: Long,
    val quizSnapshotId: Long,
    val sessionName: String,
    val sessionCode: String,          // mã join 6 ký tự
    val sessionHost: Long,            // user id của host
    val totalPlayers: Int,
    val totalQuestions: Int,
    val sessionStatus: SessionStatus,
    val gameMode: GameMode,
    val config: GameConfig,
    val currentQuestionIndex: Int,
    val currentPhase: String,         // server giữ dạng string — app chỉ đọc, không parse enum
    val phaseEndsAt: String? = null,
    val quizId: Long? = null
)

@Serializable
enum class GameMode {
    @SerialName("classic") CLASSIC,
    @SerialName("solo") SOLO,
    @SerialName("survival") SURVIVAL,
    @SerialName("marathon") MARATHON,
    @SerialName("practice") PRACTICE
}

@Serializable
enum class SessionStatus {
    @SerialName("lobby") LOBBY,
    @SerialName("active") ACTIVE,
    @SerialName("paused") PAUSED,
    @SerialName("finished") FINISHED,
    @SerialName("cancelled") CANCELLED
}