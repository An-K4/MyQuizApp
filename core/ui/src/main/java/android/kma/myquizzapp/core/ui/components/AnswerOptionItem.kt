package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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

/** Preserve single-selection defaults; multi-choice callers opt into checkbox semantics. */
enum class AnswerOptionSelectionMode { Single, Multiple }

/**
 * Stateless option row. The caller owns selection and feedback policy.
 * readOnly removes click/radio semantics and selection. Feedback is also hidden unless
 * showReadOnlyFeedback is explicitly enabled by a caller with reveal permission.
 * Existing interactive and read-only defaults are unchanged.
 */
@Composable
fun AnswerOptionItem(
    label: String,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    feedback: AnswerOptionFeedback = AnswerOptionFeedback.None,
    readOnly: Boolean = false,
    selectionMode: AnswerOptionSelectionMode = AnswerOptionSelectionMode.Single,
    showReadOnlyFeedback: Boolean = false,
) {
    val isSelected = selected && !readOnly
    val visibleFeedback = if (readOnly && !showReadOnlyFeedback) AnswerOptionFeedback.None else feedback
    val background = when {
        visibleFeedback == AnswerOptionFeedback.Correct -> ComponentColors.successSurface
        visibleFeedback == AnswerOptionFeedback.Incorrect -> ComponentColors.dangerSurface
        isSelected -> ComponentColors.selectedSurface
        else -> ComponentColors.surface
    }
    val foreground = when (visibleFeedback) {
        AnswerOptionFeedback.Correct -> ComponentColors.successForeground
        AnswerOptionFeedback.Incorrect -> ComponentColors.dangerForeground
        AnswerOptionFeedback.None -> if (isSelected) ComponentColors.selectedForeground else ComponentColors.foreground
    }
    val outline = when {
        visibleFeedback == AnswerOptionFeedback.Correct -> FrontendColors.Success
        isSelected || visibleFeedback != AnswerOptionFeedback.None -> foreground
        else -> ComponentColors.border
    }
    val stateLabel = when (visibleFeedback) {
        AnswerOptionFeedback.Correct -> "Đáp án đúng"
        AnswerOptionFeedback.Incorrect -> "Đáp án sai"
        AnswerOptionFeedback.None -> if (isSelected) "Đã chọn" else "Chưa chọn"
    }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).then(
            if (readOnly) Modifier.semantics(mergeDescendants = true) {
                if (visibleFeedback != AnswerOptionFeedback.None) stateDescription = stateLabel
            }
            else (if (selectionMode == AnswerOptionSelectionMode.Multiple)
                Modifier.toggleable(value = isSelected, enabled = enabled, role = Role.Checkbox,
                    onValueChange = { onClick() })
            else Modifier.selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick))
                .semantics(mergeDescendants = true) { stateDescription = stateLabel }
        ),
        color = background, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, color = if (isSelected || visibleFeedback != AnswerOptionFeedback.None) foreground else ComponentColors.muted,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text, modifier = Modifier.weight(1f), color = foreground,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold))
            val indicator = when {
                visibleFeedback == AnswerOptionFeedback.Correct -> Icons.Outlined.CheckCircle
                visibleFeedback == AnswerOptionFeedback.Incorrect -> Icons.Outlined.Cancel
                isSelected -> Icons.Outlined.RadioButtonChecked
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
                AnswerOptionItem("A", "Chỉ xem — không chọn hoặc tiết lộ đáp án", false, {}, readOnly = true)
                AnswerOptionItem("B", "Host xem đáp án đúng — không có thao tác chọn", false, {},
                    feedback = AnswerOptionFeedback.Correct, readOnly = true, showReadOnlyFeedback = true)
                AnswerOptionItem("B", "Đã chọn, chưa có kết quả", true, {})
                AnswerOptionItem("B", "Chọn nhiều — chưa gửi đáp án", true, {},
                    selectionMode = AnswerOptionSelectionMode.Multiple)
                AnswerOptionItem("B", "Everest", true, {}, enabled = false, feedback = AnswerOptionFeedback.Correct)
                AnswerOptionItem("D", "Lhotse", true, {}, enabled = false, feedback = AnswerOptionFeedback.Incorrect)
                AnswerOptionItem("C", "Kangchenjunga — tên đáp án dài để kiểm tra xuống dòng", false, {}, enabled = false)
            }
        }
    }
}



