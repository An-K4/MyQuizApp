package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.model.GameConfigConstraint
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.ShowLeaderboard

data class BooleanSettingUiState(
    val value: Boolean,
    val editable: Boolean
)

data class NumberSettingUiState(
    val value: String,
    val min: Int?,
    val max: Int?,
    val nullable: Boolean,
    val note: String?,
    val editable: Boolean
)

data class ChoiceSettingUiState(
    val value: String,
    val options: List<String>,
    val editable: Boolean
)

/** Typed form của presentation; không chứa dotted path hay JSON transport type. */
data class RoomConfigForm(
    val perQuestionSeconds: NumberSettingUiState,
    val autoAdvance: BooleanSettingUiState,
    val totalMatchSeconds: NumberSettingUiState,
    val maxPlayers: NumberSettingUiState,
    val allowLateJoin: BooleanSettingUiState,
    val allowGuests: BooleanSettingUiState,
    val showCorrectAnswer: BooleanSettingUiState,
    val showLeaderboard: ChoiceSettingUiState,
    val lives: NumberSettingUiState,
    val allowAnswerLate: BooleanSettingUiState,
    val shuffleQuestions: BooleanSettingUiState,
    val shuffleOptions: BooleanSettingUiState,
    val showHint: BooleanSettingUiState,
    val reviewMode: BooleanSettingUiState,
    val speedBonus: BooleanSettingUiState,
    val negativeMarking: BooleanSettingUiState
) {
    fun updateBoolean(key: GameConfigKey, value: Boolean): RoomConfigForm = when (key) {
        GameConfigKey.AUTO_ADVANCE -> copy(autoAdvance = autoAdvance.copy(value = value))
        GameConfigKey.ALLOW_LATE_JOIN -> copy(allowLateJoin = allowLateJoin.copy(value = value))
        GameConfigKey.ALLOW_GUESTS -> copy(allowGuests = allowGuests.copy(value = value))
        GameConfigKey.SHOW_CORRECT_ANSWER -> copy(showCorrectAnswer = showCorrectAnswer.copy(value = value))
        GameConfigKey.ALLOW_ANSWER_LATE -> copy(allowAnswerLate = allowAnswerLate.copy(value = value))
        GameConfigKey.SHUFFLE_QUESTIONS -> copy(shuffleQuestions = shuffleQuestions.copy(value = value))
        GameConfigKey.SHUFFLE_OPTIONS -> copy(shuffleOptions = shuffleOptions.copy(value = value))
        GameConfigKey.SHOW_HINT -> copy(showHint = showHint.copy(value = value))
        GameConfigKey.REVIEW_MODE -> copy(reviewMode = reviewMode.copy(value = value))
        GameConfigKey.SPEED_BONUS -> copy(speedBonus = speedBonus.copy(value = value))
        GameConfigKey.NEGATIVE_MARKING -> copy(negativeMarking = negativeMarking.copy(value = value))
        else -> this
    }

    fun updateNumber(key: GameConfigKey, value: String): RoomConfigForm = when (key) {
        GameConfigKey.PER_QUESTION_SECONDS -> copy(perQuestionSeconds = perQuestionSeconds.copy(value = value))
        GameConfigKey.TOTAL_MATCH_SECONDS -> copy(totalMatchSeconds = totalMatchSeconds.copy(value = value))
        GameConfigKey.MAX_PLAYERS -> copy(maxPlayers = maxPlayers.copy(value = value))
        GameConfigKey.LIVES -> copy(lives = lives.copy(value = value))
        else -> this
    }

    fun updateChoice(key: GameConfigKey, value: String): RoomConfigForm = when (key) {
        GameConfigKey.SHOW_LEADERBOARD -> copy(showLeaderboard = showLeaderboard.copy(value = value))
        else -> this
    }

    fun values(): Map<GameConfigKey, GameConfigValue> = mapOf(
        GameConfigKey.PER_QUESTION_SECONDS to perQuestionSeconds.toDomainValue(),
        GameConfigKey.AUTO_ADVANCE to GameConfigValue.BooleanValue(autoAdvance.value),
        GameConfigKey.TOTAL_MATCH_SECONDS to totalMatchSeconds.toDomainValue(),
        GameConfigKey.MAX_PLAYERS to maxPlayers.toDomainValue(),
        GameConfigKey.ALLOW_LATE_JOIN to GameConfigValue.BooleanValue(allowLateJoin.value),
        GameConfigKey.ALLOW_GUESTS to GameConfigValue.BooleanValue(allowGuests.value),
        GameConfigKey.SHOW_CORRECT_ANSWER to GameConfigValue.BooleanValue(showCorrectAnswer.value),
        GameConfigKey.SHOW_LEADERBOARD to GameConfigValue.ChoiceValue(showLeaderboard.value),
        GameConfigKey.LIVES to lives.toDomainValue(),
        GameConfigKey.ALLOW_ANSWER_LATE to GameConfigValue.BooleanValue(allowAnswerLate.value),
        GameConfigKey.SHUFFLE_QUESTIONS to GameConfigValue.BooleanValue(shuffleQuestions.value),
        GameConfigKey.SHUFFLE_OPTIONS to GameConfigValue.BooleanValue(shuffleOptions.value),
        GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(showHint.value),
        GameConfigKey.REVIEW_MODE to GameConfigValue.BooleanValue(reviewMode.value),
        GameConfigKey.SPEED_BONUS to GameConfigValue.BooleanValue(speedBonus.value),
        GameConfigKey.NEGATIVE_MARKING to GameConfigValue.BooleanValue(negativeMarking.value)
    )

    fun invalidKeys(descriptor: GameModeDescriptor): Set<GameConfigKey> {
        val currentValues = values()
        return descriptor.editable.mapNotNull { (key, spec) ->
            val invalid = when (val constraint = spec.constraint) {
                GameConfigConstraint.BooleanConstraint -> false
                is GameConfigConstraint.ChoiceConstraint ->
                    (currentValues[key] as? GameConfigValue.ChoiceValue)?.value !in constraint.values
                is GameConfigConstraint.NumberConstraint -> {
                    val input = numberSetting(key).value.trim()
                    val number = input.toIntOrNull()
                    val min = constraint.min
                    val max = constraint.max
                    when {
                        input.isEmpty() -> !constraint.nullable
                        number == null -> true
                        min != null && number < min -> true
                        max != null && number > max -> true
                        else -> false
                    }
                }
            }
            key.takeIf { invalid }
        }.toSet()
    }

    private fun numberSetting(key: GameConfigKey): NumberSettingUiState = when (key) {
        GameConfigKey.PER_QUESTION_SECONDS -> perQuestionSeconds
        GameConfigKey.TOTAL_MATCH_SECONDS -> totalMatchSeconds
        GameConfigKey.MAX_PLAYERS -> maxPlayers
        GameConfigKey.LIVES -> lives
        else -> error("$key is not a number setting")
    }

    companion object {
        fun fromDescriptor(descriptor: GameModeDescriptor): RoomConfigForm {
            val defaults = descriptor.defaultConfig
            return RoomConfigForm(
                perQuestionSeconds = descriptor.numberSetting(
                    GameConfigKey.PER_QUESTION_SECONDS,
                    defaults.timing.perQuestionSeconds
                ),
                autoAdvance = descriptor.booleanSetting(GameConfigKey.AUTO_ADVANCE, defaults.timing.autoAdvance),
                totalMatchSeconds = descriptor.numberSetting(
                    GameConfigKey.TOTAL_MATCH_SECONDS,
                    defaults.timing.totalMatchSeconds
                ),
                maxPlayers = descriptor.numberSetting(GameConfigKey.MAX_PLAYERS, defaults.lobby.maxPlayers),
                allowLateJoin = descriptor.booleanSetting(GameConfigKey.ALLOW_LATE_JOIN, defaults.lobby.allowLateJoin),
                allowGuests = descriptor.booleanSetting(GameConfigKey.ALLOW_GUESTS, defaults.lobby.allowGuests),
                showCorrectAnswer = descriptor.booleanSetting(
                    GameConfigKey.SHOW_CORRECT_ANSWER,
                    defaults.flow.showCorrectAnswer
                ),
                showLeaderboard = descriptor.choiceSetting(
                    GameConfigKey.SHOW_LEADERBOARD,
                    when (defaults.flow.showLeaderboard) {
                        ShowLeaderboard.NEVER -> "never"
                        ShowLeaderboard.BETWEEN_QUESTIONS -> "between_questions"
                        ShowLeaderboard.END_ONLY -> "end_only"
                    }
                ),
                lives = descriptor.numberSetting(GameConfigKey.LIVES, defaults.flow.lives),
                allowAnswerLate = descriptor.booleanSetting(
                    GameConfigKey.ALLOW_ANSWER_LATE,
                    defaults.flow.allowAnswerLate
                ),
                shuffleQuestions = descriptor.booleanSetting(
                    GameConfigKey.SHUFFLE_QUESTIONS,
                    defaults.flow.shuffleQuestions
                ),
                shuffleOptions = descriptor.booleanSetting(
                    GameConfigKey.SHUFFLE_OPTIONS,
                    defaults.flow.shuffleOptions
                ),
                showHint = descriptor.booleanSetting(GameConfigKey.SHOW_HINT, defaults.flow.showHint),
                reviewMode = descriptor.booleanSetting(GameConfigKey.REVIEW_MODE, defaults.flow.reviewMode),
                speedBonus = descriptor.booleanSetting(GameConfigKey.SPEED_BONUS, defaults.scoring.speedBonus),
                negativeMarking = descriptor.booleanSetting(
                    GameConfigKey.NEGATIVE_MARKING,
                    defaults.scoring.negativeMarking
                )
            )
        }
    }
}

