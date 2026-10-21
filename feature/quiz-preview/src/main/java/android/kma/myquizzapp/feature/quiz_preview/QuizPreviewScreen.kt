package android.kma.myquizzapp.feature.quiz_preview

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.AnswerOption
import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.ui.components.*
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun QuizPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val activeRun = !state.isLoading && state.quiz?.questions?.isNotEmpty() == true &&
        state.phase != PreviewPhase.FINISHED
    val requestExit: () -> Unit = {
        if (activeRun) showExitDialog = true else onNavigateBack()
    }
    BackHandler(enabled = activeRun) { requestExit() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                QuizPreviewEffect.Exit -> onNavigateBack()
                is QuizPreviewEffect.EditQuiz -> onNavigateToEditQuiz(effect.quizId)
            }
        }
    }
    LaunchedEffect(state.currentIndex) { listState.scrollToItem(0) }
    LaunchedEffect(state.phase) {
        if (state.phase != PreviewPhase.QUESTION) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    QuizPreviewScreenContent(
        state = state, onIntent = viewModel::onIntent,
        onRequestExit = requestExit, listState = listState, modifier = modifier,
    )
    if (showExitDialog && activeRun) {
        AppConfirmDialog(
            title = "Thoát chơi thử?",
            message = "Lượt chơi thử hiện tại sẽ kết thúc. Kết quả không được lưu lên máy chủ.",
            confirmText = "Thoát", dismissText = "Tiếp tục chơi",
            onConfirm = {
                showExitDialog = false
                viewModel.onIntent(QuizPreviewIntent.Exit)
            },
            onDismiss = { showExitDialog = false },
        )
    }
}

