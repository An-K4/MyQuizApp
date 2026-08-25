package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.model.QuestionType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Các composable editor câu hỏi dùng chung giữa màn Tạo quiz (N13-15) và Sửa quiz
 * (N16) — theo pattern QuizEditor.vue của frontend web: editor dùng chung, mỗi màn
 * chỉ khác cách load/save. Đặt ở presentation/components (KHÔNG phải core:ui) vì
 * đây là UI đặc thù của quiz-authoring, chỉ tồn tại trong feature:quiz-manage.
 *
 * API nhận callback riêng lẻ thay vì sealed Intent của từng màn, để cả
 * CreateQuizScreen lẫn EditQuizScreen cùng dùng mà không phụ thuộc intent của nhau.
 */

/**
 * Khối chọn/xem/xóa ảnh trong editor. [imageModel] là bất cứ kiểu nào Coil hiểu:
 * Uri (ảnh local mới chọn, chưa upload) hoặc String URL (ảnh đã lưu trên server).
 */
@Composable
fun ImagePickerSection(
    label: String,
    imageModel: Any?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label)
        if (imageModel != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                TextButton(onClick = onRemove) {
                    Text("Xóa ảnh")
                }
            }
        } else {
            OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn ảnh")
            }
        }
    }
}

/** Card soạn 1 câu hỏi: loại câu, nội dung, ảnh, đáp án theo loại, gợi ý/giải thích. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorCard(
    question: QuestionDraft,
    canRemove: Boolean,
    onTypeChanged: (QuestionType) -> Unit,
    onTextChanged: (String) -> Unit,
    onTimeLimitChanged: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onOptionChanged: (index: Int, value: String) -> Unit,
    onToggleCorrect: (index: Int) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (index: Int) -> Unit,
    onHintChanged: (String) -> Unit,
    onExplanationChanged: (String) -> Unit,
    onCorrectTextChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuestionTypeMenuButton(
                    selected = question.questionType,
                    onSelected = onTypeChanged,
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Xóa câu hỏi")
                    }
                }
            }

            OutlinedTextField(
                value = question.questionText,
                onValueChange = onTextChanged,
                label = { Text("Nội dung câu hỏi *") },
                modifier = Modifier.fillMaxWidth()
            )

            // Thời gian trả lời (giây) — time_limit trong schema. Để TRỐNG = mặc định
            // 30s lúc submit; nhập thì phải 5-600s (validate ở ViewModel, quiz.schema.ts).
            OutlinedTextField(
                value = question.timeLimit,
                onValueChange = { raw ->
                    // Chỉ nhận số, tối đa 3 chữ số; cho phép xóa trắng ô.
                    onTimeLimitChanged(raw.filter { it.isDigit() }.take(3))
                },
                label = { Text("Thời gian (giây) — trống = 30s") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            ImagePickerSection(
                label = "Ảnh câu hỏi (tùy chọn)",
                // Ưu tiên ảnh local mới chọn; nếu không có thì hiện ảnh đã lưu (màn Sửa).
                imageModel = question.imageUri ?: question.existingImageUrl,
                onPick = onPickImage,
                onRemove = onRemoveImage
            )

            if (question.isChoiceType) {
                Text(text = "Lựa chọn (chọn đáp án đúng):")
                question.options.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                            RadioButton(
                                selected = question.correctIndexes.contains(index),
                                onClick = { onToggleCorrect(index) }
                            )
                        } else {
                            Checkbox(
                                checked = question.correctIndexes.contains(index),
                                onCheckedChange = { onToggleCorrect(index) }
                            )
                        }
                        OutlinedTextField(
                            value = option,
                            onValueChange = { onOptionChanged(index, it) },
                            label = { Text("Lựa chọn ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        if (question.options.size > 2) {
                            IconButton(onClick = { onRemoveOption(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Xóa lựa chọn")
                            }
                        }
                    }
                }
                if (question.options.size < 4) {
                    TextButton(onClick = onAddOption) {
                        Text("+ Thêm lựa chọn")
                    }
                }
            } else {
                OutlinedTextField(
                    value = question.correctText,
                    onValueChange = onCorrectTextChanged,
                    label = { Text("Đáp án đúng *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = question.questionHint,
                onValueChange = onHintChanged,
                label = { Text("Gợi ý (tùy chọn)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = question.explanation,
                onValueChange = onExplanationChanged,
                label = { Text("Giải thích (tùy chọn)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuestionTypeMenuButton(
    selected: QuestionType,
    onSelected: (QuestionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(questionTypeLabel(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QuestionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(questionTypeLabel(type)) },
                    onClick = { onSelected(type); expanded = false }
                )
            }
        }
    }
}

private fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.MULTIPLE_CHOICE -> "Chọn 1 đáp án"
    QuestionType.MULTIPLE_SELECT -> "Chọn nhiều đáp án"
    QuestionType.SHORT_ANSWER -> "Trả lời ngắn"
    QuestionType.LONG_ANSWER -> "Trả lời dài"
}