private fun NumberSettingUiState.toDomainValue(): GameConfigValue.NumberValue =
    GameConfigValue.NumberValue(value.trim().toIntOrNull())

private fun GameModeDescriptor.booleanSetting(
    key: GameConfigKey,
    fallback: Boolean
): BooleanSettingUiState {
    val value = editable[key]?.defaultValue ?: locked[key] ?: GameConfigValue.BooleanValue(fallback)
    return BooleanSettingUiState(
        value = (value as? GameConfigValue.BooleanValue)?.value ?: fallback,
        editable = key in editable
    )
}

private fun GameModeDescriptor.numberSetting(
    key: GameConfigKey,
    fallback: Int?
): NumberSettingUiState {
    val spec = editable[key]
    val value = spec?.defaultValue ?: locked[key] ?: GameConfigValue.NumberValue(fallback)
    val constraint = spec?.constraint as? GameConfigConstraint.NumberConstraint
    return NumberSettingUiState(
        value = (value as? GameConfigValue.NumberValue)?.value?.toString().orEmpty(),
        min = constraint?.min,
        max = constraint?.max,
        nullable = constraint?.nullable ?: true,
        note = constraint?.note,
        editable = spec != null
    )
}

private fun GameModeDescriptor.choiceSetting(
    key: GameConfigKey,
    fallback: String
): ChoiceSettingUiState {
    val spec = editable[key]
    val value = spec?.defaultValue ?: locked[key] ?: GameConfigValue.ChoiceValue(fallback)
    val constraint = spec?.constraint as? GameConfigConstraint.ChoiceConstraint
    return ChoiceSettingUiState(
        value = (value as? GameConfigValue.ChoiceValue)?.value ?: fallback,
        options = constraint?.values.orEmpty(),
        editable = spec != null
    )
}
