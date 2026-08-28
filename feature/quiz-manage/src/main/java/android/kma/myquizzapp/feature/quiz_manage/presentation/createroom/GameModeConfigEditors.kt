package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.ui.components.SettingSwitchRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun GameModeConfigEditor(
    mode: GameMode,
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    when (mode) {
        GameMode.CLASSIC -> ClassicModeEditor(form, invalidKeys, enabled, onIntent)
        GameMode.SOLO -> SoloModeEditor(form, invalidKeys, enabled, onIntent)
        GameMode.SURVIVAL -> SurvivalModeEditor(form, invalidKeys, enabled, onIntent)
        GameMode.MARATHON -> MarathonModeEditor(form, invalidKeys, enabled, onIntent)
        GameMode.PRACTICE -> PracticeModeEditor(form, invalidKeys, enabled, onIntent)
    }
}

@Composable
private fun ClassicModeEditor(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ModeSettingsColumn {
        SettingsSection("Thời gian") {
            PerQuestionSecondsField(form, invalidKeys, enabled, onIntent)
            BooleanSetting(
                "Tự chuyển câu",
                GameConfigKey.AUTO_ADVANCE,
                form.autoAdvance,
                enabled,
                onIntent
            )
        }
        LobbySettings(form, invalidKeys, enabled, onIntent, includeLateJoin = true)
        SettingsSection("Luồng câu hỏi") {
            BooleanSetting("Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, form.showCorrectAnswer, enabled, onIntent)
            LeaderboardSetting(form, enabled, onIntent)
            CommonStudySettings(form, enabled, onIntent)
        }
        ScoringSettings(form, enabled, onIntent, includeNegativeMarking = true)
    }
}

@Composable
private fun SoloModeEditor(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ModeSettingsColumn {
        SettingsSection("Nhịp tự do") {
            PerQuestionSecondsField(form, invalidKeys, enabled, onIntent)
            BooleanSetting("Tự chuyển câu", GameConfigKey.AUTO_ADVANCE, form.autoAdvance, enabled, onIntent)
        }
        LobbySettings(form, invalidKeys, enabled, onIntent, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting("Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, form.showCorrectAnswer, enabled, onIntent)
            LeaderboardSetting(form, enabled, onIntent)
            CommonStudySettings(form, enabled, onIntent)
        }
        ScoringSettings(form, enabled, onIntent, includeNegativeMarking = true)
    }
}

@Composable
private fun SurvivalModeEditor(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ModeSettingsColumn {
        SettingsSection("Luật sinh tồn") {
            NumberSettingField(
                title = "Số mạng",
                state = form.lives,
                isError = GameConfigKey.LIVES in invalidKeys,
                enabled = enabled,
                onValueChange = { onIntent(CreateRoomIntent.NumberChanged(GameConfigKey.LIVES, it)) }
            )
            PerQuestionSecondsField(form, invalidKeys, enabled, onIntent)
        }
        LobbySettings(form, invalidKeys, enabled, onIntent, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting("Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, form.showCorrectAnswer, enabled, onIntent)
            LeaderboardSetting(form, enabled, onIntent)
            CommonStudySettings(form, enabled, onIntent)
        }
        ScoringSettings(form, enabled, onIntent, includeNegativeMarking = false)
    }
}

@Composable
private fun MarathonModeEditor(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ModeSettingsColumn {
        SettingsSection("Ngân sách thời gian") {
            NumberSettingField(
                title = "Tổng thời gian trận",
                state = form.totalMatchSeconds,
                isError = GameConfigKey.TOTAL_MATCH_SECONDS in invalidKeys,
                enabled = enabled,
                description = "Thời gian tối đa cho toàn bộ trận",
                onValueChange = {
                    onIntent(CreateRoomIntent.NumberChanged(GameConfigKey.TOTAL_MATCH_SECONDS, it))
                }
            )
            PerQuestionSecondsField(form, invalidKeys, enabled, onIntent)
            NumberSettingField(
                title = "Số mạng",
                state = form.lives,
                isError = GameConfigKey.LIVES in invalidKeys,
                enabled = enabled,
                onValueChange = { onIntent(CreateRoomIntent.NumberChanged(GameConfigKey.LIVES, it)) }
            )
        }
        LobbySettings(form, invalidKeys, enabled, onIntent, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting("Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, form.showCorrectAnswer, enabled, onIntent)
            LeaderboardSetting(form, enabled, onIntent)
            CommonStudySettings(form, enabled, onIntent)
        }
        ScoringSettings(form, enabled, onIntent, includeNegativeMarking = true)
    }
}

@Composable
private fun PracticeModeEditor(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ModeSettingsColumn {
        LobbySettings(form, invalidKeys, enabled, onIntent, includeLateJoin = false)
        SettingsSection("Hỗ trợ luyện tập") {
            BooleanSetting("Trộn câu hỏi", GameConfigKey.SHUFFLE_QUESTIONS, form.shuffleQuestions, enabled, onIntent)
            BooleanSetting("Trộn lựa chọn", GameConfigKey.SHUFFLE_OPTIONS, form.shuffleOptions, enabled, onIntent)
            BooleanSetting("Hiện gợi ý", GameConfigKey.SHOW_HINT, form.showHint, enabled, onIntent)
            BooleanSetting("Cho phép xem lại", GameConfigKey.REVIEW_MODE, form.reviewMode, enabled, onIntent)
        }
    }
}

@Composable
private fun ModeSettingsColumn(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun LobbySettings(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit,
    includeLateJoin: Boolean
) {
    SettingsSection("Phòng chờ") {
        NumberSettingField(
            title = "Số người chơi tối đa",
            state = form.maxPlayers,
            isError = GameConfigKey.MAX_PLAYERS in invalidKeys,
            enabled = enabled,
            onValueChange = { onIntent(CreateRoomIntent.NumberChanged(GameConfigKey.MAX_PLAYERS, it)) }
        )
        if (includeLateJoin) {
            BooleanSetting("Cho phép vào muộn", GameConfigKey.ALLOW_LATE_JOIN, form.allowLateJoin, enabled, onIntent)
        }
        BooleanSetting("Cho phép khách", GameConfigKey.ALLOW_GUESTS, form.allowGuests, enabled, onIntent)
    }
}

@Composable
private fun CommonStudySettings(
    form: RoomConfigForm,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    BooleanSetting("Trộn câu hỏi", GameConfigKey.SHUFFLE_QUESTIONS, form.shuffleQuestions, enabled, onIntent)
    BooleanSetting("Trộn lựa chọn", GameConfigKey.SHUFFLE_OPTIONS, form.shuffleOptions, enabled, onIntent)
    BooleanSetting("Hiện gợi ý", GameConfigKey.SHOW_HINT, form.showHint, enabled, onIntent)
    BooleanSetting("Cho phép xem lại", GameConfigKey.REVIEW_MODE, form.reviewMode, enabled, onIntent)
}

@Composable
private fun ScoringSettings(
    form: RoomConfigForm,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit,
    includeNegativeMarking: Boolean
) {
    SettingsSection("Tính điểm") {
        BooleanSetting("Thưởng tốc độ", GameConfigKey.SPEED_BONUS, form.speedBonus, enabled, onIntent)
        if (includeNegativeMarking) {
            BooleanSetting("Trừ điểm khi sai", GameConfigKey.NEGATIVE_MARKING, form.negativeMarking, enabled, onIntent)
        }
    }
}

@Composable
private fun PerQuestionSecondsField(
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    NumberSettingField(
        title = "Thời gian mỗi câu",
        state = form.perQuestionSeconds,
        isError = GameConfigKey.PER_QUESTION_SECONDS in invalidKeys,
        enabled = enabled,
        description = "Để trống: dùng thời gian của câu hỏi • 0: không giới hạn",
        onValueChange = {
            onIntent(CreateRoomIntent.NumberChanged(GameConfigKey.PER_QUESTION_SECONDS, it))
        }
    )
}

@Composable
private fun BooleanSetting(
    title: String,
    key: GameConfigKey,
    state: BooleanSettingUiState,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    SettingSwitchRow(
        title = title,
        checked = state.value,
        enabled = enabled && state.editable,
        onCheckedChange = { onIntent(CreateRoomIntent.ToggleChanged(key, it)) }
    )
}

@Composable
private fun NumberSettingField(
    title: String,
    state: NumberSettingUiState,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    description: String? = null
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = onValueChange,
        enabled = enabled && state.editable,
        isError = isError,
        label = { Text(title) },
        supportingText = {
            val range = listOfNotNull(state.min, state.max).joinToString("–")
            val support = listOfNotNull(
                range.takeIf { it.isNotBlank() },
                description,
                state.note?.takeUnless { description != null }
            ).joinToString(" • ")
            if (support.isNotBlank()) Text(support)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LeaderboardSetting(
    form: RoomConfigForm,
    enabled: Boolean,
    onIntent: (CreateRoomIntent) -> Unit
) {
    ChoiceSettingField(
        title = "Hiện bảng xếp hạng",
        state = form.showLeaderboard,
        enabled = enabled,
        onValueChange = {
            onIntent(CreateRoomIntent.ChoiceChanged(GameConfigKey.SHOW_LEADERBOARD, it))
        }
    )
}

@Composable
private fun ChoiceSettingField(
    title: String,
    state: ChoiceSettingUiState,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(title)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && state.editable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(leaderboardOptionLabel(state.value))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(leaderboardOptionLabel(option)) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    }
                )
            }
        }
    }
}

private fun leaderboardOptionLabel(value: String): String = when (value) {
    "never" -> "Không bao giờ"
    "between_questions" -> "Giữa các câu"
    "end_only" -> "Chỉ cuối trận"
    else -> value
}
