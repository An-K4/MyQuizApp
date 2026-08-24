package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.core.common.model.QuestionType
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

/**
 * Màn Tạo quiz (N13-14). N15 bổ sung chọn ảnh cover + ảnh từng câu hỏi qua
 * Android Photo Picker (không cần quyền runtime) + xem trước bằng Coil. Ảnh chọ
 * chỉ là URI local — việc upload thật (nén -> presign -> PUT) xảy ra trong
 * ViewModel lúc bấm "Tạo quiz".
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
                        label = { Text("Ngôn ngứ") },
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
                    imageUri = uiState.coverImageUri,
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
                    onIntent = viewModel::handleIntent,
                    onPickImage = { launchImagePicker(PickTarget.Question(question.localId)) }
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

@Composable
private fun ImagePickerSection(
    label: String,
    imageUri: Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label)
        if (imageUri != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(
                    model = imageUri,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditorCard(
    question: QuestionDraft,
    canRemove: Boolean,
    onIntent: (CreateQuizIntent) -> Unit,
    onPickImage: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuestionTypeMenuButton(
                    selected = question.questionType,
                    onSelected = { onIntent(CreateQuizIntent.QuestionTypeChanged(question.localId, it)) },
                    modifier = Modifier.weight(1f)
                )
                if (canRemove) {
                    IconButton(onClick = { onIntent(CreateQuizIntent.RemoveQuestion(question.localId)) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Xóa câu hỏi")
                    }
                }
            }

            OutlinedTextField(
                value = question.questionText,
                onValueChange = { onIntent(CreateQuizIntent.QuestionTextChanged(question.localId, it)) },
                label = { Text("Nội dung câu hỏi *") },
                modifier = Modifier.fillMaxWidth()
            )

            ImagePickerSection(
                label = "Ảnh câu hỏi (tùy chọn)",
                imageUri = question.imageUri,
                onPick = onPickImage,
                onRemove = { onIntent(CreateQuizIntent.RemoveQuestionImage(question.localId)) }
            )

            if (question.isChoiceType) {
                Text(text = "Lựa chọn (chọn đáp án đúng):")
                question.options.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                            RadioButton(
                                selected = question.correctIndexes.contains(index),
                                onClick = { onIntent(CreateQuizIntent.ToggleCorrectIndex(question.localId, index)) }
                            )
                        } else {
                            Checkbox(
                                checked = question.correctIndexes.contains(index),
                                onCheckedChange = { onIntent(CreateQuizIntent.ToggleCorrectIndex(question.localId, index)) }
                            )
                        }
                        OutlinedTextField(
                            value = option,
                            onValueChange = { onIntent(CreateQuizIntent.OptionChanged(question.localId, index, it)) },
                            label = { Text("Lựa chọn ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        if (question.options.size > 2) {
                            IconButton(onClick = { onIntent(CreateQuizIntent.RemoveOption(question.localId, index)) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Xóa lựa chọn")
                            }
                        }
                    }
                }
                if (question.options.size < 4) {
                    TextButton(onClick = { onIntent(CreateQuizIntent.AddOption(question.localId)) }) {
                        Text("+ Thêm lựa chọn")
                    }
                }
            } else {
                OutlinedTextField(
                    value = question.correctText,
                    onValueChange = { onIntent(CreateQuizIntent.CorrectTextChanged(question.localId, it)) },
                    label = { Text("Đáp án đúng *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = question.questionHint,
                onValueChange = { onIntent(CreateQuizIntent.HintChanged(question.localId, it)) },
                label = { Text("Gợi ý (tùy chọn)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = question.explanation,
                onValueChange = { onIntent(CreateQuizIntent.ExplanationChanged(question.localId, it)) },
                label = { Text("Giải thích (tùy chọn)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuestionTypeMenuButton(
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
