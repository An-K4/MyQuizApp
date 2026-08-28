package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigConstraint
import android.kma.myquizzapp.core.common.model.GameConfigFieldSpec
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameSession
import android.kma.myquizzapp.core.common.model.IgnoredGameConfigField
import android.kma.myquizzapp.core.common.model.IgnoredGameConfigReason
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.SessionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class GameModesResponseDto(val gameModes: List<GameModeDescriptorDto>)

@Serializable
data class GameModeDescriptorDto(
    val mode: GameMode,
    val pacing: Pacing,
    val scored: Boolean,
    val defaultConfig: GameConfig,
    val editable: Map<String, GameConfigFieldSpecDto>,
    val locked: Map<String, JsonElement>
) {
    fun toDomain(): GameModeDescriptor {
        val editableFields = editable.mapNotNull { (path, dto) ->
            val key = path.toConfigKey() ?: return@mapNotNull null
            dto.toDomain(key)?.let { key to it }
        }.toMap()
        val lockedFields = locked.mapNotNull { (path, value) ->
            val key = path.toConfigKey() ?: return@mapNotNull null
            value.toDomainValue(key)?.let { key to it }
        }.toMap()
        // Backend currently reports allowAnswerLate in both collections; locked always wins.
        return GameModeDescriptor(
            mode,
            pacing,
            scored,
            defaultConfig,
            editableFields.filterKeys { it !in lockedFields },
            lockedFields
        )
    }
}

@Serializable
data class GameConfigFieldSpecDto(
    val kind: GameConfigFieldKindDto,
    val values: List<String> = emptyList(),
    val min: Int? = null,
    val max: Int? = null,
    val nullable: Boolean = false,
    val note: String? = null,
    val default: JsonElement = JsonNull
) {
    fun toDomain(key: GameConfigKey): GameConfigFieldSpec? {
        val constraint = when (kind) {
            GameConfigFieldKindDto.BOOLEAN -> GameConfigConstraint.BooleanConstraint
            GameConfigFieldKindDto.ENUM -> GameConfigConstraint.ChoiceConstraint(values)
            GameConfigFieldKindDto.NUMBER -> GameConfigConstraint.NumberConstraint(min, max, nullable, note)
        }
        val defaultValue = default.toDomainValue(key) ?: return null
        return GameConfigFieldSpec(key, constraint, defaultValue)
    }
}

@Serializable
enum class GameConfigFieldKindDto {
    @SerialName("boolean") BOOLEAN,
    @SerialName("enum") ENUM,
    @SerialName("number") NUMBER
}

@Serializable
data class CreateGameRequestDto(
    @SerialName("quiz_id") val quizId: Long,
    @SerialName("session_name") val sessionName: String,
    val mode: GameMode,
    val config: JsonObject? = null
)

fun CreateGameSessionParams.toRequestDto(): CreateGameRequestDto {
    val config = configPatch.toWireJsonObject()
    return CreateGameRequestDto(
        quizId = quizId,
        sessionName = sessionName,
        mode = mode,
        config = config.takeIf { it.isNotEmpty() }
    )
}

@Serializable
data class CreateGameResponseDto(
    val data: CreateGameDataDto,
    val ignored: List<IgnoredGameConfigFieldDto> = emptyList()
) {
    fun toDomain() = CreateGameSessionResult(
        session = data.session.toDomain(),
        ignored = ignored.map { it.toDomain() }
    )
}

@Serializable
data class CreateGameDataDto(val session: GameSessionDto)

@Serializable
data class GameSessionDto(
    val id: Long,
    @SerialName("quiz_snapshot_id") val quizSnapshotId: Long,
    @SerialName("session_name") val sessionName: String,
    @SerialName("session_code") val sessionCode: String,
    @SerialName("session_host") val sessionHost: Long,
    @SerialName("total_players") val totalPlayers: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("session_status") val sessionStatus: SessionStatus,
    @SerialName("game_mode") val gameMode: GameMode,
    val config: GameConfig,
    @SerialName("current_question_index") val currentQuestionIndex: Int,
    @SerialName("current_phase") val currentPhase: String,
    @SerialName("phase_ends_at") val phaseEndsAt: String? = null,
    @SerialName("quiz_id") val quizId: Long? = null
) {
    fun toDomain() = GameSession(
        id = id,
        quizSnapshotId = quizSnapshotId,
        sessionName = sessionName,
        sessionCode = sessionCode,
        sessionHost = sessionHost,
        totalPlayers = totalPlayers,
        totalQuestions = totalQuestions,
        sessionStatus = sessionStatus,
        gameMode = gameMode,
        config = config,
        currentQuestionIndex = currentQuestionIndex,
        currentPhase = currentPhase,
        phaseEndsAt = phaseEndsAt,
        quizId = quizId
    )
}

@Serializable
data class IgnoredGameConfigFieldDto(
    val path: String,
    val value: JsonElement,
    val reason: IgnoredGameConfigReasonDto
) {
    fun toDomain() = IgnoredGameConfigField(
        rawPath = path,
        reason = when (reason) {
            IgnoredGameConfigReasonDto.UNKNOWN -> IgnoredGameConfigReason.UNKNOWN
            IgnoredGameConfigReasonDto.LOCKED -> IgnoredGameConfigReason.LOCKED
            IgnoredGameConfigReasonDto.INVALID -> IgnoredGameConfigReason.INVALID
        }
    )
}

