package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.ui.components.AnswerOptionItem
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Read-only accordion, distinct from QuestionEditorCard. Expansion belongs to the caller. */
@Composable
fun QuestionPreviewCard(
    questionText: String,
    questionType: QuestionType,
    timeLimitSeconds: Int,
    imageUrl: String?,
    optionTexts: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val soft = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val foreground = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = soft) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button, onClick = { onExpandedChange(!expanded) })
                    .semantics { stateDescription = if (expanded) "Đã mở câu hỏi" else "Đã thu gọn câu hỏi" }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        questionText,
                        color = foreground,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp,
                        ),
                    )
                    Text("${questionTypeLabel(questionType)} • $timeLimitSeconds giây",
                        color = muted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null, tint = muted, modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Fit preserves diagrams/text; never crop question artwork.
                    QuestionImage(imageUrl = imageUrl, contentDescription = "Ảnh câu hỏi")
                    when (questionType) {
                        QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT -> {
                            if (optionTexts.isEmpty()) {
                                Text("Câu hỏi chưa có lựa chọn.", color = muted, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                optionTexts.forEachIndexed { index, text ->
                                    AnswerOptionItem(
                                        label = if (index < 26) ('A' + index).toString() else (index + 1).toString(),
                                        text = text,
                                        selected = false,
                                        onClick = {},
                                        readOnly = true,
                                    )
                                }
                            }
                        }
                        QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(
                                    min = if (questionType == QuestionType.LONG_ANSWER) 96.dp else 52.dp,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Text(
                                    if (questionType == QuestionType.SHORT_ANSWER)
                                        "Người chơi nhập câu trả lời ngắn."
                                    else "Người chơi nhập câu trả lời dài.",
                                    modifier = Modifier.padding(12.dp), color = muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Question Collapsed Light", showBackground = true, widthDp = 390)
@Preview(name = "Question Collapsed Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuestionPreviewCardCollapsedPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuestionType.entries.forEach { type ->
                    QuestionPreviewCard("Kim tự tháp Giza nằm ở quốc gia nào?", type, 30, null,
                        listOf("Ai Cập", "Nhật Bản", "Pháp", "Ấn Độ"), false, {})
                }
            }
        }
    }
}

@Composable
private fun ExpandedQuestionPreview(type: QuestionType, withImage: Boolean = false) {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuestionPreviewCard(
                questionText = "Kim tự tháp Giza nằm ở quốc gia nào?",
                questionType = type, timeLimitSeconds = 30,
                imageUrl = if (withImage) "https://preview.invalid/question.png" else null,
                optionTexts = listOf("Ai Cập", "Nhật Bản", "Pháp", "Ấn Độ"),
                expanded = true, onExpandedChange = {}, modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview(name = "Question Single Choice", showBackground = true, widthDp = 390)
@Preview(name = "Question Single Choice Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Question Large Text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun QuestionSingleChoicePreview() = ExpandedQuestionPreview(QuestionType.MULTIPLE_CHOICE)

@Preview(name = "Question Multi Choice With Image", showBackground = true, widthDp = 390)
@Composable
private fun QuestionMultiChoicePreview() = ExpandedQuestionPreview(QuestionType.MULTIPLE_SELECT, withImage = true)

@Preview(name = "Question Short Answer", showBackground = true, widthDp = 390)
@Composable
private fun QuestionShortAnswerPreview() = ExpandedQuestionPreview(QuestionType.SHORT_ANSWER)

@Preview(name = "Question Long Answer", showBackground = true, widthDp = 390)
@Composable
private fun QuestionLongAnswerPreview() = ExpandedQuestionPreview(QuestionType.LONG_ANSWER)
