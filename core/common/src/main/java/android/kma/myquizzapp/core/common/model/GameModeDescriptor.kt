package android.kma.myquizzapp.core.common.model

/** Khóa cấu hình typed; wire dotted-path chỉ được ánh xạ trong core:network. */
enum class GameConfigKey {
    PER_QUESTION_SECONDS,
    AUTO_ADVANCE,
    TOTAL_MATCH_SECONDS,
    MAX_PLAYERS,
    ALLOW_LATE_JOIN,
    ALLOW_GUESTS,
    SHOW_CORRECT_ANSWER,
    SHOW_LEADERBOARD,
    LIVES,
    ALLOW_ANSWER_LATE,
    SHUFFLE_QUESTIONS,
    SHUFFLE_OPTIONS,
    SHOW_HINT,
    REVIEW_MODE,
    SPEED_BONUS,
    NEGATIVE_MARKING
}

sealed interface GameConfigValue {
    data class BooleanValue(val value: Boolean) : GameConfigValue
    data class NumberValue(val value: Int?) : GameConfigValue
    data class ChoiceValue(val value: String) : GameConfigValue
}

sealed interface GameConfigConstraint {
    data object BooleanConstraint : GameConfigConstraint

    data class NumberConstraint(
        val min: Int? = null,
        val max: Int? = null,
        val nullable: Boolean = false,
        val note: String? = null
    ) : GameConfigConstraint

    data class ChoiceConstraint(val values: List<String>) : GameConfigConstraint
}

data class GameConfigFieldSpec(
    val key: GameConfigKey,
    val constraint: GameConfigConstraint,
    val defaultValue: GameConfigValue
)

data class GameModeDescriptor(
    val mode: GameMode,
    val pacing: Pacing,
    val scored: Boolean,
    val defaultConfig: GameConfig,
    val editable: Map<GameConfigKey, GameConfigFieldSpec>,
    val locked: Map<GameConfigKey, GameConfigValue>
)

data class CreateGameSessionParams(
    val quizId: Long,
    val sessionName: String,
    val mode: GameMode,
    val configPatch: Map<GameConfigKey, GameConfigValue>
)

data class IgnoredGameConfigField(
    val rawPath: String,
    val reason: IgnoredGameConfigReason
)

enum class IgnoredGameConfigReason { UNKNOWN, LOCKED, INVALID }

data class CreateGameSessionResult(
    val session: GameSession,
    val ignored: List<IgnoredGameConfigField>
)
