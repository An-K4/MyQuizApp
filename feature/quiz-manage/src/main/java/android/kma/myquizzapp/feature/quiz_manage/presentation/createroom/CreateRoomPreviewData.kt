package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.ui.gameconfig.RoomConfigForm
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/** Synthetic UI fixtures only: not server defaults, validation limits or API evidence. */
class CreateRoomPreviewProvider : PreviewParameterProvider<CreateRoomUiState> {
    override val values: Sequence<CreateRoomUiState>
        get() {
            val classic = createRoomPreviewState(GameMode.CLASSIC)
            return GameMode.entries.asSequence().map(::createRoomPreviewState) + sequenceOf(
                CreateRoomUiState(),
                CreateRoomUiState(isLoadingModes = false, errorMessage = "Không thể kết nối máy chủ. Hãy thử lại."),
                classic.copy(
                    sessionName = "A",
                    modeConfig = classic.modeConfig?.updateNumber(GameConfigKey.MAX_PLAYERS, "0"),
                    invalidConfigKeys = setOf(GameConfigKey.MAX_PLAYERS),
                    validationErrors = listOf("Tên phòng phải từ 2 đến 100 ký tự.", "Hãy kiểm tra lại các cấu hình đang được đánh dấu."),
                ),
                classic.copy(isSubmitting = true),
                classic.copy(
                    pendingSession = GameSession(
                        id = 1L,
                        quizSnapshotId = 1L,
                        sessionName = classic.sessionName,
                        sessionCode = "ABC234",
                        sessionHost = 1L,
                        totalPlayers = 0,
                        totalQuestions = 10,
                        sessionStatus = SessionStatus.LOBBY,
                        gameMode = GameMode.CLASSIC,
                        config = GameConfig(),
                        currentQuestionIndex = 0,
                        currentPhase = "lobby",
                    ),
                    errorMessage = "Phòng ABC234 đã được tạo, nhưng chưa lấy được quyền host. Hãy thử lại; ứng dụng sẽ không tạo phòng thứ hai.",
                    ignoredFields = listOf(IgnoredGameConfigField("example", IgnoredGameConfigReason.LOCKED)),
                ),
            )
        }
}

internal fun createRoomPreviewState(mode: GameMode): CreateRoomUiState {
    val descriptors = GameMode.entries.map(::previewDescriptor)
    val descriptor = descriptors.first { it.mode == mode }
    return CreateRoomUiState(
        isLoadingModes = false,
        modes = descriptors,
        selectedMode = mode,
        modeConfig = RoomConfigForm.fromDescriptor(descriptor),
    )
}

private fun previewDescriptor(mode: GameMode): GameModeDescriptor {
    val selfPaced = mode != GameMode.CLASSIC
    val config = GameConfig(
        flow = GameConfig.Flow(
            pacing = if (selfPaced) Pacing.SELF else Pacing.HOST,
            showLeaderboard = if (selfPaced) ShowLeaderboard.END_ONLY else ShowLeaderboard.BETWEEN_QUESTIONS,
            lives = if (mode == GameMode.SURVIVAL) 3 else null,
        ),
        timing = GameConfig.Timing(totalMatchSeconds = if (mode == GameMode.MARATHON) 600 else null),
    )
    fun number(key: GameConfigKey, value: Int?, min: Int, max: Int, nullable: Boolean = false) =
        key to GameConfigFieldSpec(key, GameConfigConstraint.NumberConstraint(min, max, nullable), GameConfigValue.NumberValue(value))
    val editable = mutableMapOf(
        number(GameConfigKey.PER_QUESTION_SECONDS, null, 0, 600, true),
        number(GameConfigKey.MAX_PLAYERS, 100, 1, 200),
        number(GameConfigKey.LIVES, config.flow.lives, 1, 10, true),
        number(GameConfigKey.TOTAL_MATCH_SECONDS, config.timing.totalMatchSeconds, 1, 3600, true),
    )
    val booleanValues = mapOf(
        GameConfigKey.AUTO_ADVANCE to true,
        GameConfigKey.ALLOW_LATE_JOIN to false,
        GameConfigKey.ALLOW_GUESTS to true,
        GameConfigKey.SHOW_CORRECT_ANSWER to true,
        GameConfigKey.ALLOW_ANSWER_LATE to false,
        GameConfigKey.SHUFFLE_QUESTIONS to false,
        GameConfigKey.SHUFFLE_OPTIONS to false,
        GameConfigKey.SHOW_HINT to false,
        GameConfigKey.REVIEW_MODE to true,
        GameConfigKey.SPEED_BONUS to true,
        GameConfigKey.NEGATIVE_MARKING to false,
    )
    booleanValues.forEach { (key, value) ->
        editable[key] = GameConfigFieldSpec(key, GameConfigConstraint.BooleanConstraint, GameConfigValue.BooleanValue(value))
    }
    val leaderboardOptions = if (selfPaced) listOf("never", "end_only") else listOf("never", "between_questions", "end_only")
    editable[GameConfigKey.SHOW_LEADERBOARD] = GameConfigFieldSpec(
        GameConfigKey.SHOW_LEADERBOARD,
        GameConfigConstraint.ChoiceConstraint(leaderboardOptions),
        GameConfigValue.ChoiceValue(if (selfPaced) "end_only" else "between_questions"),
    )
    return GameModeDescriptor(
        mode = mode,
        pacing = config.flow.pacing,
        scored = mode != GameMode.PRACTICE,
        defaultConfig = config,
        editable = editable,
        locked = emptyMap(),
    )
}
