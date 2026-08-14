package android.kma.myquizzapp.core.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val version: Int = 1,
    val scoring: Scoring = Scoring(),
    val timing: Timing = Timing(),
    val lobby: LobbyConfig = LobbyConfig(),
    val flow: Flow = Flow()
) {
    @Serializable
    data class Scoring(
        val basePoints: Int = 1000,
        val speedBonus: Boolean = true,
        val streak: Streak = Streak(),
        val negativeMarking: Boolean = false,
        val latePenaltyRatio: Double = 0.9
    ) {
        @Serializable
        data class Streak(
            val enabled: Boolean = false,
            val bonusPerStep: Int = 100,
            val max: Int = 500
        )
    }

    @Serializable
    data class Timing(
        val countdownSeconds: Int = 3,
        val perQuestionSeconds: Int? = null,  // null = dùng time_limit của từng câu
        val autoAdvance: Boolean = true,
        val showResultsSeconds: Int = 2,
        val totalMatchSeconds: Int? = null    // marathon dùng cái này
    )

    @Serializable
    data class LobbyConfig(
        val maxPlayers: Int = 100,
        val allowLateJoin: Boolean = false,
        val allowGuests: Boolean = true
    )

    @Serializable
    data class Flow(
        val pacing: Pacing = Pacing.HOST,
        val showCorrectAnswer: Boolean = true,
        val showLeaderboard: ShowLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        val lives: Int? = null,               // survival: số mạng; null = không giới hạn
        val allowAnswerLate: Boolean = false,
        val shuffleQuestions: Boolean = false,
        val shuffleOptions: Boolean = false,
        val showHint: Boolean = false,
        val reviewMode: Boolean = true
    )
}

@Serializable
enum class Pacing { @SerialName("host") HOST, @SerialName("self") SELF }

@Serializable
enum class ShowLeaderboard {
    @SerialName("never") NEVER,
    @SerialName("between_questions") BETWEEN_QUESTIONS,
    @SerialName("end_only") END_ONLY
}