package android.kma.myquizzapp.core.ui.gameconfig

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

class GameModeConfigPreviewProvider : PreviewParameterProvider<GameMode> {
    override val values = GameMode.entries.asSequence()
}

/** The shared editor also used in Host Lobby; fixtures do not represent backend constraints. */
@Preview(name = "Shared lobby config Light", showBackground = true, widthDp = 390, heightDp = 700)
@Preview(name = "Shared lobby config Dark", showBackground = true, widthDp = 390, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameModeConfigEditorPreview(
    @PreviewParameter(GameModeConfigPreviewProvider::class) mode: GameMode,
) {
    val toggle = BooleanSettingUiState(value = false, editable = true)
    val form = RoomConfigForm(
        perQuestionSeconds = NumberSettingUiState("", 0, 600, true, null, true),
        autoAdvance = toggle.copy(value = true),
        totalMatchSeconds = NumberSettingUiState("600", 1, 3600, true, null, true),
        maxPlayers = NumberSettingUiState("100", 1, 200, false, null, true),
        allowLateJoin = toggle,
        allowGuests = toggle.copy(value = true),
        showCorrectAnswer = toggle.copy(value = true, editable = mode != GameMode.MARATHON),
        showLeaderboard = ChoiceSettingUiState("end_only", listOf("never", "end_only"), true),
        lives = NumberSettingUiState("3", 1, 10, true, null, true),
        allowAnswerLate = toggle,
        shuffleQuestions = toggle,
        shuffleOptions = toggle,
        showHint = toggle,
        reviewMode = toggle.copy(value = true),
        speedBonus = toggle.copy(value = true),
        negativeMarking = toggle,
    )
    MyQuizAppTheme {
        Surface(color = ComponentColors.surface) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                GameModeConfigEditor(mode, form, emptySet(), true, { _, _ -> }, { _, _ -> }, { _, _ -> })
            }
        }
    }
}
