package android.kma.myquizzapp.feature.game_player.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import android.kma.myquizzapp.core.ui.components.AnswerOptionItem
import android.kma.myquizzapp.core.ui.components.AnswerOptionSelectionMode
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless self-paced layout. Screen owns clocks, focus, scroll, dialogs and navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelfPacedQuestionScreenContent(
    state: GameUiState,
    time: SelfPacedTime,
    onIntent: (GameIntent) -> Unit,
    onRequestExit: () -> Unit,
    listState: LazyListState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val page = if (dark) MaterialTheme.colorScheme.background else FrontendColors.PageBackground
    val border = if (dark) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border
    val resultPhase = state.phase is GamePhaseUi.Results
    val question = state.question?.takeUnless { state.isPersonallyDone || state.phase == GamePhaseUi.Finished ||
        (resultPhase && !state.selfPacedResultMatchesQuestion) }
    val selected = selfPacedSelectedKeys(state)
    val correct = state.results?.correctAnswers.orEmpty().takeIf { state.selfPacedRevealsResult }.orEmpty()
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(), containerColor = page,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Column {
                        Text(selfPacedModeLabel(state), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(selfPacedProgressLabel(state), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } },
                    navigationIcon = { IconButton(onClick = onRequestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Rời màn chơi")
                    } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = page),
                )
                if (!state.isPersonallyDone && state.phase != GamePhaseUi.Finished) {
                    SelfPacedStatusHeader(state, time)
                }
            }
        },
        bottomBar = {
            Surface(color = page) {
                Column(Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = border)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(selfPacedFooterText(state), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        when {
                            state.connection == GameConnection.RECONNECT_FAILED -> QuizPrimaryButton(
                                "Kết nối lại", { onIntent(GameIntent.Retry) }, modifier = Modifier.fillMaxWidth())
                            state.shouldShowNextAction -> QuizPrimaryButton(
                                if (state.isRequestingNext) "Đang tải câu tiếp theo…" else "Câu tiếp theo",
                                { onIntent(GameIntent.NextQuestion) }, enabled = state.selfPacedCanRequestNext,
                                modifier = Modifier.fillMaxWidth())
                            state.selfPacedShowsSubmitAction ->
                                QuizPrimaryButton(if (state.isSubmitting) "Đang gửi…" else if (state.isConfirming)
                                    "Đang xác nhận…" else if (state.phase == GamePhaseUi.Submitted) "Đã gửi đáp án" else "Gửi đáp án",
                                    { onIntent(GameIntent.Submit) }, enabled = state.selfPacedCanAnswer && state.canSubmit,
                                    modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!state.selfPacedIsOnline || state.isPaused) item(key = "connection") {
                SelfPacedNotice(when {
                    !state.selfPacedIsOnline -> "Đang hiển thị trạng thái từ lần đồng bộ gần nhất."
                    else -> "Host đang tạm dừng trận. Câu trả lời hiện không thể thay đổi."
                })
            }
            when {
                state.isPersonallyDone -> item(key = "completion") { SelfPacedCompletionCard(state) }
                state.phase == GamePhaseUi.Finished -> item { SelfPacedNotice("Đang mở kết quả chung cuộc…") }
                resultPhase -> {
                    item(key = "feedback") { SelfPacedOutcomeCard(state) }
                    selfPacedStreakLabel(state)?.let { label -> item(key = "streak") { SelfPacedStreakBadge(label) } }
                }
            }
            if (question != null) {
                item(key = "question") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.mode == GameMode.MARATHON) Text("Câu ${question.index + 1}",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(question.questionText, style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
                            modifier = Modifier.semantics { heading() })
                    }
                }
                if (!question.questionImage.isNullOrBlank()) item(key = "image") {
                    QuestionImage(question.questionImage, "Ảnh minh họa câu hỏi")
                }
                when (question.questionType) {
                    "multiple_choice", "multiple_select" -> itemsIndexed(question.answerOptions,
                        key = { _, option -> "option-${option.id}" }) { index, option ->
                        AnswerOptionItem(
                            label = if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString(),
                            text = option.text ?: option.id, selected = option.id in selected,
                            onClick = { onIntent(if (question.questionType == "multiple_select")
                                GameIntent.ToggleOption(option.id) else GameIntent.SelectOption(option.id)) },
                            enabled = state.selfPacedCanAnswer,
                            feedback = when {
                                !state.selfPacedRevealsResult || correct.isEmpty() -> AnswerOptionFeedback.None
                                option.id in correct -> AnswerOptionFeedback.Correct
                                option.id in selected -> AnswerOptionFeedback.Incorrect
                                else -> AnswerOptionFeedback.None
                            },
                            selectionMode = if (question.questionType == "multiple_select")
                                AnswerOptionSelectionMode.Multiple else AnswerOptionSelectionMode.Single,
                        )
                    }
                    // TextField saves its scroll orientation. Do not carry horizontal state from
                    // short_answer into a multiline long_answer (or across question attempts).
                    "short_answer", "long_answer" -> item(
                        key = "text-answer-${question.id}-${question.index}-${question.questionType}",
                        contentType = question.questionType,
                    ) {
                        val longAnswer = question.questionType == "long_answer"
                        OutlinedTextField(value = if (resultPhase) state.acceptedAnswerKeys?.singleOrNull().orEmpty() else state.textAnswer,
                            onValueChange = { onIntent(GameIntent.ChangeText(it)) },
                            readOnly = !state.selfPacedCanAnswer, label = { Text("Câu trả lời") },
                            placeholder = { Text("Nhập câu trả lời của bạn") },
                            singleLine = !longAnswer, minLines = if (longAnswer) 4 else 1,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = border))
                    }
                    else -> item { SelfPacedNotice("Chưa hỗ trợ loại câu hỏi này.") }
                }
                if (state.selfPacedRevealsResult && correct.isNotEmpty() &&
                    question.questionType in listOf("short_answer", "long_answer")) item(key = "text-key") {
                    SelfPacedNotice("Đáp án được chấp nhận: ${correct.joinToString(" · ")}")
                }
                if (state.selfPacedRevealsResult && question.questionType == "multiple_select") {
                    state.acceptedAnswerKeys?.let { keys -> item(key = "selected-set") {
                        val labels = question.answerOptions.mapIndexedNotNull { index, option ->
                            if (option.id in keys) "${if (index < 26) ('A'.code + index).toChar().toString() else (index + 1).toString()}. ${option.text ?: option.id}" else null
                        }
                        Text("Bạn trả lời: ${labels.joinToString(" · ").ifBlank { "Không chọn đáp án" }}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                }
                question.questionHint?.takeIf { it.isNotBlank() && state.phase == GamePhaseUi.Question }?.let { hint ->
                    item(key = "hint") { SelfPacedNotice("Gợi ý: $hint") }
                }
            } else if (!state.isPersonallyDone && !resultPhase && state.phase != GamePhaseUi.Finished) item {
                SelfPacedNotice("Đang chờ máy chủ gửi câu hỏi…")
            }
        }
    }
}

@Composable
private fun SelfPacedStatusHeader(state: GameUiState, time: SelfPacedTime) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.lives?.let { lives ->
            SelfPacedNotice("♥  Mạng còn lại: $lives", emphasized = true)
        }
        if (state.hasMatchBudget) {
            val clock = time.matchSeconds?.let(::selfPacedClockLabel)
            val color = if (time.matchSeconds?.let { it <= 30 } == true) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thời gian của bạn", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Text(clock ?: if (state.isPaused) "Tạm dừng" else "Đang đồng bộ…",
                        fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        time.question.seconds?.let { seconds ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (state.allowAnswerLate && seconds == 0) "Vẫn cho phép trả lời muộn" else "Thời gian câu hỏi",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${seconds}s", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                    color = if (time.question.critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
        time.question.fraction?.let { fraction ->
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(5.dp)
                .semantics { contentDescription = "Thời gian câu hỏi còn lại" },
                color = if (time.question.critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SelfPacedNotice(text: String, emphasized: Boolean = false) {
    Surface(color = if (emphasized) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (emphasized) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SelfPacedOutcomeCard(state: GameUiState) {
    val positive = state.selfPacedRevealsResult && state.outcome == QuestionOutcome.CORRECT
    val negative = state.selfPacedRevealsResult && state.outcome == QuestionOutcome.INCORRECT
    val dark = isSystemInDarkTheme()
    val background = when {
        positive -> if (dark) Color(0xFF123F32) else Color(0xFFE5F8EF)
        negative -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when {
        positive -> if (dark) Color(0xFF7FE0B2) else Color(0xFF006B48)
        negative -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(when { positive -> Icons.Outlined.CheckCircle; negative -> Icons.Outlined.Cancel; else -> Icons.Outlined.Info },
                null, Modifier.size(36.dp))
            Text(selfPacedOutcomeTitle(state), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            selfPacedScoreDeltaLabel(state)?.let { Text(it, fontWeight = FontWeight.Bold) }
            if (state.selfPacedRevealsResult && !state.selfPacedIsPractice) state.totalScore?.let {
                Text("Tổng điểm: $it", style = MaterialTheme.typography.bodyMedium)
            }
            if (state.timedOut && state.selfPacedResultMatchesQuestion && state.acceptedAnswerKeys == null)
                Text("Bạn chưa gửi đáp án cho câu này.", style = MaterialTheme.typography.bodyMedium)
            if (state.wasLate && state.selfPacedResultMatchesQuestion) Text("Đáp án được ghi nhận sau thời hạn.",
                style = MaterialTheme.typography.bodySmall)
            if ((state.phase as? GamePhaseUi.Results)?.restoredWithoutDetails == true ||
                !state.selfPacedResultMatchesQuestion) Text("Chi tiết câu trước chưa có trong lần đồng bộ này.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SelfPacedStreakBadge(label: String) {
    val dark = isSystemInDarkTheme()
    Surface(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        .clearAndSetSemantics { contentDescription = label }, shape = RoundedCornerShape(50),
        color = if (dark) Color(0xFF3F2E13) else Color(0xFFFFF7DF),
        contentColor = if (dark) Color(0xFFFFD98A) else Color(0xFF8A4B00)) {
        Text("$label 🔥", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelfPacedCompletionCard(state: GameUiState) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(if (state.phase == GamePhaseUi.Eliminated) Icons.Outlined.Info else Icons.Outlined.CheckCircle,
                null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(selfPacedCompletionTitle(state), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("Kết quả đã được ghi nhận. Đang chờ những người chơi còn lại.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (!state.selfPacedIsPractice) (state.totalScore ?: state.playerScore)?.let {
                Text("Tổng điểm: $it", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            if (!state.selfPacedIsPractice && state.canShowLiveLeaderboard) state.playerRank?.let {
                Text("Thứ hạng hiện tại: #$it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Preview-only fixtures: no socket, ViewModel, ticking clock or runtime fallback data.
private fun selfPacedFixture(mode: GameMode = GameMode.SOLO, type: String = "multiple_choice") = GameUiState(
    connection = GameConnection.CONNECTED, sessionStatus = SessionStatus.ACTIVE,
    mode = mode, pacing = Pacing.SELF, phase = GamePhaseUi.Question, showCorrectAnswer = true,
    isInputLocked = false, autoAdvance = true, timeLimitSeconds = 30,
    lives = if (mode == GameMode.SURVIVAL) 3 else null,
    matchEndsAt = if (mode == GameMode.MARATHON) "2026-10-20T04:05:00Z" else null,
    question = PublicQuestion(0, 10, 1, type, "Quốc gia nào có diện tích lớn nhất thế giới?",
        questionHint = "Hãy nghĩ đến quốc gia trải dài qua hai châu lục.",
        answerOptions = if (type in listOf("multiple_choice", "multiple_select")) listOf(
            PublicAnswerOption("a", "Canada"), PublicAnswerOption("b", "Liên bang Nga"),
            PublicAnswerOption("c", "Hoa Kỳ"), PublicAnswerOption("d", "Trung Quốc")) else emptyList()),
)
private fun selfPacedFeedbackFixture(mode: GameMode = GameMode.SOLO) = selfPacedFixture(mode).copy(
    phase = GamePhaseUi.Results(), isInputLocked = true, outcome = QuestionOutcome.CORRECT,
    selectedOptionId = "b", acceptedAnswerKeys = listOf("b"), hasAnswerAttempt = true,
    results = QuestionResults(0, correctAnswers = listOf("b")), scoreEarned = 950, totalScore = 3420, streak = 3,
)
@Composable
private fun SelfPacedPreview(state: GameUiState, time: SelfPacedTime = SelfPacedTime()) {
    MyQuizAppTheme { SelfPacedQuestionScreenContent(state, time, {}, {}, rememberLazyListState(), remember { SnackbarHostState() }) }
}
@Preview(name = "Solo · Chọn rồi gửi", widthDp = 390, heightDp = 844)
@Composable private fun SoloQuestionPreview() = SelfPacedPreview(selfPacedFixture().copy(selectedOptionId = "b"),
    SelfPacedTime(ClassicQuestionTime(18, 0.6f)))
@Preview(name = "Solo · Kết quả và Next thủ công", widthDp = 390, heightDp = 844)
@Composable private fun SoloFeedbackPreview() = SelfPacedPreview(selfPacedFeedbackFixture().copy(autoAdvance = false, canRequestNext = true))
@Preview(name = "Solo · Nhiều lựa chọn sai", widthDp = 390, heightDp = 844)
@Composable private fun SoloMultiplePreview() = SelfPacedPreview(selfPacedFeedbackFixture().let {
    it.copy(question = it.question?.copy(questionType = "multiple_select"), outcome = QuestionOutcome.INCORRECT,
        results = QuestionResults(0, correctAnswers = listOf("a", "b")), acceptedAnswerKeys = listOf("a", "c"), streak = 0, scoreEarned = 0)
})
@Preview(name = "Solo · Nhập ngắn", widthDp = 390, heightDp = 844)
@Composable private fun SoloShortPreview() = SelfPacedPreview(selfPacedFixture(type = "short_answer").copy(textAnswer = "Liên bang Nga"))
@Preview(name = "Solo · Nhập dài, hẹp chữ lớn", widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Composable private fun SoloLongPreview() = SelfPacedPreview(selfPacedFixture(type = "long_answer").copy(textAnswer = "Câu trả lời dài để kiểm tra xuống dòng và vùng cuộn."))
@Preview(name = "Survival · Còn một mạng", widthDp = 390, heightDp = 844)
@Composable private fun SurvivalQuestionPreview() = SelfPacedPreview(selfPacedFixture(GameMode.SURVIVAL).copy(lives = 1),
    SelfPacedTime(ClassicQuestionTime(5, 0.16f, critical = true)))
@Preview(name = "Survival · Bị loại", widthDp = 390, heightDp = 844)
@Composable private fun SurvivalEliminatedPreview() = SelfPacedPreview(selfPacedFixture(GameMode.SURVIVAL).copy(
    phase = GamePhaseUi.Eliminated, lives = 0, totalScore = 1800, isInputLocked = true))
@Preview(name = "Marathon · Đồng hồ riêng", widthDp = 390, heightDp = 844)
@Composable private fun MarathonQuestionPreview() = SelfPacedPreview(selfPacedFixture(GameMode.MARATHON).let {
    it.copy(question = it.question?.copy(index = 12), lives = 2)
}, SelfPacedTime(ClassicQuestionTime(8, 0.27f, critical = true), matchSeconds = 24))
@Preview(name = "Marathon · Hoàn thành", widthDp = 390, heightDp = 844)
@Composable private fun MarathonFinishedPreview() = SelfPacedPreview(selfPacedFixture(GameMode.MARATHON).copy(
    phase = GamePhaseUi.PlayerFinished, matchTimeExpired = true, isInputLocked = true, totalScore = 9650))
@Preview(name = "Practice · Không đồng hồ hoặc điểm", widthDp = 390, heightDp = 844)
@Composable private fun PracticeQuestionPreview() = SelfPacedPreview(selfPacedFixture(GameMode.PRACTICE))
@Preview(name = "Practice · Feedback và streak", widthDp = 390, heightDp = 844)
@Composable private fun PracticeFeedbackPreview() = SelfPacedPreview(selfPacedFeedbackFixture(GameMode.PRACTICE))
@Preview(name = "Self-paced · Kết quả ẩn", widthDp = 390, heightDp = 844)
@Composable private fun SelfPacedHiddenPreview() = SelfPacedPreview(selfPacedFeedbackFixture().copy(showCorrectAnswer = false))
@Preview(name = "Self-paced · Đồng bộ thiếu chi tiết, Next", widthDp = 390, heightDp = 844)
@Composable private fun SelfPacedRestoredPreview() = SelfPacedPreview(selfPacedFeedbackFixture().copy(
    phase = GamePhaseUi.Results(restoredWithoutDetails = true), autoAdvance = false, canRequestNext = true))
@Preview(name = "Self-paced · Mất kết nối", widthDp = 320, heightDp = 740, fontScale = 1.3f)
@Composable private fun SelfPacedReconnectPreview() = SelfPacedPreview(selfPacedFixture().copy(
    connection = GameConnection.RECONNECT_FAILED, isInputLocked = true))
@Preview(name = "Self-paced · Tạm dừng dark", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun SelfPacedPausedPreview() = SelfPacedPreview(selfPacedFixture(GameMode.MARATHON).copy(sessionStatus = SessionStatus.PAUSED))
