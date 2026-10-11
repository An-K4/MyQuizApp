package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizCreated: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateQuizViewModel = hiltViewModel()
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
            if (target == "cover") viewModel.onIntent(CreateQuizIntent.PickCoverImage(uri))
            else viewModel.onIntent(CreateQuizIntent.PickQuestionImage(target, uri))
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
            if (effect is CreateQuizEffect.QuizCreated) onQuizCreated(effect.quizId)
        }
    }

    CreateQuizScreenContent(
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
        onDiscardAndNavigateBack = onNavigateBack,
        onNavigateBack = requestBack,
        onIntent = viewModel::onIntent,
        onPickCoverImage = { launchImagePicker("cover") },
        onPickQuestionImage = { launchImagePicker(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreenContent(
    uiState: CreateQuizUiState,
    listState: LazyListState,
    typeScrollStates: Map<String, ScrollState>,
    timeScrollStates: Map<String, ScrollState>,
    supplementaryExpandedIds: List<String>,
    onSupplementaryExpandedChange: (String, Boolean) -> Unit,
    showDiscardDialog: Boolean,
    onShowDiscardDialogChange: (Boolean) -> Unit,
    onDiscardAndNavigateBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onIntent: (CreateQuizIntent) -> Unit,
    onPickCoverImage: () -> Unit,
    onPickQuestionImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                title = { Text("Tạo quiz mới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        QuizEditorContent(
            quizName = uiState.quizName,
            quizDescription = uiState.quizDescription,
            quizLanguage = uiState.quizLanguage,
            quizCategory = uiState.quizCategory,
            isPublic = uiState.isPublic,
            coverImageModel = uiState.coverImageUri,
            questions = uiState.questions,
            validationErrors = uiState.validationErrors,
            listState = listState,
            typeScrollStates = typeScrollStates,
            timeScrollStates = timeScrollStates,
            supplementaryExpandedIds = supplementaryExpandedIds,
            onSupplementaryExpandedChange = onSupplementaryExpandedChange,
            errorMessage = uiState.errorMessage,
            isSubmitting = uiState.isSubmitting,
            submitLabel = if (uiState.isPublic) "Lưu & xuất bản" else "Lưu quiz",
            onQuizNameChanged = { onIntent(CreateQuizIntent.QuizNameChanged(it)) },
            onQuizDescriptionChanged = { onIntent(CreateQuizIntent.QuizDescriptionChanged(it)) },
            onQuizLanguageChanged = { onIntent(CreateQuizIntent.QuizLanguageChanged(it)) },
            onQuizCategoryChanged = { onIntent(CreateQuizIntent.QuizCategoryChanged(it)) },
            onIsPublicChanged = { onIntent(CreateQuizIntent.IsPublicChanged(it)) },
            onPickCoverImage = onPickCoverImage,
            onRemoveCoverImage = { onIntent(CreateQuizIntent.RemoveCoverImage) },
            onQuestionTypeChanged = { id, type -> onIntent(CreateQuizIntent.QuestionTypeChanged(id, type)) },
            onQuestionTextChanged = { id, value -> onIntent(CreateQuizIntent.QuestionTextChanged(id, value)) },
            onTimeLimitChanged = { id, value -> onIntent(CreateQuizIntent.TimeLimitChanged(id, value)) },
            onTimePresetSelected = { id, seconds -> onIntent(CreateQuizIntent.TimePresetSelected(id, seconds)) },
            onCustomTimeSelected = { onIntent(CreateQuizIntent.CustomTimeSelected(it)) },
            onPickQuestionImage = onPickQuestionImage,
            onRemoveQuestionImage = { onIntent(CreateQuizIntent.RemoveQuestionImage(it)) },
            onOptionChanged = { id, index, value -> onIntent(CreateQuizIntent.OptionChanged(id, index, value)) },
            onToggleCorrect = { id, index -> onIntent(CreateQuizIntent.ToggleCorrectIndex(id, index)) },
            onAddOption = { onIntent(CreateQuizIntent.AddOption(it)) },
            onRemoveOption = { id, index -> onIntent(CreateQuizIntent.RemoveOption(id, index)) },
            onHintChanged = { id, value -> onIntent(CreateQuizIntent.HintChanged(id, value)) },
            onExplanationChanged = { id, value -> onIntent(CreateQuizIntent.ExplanationChanged(id, value)) },
            onCorrectTextChanged = { id, value -> onIntent(CreateQuizIntent.CorrectTextChanged(id, value)) },
            onRemoveQuestion = { onIntent(CreateQuizIntent.RemoveQuestion(it)) },
            onAddQuestion = { onIntent(CreateQuizIntent.AddQuestion) },
            onSubmit = { onIntent(CreateQuizIntent.Submit) },
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        )
    }
}

@Composable
private fun CreateQuizPreviewContent(uiState: CreateQuizUiState = CreateQuizUiState(), initialItem: Int = 0) {
    MyQuizAppTheme {
        CreateQuizScreenContent(
            uiState = uiState, listState = rememberLazyListState(initialFirstVisibleItemIndex = initialItem),
            typeScrollStates = rememberQuestionScrollStates(uiState.questions),
            timeScrollStates = rememberQuestionScrollStates(uiState.questions),
            supplementaryExpandedIds = emptyList(), onSupplementaryExpandedChange = { _, _ -> },
            showDiscardDialog = false, onShowDiscardDialogChange = {}, onDiscardAndNavigateBack = {},
            onNavigateBack = {}, onIntent = {}, onPickCoverImage = {}, onPickQuestionImage = {},
        )
    }
}

@Preview(name = "Create Quiz - Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Create Quiz - Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun CreateQuizScreenContentPreview() = CreateQuizPreviewContent()
@Preview(name = "Create Quiz - Filled", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizFilledPreview() = CreateQuizPreviewContent(CreateQuizUiState(
    quizName = "Khám phá Văn hóa Thế giới", quizDescription = "Phong tục và danh lam thắng cảnh trên toàn cầu.",
    quizCategory = "Văn hóa", isDirty = true,
))
@Preview(name = "Create Quiz - Private", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizPrivatePreview() = CreateQuizPreviewContent(CreateQuizUiState(isPublic = false))
@Preview(name = "Create Quiz - Validation", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizValidationPreview() = CreateQuizPreviewContent(CreateQuizUiState(
    validationErrors = android.kma.myquizzapp.core.common.validator.QuizEditorErrors(name = "Tên quiz phải có ít nhất 3 ký tự"),
))
@Preview(name = "Create Quiz - Saving", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizSavingPreview() = CreateQuizPreviewContent(CreateQuizUiState(isSubmitting = true))
@Preview(name = "Create Quiz - Save Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizSaveErrorPreview() = CreateQuizPreviewContent(CreateQuizUiState(errorMessage = "Không có kết nối mạng. Vui lòng thử lại."))

@Preview(name = "Create Quiz - Question Section", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun CreateQuizQuestionSectionPreview() = CreateQuizPreviewContent(initialItem = 6)
