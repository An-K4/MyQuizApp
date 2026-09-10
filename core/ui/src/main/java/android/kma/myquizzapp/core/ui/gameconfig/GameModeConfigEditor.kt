package android.kma.myquizzapp.core.ui.gameconfig

import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Form sửa cấu hình theo từng chế độ chơi.
 *
 * Dùng ở cả CreateRoom (trước khi tạo phòng) và HostLobby (sửa phòng đang mở).
 * Vì vậy callback ở đây là ba hàm trung tính theo [GameConfigKey], KHÔNG phải
 * intent của một màn cụ thể — nếu để component phụ thuộc CreateRoomIntent thì
 * màn thứ hai không thể dùng lại, đúng vấn đề của bản N17.
 *
 * Giá trị số trả về dạng String, chưa lọc — việc lọc ký tự và giới hạn độ dài
 * thuộc ViewModel, để hai màn có thể có luật khác nhau mà component không biết.
 */
@Composable
fun GameModeConfigEditor(
    mode: GameMode,
    form: RoomConfigForm,
    invalidKeys: Set<GameConfigKey>,
    enabled: Boolean,
    onToggle: (GameConfigKey, Boolean) -> Unit,
    onNumberChange: (GameConfigKey, String) -> Unit,
    onChoiceChange: (GameConfigKey, String) -> Unit
) {
    val scope = EditorScope(form, invalidKeys, enabled, onToggle, onNumberChange, onChoiceChange)
    when (mode) {
        GameMode.CLASSIC -> ClassicModeEditor(scope)
        GameMode.SOLO -> SoloModeEditor(scope)
        GameMode.SURVIVAL -> SurvivalModeEditor(scope)
        GameMode.MARATHON -> MarathonModeEditor(scope)
        GameMode.PRACTICE -> PracticeModeEditor(scope)
    }
}

/**
 * Gói sáu tham số luôn đi cùng nhau lại một chỗ.
 *
 * Trước đây mỗi editor con nhận 4–5 tham số rồi chuyển tiếp nguyên xuống hàm
 * con; thêm một callback là phải sửa hàng chục chỗ. Đóng thành một đối tượng
 * tránh hẳn việc đó.
 */
private class EditorScope(
    val form: RoomConfigForm,
    val invalidKeys: Set<GameConfigKey>,
    val enabled: Boolean,
    val onToggle: (GameConfigKey, Boolean) -> Unit,
    val onNumberChange: (GameConfigKey, String) -> Unit,
    val onChoiceChange: (GameConfigKey, String) -> Unit
)

