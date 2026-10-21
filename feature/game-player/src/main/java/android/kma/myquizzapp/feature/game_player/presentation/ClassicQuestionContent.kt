package android.kma.myquizzapp.feature.game_player.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import android.kma.myquizzapp.core.ui.components.AnswerOptionItem
import android.kma.myquizzapp.core.ui.components.AnswerOptionSelectionMode
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pure Classic question presentation. No local grading, clock, socket, VM or effects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassicQuestionScreenContent(
    state: GameUiState,
    time: ClassicQuestionTime,
    onIntent: (GameIntent) -> Unit,
    onRequestExit: () -> Unit,
    listState: LazyListState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val question = state.question ?: return
    val page = classicPageColor()
    val border = classicBorderColor()
    Scaffold(
        // Apply IME insets to the screen viewport, not the bottomBar's measured height.
        // The footer stays above the keyboard; its navigation-bar inset is consumed here when IME is open.
        modifier = modifier.fillMaxSize().imePadding(), containerColor = page,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text("Câu ${question.index + 1}/${question.total}", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onRequestExit) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Rời màn chơi")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = page),
                )
                // Reconnect may have a deadline but no original duration: show seconds only.
                time.seconds?.let { seconds ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (state.isPaused) "Thời gian tạm dừng" else "Thời gian còn lại",
                            color = classicMutedColor(), style = MaterialTheme.typography.labelMedium)
                        Text("${seconds}s", fontWeight = FontWeight.SemiBold,
                            color = if (time.critical) FrontendColors.Danger else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                time.fraction?.let { fraction ->
                    Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                        .fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(border).semantics {
                            contentDescription = "Thời gian còn lại"
                            stateDescription = "${time.seconds ?: 0} giây"
                            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                        }) {
                        if (fraction > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction)
                            .background(if (time.critical) FrontendColors.Danger else MaterialTheme.colorScheme.primary))
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = page, tonalElevation = 0.dp) {
                Column(Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = border)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (state.revealsClassicFeedback) "${classicResultTitle(state)} · ${classicResultsFooter(state)}"
                            else if (state.phase is GamePhaseUi.Results) classicResultsFooter(state) else classicQuestionStatus(state), style = MaterialTheme.typography.bodyMedium,
                            color = classicMutedColor(), modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            })
                        when {
                            state.connection == GameConnection.RECONNECT_FAILED -> QuizPrimaryButton(
                                "Kết nối lại", { onIntent(GameIntent.Retry) }, modifier = Modifier.fillMaxWidth())
                            state.phase == GamePhaseUi.Question && question.questionType != "multiple_choice" ->
                                QuizPrimaryButton("Gửi đáp án", { onIntent(GameIntent.Submit) },
                                    enabled = state.canSubmit, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer
                    else FrontendColors.BrandTint, shape = RoundedCornerShape(50)) {
                    Text("Cổ điển", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                        lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (state.connection != GameConnection.CONNECTED || state.isPaused) item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                    Text(when {
                        state.connection != GameConnection.CONNECTED -> "Đang hiển thị câu hỏi từ lần đồng bộ gần nhất."
                        else -> "Host đang tạm dừng trận. Câu trả lời hiện không thể thay đổi."
                    }, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Text(question.questionText, style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
                    modifier = Modifier.semantics { heading() })
            }
            if (!question.questionImage.isNullOrBlank()) item {
                QuestionImage(question.questionImage, "Ảnh minh họa câu hỏi")
            }

            when (question.questionType) {
                "multiple_choice", "multiple_select" -> itemsIndexed(question.answerOptions, key = { _, option -> option.id }) { index, option ->
                    AnswerOptionItem(
                        label = if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString(),
                        text = option.text ?: option.id,
                        selected = if (question.questionType == "multiple_select") option.id in state.selectedOptionIds
                            else option.id == state.selectedOptionId,
                        onClick = {
                            onIntent(if (question.questionType == "multiple_select") GameIntent.ToggleOption(option.id)
                                else GameIntent.SelectOption(option.id))
                        },
                        enabled = state.isAnswerInputEnabled && state.phase == GamePhaseUi.Question,
                        feedback = when {
                            !state.revealsClassicFeedback -> AnswerOptionFeedback.None
                            option.id in state.results!!.correctAnswers -> AnswerOptionFeedback.Correct
                            option.id in state.acceptedAnswerKeys.orEmpty() -> AnswerOptionFeedback.Incorrect
                            else -> AnswerOptionFeedback.None
                        },
                        selectionMode = if (question.questionType == "multiple_select") AnswerOptionSelectionMode.Multiple
                            else AnswerOptionSelectionMode.Single,
                    )
                }
                "short_answer", "long_answer" -> item {
                    val longAnswer = question.questionType == "long_answer"
                    OutlinedTextField(
                        value = state.textAnswer, onValueChange = { onIntent(GameIntent.ChangeText(it)) },
                        readOnly = !state.isAnswerInputEnabled || state.phase != GamePhaseUi.Question,
                        label = { Text("Câu trả lời") },
                        placeholder = { Text(if (longAnswer) "Nhập câu trả lời của bạn" else "Nhập đáp án") },
                        singleLine = !longAnswer, minLines = if (longAnswer) 4 else 1,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = border),
                    )
                }
            }
            if (state.revealsClassicFeedback && question.questionType in listOf("short_answer", "long_answer")) item {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text("Đáp án được chấp nhận: ${state.results!!.correctAnswers.joinToString(" · ")}",
                        Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            question.questionHint?.takeIf { it.isNotBlank() && state.phase !is GamePhaseUi.Results }?.let { hint -> item {
                val dark = isSystemInDarkTheme()
                val accent = if (dark) MaterialTheme.colorScheme.onSecondaryContainer else FrontendColors.Info
                Surface(color = if (dark) MaterialTheme.colorScheme.secondaryContainer else FrontendColors.InfoBackground,
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp), tint = accent)
                            Text("Gợi ý", fontWeight = FontWeight.SemiBold, color = accent, fontSize = 13.sp)
                        }
                        Text(hint, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = if (dark) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } }
        }
    }
}

