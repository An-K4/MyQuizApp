package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.kma.myquizzapp.feature.quiz_manage.presentation.components.ImagePickerSection
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionEditorCard
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Màn Sửa quiz (N16) — dùng chung editor components với màn Tạo quiz
 * (presentation/components), chỉ khác cách load (GET detail, pre-fill cả đáp án)
 * và save (PATCH thay thế toàn bộ câu hỏi) — đúng pattern QuizEditor.vue của web.
 *
 * Có dialog xác nhận khi thoát mà chưa lưu (isDirty).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizUpdated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun requestBack() {
        if (uiState.isDirty && !uiState.isSubmitting) showDiscardDialog = true else onNavigateBack()
    }

    // Chặn nút back hệ thống khi có thay đổi chưa lưu.
    BackHandler(enabled = uiState.isDirty && !uiState.isSubmitting) {
        showDiscardDialog = true
    }

    // Launcher ảnh chung cho cover + ảnh câu hỏi — giống CreateQuizScreen.
    var pendingPickTarget by remember { mutableStateOf<PickTarget?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        when (target) {
            is PickTarget.Cover -> viewModel.handleIntent(EditQuizIntent.PickCoverImage(uri))
            is PickTarget.Question -> viewModel.handleIntent(
                EditQuizIntent.PickQuestionImage(target.localId, uri)
            )
        }
    }
    fun launchImagePicker(target: PickTarget) {
        pendingPickTarget = target
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditQuizEffect.QuizUpdated -> onQuizUpdated(effect.quizId)
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Bỏ thay đổi?") },
            text = { Text("Bạn có thay đổi chưa được lưu. Thoát sẽ mất toàn bộ thay đổi này.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text("Thoát")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Ở lại chỉnh sửa")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa quiz") },
                navigationIcon = {
                    IconButton(onClick = ::requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.loadError != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = uiState.loadError ?: "Đã có lỗi xảy ra")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.handleIntent(EditQuizIntent.Retry) }) {
                        Text("Thử lại")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = uiState.quizName,
                            onValueChange = { viewModel.handleIntent(EditQuizIntent.QuizNameChanged(it)) },
                            label = { Text("Tên quiz *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = uiState.quizDescription,
                            onValueChange = { viewModel.handleIntent(EditQuizIntent.QuizDescriptionChanged(it)) },
                            label = { Text("Mô tả") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.quizLanguage,
                                onValueChange = { viewModel.handleIntent(EditQuizIntent.QuizLanguageChanged(it)) },
                                label = { Text("Ngôn ngữ") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.quizCategory,
                                onValueChange = { viewModel.handleIntent(EditQuizIntent.QuizCategoryChanged(it)) },
                                label = { Text("Chủ đề") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Công khai quiz này", modifier = Modifier.weight(1f))
                            Switch(
                                checked = uiState.isPublic,
                                onCheckedChange = { viewModel.handleIntent(EditQuizIntent.IsPublicChanged(it)) }
                            )
                        }
                    }

                    item {
                        ImagePickerSection(
                            label = "Ảnh quiz (tùy chọn)",
                            imageModel = if (uiState.coverRemoved) null
                            else uiState.coverImageUri ?: uiState.existingCoverUrl,
                            onPick = { launchImagePicker(PickTarget.Cover) },
                            onRemove = { viewModel.handleIntent(EditQuizIntent.RemoveCoverImage) }
                        )
                    }

                    item {
                        Text(text = "Câu hỏi (${uiState.questions.size})")
                    }

                    items(uiState.questions, key = { it.localId }) { question ->
                        QuestionEditorCard(
                            question = question,
                            canRemove = uiState.questions.size > 1,
                            onTypeChanged = { viewModel.handleIntent(EditQuizIntent.QuestionTypeChanged(question.localId, it)) },
                            onTextChanged = { viewModel.handleIntent(EditQuizIntent.QuestionTextChanged(question.localId, it)) },
                    onTimeLimitChanged = { viewModel.handleIntent(EditQuizIntent.TimeLimitChanged(question.localId, it)) },
                            onPickImage = { launchImagePicker(PickTarget.Question(question.localId)) },
                            onRemoveImage = { viewModel.handleIntent(EditQuizIntent.RemoveQuestionImage(question.localId)) },
                            onOptionChanged = { index, value -> viewModel.handleIntent(EditQuizIntent.OptionChanged(question.localId, index, value)) },
                            onToggleCorrect = { index -> viewModel.handleIntent(EditQuizIntent.ToggleCorrectIndex(question.localId, index)) },
                            onAddOption = { viewModel.handleIntent(EditQuizIntent.AddOption(question.localId)) },
                            onRemoveOption = { index -> viewModel.handleIntent(EditQuizIntent.RemoveOption(question.localId, index)) },
                            onHintChanged = { viewModel.handleIntent(EditQuizIntent.HintChanged(question.localId, it)) },
                            onExplanationChanged = { viewModel.handleIntent(EditQuizIntent.ExplanationChanged(question.localId, it)) },
                            onCorrectTextChanged = { viewModel.handleIntent(EditQuizIntent.CorrectTextChanged(question.localId, it)) },
                            onRemove = { viewModel.handleIntent(EditQuizIntent.RemoveQuestion(question.localId)) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { viewModel.handleIntent(EditQuizIntent.AddQuestion) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.height(0.dp))
                            Text("  Thêm câu hỏi")
                        }
                    }

                    if (uiState.fieldErrors.isNotEmpty()) {
                        item {
                            Column {
                                uiState.fieldErrors.forEach { err ->
                                    Text(text = "• $err")
                                }
                            }
                        }
                    }
                    if (uiState.errorMessage != null) {
                        item { Text(text = uiState.errorMessage ?: "") }
                    }

                    item {
                        Button(
                            onClick = { viewModel.handleIntent(EditQuizIntent.Submit) },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            } else {
                                Text("Lưu thay đổi")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Đối tượng đang chờ chọn ảnh qua Photo Picker chung của màn hình. */
private sealed interface PickTarget {
    data object Cover : PickTarget
    data class Question(val localId: String) : PickTarget
}