@Composable
private fun ClassicModeEditor(scope: EditorScope) {
    ModeSettingsColumn {
        SettingsSection("Thời gian") {
            PerQuestionSecondsField(scope)
            BooleanSetting(scope, "Tự chuyển câu", GameConfigKey.AUTO_ADVANCE, scope.form.autoAdvance)
        }
        LobbySettings(scope, includeLateJoin = true)
        SettingsSection("Luồng câu hỏi") {
            BooleanSetting(scope, "Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, scope.form.showCorrectAnswer)
            LeaderboardSetting(scope)
            CommonStudySettings(scope)
        }
        ScoringSettings(scope, includeNegativeMarking = true)
    }
}

@Composable
private fun SoloModeEditor(scope: EditorScope) {
    ModeSettingsColumn {
        SettingsSection("Nhịp tự do") {
            PerQuestionSecondsField(scope)
            BooleanSetting(scope, "Tự chuyển câu", GameConfigKey.AUTO_ADVANCE, scope.form.autoAdvance)
        }
        LobbySettings(scope, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting(scope, "Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, scope.form.showCorrectAnswer)
            LeaderboardSetting(scope)
            CommonStudySettings(scope)
        }
        ScoringSettings(scope, includeNegativeMarking = true)
    }
}

@Composable
private fun SurvivalModeEditor(scope: EditorScope) {
    ModeSettingsColumn {
        SettingsSection("Luật sinh tồn") {
            NumberSetting(scope, "Số mạng", GameConfigKey.LIVES, scope.form.lives)
            PerQuestionSecondsField(scope)
        }
        LobbySettings(scope, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting(scope, "Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, scope.form.showCorrectAnswer)
            LeaderboardSetting(scope)
            CommonStudySettings(scope)
        }
        ScoringSettings(scope, includeNegativeMarking = false)
    }
}

@Composable
private fun MarathonModeEditor(scope: EditorScope) {
    ModeSettingsColumn {
        SettingsSection("Ngân sách thời gian") {
            NumberSetting(
                scope = scope,
                title = "Tổng thời gian trận",
                key = GameConfigKey.TOTAL_MATCH_SECONDS,
                state = scope.form.totalMatchSeconds,
                description = "Thời gian tối đa cho toàn bộ trận"
            )
            PerQuestionSecondsField(scope)
            NumberSetting(scope, "Số mạng", GameConfigKey.LIVES, scope.form.lives)
        }
        LobbySettings(scope, includeLateJoin = true)
        SettingsSection("Hiển thị") {
            BooleanSetting(scope, "Hiện đáp án đúng", GameConfigKey.SHOW_CORRECT_ANSWER, scope.form.showCorrectAnswer)
            LeaderboardSetting(scope)
            CommonStudySettings(scope)
        }
        ScoringSettings(scope, includeNegativeMarking = true)
    }
}

@Composable
private fun PracticeModeEditor(scope: EditorScope) {
    ModeSettingsColumn {
        LobbySettings(scope, includeLateJoin = false)
        SettingsSection("Hỗ trợ luyện tập") {
            CommonStudySettings(scope)
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
private fun LobbySettings(scope: EditorScope, includeLateJoin: Boolean) {
    SettingsSection("Phòng chờ") {
        NumberSetting(scope, "Số người chơi tối đa", GameConfigKey.MAX_PLAYERS, scope.form.maxPlayers)
        if (includeLateJoin) {
            BooleanSetting(scope, "Cho phép vào muộn", GameConfigKey.ALLOW_LATE_JOIN, scope.form.allowLateJoin)
        }
        BooleanSetting(scope, "Cho phép khách", GameConfigKey.ALLOW_GUESTS, scope.form.allowGuests)
    }
}

@Composable
private fun CommonStudySettings(scope: EditorScope) {
    BooleanSetting(scope, "Trộn câu hỏi", GameConfigKey.SHUFFLE_QUESTIONS, scope.form.shuffleQuestions)
    BooleanSetting(scope, "Trộn lựa chọn", GameConfigKey.SHUFFLE_OPTIONS, scope.form.shuffleOptions)
    BooleanSetting(scope, "Hiện gợi ý", GameConfigKey.SHOW_HINT, scope.form.showHint)
    BooleanSetting(scope, "Cho phép xem lại", GameConfigKey.REVIEW_MODE, scope.form.reviewMode)
}

@Composable
private fun ScoringSettings(scope: EditorScope, includeNegativeMarking: Boolean) {
    SettingsSection("Tính điểm") {
        BooleanSetting(scope, "Thưởng tốc độ", GameConfigKey.SPEED_BONUS, scope.form.speedBonus)
        if (includeNegativeMarking) {
            BooleanSetting(scope, "Trừ điểm khi sai", GameConfigKey.NEGATIVE_MARKING, scope.form.negativeMarking)
        }
    }
}

@Composable
private fun PerQuestionSecondsField(scope: EditorScope) {
    NumberSetting(
        scope = scope,
        title = "Thời gian mỗi câu",
        key = GameConfigKey.PER_QUESTION_SECONDS,
        state = scope.form.perQuestionSeconds,
        description = "Để trống: dùng thời gian của câu hỏi \u2022 0: không giới hạn"
    )
}

@Composable
private fun BooleanSetting(
    scope: EditorScope,
    title: String,
    key: GameConfigKey,
    state: BooleanSettingUiState
) {
    BooleanSettingRow(
        title = title,
        state = state,
        enabled = scope.enabled,
        onCheckedChange = { scope.onToggle(key, it) }
    )
}

@Composable
private fun NumberSetting(
    scope: EditorScope,
    title: String,
    key: GameConfigKey,
    state: NumberSettingUiState,
    description: String? = null
) {
    NumberSettingField(
        title = title,
        state = state,
        isError = key in scope.invalidKeys,
        enabled = scope.enabled,
        onValueChange = { scope.onNumberChange(key, it) },
        description = description
    )
}

@Composable
private fun LeaderboardSetting(scope: EditorScope) {
    ChoiceSettingField(
        title = "Hiện bảng xếp hạng",
        state = scope.form.showLeaderboard,
        enabled = scope.enabled,
        optionLabel = ::leaderboardOptionLabel,
        onValueChange = { scope.onChoiceChange(GameConfigKey.SHOW_LEADERBOARD, it) }
    )
}
