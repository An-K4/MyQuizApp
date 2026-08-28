package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuizEditorContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
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
    var pendingPickTarget by remember { mutableStateOf<PickTarget?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri != null && target != null) {
            when (target) {
                PickTarget.Cover -> viewModel.handleIntent(CreateQuizIntent.PickCoverImage(uri))
                is PickTarget.Question -> viewModel.handleIntent(CreateQuizIntent.PickQuestionImage(target.localId, uri))
            }
        }
    }
    val launchImagePicker: (PickTarget) -> Unit = { target ->
        pendingPickTarget = target
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is CreateQuizEffect.QuizCreated) onQuizCreated(effect.quizId)
        }
    }

    CreateQuizScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::handleIntent,
        onPickCoverImage = { launchImagePicker(PickTarget.Cover) },
        onPickQuestionImage = { launchImagePicker(PickTarget.Question(it)) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreenContent(
    uiState: CreateQuizUiState,
    onNavigateBack: () -> Unit,
    onIntent: (CreateQuizIntent) -> Unit,
    onPickCoverImage: () -> Unit,
    onPickQuestionImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
        QuizEditorContent(
            quizName = uiState.quizName,
            quizDescription = uiState.quizDescription,
            quizLanguage = uiState.quizLanguage,
            quizCategory = uiState.quizCategory,
            isPublic = uiState.isPublic,
            coverImageModel = uiState.coverImageUri,
            questions = uiState.questions,
            fieldErrors = uiState.fieldErrors,
            errorMessage = uiState.errorMessage,
            isSubmitting = uiState.isSubmitting,
            submitLabel = "Tạo quiz",
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

private sealed interface PickTarget {
    data object Cover : PickTarget
    data class Question(val localId: String) : PickTarget
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CreateQuizScreenContentPreview() {
    MyQuizAppTheme {
        CreateQuizScreenContent(
            uiState = CreateQuizUiState(),
            onNavigateBack = {},
            onIntent = {},
            onPickCoverImage = {},
            onPickQuestionImage = {}
        )
    }
}
