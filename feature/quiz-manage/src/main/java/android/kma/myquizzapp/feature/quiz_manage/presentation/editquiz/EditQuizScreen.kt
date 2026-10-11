package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.*
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import android.kma.myquizzapp.core.ui.components.AppDialogVariant
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizUpdated: (Long) -> Unit,
    onResourceMissing: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    // String is saveable across recreation while the system Photo Picker is open.
    var pendingPickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var supplementaryExpandedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()
    val typeScrollStates = rememberQuestionScrollStates(uiState.questions)
    val timeScrollStates = rememberQuestionScrollStates(uiState.questions)
    ScrollToEditorError(uiState.validationErrors, uiState.validationAttempt, uiState.questions, listState)



    val requestBack: () -> Unit = {
        when {
            uiState.isSubmitting -> Unit
            uiState.isDirty -> showDiscardDialog = true
            else -> onNavigateBack()
        }
    }
    BackHandler(enabled = uiState.isDirty || uiState.isSubmitting) { requestBack() }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri != null && target != null) {
            if (target == "cover") viewModel.onIntent(EditQuizIntent.PickCoverImage(uri))
            else viewModel.onIntent(EditQuizIntent.PickQuestionImage(target, uri))
        }
    }
    val launchImagePicker: (String) -> Unit = { target ->
        if (!uiState.isSubmitting) {
            pendingPickTarget = target
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EditQuizEffect.QuizUpdated -> onQuizUpdated(effect.quizId)
                is EditQuizEffect.ResourceMissing -> onResourceMissing(effect.message)
            }
        }
    }

    EditQuizScreenContent(
        uiState = uiState,
        listState = listState,
        typeScrollStates = typeScrollStates,
        timeScrollStates = timeScrollStates,
        supplementaryExpandedIds = supplementaryExpandedIds,
        onSupplementaryExpandedChange = { id, expanded ->
            supplementaryExpandedIds = if (expanded) (supplementaryExpandedIds + id).distinct() else supplementaryExpandedIds - id
        },
        showDiscardDialog = showDiscardDialog,
        onShowDiscardDialogChange = { showDiscardDialog = it },
        onNavigateBack = requestBack,
        onDiscardAndNavigateBack = onNavigateBack,
        onIntent = viewModel::onIntent,
        onPickCoverImage = { launchImagePicker("cover") },
        onPickQuestionImage = { launchImagePicker(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizScreenContent(
    uiState: EditQuizUiState,
    listState: LazyListState,
    typeScrollStates: Map<String, ScrollState>,
    timeScrollStates: Map<String, ScrollState>,
    supplementaryExpandedIds: List<String>,
    onSupplementaryExpandedChange: (String, Boolean) -> Unit,
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
        AppConfirmDialog(
            title = "Bỏ thay đổi?",
            message = "Bạn có thay đổi chưa được lưu. Thoát sẽ mất toàn bộ thay đổi này.",
            confirmText = "Bỏ thay đổi", dismissText = "Ở lại",
            variant = AppDialogVariant.Danger,
            onConfirm = { onShowDiscardDialogChange(false); onDiscardAndNavigateBack() },
            onDismiss = { onShowDiscardDialogChange(false) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa quiz", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isSubmitting) {
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
                validationErrors = uiState.validationErrors,
                listState = listState,
                typeScrollStates = typeScrollStates,
                timeScrollStates = timeScrollStates,
                supplementaryExpandedIds = supplementaryExpandedIds,
                onSupplementaryExpandedChange = onSupplementaryExpandedChange,
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
                onTimePresetSelected = { id, seconds -> onIntent(EditQuizIntent.TimePresetSelected(id, seconds)) },
                onCustomTimeSelected = { onIntent(EditQuizIntent.CustomTimeSelected(it)) },
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
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
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

@Composable
private fun EditQuizPreviewContent(uiState: EditQuizUiState = EditQuizUiState(
    isLoading = false, quizName = "Khám phá Văn hóa Thế giới", quizCategory = "Văn hóa",
    existingCoverUrl = "https://preview.invalid/cover.png", questions = listOf(QuestionDraft()),
)) {
    MyQuizAppTheme {
        EditQuizScreenContent(
            uiState = uiState, listState = rememberLazyListState(),
            typeScrollStates = rememberQuestionScrollStates(uiState.questions),
            timeScrollStates = rememberQuestionScrollStates(uiState.questions),
            supplementaryExpandedIds = emptyList(), onSupplementaryExpandedChange = { _, _ -> },
            showDiscardDialog = false, onShowDiscardDialogChange = {},
            onNavigateBack = {}, onDiscardAndNavigateBack = {}, onIntent = {},
            onPickCoverImage = {}, onPickQuestionImage = {},
        )
    }
}

@Preview(name = "Edit Quiz - Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Edit Quiz - Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun EditQuizScreenContentPreview() = EditQuizPreviewContent()
@Preview(name = "Edit Quiz - Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun EditQuizLoadingPreview() = EditQuizPreviewContent(EditQuizUiState())
@Preview(name = "Edit Quiz - Load Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun EditQuizLoadErrorPreview() = EditQuizPreviewContent(EditQuizUiState(isLoading = false, loadError = "Không thể tải quiz. Vui lòng thử lại."))
