package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuizEditorContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizUpdated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingPickTarget by remember { mutableStateOf<PickTarget?>(null) }

    val requestBack = {
        if (uiState.isDirty && !uiState.isSubmitting) showDiscardDialog = true else onNavigateBack()
    }
    BackHandler(enabled = uiState.isDirty && !uiState.isSubmitting) { showDiscardDialog = true }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri != null && target != null) {
            when (target) {
                PickTarget.Cover -> viewModel.handleIntent(EditQuizIntent.PickCoverImage(uri))
                is PickTarget.Question -> viewModel.handleIntent(EditQuizIntent.PickQuestionImage(target.localId, uri))
            }
        }
    }
    val launchImagePicker: (PickTarget) -> Unit = { target ->
        pendingPickTarget = target
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is EditQuizEffect.QuizUpdated) onQuizUpdated(effect.quizId)
        }
    }

    EditQuizScreenContent(
        uiState = uiState,
        showDiscardDialog = showDiscardDialog,
        onShowDiscardDialogChange = { showDiscardDialog = it },
        onNavigateBack = requestBack,
        onDiscardAndNavigateBack = onNavigateBack,
        onIntent = viewModel::handleIntent,
        onPickCoverImage = { launchImagePicker(PickTarget.Cover) },
        onPickQuestionImage = { launchImagePicker(PickTarget.Question(it)) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizScreenContent(
    uiState: EditQuizUiState,
    showDiscardDialog: Boolean,
    onShowDiscardDialogChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onDiscardAndNavigateBack: () -> Unit,
    onIntent: (EditQuizIntent) -> Unit,
    onPickCoverImage: () -> Unit,
    onPickQuestionImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val loadError = uiState.loadError
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onShowDiscardDialogChange(false) },
            title = { Text("Bỏ thay đổi?") },
            text = { Text("Bạn có thay đổi chưa được lưu. Thoát sẽ mất toàn bộ thay đổi này.") },
            confirmButton = {
                TextButton(onClick = {
                    onShowDiscardDialogChange(false)
                    onDiscardAndNavigateBack()
                }) { Text("Thoát") }
            },
            dismissButton = {
                TextButton(onClick = { onShowDiscardDialogChange(false) }) { Text("Ở lại chỉnh sửa") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa quiz") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> CenterStatusContent(Modifier.padding(innerPadding)) { CircularProgressIndicator() }
            loadError != null -> CenterStatusContent(Modifier.padding(innerPadding)) {
                Text(loadError)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onIntent(EditQuizIntent.Retry) }) { Text("Thử lại") }
            }
            else -> QuizEditorContent(
                quizName = uiState.quizName,
                quizDescription = uiState.quizDescription,
                quizLanguage = uiState.quizLanguage,
                quizCategory = uiState.quizCategory,
                isPublic = uiState.isPublic,
                coverImageModel = if (uiState.coverRemoved) null else uiState.coverImageUri ?: uiState.existingCoverUrl,
                questions = uiState.questions,
                fieldErrors = uiState.fieldErrors,
                errorMessage = uiState.errorMessage,
                isSubmitting = uiState.isSubmitting,
                submitLabel = "Lưu thay đổi",
                onQuizNameChanged = { onIntent(EditQuizIntent.QuizNameChanged(it)) },
                onQuizDescriptionChanged = { onIntent(EditQuizIntent.QuizDescriptionChanged(it)) },
                onQuizLanguageChanged = { onIntent(EditQuizIntent.QuizLanguageChanged(it)) },
                onQuizCategoryChanged = { onIntent(EditQuizIntent.QuizCategoryChanged(it)) },
                onIsPublicChanged = { onIntent(EditQuizIntent.IsPublicChanged(it)) },
                onPickCoverImage = onPickCoverImage,
                onRemoveCoverImage = { onIntent(EditQuizIntent.RemoveCoverImage) },
                onQuestionTypeChanged = { id, type -> onIntent(EditQuizIntent.QuestionTypeChanged(id, type)) },
                onQuestionTextChanged = { id, value -> onIntent(EditQuizIntent.QuestionTextChanged(id, value)) },
                onTimeLimitChanged = { id, value -> onIntent(EditQuizIntent.TimeLimitChanged(id, value)) },
                onPickQuestionImage = onPickQuestionImage,
                onRemoveQuestionImage = { onIntent(EditQuizIntent.RemoveQuestionImage(it)) },
                onOptionChanged = { id, index, value -> onIntent(EditQuizIntent.OptionChanged(id, index, value)) },
                onToggleCorrect = { id, index -> onIntent(EditQuizIntent.ToggleCorrectIndex(id, index)) },
                onAddOption = { onIntent(EditQuizIntent.AddOption(it)) },
                onRemoveOption = { id, index -> onIntent(EditQuizIntent.RemoveOption(id, index)) },
                onHintChanged = { id, value -> onIntent(EditQuizIntent.HintChanged(id, value)) },
                onExplanationChanged = { id, value -> onIntent(EditQuizIntent.ExplanationChanged(id, value)) },
                onCorrectTextChanged = { id, value -> onIntent(EditQuizIntent.CorrectTextChanged(id, value)) },
                onRemoveQuestion = { onIntent(EditQuizIntent.RemoveQuestion(it)) },
                onAddQuestion = { onIntent(EditQuizIntent.AddQuestion) },
                onSubmit = { onIntent(EditQuizIntent.Submit) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CenterStatusContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

private sealed interface PickTarget {
    data object Cover : PickTarget
    data class Question(val localId: String) : PickTarget
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditQuizScreenContentPreview() {
    MyQuizAppTheme {
        EditQuizScreenContent(
            uiState = EditQuizUiState(isLoading = false),
            showDiscardDialog = false,
            onShowDiscardDialogChange = {},
            onNavigateBack = {},
            onDiscardAndNavigateBack = {},
            onIntent = {},
            onPickCoverImage = {},
            onPickQuestionImage = {}
        )
    }
}
