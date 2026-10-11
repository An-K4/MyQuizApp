package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Stateless authoring card. Draft, expansion and scroll state all come from its owner. */
@Composable
fun QuestionEditorCard(
    question: QuestionDraft,
    questionNumber: Int,
    canRemove: Boolean,
    supplementaryExpanded: Boolean,
    onSupplementaryExpandedChange: (Boolean) -> Unit,
    typeScrollState: ScrollState,
    timeScrollState: ScrollState,
    onTypeChanged: (QuestionType) -> Unit,
    onTextChanged: (String) -> Unit,
    onTimeLimitChanged: (String) -> Unit,
    onTimePresetSelected: (Int) -> Unit,
    onCustomTimeSelected: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onOptionChanged: (Int, String) -> Unit,
    onToggleCorrect: (Int) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onHintChanged: (String) -> Unit,
    onExplanationChanged: (String) -> Unit,
    onCorrectTextChanged: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errors: List<String> = emptyList(),
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = EditorColors.soft) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Câu hỏi $questionNumber", Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EditorColors.foreground)
                IconButton(onClick = onRemove, enabled = enabled && canRemove) {
                    Icon(Icons.Default.DeleteOutline, "Xóa câu hỏi $questionNumber",
                        tint = if (enabled && canRemove) FrontendColors.Danger else EditorColors.muted)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(typeScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuestionType.entries.forEach { type ->
                    EditorChip(type.editorLabel(), question.questionType == type, { onTypeChanged(type) }, enabled)
                }
            }
            Text("Thời gian trả lời", color = EditorColors.muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(timeScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 20, 30, 60).forEach { seconds ->
                    EditorChip("${seconds}s", !question.usesCustomTime && question.timeLimit.toIntOrNull() == seconds,
                        { onTimePresetSelected(seconds) }, enabled)
                }
                EditorChip("Khác", question.usesCustomTime, onCustomTimeSelected, enabled)
            }
            if (question.usesCustomTime) {
                EditorField("Thời gian (giây) *", question.timeLimit,
                    { raw -> onTimeLimitChanged(raw.filter { it in '0'..'9' }.take(3)) }, enabled = enabled,
                    placeholder = "Nhập từ 5 đến 600", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            EditorField("Nội dung câu hỏi *", question.questionText, onTextChanged,
                placeholder = "Nhập nội dung câu hỏi tại đây...", enabled = enabled, minLines = 3, singleLine = false)
            ImagePickerSection("Ảnh câu hỏi $questionNumber", question.imageUri ?: question.existingImageUrl,
                onPickImage, onRemoveImage, enabled = enabled)
            if (question.isChoiceType) {
                Text(if (question.questionType == QuestionType.MULTIPLE_SELECT) "Chọn các đáp án đúng" else "Chọn một đáp án đúng",
                    style = MaterialTheme.typography.bodySmall, color = EditorColors.muted)
                question.options.forEachIndexed { index, option ->
                    AnswerEditorRow(
                        index, option, index in question.correctIndexes,
                        question.questionType == QuestionType.MULTIPLE_SELECT, question.options.size > 2,
                        { onOptionChanged(index, it) }, { onToggleCorrect(index) }, { onRemoveOption(index) }, enabled,
                    )
                }
                if (question.options.size < 4) {
                    TextButton(onClick = onAddOption, enabled = enabled, contentPadding = PaddingValues(0.dp)) {
                        Text("+ Thêm đáp án (Tối đa 4)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                EditorField("Đáp án đúng *", question.correctText, onCorrectTextChanged, enabled = enabled,
                    placeholder = "Nhập đáp án đúng", singleLine = question.questionType == QuestionType.SHORT_ANSWER,
                    minLines = if (question.questionType == QuestionType.LONG_ANSWER) 3 else 1)
            }
            TextButton(onClick = { onSupplementaryExpandedChange(!supplementaryExpanded) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = if (supplementaryExpanded) "Đang mở" else "Đang thu gọn" },
                contentPadding = PaddingValues(0.dp)) {
                Text("Thông tin bổ sung", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Icon(if (supplementaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (supplementaryExpanded) {
                EditorField("Gợi ý (tùy chọn)", question.questionHint, onHintChanged,
                    enabled = enabled, singleLine = false, minLines = 2)
                EditorField("Giải thích (tùy chọn)", question.explanation, onExplanationChanged,
                    enabled = enabled, singleLine = false, minLines = 2)
            }
            EditorErrorMessages(errors)
        }
    }
}

private fun QuestionType.editorLabel(): String = when (this) {
    QuestionType.MULTIPLE_CHOICE -> "Một lựa chọn"
    QuestionType.MULTIPLE_SELECT -> "Nhiều lựa chọn"
    QuestionType.SHORT_ANSWER -> "Trả lời ngắn"
    QuestionType.LONG_ANSWER -> "Trả lời dài"
}

@Composable
private fun EditorChip(label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else EditorColors.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else EditorColors.muted,
        modifier = Modifier.heightIn(min = 44.dp).semantics { this.selected = selected }) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Coloured inline editor; A/B/C/D + radio/checkbox also convey meaning without colour. */
@Composable
private fun AnswerEditorRow(
    index: Int, value: String, correct: Boolean, multiple: Boolean, canRemove: Boolean,
    onValueChange: (String) -> Unit, onToggleCorrect: () -> Unit, onRemove: () -> Unit, enabled: Boolean,
) {
    val background = when (index % 4) {
        0 -> FrontendColors.Danger
        1 -> FrontendColors.Info
        2 -> FrontendColors.EditorAnswerAmber
        else -> FrontendColors.Success
    }
    // Dark foreground keeps normal-sized editable text legible on all four bright accents.
    val foreground = FrontendColors.Foreground
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val semantics = Modifier.semantics { contentDescription = "Đánh dấu đáp án ${'A' + index} đúng" }
            if (multiple) Checkbox(correct, { onToggleCorrect() }, enabled = enabled, modifier = semantics,
                colors = CheckboxDefaults.colors(checkedColor = foreground, uncheckedColor = foreground, checkmarkColor = background))
            else RadioButton(correct, onToggleCorrect, enabled = enabled, modifier = semantics,
                colors = RadioButtonDefaults.colors(selectedColor = foreground, unselectedColor = foreground))
            OutlinedTextField(value, onValueChange, modifier = Modifier.weight(1f).semantics { contentDescription = "Nội dung đáp án ${'A' + index}" }, enabled = enabled,
                placeholder = { Text("Nhập đáp án", color = foreground) },
                prefix = { Text("${'A' + index}. ", color = foreground, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = foreground, unfocusedTextColor = foreground, disabledTextColor = foreground.copy(alpha = 0.6f),
                    cursorColor = foreground,
                    // The coloured answer row is the only container; no inner outline, even while typing.
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                ))
            if (canRemove) IconButton(onRemove, enabled = enabled) {
                Icon(Icons.Default.Close, "Xóa đáp án ${'A' + index}", tint = foreground)
            }
        }
    }
}

/** Shared labelled field: keeps labels outside and the same 16dp outline as auth screens. */
@Composable
fun EditorField(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, placeholder: String = "", enabled: Boolean = true,
    singleLine: Boolean = true, minLines: Int = 1, error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = EditorColors.foreground)
        OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
            singleLine = singleLine, minLines = minLines, isError = error != null,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            shape = RoundedCornerShape(16.dp), keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = EditorColors.surface, unfocusedContainerColor = EditorColors.surface,
                disabledContainerColor = EditorColors.surface, unfocusedBorderColor = EditorColors.border,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            supportingText = if (error != null) {{ Text(error) }} else null,
        )
    }
}

@Composable
fun EditorErrorMessages(errors: List<String>) {
    if (errors.isNotEmpty()) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun QuestionEditorPreviewContent(
    type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    custom: Boolean = false, supplementary: Boolean = false, withImage: Boolean = false,
    errors: List<String> = emptyList(), enabled: Boolean = true, optionCount: Int = 4,
) {
    val question = QuestionDraft(
        questionType = type, questionText = "Đỉnh núi nào cao nhất thế giới?",
        timeLimit = if (custom) "45" else "30", usesCustomTime = custom,
        options = listOf("K2", "Everest", "Kangchenjunga", "Lhotse").take(optionCount), correctIndexes = setOf(1),
        correctText = "Everest", questionHint = "Ngọn núi thuộc dãy Himalaya.",
        explanation = "Everest có độ cao khoảng 8.848 m.",
        existingImageUrl = if (withImage) "https://preview.invalid/question.png" else null,
    )
    MyQuizAppTheme {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            QuestionEditorCard(question, 1, true, supplementary, {}, rememberScrollState(), rememberScrollState(),
                {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}, {}, enabled = enabled, errors = errors)
        }
    }
}

@Preview(name = "Question Editor - Single", showBackground = true, widthDp = 390)
@Preview(name = "Question Editor - Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun QuestionEditorSinglePreview() = QuestionEditorPreviewContent()
@Preview(name = "Question Editor - Multiple", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorMultiplePreview() = QuestionEditorPreviewContent(QuestionType.MULTIPLE_SELECT)
@Preview(name = "Question Editor - Short", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorShortPreview() = QuestionEditorPreviewContent(QuestionType.SHORT_ANSWER)
@Preview(name = "Question Editor - Long", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorLongPreview() = QuestionEditorPreviewContent(QuestionType.LONG_ANSWER)
@Preview(name = "Question Editor - Custom Time", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorCustomPreview() = QuestionEditorPreviewContent(custom = true)
@Preview(name = "Question Editor - Supplementary", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorSupplementaryPreview() = QuestionEditorPreviewContent(supplementary = true)
@Preview(name = "Question Editor - Image", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorImagePreview() = QuestionEditorPreviewContent(withImage = true)
@Preview(name = "Question Editor - Error", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorErrorPreview() = QuestionEditorPreviewContent(errors = listOf("Câu 1: chọn ít nhất 1 đáp án đúng."))
@Preview(name = "Question Editor - Saving", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorSavingPreview() = QuestionEditorPreviewContent(enabled = false)

@Preview(name = "Question Editor - Initial Two Options", showBackground = true, widthDp = 390)
@Composable private fun QuestionEditorTwoOptionsPreview() = QuestionEditorPreviewContent(optionCount = 2)