@Composable
private fun classicPageColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else FrontendColors.PageBackground
@Composable
private fun classicBorderColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border
@Composable
private fun classicMutedColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground

private fun classicQuestionFixture(type: String = "multiple_choice") = GameUiState(
    connection = GameConnection.CONNECTED, mode = GameMode.CLASSIC,
    sessionStatus = SessionStatus.ACTIVE, phase = GamePhaseUi.Question, isInputLocked = false,
    timeLimitSeconds = 30,
    question = PublicQuestion(0, 10, 1, type, "Quốc gia nào có diện tích lớn nhất thế giới?",
        questionHint = "Hãy nghĩ đến quốc gia trải dài qua hai châu lục.",
        answerOptions = listOf(PublicAnswerOption("0", "Canada"), PublicAnswerOption("1", "Liên bang Nga"),
            PublicAnswerOption("2", "Hoa Kỳ"), PublicAnswerOption("3", "Trung Quốc"))),
)

@Composable
private fun ClassicQuestionFixture(state: GameUiState, time: ClassicQuestionTime = ClassicQuestionTime(18, 0.6f)) {
    MyQuizAppTheme {
        ClassicQuestionScreenContent(state, time, {}, {}, rememberLazyListState(), remember { SnackbarHostState() })
    }
}

@Preview(name = "Classic - Một đáp án", showBackground = true, widthDp = 390)
@Composable private fun ClassicSinglePreview() = ClassicQuestionFixture(classicQuestionFixture().let { state ->
    state.copy(question = state.question?.copy(questionImage = "https://preview.invalid/question.png"))
})
@Preview(name = "Classic - Nhiều đáp án", showBackground = true, widthDp = 390)
@Composable private fun ClassicMultiplePreview() = ClassicQuestionFixture(
    classicQuestionFixture("multiple_select").copy(selectedOptionIds = setOf("0", "1")))
