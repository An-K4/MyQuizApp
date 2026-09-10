package android.kma.myquizzapp.core.ui.gameconfig

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigConstraint
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.ShowLeaderboard

/*
 * Form cấu hình phòng, dùng chung cho hai màn:
 *  - CreateRoom (feature:quiz-manage): dựng từ default của descriptor.
 *  - HostLobby (feature:lobby): dựng từ config THẬT của phòng đang mở.
 *
 * Vì sao nằm ở core:ui chứ không ở feature: N17 đặt nó trong quiz-manage vì lúc
 * đó chỉ CreateRoom cần. Sang N20 host phải sửa lại config ngay trong lobby, và
 * quy ước kiến trúc cấm feature phụ thuộc feature — nên thứ dùng chung phải hạ
 * xuống core:ui.
 */

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
        /**
         * Form cho phòng CHƯA tồn tại: mọi giá trị lấy từ default của descriptor.
         * Dùng ở CreateRoom.
         */
        fun fromDescriptor(descriptor: GameModeDescriptor): RoomConfigForm =
            descriptor.buildForm { key ->
                descriptor.editable[key]?.defaultValue
                    ?: descriptor.locked[key]
                    ?: descriptor.defaultConfig.valueOf(key)
            }

        /**
         * Form cho phòng ĐANG tồn tại: giá trị lấy từ [config] thật của phòng, còn
         * quyền sửa và ràng buộc vẫn lấy từ [descriptor].
         *
         * Không được suy ra editable/locked từ config — descriptor là nguồn duy
         * nhất cho việc đó (backend `describeModeConfig`), và locked luôn thắng.
         */
        fun fromConfig(descriptor: GameModeDescriptor, config: GameConfig): RoomConfigForm =
            descriptor.buildForm { key -> config.valueOf(key) }
    }
}

/** Đọc một khóa typed ra khỏi [GameConfig]. Chỗ duy nhất biết key nào nằm ở nhóm nào. */
fun GameConfig.valueOf(key: GameConfigKey): GameConfigValue = when (key) {
    GameConfigKey.PER_QUESTION_SECONDS -> GameConfigValue.NumberValue(timing.perQuestionSeconds)
    GameConfigKey.AUTO_ADVANCE -> GameConfigValue.BooleanValue(timing.autoAdvance)
    GameConfigKey.TOTAL_MATCH_SECONDS -> GameConfigValue.NumberValue(timing.totalMatchSeconds)
    GameConfigKey.MAX_PLAYERS -> GameConfigValue.NumberValue(lobby.maxPlayers)
    GameConfigKey.ALLOW_LATE_JOIN -> GameConfigValue.BooleanValue(lobby.allowLateJoin)
    GameConfigKey.ALLOW_GUESTS -> GameConfigValue.BooleanValue(lobby.allowGuests)
    GameConfigKey.SHOW_CORRECT_ANSWER -> GameConfigValue.BooleanValue(flow.showCorrectAnswer)
    GameConfigKey.SHOW_LEADERBOARD -> GameConfigValue.ChoiceValue(flow.showLeaderboard.wireValue())
    GameConfigKey.LIVES -> GameConfigValue.NumberValue(flow.lives)
    GameConfigKey.ALLOW_ANSWER_LATE -> GameConfigValue.BooleanValue(flow.allowAnswerLate)
    GameConfigKey.SHUFFLE_QUESTIONS -> GameConfigValue.BooleanValue(flow.shuffleQuestions)
    GameConfigKey.SHUFFLE_OPTIONS -> GameConfigValue.BooleanValue(flow.shuffleOptions)
    GameConfigKey.SHOW_HINT -> GameConfigValue.BooleanValue(flow.showHint)
    GameConfigKey.REVIEW_MODE -> GameConfigValue.BooleanValue(flow.reviewMode)
    GameConfigKey.SPEED_BONUS -> GameConfigValue.BooleanValue(scoring.speedBonus)
    GameConfigKey.NEGATIVE_MARKING -> GameConfigValue.BooleanValue(scoring.negativeMarking)
}

/**
 * Chuỗi wire của showLeaderboard.
 *
 * Constraint từ backend là danh sách chuỗi (`never|between_questions|end_only`),
 * nên form phải so sánh cùng dạng chuỗi đó, không dùng tên enum Kotlin.
 */
