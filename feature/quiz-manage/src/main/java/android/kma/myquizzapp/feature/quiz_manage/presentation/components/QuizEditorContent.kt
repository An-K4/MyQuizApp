package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.ui.components.SettingSwitchRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Stateless editor body shared by create/edit quiz screens. */
@Composable
fun QuizEditorContent(
    quizName: String,
    quizDescription: String,
    quizLanguage: String,
    quizCategory: String,
    isPublic: Boolean,
    coverImageModel: Any?,
    questions: List<QuestionDraft>,
    fieldErrors: List<String>,
    errorMessage: String?,
    isSubmitting: Boolean,
    submitLabel: String,
    onQuizNameChanged: (String) -> Unit,
    onQuizDescriptionChanged: (String) -> Unit,
    onQuizLanguageChanged: (String) -> Unit,
    onQuizCategoryChanged: (String) -> Unit,
    onIsPublicChanged: (Boolean) -> Unit,
    onPickCoverImage: () -> Unit,
    onRemoveCoverImage: () -> Unit,
    onQuestionTypeChanged: (String, QuestionType) -> Unit,
    onQuestionTextChanged: (String, String) -> Unit,
    onTimeLimitChanged: (String, String) -> Unit,
    onPickQuestionImage: (String) -> Unit,
    onRemoveQuestionImage: (String) -> Unit,
    onOptionChanged: (String, Int, String) -> Unit,
    onToggleCorrect: (String, Int) -> Unit,
    onAddOption: (String) -> Unit,
    onRemoveOption: (String, Int) -> Unit,
    onHintChanged: (String, String) -> Unit,
    onExplanationChanged: (String, String) -> Unit,
    onCorrectTextChanged: (String, String) -> Unit,
    onRemoveQuestion: (String) -> Unit,
    onAddQuestion: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = quizName,
                onValueChange = onQuizNameChanged,
                label = { Text("Tên quiz *") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = quizDescription,
                onValueChange = onQuizDescriptionChanged,
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quizLanguage,
                    onValueChange = onQuizLanguageChanged,
                    label = { Text("Ngôn ngữ") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quizCategory,
                    onValueChange = onQuizCategoryChanged,
                    label = { Text("Chủ đề") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SettingSwitchRow(
                title = "Công khai quiz này",
                checked = isPublic,
                onCheckedChange = onIsPublicChanged
            )
        }
        item {
            ImagePickerSection(
                label = "Ảnh quiz (tùy chọn)",
                imageModel = coverImageModel,
                onPick = onPickCoverImage,
                onRemove = onRemoveCoverImage
            )
        }
        item { Text("Câu hỏi (${questions.size})") }
        items(questions, key = { it.localId }) { question ->
            QuestionEditorCard(
                question = question,
                canRemove = questions.size > 1,
                onTypeChanged = { onQuestionTypeChanged(question.localId, it) },
                onTextChanged = { onQuestionTextChanged(question.localId, it) },
                onTimeLimitChanged = { onTimeLimitChanged(question.localId, it) },
                onPickImage = { onPickQuestionImage(question.localId) },
                onRemoveImage = { onRemoveQuestionImage(question.localId) },
                onOptionChanged = { index, value -> onOptionChanged(question.localId, index, value) },
                onToggleCorrect = { onToggleCorrect(question.localId, it) },
                onAddOption = { onAddOption(question.localId) },
                onRemoveOption = { onRemoveOption(question.localId, it) },
                onHintChanged = { onHintChanged(question.localId, it) },
                onExplanationChanged = { onExplanationChanged(question.localId, it) },
                onCorrectTextChanged = { onCorrectTextChanged(question.localId, it) },
                onRemove = { onRemoveQuestion(question.localId) }
            )
        }
        item {
            OutlinedButton(onClick = onAddQuestion, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Thêm câu hỏi")
            }
        }
        if (fieldErrors.isNotEmpty()) item {
            Column { fieldErrors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) } }
        }
        errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.height(20.dp)) else Text(submitLabel)
            }
        }
    }
}
