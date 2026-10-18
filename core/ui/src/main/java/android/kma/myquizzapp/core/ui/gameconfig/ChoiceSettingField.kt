package android.kma.myquizzapp.core.ui.gameconfig

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Menu visibility belongs here; allowed values and callbacks still come from the caller. */
@Composable
fun ChoiceSettingField(
    title: String,
    state: ChoiceSettingUiState,
    enabled: Boolean,
    optionLabel: (String) -> String,
    onValueChange: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val interactive = enabled && state.editable
    LaunchedEffect(interactive) {
        if (!interactive) expanded = false
    }
    ChoiceSettingFieldContent(
        title = title,
        state = state,
        enabled = enabled,
        expanded = expanded && interactive,
        onExpandedChange = { expanded = it && interactive },
        optionLabel = optionLabel,
        onValueChange = onValueChange,
    )
}

/** Stateless rounded selector; never invent options or turn a multi-value choice into a switch. */
@Composable
fun ChoiceSettingFieldContent(
    title: String,
    state: ChoiceSettingUiState,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    optionLabel: (String) -> String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactive = enabled && state.editable
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ComponentColors.foreground.copy(alpha = if (interactive) 1f else 0.55f),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val menuWidth = maxWidth
            OutlinedButton(
                onClick = { onExpandedChange(true) },
                enabled = interactive,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .semantics { contentDescription = title },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ComponentColors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ComponentColors.surface,
                    contentColor = ComponentColors.foreground,
                    disabledContainerColor = ComponentColors.softSurface,
                    disabledContentColor = ComponentColors.muted,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(optionLabel(state.value), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                }
            }
            DropdownMenu(
                expanded = expanded && interactive,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.width(menuWidth),
            ) {
                state.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onExpandedChange(false)
                            if (interactive) onValueChange(option)
                        },
                    )
                }
            }
        }
    }
}

/** Vietnamese labels only; unknown wire values remain visible, not silently remapped. */
fun leaderboardOptionLabel(value: String): String = when (value) {
    "never" -> "Không bao giờ"
    "between_questions" -> "Giữa các câu"
    "end_only" -> "Chỉ cuối trận"
    else -> value
}

@Preview(name = "Leaderboard choice Light", showBackground = true, widthDp = 390)
@Preview(name = "Leaderboard choice Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChoiceSettingFieldPreview() {
    MyQuizAppTheme {
        Surface(color = ComponentColors.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val editable = ChoiceSettingUiState("between_questions", listOf("never", "between_questions", "end_only"), true)
                ChoiceSettingField("Hiện bảng xếp hạng", editable, true, ::leaderboardOptionLabel, {})
                ChoiceSettingField("Chế độ giới hạn lựa chọn", editable.copy(value = "end_only", options = listOf("never", "end_only")), true, ::leaderboardOptionLabel, {})
                ChoiceSettingField("Thiết lập bị khóa", editable.copy(editable = false), true, ::leaderboardOptionLabel, {})
            }
        }
    }
}
