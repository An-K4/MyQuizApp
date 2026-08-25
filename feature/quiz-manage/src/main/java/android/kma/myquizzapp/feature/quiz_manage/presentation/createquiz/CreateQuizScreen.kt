package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.feature.quiz_manage.presentation.components.ImagePickerSection
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionEditorCard
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
 * Màn Tạo quiz (N13-14). N15 bổ sung chọn ảnh cover + ảnh từng câu hỏi qua
 * Android Photo Picker (không cần quyền runtime). Ảnh chọn chỉ là URI local —
 * việc upload thật (nén -> presign -> PUT) xảy ra trong ViewModel lúc bấm "Tạo quiz".
 *
 * N16: các composable editor (ImagePickerSection/QuestionEditorCard) chuyển sang
 * presentation/components để dùng chung với màn Sửa quiz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizCreated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Launcher DUY NHẤT dùng chung cho cả ảnh cover và ảnh từng câu hỏi — Android
    // Photo Picker không cần quyền runtime. pendingPickTarget theo dõi đang chọn
    // ảnh cho đối tượng nào (set trước khi launch, đọc khi picker trả về uri).
    var pendingPickTarget by remember { mutableStateOf<PickTarget?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        when (target) {
            is PickTarget.Cover -> viewModel.handleIntent(CreateQuizIntent.PickCoverImage(uri))
            is PickTarget.Question -> viewModel.handleIntent(
                CreateQuizIntent.PickQuestionImage(target.localId, uri)
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
                is CreateQuizEffect.QuizCreated -> onQuizCreated(effect.quizId)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tạo quiz mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.quizName,
                    onValueChange = { viewModel.handleIntent(CreateQuizIntent.QuizNameChanged(it)) },
                    label = { Text("Tên quiz *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.quizDescription,
                    onValueChange = { viewModel.handleIntent(CreateQuizIntent.QuizDescriptionChanged(it)) },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.quizLanguage,
                        onValueChange = { viewModel.handleIntent(CreateQuizIntent.QuizLanguageChanged(it)) },
                        label = { Text("Ngôn ngữ") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.quizCategory,
                        onValueChange = { viewModel.handleIntent(CreateQuizIntent.QuizCategoryChanged(it)) },
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
                        onCheckedChange = { viewModel.handleIntent(CreateQuizIntent.IsPublicChanged(it)) }
                    )
                }
            }

            item {
                ImagePickerSection(
                    label = "Ảnh quiz (tùy chọn)",
                    imageModel = uiState.coverImageUri,
                    onPick = { launchImagePicker(PickTarget.Cover) },
                    onRemove = { viewModel.handleIntent(CreateQuizIntent.RemoveCoverImage) }
                )
            }

            item {
                Text(text = "Câu hỏi (${uiState.questions.size})")
            }

            items(uiState.questions, key = { it.localId }) { question ->
                QuestionEditorCard(
                    question = question,
                    canRemove = uiState.questions.size > 1,
                    onTypeChanged = { viewModel.handleIntent(CreateQuizIntent.QuestionTypeChanged(question.localId, it)) },
                    onTextChanged = { viewModel.handleIntent(CreateQuizIntent.QuestionTextChanged(question.localId, it)) },
                    onTimeLimitChanged = { viewModel.handleIntent(CreateQuizIntent.TimeLimitChanged(question.localId, it)) },
                    onPickImage = { launchImagePicker(PickTarget.Question(question.localId)) },
                    onRemoveImage = { viewModel.handleIntent(CreateQuizIntent.RemoveQuestionImage(question.localId)) },
                    onOptionChanged = { index, value -> viewModel.handleIntent(CreateQuizIntent.OptionChanged(question.localId, index, value)) },
                    onToggleCorrect = { index -> viewModel.handleIntent(CreateQuizIntent.ToggleCorrectIndex(question.localId, index)) },
                    onAddOption = { viewModel.handleIntent(CreateQuizIntent.AddOption(question.localId)) },
                    onRemoveOption = { index -> viewModel.handleIntent(CreateQuizIntent.RemoveOption(question.localId, index)) },
                    onHintChanged = { viewModel.handleIntent(CreateQuizIntent.HintChanged(question.localId, it)) },
                    onExplanationChanged = { viewModel.handleIntent(CreateQuizIntent.ExplanationChanged(question.localId, it)) },
                    onCorrectTextChanged = { viewModel.handleIntent(CreateQuizIntent.CorrectTextChanged(question.localId, it)) },
                    onRemove = { viewModel.handleIntent(CreateQuizIntent.RemoveQuestion(question.localId)) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.handleIntent(CreateQuizIntent.AddQuestion) },
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
                    onClick = { viewModel.handleIntent(CreateQuizIntent.Submit) },
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Tạo quiz")
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