@Serializable
enum class IgnoredGameConfigReasonDto {
    @SerialName("unknown") UNKNOWN,
    @SerialName("locked") LOCKED,
    @SerialName("invalid") INVALID
}

@Serializable
data class HostTokenResponseDto(val hostToken: HostTokenDto)

@Serializable
data class HostTokenDto(val socketToken: String)

private fun String.toConfigKey(): GameConfigKey? = when (this) {
    "timing.perQuestionSeconds" -> GameConfigKey.PER_QUESTION_SECONDS
    "timing.autoAdvance" -> GameConfigKey.AUTO_ADVANCE
    "timing.totalMatchSeconds" -> GameConfigKey.TOTAL_MATCH_SECONDS
    "lobby.maxPlayers" -> GameConfigKey.MAX_PLAYERS
    "lobby.allowLateJoin" -> GameConfigKey.ALLOW_LATE_JOIN
    "lobby.allowGuests" -> GameConfigKey.ALLOW_GUESTS
    "flow.showCorrectAnswer" -> GameConfigKey.SHOW_CORRECT_ANSWER
    "flow.showLeaderboard" -> GameConfigKey.SHOW_LEADERBOARD
    "flow.lives" -> GameConfigKey.LIVES
    "flow.allowAnswerLate" -> GameConfigKey.ALLOW_ANSWER_LATE
    "flow.shuffleQuestions" -> GameConfigKey.SHUFFLE_QUESTIONS
    "flow.shuffleOptions" -> GameConfigKey.SHUFFLE_OPTIONS
    "flow.showHint" -> GameConfigKey.SHOW_HINT
    "flow.reviewMode" -> GameConfigKey.REVIEW_MODE
    "scoring.speedBonus" -> GameConfigKey.SPEED_BONUS
    "scoring.negativeMarking" -> GameConfigKey.NEGATIVE_MARKING
    else -> null
}

private fun GameConfigKey.toWirePath(): String = when (this) {
    GameConfigKey.PER_QUESTION_SECONDS -> "timing.perQuestionSeconds"
    GameConfigKey.AUTO_ADVANCE -> "timing.autoAdvance"
    GameConfigKey.TOTAL_MATCH_SECONDS -> "timing.totalMatchSeconds"
    GameConfigKey.MAX_PLAYERS -> "lobby.maxPlayers"
    GameConfigKey.ALLOW_LATE_JOIN -> "lobby.allowLateJoin"
    GameConfigKey.ALLOW_GUESTS -> "lobby.allowGuests"
    GameConfigKey.SHOW_CORRECT_ANSWER -> "flow.showCorrectAnswer"
    GameConfigKey.SHOW_LEADERBOARD -> "flow.showLeaderboard"
    GameConfigKey.LIVES -> "flow.lives"
    GameConfigKey.ALLOW_ANSWER_LATE -> "flow.allowAnswerLate"
    GameConfigKey.SHUFFLE_QUESTIONS -> "flow.shuffleQuestions"
    GameConfigKey.SHUFFLE_OPTIONS -> "flow.shuffleOptions"
    GameConfigKey.SHOW_HINT -> "flow.showHint"
    GameConfigKey.REVIEW_MODE -> "flow.reviewMode"
    GameConfigKey.SPEED_BONUS -> "scoring.speedBonus"
    GameConfigKey.NEGATIVE_MARKING -> "scoring.negativeMarking"
}

private fun JsonElement.toDomainValue(key: GameConfigKey): GameConfigValue? = when (key) {
    GameConfigKey.SHOW_LEADERBOARD ->
        (this as? JsonPrimitive)?.content?.let { GameConfigValue.ChoiceValue(it) }

    GameConfigKey.PER_QUESTION_SECONDS,
    GameConfigKey.TOTAL_MATCH_SECONDS,
    GameConfigKey.MAX_PLAYERS,
    GameConfigKey.LIVES -> when (this) {
        JsonNull -> GameConfigValue.NumberValue(null)
        is JsonPrimitive -> intOrNull?.let { GameConfigValue.NumberValue(it) }
        else -> null
    }

    else -> (this as? JsonPrimitive)?.booleanOrNull?.let { GameConfigValue.BooleanValue(it) }
}

private fun Map<GameConfigKey, GameConfigValue>.toWireJsonObject(): JsonObject {
    val root = linkedMapOf<String, Any>()
    forEach { (key, value) -> putDottedPath(root, key.toWirePath(), value.toJsonElement()) }
    return root.toJsonObject()
}

private fun GameConfigValue.toJsonElement(): JsonElement = when (this) {
    is GameConfigValue.BooleanValue -> JsonPrimitive(value)
    is GameConfigValue.NumberValue -> value?.let { JsonPrimitive(it) } ?: JsonNull
    is GameConfigValue.ChoiceValue -> JsonPrimitive(value)
}

private fun putDottedPath(target: MutableMap<String, Any>, path: String, value: JsonElement) {
    val keys = path.split('.')
    var cursor = target
    keys.dropLast(1).forEach { key ->
        @Suppress("UNCHECKED_CAST")
        val child = cursor.getOrPut(key) { linkedMapOf<String, Any>() } as MutableMap<String, Any>
        cursor = child
    }
    cursor[keys.last()] = value
}

private fun Map<String, Any>.toJsonObject(): JsonObject = JsonObject(
    mapValues { (_, value) ->
        when (value) {
            is JsonElement -> value
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (value as Map<String, Any>).toJsonObject()
            }
            else -> error("Unsupported game config value: $value")
        }
    }
)
