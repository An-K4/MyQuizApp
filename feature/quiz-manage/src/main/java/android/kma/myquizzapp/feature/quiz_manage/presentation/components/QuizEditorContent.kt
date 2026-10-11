package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.validator.QuizEditorErrors
import android.kma.myquizzapp.core.ui.components.SettingSwitchRow
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    validationErrors: QuizEditorErrors,
    listState: LazyListState,
    supplementaryExpandedIds: List<String>,
    onSupplementaryExpandedChange: (String, Boolean) -> Unit,
    typeScrollStates: Map<String, ScrollState>,
    timeScrollStates: Map<String, ScrollState>,
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
    onTimePresetSelected: (String, Int) -> Unit,
    onCustomTimeSelected: (String) -> Unit,
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
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "cover") {
                ImagePickerSection("Ảnh bìa quiz", coverImageModel, onPickCoverImage, onRemoveCoverImage,
                    enabled = !isSubmitting, isCover = true)
            }
            item(key = "name") {
                EditorField("Tên Quiz *", quizName, onQuizNameChanged, enabled = !isSubmitting,
                    placeholder = "Nhập tên bộ câu hỏi cuốn hút", error = validationErrors.name)
            }
            item(key = "description") {
                EditorField("Mô tả", quizDescription, onQuizDescriptionChanged, enabled = !isSubmitting,
                    placeholder = "Giới thiệu ngắn về chủ đề câu hỏi", singleLine = false, minLines = 3,
                    error = validationErrors.description)
            }
            item(key = "language_category") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField("Ngôn ngữ *", quizLanguage, onQuizLanguageChanged, Modifier.weight(1f),
                        enabled = !isSubmitting, placeholder = "vi", error = validationErrors.language)
                    EditorField("Chủ đề", quizCategory, onQuizCategoryChanged, Modifier.weight(1f),
                        enabled = !isSubmitting, placeholder = "Khoa học", error = validationErrors.category)
                }
            }
            item(key = "visibility") {
                SettingSwitchRow(
                    title = "Công khai quiz này", checked = isPublic,
                    onCheckedChange = onIsPublicChanged, enabled = !isSubmitting,
                )
            }
            item(key = "question_heading") {
                Text("Câu hỏi (${questions.size})", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = EditorColors.foreground)
                EditorErrorMessages(validationErrors.general)
            }
            itemsIndexed(questions, key = { _, question -> question.localId }) { index, question ->
                QuestionEditorCard(
                    question = question, questionNumber = index + 1,
                    canRemove = questions.size > 1, enabled = !isSubmitting,
                    supplementaryExpanded = question.localId in supplementaryExpandedIds,
                    onSupplementaryExpandedChange = { onSupplementaryExpandedChange(question.localId, it) },
                    typeScrollState = checkNotNull(typeScrollStates[question.localId]),
                    timeScrollState = checkNotNull(timeScrollStates[question.localId]),
                    errors = validationErrors.questions[question.localId].orEmpty(),
                    onTypeChanged = { onQuestionTypeChanged(question.localId, it) },
                    onTextChanged = { onQuestionTextChanged(question.localId, it) },
                    onTimeLimitChanged = { onTimeLimitChanged(question.localId, it) },
                    onTimePresetSelected = { onTimePresetSelected(question.localId, it) },
                    onCustomTimeSelected = { onCustomTimeSelected(question.localId) },
                    onPickImage = { onPickQuestionImage(question.localId) },
                    onRemoveImage = { onRemoveQuestionImage(question.localId) },
                    onOptionChanged = { optionIndex, value -> onOptionChanged(question.localId, optionIndex, value) },
                    onToggleCorrect = { onToggleCorrect(question.localId, it) },
                    onAddOption = { onAddOption(question.localId) },
                    onRemoveOption = { onRemoveOption(question.localId, it) },
                    onHintChanged = { onHintChanged(question.localId, it) },
                    onExplanationChanged = { onExplanationChanged(question.localId, it) },
                    onCorrectTextChanged = { onCorrectTextChanged(question.localId, it) },
                    onRemove = { onRemoveQuestion(question.localId) },
                )
            }
            item(key = "add_question") {
                QuizSecondaryButton("Thêm câu hỏi", onAddQuestion, Modifier.fillMaxWidth(), enabled = !isSubmitting,
                    leadingIcon = { Icon(Icons.Default.Add, null) })
            }
        }
        Surface(color = EditorColors.surface, shadowElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errorMessage?.let { EditorErrorMessages(listOf(it)) }
                QuizPrimaryButton(submitLabel, onSubmit, Modifier.fillMaxWidth(), enabled = !isSubmitting, loading = isSubmitting)
            }
        }
    }
}