@Preview(name = "Classic - Trả lời ngắn, không giới hạn", showBackground = true, widthDp = 390)
@Composable private fun ClassicShortPreview() = ClassicQuestionFixture(
    classicQuestionFixture("short_answer").copy(textAnswer = "Liên bang Nga"), ClassicQuestionTime())
@Preview(name = "Classic - Trả lời dài, hẹp", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable private fun ClassicLongPreview() = ClassicQuestionFixture(
    classicQuestionFixture("long_answer").copy(textAnswer = "Câu trả lời dài để kiểm tra xuống dòng và phần nội dung cuộn."))
@Preview(name = "Classic - Đang xác nhận", showBackground = true, widthDp = 390)
@Composable private fun ClassicConfirmingPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    phase = GamePhaseUi.Submitted, isInputLocked = true, isConfirming = true, selectedOptionId = "1"))
@Preview(name = "Classic - Đã gửi", showBackground = true, widthDp = 390)
@Composable private fun ClassicSubmittedPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    phase = GamePhaseUi.Submitted, isInputLocked = true, selectedOptionId = "1"))
@Preview(name = "Classic - Khóa câu, thời gian cạn", showBackground = true, widthDp = 390)
@Composable private fun ClassicLockedPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    phase = GamePhaseUi.Locked, isInputLocked = true), ClassicQuestionTime(0, 0f, critical = true, expired = true))
@Preview(name = "Classic - Pause dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun ClassicPausedPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    sessionStatus = SessionStatus.PAUSED, isInputLocked = true))
@Preview(name = "Classic - Reconnect thiếu thời lượng gốc", showBackground = true, widthDp = 390)
@Composable private fun ClassicReconnectPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    connection = GameConnection.RECONNECT_FAILED, timeLimitSeconds = null, isInputLocked = true), ClassicQuestionTime(8))
@Preview(name = "Classic - Phản hồi sai tại chỗ", showBackground = true, widthDp = 390)
@Composable private fun ClassicInlineFeedbackPreview() = ClassicQuestionFixture(classicQuestionFixture().copy(
    phase = GamePhaseUi.Results(), isInputLocked = true, showCorrectAnswer = true,
    results = android.kma.myquizzapp.core.common.model.QuestionResults(0, correctAnswers = listOf("1")),
    outcome = QuestionOutcome.INCORRECT, selectedOptionId = "0", acceptedAnswerKeys = listOf("0"),
    hasAnswerAttempt = true, classicFeedbackRemainingMs = CLASSIC_FEEDBACK_DURATION_MS), ClassicQuestionTime())
@Preview(name = "Classic - Phản hồi chọn thiếu", showBackground = true, widthDp = 390)
@Composable private fun ClassicPartialFeedbackPreview() = ClassicQuestionFixture(classicQuestionFixture("multiple_select").copy(
    phase = GamePhaseUi.Results(), isInputLocked = true, showCorrectAnswer = true,
    results = android.kma.myquizzapp.core.common.model.QuestionResults(0, correctAnswers = listOf("0", "1")),
    outcome = QuestionOutcome.INCORRECT, selectedOptionIds = setOf("0"), acceptedAnswerKeys = listOf("0"),
    hasAnswerAttempt = true, classicFeedbackRemainingMs = CLASSIC_FEEDBACK_DURATION_MS), ClassicQuestionTime())
@Preview(name = "Classic - Phản hồi trả lời dài", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable private fun ClassicTextFeedbackPreview() = ClassicQuestionFixture(classicQuestionFixture("long_answer").copy(
    phase = GamePhaseUi.Results(), isInputLocked = true, showCorrectAnswer = true, textAnswer = "Nga",
    results = android.kma.myquizzapp.core.common.model.QuestionResults(0, correctAnswers = listOf("Nga", "Liên bang Nga")),
    outcome = QuestionOutcome.CORRECT, acceptedAnswerKeys = listOf("Nga"), hasAnswerAttempt = true,
    classicFeedbackRemainingMs = CLASSIC_FEEDBACK_DURATION_MS), ClassicQuestionTime())

