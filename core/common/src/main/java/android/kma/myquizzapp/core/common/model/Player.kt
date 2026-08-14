package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Player(
    val id: Long,
    val gameSessionId: Long,
    val playerId: Long? = null,          // null nếu là guest
    val playerGuestId: String? = null,   // uuid nếu là guest
    val playerName: String,
    val playerScore: Int,
    val correctAnswersCount: Int,
    val answeredQuestions: List<AnsweredQuestion> = emptyList(),
    val streak: Int = 0,
    val lives: Int? = null,              // survival
    val currentQuestionIndex: Int = 0,
    val status: String                   // 'active' | 'eliminated' | 'left'... server-managed string
)

@Serializable
data class AnsweredQuestion(
    val questionId: Long,
    val questionIndex: Int,
    val answer: JsonElement,             // đa kiểu: array option id, hoặc string — đúng bản chất backend
    val isCorrect: Boolean,
    val isLate: Boolean,
    val timeTaken: Double,               // giây, có phần thập phân
    val scoreEarned: Int,
    val answeredAt: String
)

@Serializable
data class LeaderboardEntry(
    val id: Long,
    val playerName: String,
    val playerScore: Int,
    val correctAnswersCount: Int,
    val streak: Int,
    val status: String
)

@Serializable
data class LobbyPlayer(
    val id: Long,
    val playerName: String,
    val playerScore: Int,
    val status: String
)