private fun ShowLeaderboard.wireValue(): String = when (this) {
    ShowLeaderboard.NEVER -> "never"
    ShowLeaderboard.BETWEEN_QUESTIONS -> "between_questions"
    ShowLeaderboard.END_ONLY -> "end_only"
}

private fun NumberSettingUiState.toDomainValue(): GameConfigValue.NumberValue =
    GameConfigValue.NumberValue(value.trim().toIntOrNull())

/**
 * Dựng form; [resolve] quyết định LẤY GIÁ TRỊ Ở ĐÂU, còn quyền sửa và ràng buộc
 * luôn lấy từ descriptor. Tách như vậy để fromDescriptor và fromConfig không phải
 * nhân đôi danh sách 16 field.
 */
private fun GameModeDescriptor.buildForm(
    resolve: (GameConfigKey) -> GameConfigValue
): RoomConfigForm = RoomConfigForm(
    perQuestionSeconds = numberSetting(GameConfigKey.PER_QUESTION_SECONDS, resolve),
    autoAdvance = booleanSetting(GameConfigKey.AUTO_ADVANCE, resolve),
    totalMatchSeconds = numberSetting(GameConfigKey.TOTAL_MATCH_SECONDS, resolve),
    maxPlayers = numberSetting(GameConfigKey.MAX_PLAYERS, resolve),
    allowLateJoin = booleanSetting(GameConfigKey.ALLOW_LATE_JOIN, resolve),
    allowGuests = booleanSetting(GameConfigKey.ALLOW_GUESTS, resolve),
    showCorrectAnswer = booleanSetting(GameConfigKey.SHOW_CORRECT_ANSWER, resolve),
    showLeaderboard = choiceSetting(GameConfigKey.SHOW_LEADERBOARD, resolve),
    lives = numberSetting(GameConfigKey.LIVES, resolve),
    allowAnswerLate = booleanSetting(GameConfigKey.ALLOW_ANSWER_LATE, resolve),
    shuffleQuestions = booleanSetting(GameConfigKey.SHUFFLE_QUESTIONS, resolve),
    shuffleOptions = booleanSetting(GameConfigKey.SHUFFLE_OPTIONS, resolve),
    showHint = booleanSetting(GameConfigKey.SHOW_HINT, resolve),
    reviewMode = booleanSetting(GameConfigKey.REVIEW_MODE, resolve),
    speedBonus = booleanSetting(GameConfigKey.SPEED_BONUS, resolve),
    negativeMarking = booleanSetting(GameConfigKey.NEGATIVE_MARKING, resolve)
)

private fun GameModeDescriptor.booleanSetting(
    key: GameConfigKey,
    resolve: (GameConfigKey) -> GameConfigValue
): BooleanSettingUiState {
    val fallback = (defaultConfig.valueOf(key) as? GameConfigValue.BooleanValue)?.value ?: false
    return BooleanSettingUiState(
        value = (resolve(key) as? GameConfigValue.BooleanValue)?.value ?: fallback,
        editable = key in editable
    )
}

private fun GameModeDescriptor.numberSetting(
    key: GameConfigKey,
    resolve: (GameConfigKey) -> GameConfigValue
): NumberSettingUiState {
    val spec = editable[key]
    val constraint = spec?.constraint as? GameConfigConstraint.NumberConstraint
    return NumberSettingUiState(
        value = (resolve(key) as? GameConfigValue.NumberValue)?.value?.toString().orEmpty(),
        min = constraint?.min,
        max = constraint?.max,
        nullable = constraint?.nullable ?: true,
        note = constraint?.note,
        editable = spec != null
    )
}

private fun GameModeDescriptor.choiceSetting(
    key: GameConfigKey,
    resolve: (GameConfigKey) -> GameConfigValue
): ChoiceSettingUiState {
    val spec = editable[key]
    val constraint = spec?.constraint as? GameConfigConstraint.ChoiceConstraint
    val fallback = (defaultConfig.valueOf(key) as? GameConfigValue.ChoiceValue)?.value.orEmpty()
    return ChoiceSettingUiState(
        value = (resolve(key) as? GameConfigValue.ChoiceValue)?.value ?: fallback,
        options = constraint?.values.orEmpty(),
        editable = spec != null
    )
}