/** Pure presentation: lifecycle, navigation effects, list and dialog state belong to Screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPreviewScreenContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    onRequestExit: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val showQuestion = !state.isLoading && state.currentQuestion != null &&
        state.phase in setOf(PreviewPhase.QUESTION, PreviewPhase.FEEDBACK)
    Scaffold(
        // Resize the screen viewport for IME, not the footer/button's measured height.
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = previewPageColor(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            if (showQuestion) "Chơi thử (Câu ${state.currentIndex + 1}/${state.quiz?.questions?.size})"
                            else "Chơi thử",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onRequestExit) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Thoát chơi thử")
                        }
                    },
                    actions = {
                        if (showQuestion) {
                            IconButton(
                                onClick = { onIntent(QuizPreviewIntent.Skip) },
                                enabled = state.phase == PreviewPhase.QUESTION,
                            ) { Icon(Icons.Outlined.Close, "Bỏ qua câu hỏi") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = previewPageColor()),
                )
                if (showQuestion && (state.currentQuestion?.timeLimit ?: 0) > 0) {
                    PreviewTimeBar(
                        fraction = previewTimeFraction(state.currentQuestion, state.remainingSeconds, state.remainingTimeMillis),
                        critical = previewTimeIsCritical(state.currentQuestion, state.remainingSeconds, state.remainingTimeMillis),
                        remainingSeconds = state.remainingSeconds,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    )
                }
            }
        },
        bottomBar = {
            if (showQuestion) {
                Surface(color = previewPageColor(), tonalElevation = 0.dp) {
                    Column {
                        HorizontalDivider(color = previewBorderColor())
                        QuizPrimaryButton(
                            text = previewPrimaryLabel(state),
                            onClick = {
                                onIntent(if (state.phase == PreviewPhase.FEEDBACK)
                                    QuizPreviewIntent.Next else QuizPreviewIntent.Submit)
                            },
                            enabled = state.phase == PreviewPhase.FEEDBACK || state.canSubmit,
                            trailingIcon = if (state.phase == PreviewPhase.FEEDBACK) {
                                { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when {
            state.isLoading -> Centered(contentModifier) { CircularProgressIndicator() }
            state.errorMessage != null -> Centered(contentModifier) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                QuizPrimaryButton("Thử lại", { onIntent(QuizPreviewIntent.Retry) })
                TextButton(onClick = onRequestExit) { Text("Quay lại") }
            }
            state.quiz?.questions.isNullOrEmpty() -> Centered(contentModifier) {
                Text("Quiz này chưa có câu hỏi để chơi thử.")
                Spacer(Modifier.height(12.dp))
                QuizPrimaryButton("Quay lại quiz", onRequestExit)
            }
            else -> when (state.phase) {
                PreviewPhase.COUNTDOWN -> CountdownContent(state, contentModifier)
                PreviewPhase.QUESTION, PreviewPhase.FEEDBACK -> PreviewQuestionContent(
                    state, onIntent, listState, contentModifier,
                )
                PreviewPhase.FINISHED -> SummaryContent(state, onIntent, listState, contentModifier)
            }
        }
    }
}

@Composable
private fun previewPageColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else FrontendColors.PageBackground

@Composable
private fun previewBorderColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border

@Composable
private fun previewMutedColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground

@Composable
private fun PreviewTimeBar(
    fraction: Float,
    critical: Boolean,
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val safeFraction = fraction.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        .background(previewBorderColor()).semantics {
            contentDescription = "Thời gian còn lại"
            stateDescription = "${remainingSeconds.coerceAtLeast(0)} giây"
            progressBarRangeInfo = ProgressBarRangeInfo(safeFraction, 0f..1f)
        }) {
        if (safeFraction > 0f) Box(
            Modifier.fillMaxHeight().fillMaxWidth(safeFraction)
                .background(if (critical) FrontendColors.Danger else MaterialTheme.colorScheme.primary),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewBadges(totalScore: Int) {
    val tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(color = tint, shape = RoundedCornerShape(50)) {
            Text("Chế độ luyện tập • Không tính xếp hạng",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Surface(color = tint, shape = RoundedCornerShape(50), modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Tổng điểm ước tính: $totalScore"
        }) {
            Text("$totalScore điểm", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                lineHeight = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PreviewQuestionContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    listState: LazyListState,
    modifier: Modifier,
) {
    val question = state.currentQuestion ?: return
    val result = state.currentResult.takeIf { state.phase == PreviewPhase.FEEDBACK }
    val feedback = state.phase == PreviewPhase.FEEDBACK
    val selectedIds = if (feedback && result?.isAnswered != true) emptySet() else state.selectedOptionIds
    LazyColumn(
        state = listState, modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PreviewBadges(state.totalScore) }
        item {
            Text(question.questionText, style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp,
            ), modifier = Modifier.semantics { heading() })
        }
        if (!question.questionImage.isNullOrBlank()) item {
            QuestionImage(question.questionImage, "Ảnh minh họa câu hỏi")
        }
        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT -> {
                val options = question.answerOptions.orEmpty()
                items(options.size, key = { options[it].id }) { index ->
                    val option = options[index]
                    AnswerOptionItem(
                        label = if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString(),
                        text = option.optionText, selected = option.id in selectedIds,
                        onClick = {
                            onIntent(if (question.questionType == QuestionType.MULTIPLE_SELECT)
                                QuizPreviewIntent.ToggleOption(option.id) else QuizPreviewIntent.SelectOption(option.id))
                        },
                        enabled = !feedback,
                        feedback = previewOptionFeedback(question, option.id, selectedIds, result),
                        selectionMode = if (question.questionType == QuestionType.MULTIPLE_SELECT)
                            AnswerOptionSelectionMode.Multiple else AnswerOptionSelectionMode.Single,
                    )
                }
            }
            QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER -> item {
                PreviewTextAnswer(
                    value = if (feedback) result?.submittedAnswer.orEmpty() else state.textAnswer,
                    onChange = { onIntent(QuizPreviewIntent.ChangeText(it)) },
                    longAnswer = question.questionType == QuestionType.LONG_ANSWER,
                    feedback = feedback, result = result,
                )
            }
        }
        if (feedback && result != null && question.questionType in
            setOf(QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT)
        ) {
            val notice = when {
                !result.isAnswered -> "Bạn đã bỏ qua câu này"
                result.isCorrect == null -> "Không thể chấm câu hỏi này"
                result.isCorrect == false && question.questionType == QuestionType.MULTIPLE_SELECT ->
                    "Bộ đáp án chưa chính xác"
                else -> null
            }
            if (notice != null) item {
                // A missing correct option is green too: state the overall multi-answer result explicitly.
                Text(notice, color = if (result.isAnswered && result.isCorrect == false)
                    FrontendColors.Danger else previewMutedColor(), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        val supportingText = if (feedback) question.explanation else question.questionHint
        if (!supportingText.isNullOrBlank()) item {
            PreviewSupportingCard(if (feedback) "Giải thích" else "Gợi ý", supportingText)
        }
    }
}

@Composable
private fun PreviewTextAnswer(
    value: String,
    onChange: (String) -> Unit,
    longAnswer: Boolean,
    feedback: Boolean,
    result: PreviewQuestionResult?,
) {
    val correct = result?.isCorrect
    val accent = when (correct) {
        true -> if (isSystemInDarkTheme()) FrontendColors.Success else FrontendColors.SuccessForeground
        false -> FrontendColors.Danger
        null -> previewBorderColor()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value, onValueChange = onChange,
            readOnly = feedback,
            label = { Text("Câu trả lời") },
            placeholder = { Text(if (longAnswer) "Nhập câu trả lời của bạn" else "Nhập đáp án") },
            singleLine = !longAnswer, minLines = if (longAnswer) 4 else 1,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = accent,
                focusedBorderColor = if (feedback) accent else MaterialTheme.colorScheme.primary,
            ),
        )
        if (feedback && result != null) {
            val status = when (correct) {
                true -> "Chính xác!"
                false -> if (result.isAnswered) "Chưa chính xác" else "Đã bỏ qua"
                null -> "Không thể chấm câu hỏi này"
            }
            Surface(
                color = when (correct) {
                    true -> if (isSystemInDarkTheme()) FrontendColors.Success.copy(alpha = 0.12f)
                        else FrontendColors.CorrectAnswerBackground
                    false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    null -> MaterialTheme.colorScheme.surfaceVariant
                }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(status, color = if (correct == null) previewMutedColor() else accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    result.correctAnswer?.let { Text("Đáp án mẫu: $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun PreviewSupportingCard(title: String, body: String) {
    val dark = isSystemInDarkTheme()
    Surface(
        color = if (dark) MaterialTheme.colorScheme.secondaryContainer else FrontendColors.InfoBackground,
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val color = if (dark) MaterialTheme.colorScheme.onSecondaryContainer else FrontendColors.Info
                Icon(Icons.Outlined.Lightbulb, null, tint = color, modifier = Modifier.size(18.dp))
                Text(title, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Text(body, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = if (dark) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun CountdownContent(state: QuizPreviewUiState, modifier: Modifier) {
    GameStartCountdownContent(
        seconds = state.countdownSeconds,
        message = "Câu hỏi đầu tiên sắp xuất hiện. Hãy sẵn sàng!",
        subtitle = state.quiz?.quizName,
        modifier = modifier,
    )
}

@Composable
private fun SummaryContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    listState: LazyListState,
    modifier: Modifier
) {
    val ordered = state.results.sortedBy {
        when {
            it.isCorrect == false && it.isAnswered -> 0
            !it.isAnswered -> 1
            it.isCorrect == null -> 2
            else -> 3
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hoàn thành lượt chơi thử", style = MaterialTheme.typography.headlineSmall)
            Text("Không có phòng hoặc kết quả nào được lưu lên máy chủ.")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Điểm ước tính: ${state.totalScore}", style = MaterialTheme.typography.titleLarge)
                    Text("Đúng: ${state.correctCount}/${state.quiz?.questions?.size}")
                    Text("Đã trả lời: ${state.answeredCount}/${state.quiz?.questions?.size}")
                    Text("Độ chính xác: ${state.accuracy}%")
                    Text("Thời gian: ${state.elapsedSeconds}s")
                    Text(
                        "Điểm được tính cục bộ theo cấu hình Classic mặc định và có thể khác trận thật.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item { Text("Xem lại câu trả lời", style = MaterialTheme.typography.titleMedium) }
        items(ordered, key = { it.question.id }) { result -> PreviewReviewItem(result) }
        item {
            Button(
                onClick = { onIntent(QuizPreviewIntent.Restart) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Chơi lại") }
        }
        if (state.isOwner) {
            item {
                OutlinedButton(
                    onClick = { onIntent(QuizPreviewIntent.EditQuiz) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chỉnh sửa quiz") }
            }
        }
        item {
            TextButton(
                onClick = { onIntent(QuizPreviewIntent.Exit) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Thoát chơi thử") }
        }
    }
}

@Composable
private fun PreviewReviewItem(result: PreviewQuestionResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.question.questionText, style = MaterialTheme.typography.titleSmall)
            Text(
                when (result.isCorrect) {
                    true -> "Đúng • +${result.scoreEarned} điểm"
                    false -> if (result.isAnswered) "Sai" else "Bỏ qua"
                    null -> "Không thể chấm"
                }
            )
            result.submittedAnswer?.let { Text("Bạn trả lời: $it") }
            result.correctAnswer?.let { Text("Đáp án đúng: $it") }
            result.question.explanation?.takeIf(String::isNotBlank)?.let { Text(it) }
        }
    }
}


// Preview fixtures only. No ViewModel, Hilt, navigation, Flow or network dependency.
private fun previewFixture(
    type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    phase: PreviewPhase = PreviewPhase.QUESTION,
    selected: Set<Long> = emptySet(),
    correct: Boolean? = true,
    answered: Boolean = true,
    seconds: Int = 24,
    limit: Int = 30,
    image: Boolean = true,
    hint: String? = "Hãy nghĩ đến dãy Himalaya.",
    explanation: String? = "Đỉnh Everest nằm ở dãy Himalaya với độ cao 8.848m so với mực nước biển.",
): QuizPreviewUiState {
    val choice = type in setOf(QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT)
    val question = Question(
        id = 1, quizId = 1, questionType = type, questionText = "Đỉnh núi nào cao nhất thế giới?",
        timeLimit = limit, questionHint = hint, explanation = explanation,
        questionImage = if (image) "https://preview.invalid/mountain.png" else null,
        answerOptions = if (choice) listOf(AnswerOption(0, "K2"), AnswerOption(1, "Everest"),
            AnswerOption(2, "Kangchenjunga"), AnswerOption(3, "Lhotse")) else null,
        correctAnswer = if (type == QuestionType.MULTIPLE_SELECT)
            JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2)))
        else if (choice) JsonArray(listOf(JsonPrimitive(1))) else JsonPrimitive("Everest"),
    )
    val result = if (phase == PreviewPhase.FEEDBACK || phase == PreviewPhase.FINISHED) PreviewQuestionResult(
        question = question, submittedAnswer = if (!answered) null else if (correct == true) "Everest" else "K2",
        correctAnswer = "Everest", isCorrect = correct, isAnswered = answered,
        isLate = seconds == 0 && limit > 0, scoreEarned = if (correct == true && answered) 1_250 else 0,
        timeTakenSeconds = 15.0,
    ) else null
    return QuizPreviewUiState(
        isLoading = false,
        quiz = Quiz(id = 1, quizOwner = 10, quizName = "Đố vui địa lý", quizLanguage = "vi",
            isPublic = true, createdAt = "2026-10-11T00:00:00Z", updatedAt = "2026-10-11T00:00:00Z",
            questions = if (phase == PreviewPhase.FINISHED) listOf(question) else
                listOf(question) + (2L..15L).map { question.copy(id = it) }),
        phase = phase, remainingSeconds = seconds, selectedOptionIds = selected,
        textAnswer = if (phase == PreviewPhase.QUESTION && !choice) "Everest" else "",
        currentResult = result, results = listOfNotNull(result), totalScore = result?.scoreEarned ?: 0,
        isLate = seconds == 0 && limit > 0,
    )
}

@Composable
private fun PreviewScreenFixture(state: QuizPreviewUiState) {
    MyQuizAppTheme {
        val listState = rememberLazyListState()
        QuizPreviewScreenContent(state, {}, {}, listState)
    }
}

@Preview(name = "Preview Single Hint", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSingleHint() = PreviewScreenFixture(previewFixture())

@Preview(name = "Preview Single Correct", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSingleCorrect() = PreviewScreenFixture(previewFixture(
    phase = PreviewPhase.FEEDBACK, selected = setOf(1L),
))

@Preview(name = "Preview Single Wrong", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSingleWrong() = PreviewScreenFixture(previewFixture(
    phase = PreviewPhase.FEEDBACK, selected = setOf(0L), correct = false,
))

@Preview(name = "Preview Multiple Selecting", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewMultipleSelecting() = PreviewScreenFixture(previewFixture(
    type = QuestionType.MULTIPLE_SELECT, selected = setOf(0L, 1L),
))

@Preview(name = "Preview Multiple Wrong", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewMultipleWrong() = PreviewScreenFixture(previewFixture(
    type = QuestionType.MULTIPLE_SELECT, phase = PreviewPhase.FEEDBACK,
    selected = setOf(0L, 1L), correct = false,
))

@Preview(name = "Preview Short Answer", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewShortAnswer() = PreviewScreenFixture(previewFixture(type = QuestionType.SHORT_ANSWER, image = false))

@Preview(name = "Preview Short Feedback", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewShortFeedback() = PreviewScreenFixture(previewFixture(
    type = QuestionType.SHORT_ANSWER, phase = PreviewPhase.FEEDBACK, image = false, correct = false,
))

@Preview(name = "Preview Long Answer", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLongAnswer() = PreviewScreenFixture(previewFixture(type = QuestionType.LONG_ANSWER, image = false))

@Preview(name = "Preview Long Feedback", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLongFeedback() = PreviewScreenFixture(previewFixture(
    type = QuestionType.LONG_ANSWER, phase = PreviewPhase.FEEDBACK, image = false,
))

@Preview(name = "Preview Skipped", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSkipped() = PreviewScreenFixture(previewFixture(
    phase = PreviewPhase.FEEDBACK, correct = false, answered = false,
))

@Preview(name = "Preview Critical Time", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCriticalTime() = PreviewScreenFixture(previewFixture(seconds = 9))

@Preview(name = "Preview Time Expired", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewTimeExpired() = PreviewScreenFixture(previewFixture(seconds = 0))

@Preview(name = "Preview Untimed No Supporting Text", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewUntimed() = PreviewScreenFixture(previewFixture(limit = 0, seconds = 0, image = false, hint = null, explanation = null))

@Preview(name = "Preview Dark Feedback", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDarkFeedback() = PreviewScreenFixture(previewFixture(phase = PreviewPhase.FEEDBACK, selected = setOf(1L)))

@Preview(name = "Preview Narrow Large Font", showBackground = true, widthDp = 320, heightDp = 844, fontScale = 1.3f)
@Composable
private fun PreviewNarrow() = PreviewScreenFixture(previewFixture(type = QuestionType.LONG_ANSWER))

@Preview(name = "Preview Countdown", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCountdown() = PreviewScreenFixture(previewFixture(phase = PreviewPhase.COUNTDOWN))

@Preview(name = "Preview Summary", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSummary() = PreviewScreenFixture(previewFixture(phase = PreviewPhase.FINISHED, selected = setOf(1L)))

@Preview(name = "Preview Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewLoading() = PreviewScreenFixture(QuizPreviewUiState())

@Preview(name = "Preview Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewError() = PreviewScreenFixture(QuizPreviewUiState(isLoading = false, errorMessage = "Không có kết nối mạng. Vui lòng thử lại."))

@Preview(name = "Preview Exit Confirmation", showBackground = true, widthDp = 390)
@Composable
private fun PreviewExitConfirmation() {
    MyQuizAppTheme {
        val scroll = rememberScrollState()
        AppConfirmDialogContent(
            title = "Thoát chơi thử?",
            message = "Lượt chơi thử hiện tại sẽ kết thúc. Kết quả không được lưu lên máy chủ.",
            confirmText = "Thoát", dismissText = "Tiếp tục chơi",
            onConfirm = {}, onDismiss = {}, scrollState = scroll,
        )
    }
}

@Preview(name = "Preview Empty Quiz", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewEmptyQuiz() {
    val fixture = previewFixture()
    PreviewScreenFixture(fixture.copy(quiz = fixture.quiz?.copy(questions = emptyList())))
}

@Preview(name = "Preview Multiple Missing Correct Option", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewMultipleMissingOption() = PreviewScreenFixture(previewFixture(
    type = QuestionType.MULTIPLE_SELECT, phase = PreviewPhase.FEEDBACK,
    selected = setOf(1L), correct = false,
))
