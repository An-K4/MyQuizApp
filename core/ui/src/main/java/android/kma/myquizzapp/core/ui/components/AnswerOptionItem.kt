package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Supplied only when the caller is allowed to reveal server/review feedback. */
enum class AnswerOptionFeedback { None, Correct, Incorrect }

/** Stateless single-choice row. The caller owns group selection and feedback visibility policy. */
@Composable
fun AnswerOptionItem(
    label: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feedback: AnswerOptionFeedback = AnswerOptionFeedback.None,
) {
    val background = when {
        feedback == AnswerOptionFeedback.Correct -> ComponentColors.successSurface
        feedback == AnswerOptionFeedback.Incorrect -> ComponentColors.dangerSurface
        selected -> ComponentColors.selectedSurface
        else -> ComponentColors.surface
    }
    val foreground = when (feedback) {
        AnswerOptionFeedback.Correct -> ComponentColors.successForeground
        AnswerOptionFeedback.Incorrect -> ComponentColors.dangerForeground
        AnswerOptionFeedback.None -> if (selected) ComponentColors.selectedForeground else ComponentColors.foreground
    }
    val outline = when {
        feedback == AnswerOptionFeedback.Correct -> FrontendColors.Success
        selected || feedback != AnswerOptionFeedback.None -> foreground
        else -> ComponentColors.border
    }
    val stateLabel = when (feedback) {
        AnswerOptionFeedback.Correct -> "Đáp án đúng"
        AnswerOptionFeedback.Incorrect -> "Đáp án sai"
        AnswerOptionFeedback.None -> if (selected) "Đã chọn" else "Chưa chọn"
    }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = stateLabel },
        color = background, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, color = if (selected || feedback != AnswerOptionFeedback.None) foreground else ComponentColors.muted,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text, modifier = Modifier.weight(1f), color = foreground,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold))
            val indicator = when {
                feedback == AnswerOptionFeedback.Correct -> Icons.Outlined.CheckCircle
                feedback == AnswerOptionFeedback.Incorrect -> Icons.Outlined.Cancel
                selected -> Icons.Outlined.RadioButtonChecked
                else -> null
            }
            if (indicator != null) Icon(indicator, null, tint = outline, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(name = "Answer States Light", showBackground = true, widthDp = 390)
@Preview(name = "Answer States Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnswerOptionItemPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnswerOptionItem("A", "K2", false, {})
                AnswerOptionItem("B", "Đã chọn, chưa có kết quả", true, {})
                AnswerOptionItem("B", "Everest", true, {}, enabled = false, feedback = AnswerOptionFeedback.Correct)
                AnswerOptionItem("D", "Lhotse", true, {}, enabled = false, feedback = AnswerOptionFeedback.Incorrect)
                AnswerOptionItem("C", "Kangchenjunga — tên đáp án dài để kiểm tra xuống dòng", false, {}, enabled = false)
            }
        }
    }
}

