package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.AnswerOptionItem
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One stateless shell for Classic active/locked/results. All local state and clocks live in Screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HostClassicQuestionScreenContent(
    state: HostGameUiState,
    time: HostQuestionTime,
    onIntent: (HostGameIntent) -> Unit,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    optionsExpanded: Boolean,
    onToggleOptions: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onRequestClose: () -> Unit,
    onRequestNext: () -> Unit,
    onRequestEnd: () -> Unit,
    onRequestLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = hostPageColor()
    val border = hostBorderColor()
    val muted = hostMutedColor()
    val showingResults = state.isShowingQuestionResults
    val progress = hostQuestionProgress(state)
    Scaffold(
        modifier = modifier.fillMaxSize(), containerColor = page,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${state.questionNumberLabel} · Cổ điển", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = page),
                actions = {
                    Box {
                        IconButton(onClick = { onMenuExpandedChange(true) }) {
                            Icon(Icons.Default.MoreHoriz, "Tùy chọn điều khiển trận")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                            DropdownMenuItem(text = { Text("Kết thúc trận", color = MaterialTheme.colorScheme.error) },
                                enabled = state.canEndGame, onClick = onRequestEnd)
                            DropdownMenuItem(text = { Text("Rời màn điều khiển") }, onClick = onRequestLeave)
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = page) {
                Column(Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = border)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val status = if (showingResults && state.autoAdvance && !state.isPaused &&
                            !state.isSendingCommand && time.seconds != null && state.isStateSynchronized &&
                            state.connection == HostGameConnection.CONNECTED)
                            if (time.seconds == 0) time.label else "Câu tiếp theo sẽ bắt đầu sau: ${time.seconds}s"
                        else hostQuestionStatus(state)
                        Text(status, color = muted, fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        if (state.connection == HostGameConnection.RECONNECT_FAILED) {
                            QuizPrimaryButton("Kết nối lại", { onIntent(HostGameIntent.Retry) }, Modifier.fillMaxWidth())
                        } else {
                            QuizSecondaryButton(state.pauseLabel, { onIntent(HostGameIntent.PauseOrResume) },
                                Modifier.fillMaxWidth(), enabled = state.canPauseOrResume)
                            if (!state.autoAdvance) {
                                if (showingResults) QuizPrimaryButton("Câu tiếp theo", onRequestNext,
                                    Modifier.fillMaxWidth(), enabled = state.canAdvanceFromResults, loading = state.isSendingCommand)
                                else if (state.phase == GamePhase.QUESTION_ACTIVE)
                                    QuizPrimaryButton("Chốt câu & hiện kết quả", onRequestClose, Modifier.fillMaxWidth(),
                                        enabled = state.canCloseClassicQuestion, loading = state.isSendingCommand)
                            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.connection != HostGameConnection.CONNECTED || !state.isStateSynchronized || state.isPaused) item(key = "connection") {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                    Text(if (state.isPaused && state.isStateSynchronized) "Trận đang tạm dừng. Đồng hồ sẽ tiếp tục theo mốc máy chủ khi bạn tiếp tục."
                        else "Đang hiển thị dữ liệu từ lần đồng bộ gần nhất. Các lệnh cần trạng thái mới đã bị khóa.",
                        Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (showingResults) {
                item(key = "question-results") {
                    Text("Kết quả câu ${state.index + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() })
                }
                // No distribution block or substitute summary for short/long text questions.
                if (hostIsChoiceQuestion(state)) item(key = "distribution") { HostResultsDistribution(state) }
            } else {
                item(key = "live-progress") {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        HostAnswerProgressRing(progress, emptyRoom = state.hasAnswerProgress && state.activePlayers == 0)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Timer, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(time.seconds?.let { "${it.toString().padStart(2, '0')}s" } ?: time.label,
                                    fontSize = if (time.seconds != null) 30.sp else 16.sp,
                                    lineHeight = if (time.seconds != null) 36.sp else 22.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            if (time.seconds != null) Text(time.label, color = muted, fontSize = 12.sp)
                        }
                    }
                }
                item(key = "live-progress-detail") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tiến độ trả lời trực tiếp", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() })
                        HostProgressLine("Đã trả lời", progress?.answered, progress?.fraction, MaterialTheme.colorScheme.primary)
                        HostProgressLine("Chưa trả lời", progress?.pending, progress?.let { 1f - it.fraction }, FrontendColors.Warning)
                        Text("Tính trên người chơi đang kết nối. Không công bố lựa chọn của người chơi trước kết quả.",
                            color = muted, fontSize = 11.sp, lineHeight = 16.sp)
                        if (progress == null) Text("Chưa đủ dữ liệu của câu hiện tại; chờ cập nhật từ máy chủ.",
                            color = muted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            if (!showingResults) item(key = "question") {
                HostRoundQuestionCard(state, optionsExpanded, onToggleOptions, onIntent)
            }
            if (showingResults) {
                if (hostShowsResultsLeaderboard(state)) {
                    item(key = "results-board-heading") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bảng xếp hạng", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() })
                            Text("Điểm và số câu đúng/sai tích lũy của cả trận.", color = muted, fontSize = 12.sp)
                        }
                    }
                    if (!state.hasResultsLeaderboard) item(key = "results-board-waiting") {
                        Text("Đang chờ bảng xếp hạng cập nhật sau câu này…", color = muted)
                    } else if (state.leaderboard.rows.isEmpty()) item(key = "results-board-empty") {
                        Text("Chưa có người chơi trong bảng xếp hạng.", color = muted)
                    } else items(state.leaderboard.rows, key = { "results-player:${it.id}" }) { row -> HostCumulativePlayerRow(row) }
                } else if (state.showLeaderboard == null) item(key = "results-board-config") {
                    Text("Đang đồng bộ cấu hình bảng xếp hạng…", color = muted)
                }
            }
        }
    }
}

@Composable
private fun HostRoundQuestionCard(state: HostGameUiState, expanded: Boolean, onToggle: () -> Unit,
    onIntent: (HostGameIntent) -> Unit) {
    val question = hostCurrentQuestion(state)
    Surface(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer
        else FrontendColors.BrandTint, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when {
                state.isShowingQuestionResults -> "CÂU HỎI VỪA KẾT THÚC"
                state.phase == GamePhase.QUESTION_LOCKED -> "CÂU HỎI ĐÃ ĐÓNG"
                else -> "CÂU HỎI ĐANG DIỄN RA"
            }, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
            if (question == null) Text("Đang chờ nội dung câu hỏi từ máy chủ…")
            else {
                Text(question.questionText, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                // The image is part of the question and stays visible even with the answer section collapsed.
                QuestionImage(question.questionImage, "Ảnh minh họa câu hỏi")
                HostDisclosure(if (expanded) "Thu gọn phần trả lời" else "Mở phần trả lời", expanded, onToggle)
                if (expanded) {
                    if (hostIsChoiceQuestion(state)) {
                        question.answerOptions.forEachIndexed { index, option ->
                            AnswerOptionItem(hostOptionLetter(index), option.text ?: option.id, selected = false,
                                onClick = {}, readOnly = true, showReadOnlyFeedback = true,
                                feedback = if (hostOptionIsCorrect(state, option.id)) AnswerOptionFeedback.Correct else AnswerOptionFeedback.None)
                        }
                    } else if (state.isAnswerRevealed && state.hasAnswerKey) {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Đáp án được chấp nhận", fontWeight = FontWeight.Bold)
                                val lines = hostAnswerKeyLines(state)
                                if (lines.isEmpty()) Text("Không có đáp án mẫu để hiển thị.")
                                lines.forEach { Text(it, fontSize = 14.sp, lineHeight = 20.sp) }
                            }
                        }
                    }
                    if (state.canRevealAnswer) {
                        Text(if (state.isShowingQuestionResults && state.showCorrectAnswer == true)
                            "Đáp án được công bố theo cấu hình phòng."
                            else "Đáp án riêng Host — lưu ý khi trình chiếu.", color = hostMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
                        QuizPrimaryButton(if (state.isAnswerRevealed) "Ẩn đáp án đúng" else "Hiện đáp án đúng",
                            { onIntent(HostGameIntent.ToggleAnswerKey) }, Modifier.fillMaxWidth())
                    } else Text("Chưa nhận được đáp án mẫu cho câu này sau đồng bộ. Không suy đoán từ nội dung câu hỏi.",
                        color = hostMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun HostResultsDistribution(state: HostGameUiState) {
    val columns = hostResultColumns(state)
    Surface(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant
        else FrontendColors.BrandTint.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Phân bố câu trả lời", fontWeight = FontWeight.Bold)
            if (columns == null) Text(hostDistributionUnavailableLabel(state), color = hostMutedColor(), fontSize = 13.sp, lineHeight = 19.sp)
            else {
                val total = hostCurrentResults(state)?.stats?.total ?: 0
                Text("Đã nhận $total bài · mỗi cột là số lượt chọn", color = hostMutedColor(), fontSize = 12.sp, lineHeight = 17.sp)
                val maxSelections = columns.maxOfOrNull { it.selections }?.coerceAtLeast(1) ?: 1
                Text("Thang đo: 0–$maxSelections lượt chọn", color = hostMutedColor(), fontSize = 11.sp)
                // Wrap after four options without reordering or changing the common scale.
                columns.withIndex().toList().chunked(4).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        group.forEach { indexed ->
                            val column = indexed.value
                            val color = hostOptionColor(indexed.index)
                            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                                contentDescription = "${column.label}. ${column.text}: ${column.selections} lượt chọn" +
                                    if (column.isCorrect) ", đáp án đúng" else ""
                            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${column.selections}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.BottomCenter) {
                                    if (column.selections > 0) Box(Modifier.widthIn(max = 42.dp).fillMaxWidth()
                                        .height(116.dp * (column.selections.toFloat() / maxSelections))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(color))
                                    HorizontalDivider(color = hostBorderColor(), modifier = Modifier.align(Alignment.BottomCenter))
                                }
                                Text(column.label + if (column.isCorrect) " ✓" else "", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        repeat(minOf(4, columns.size) - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                if (total == 0) Text("Chưa có bài trả lời cho câu này.", color = hostMutedColor(), fontSize = 12.sp)
                if (hostCurrentQuestion(state)?.questionType == "multiple_select") Text(
                    "Một người có thể chọn nhiều đáp án; tổng lượt chọn không phải số người chơi.",
                    color = hostMutedColor(), fontSize = 11.sp, lineHeight = 16.sp)
                // A text legend keeps long option labels and counts available without relying on color or hover.
                columns.forEach { column ->
                    Text("${column.label}. ${column.text} — ${column.selections} lượt chọn" + if (column.isCorrect) " · Đúng ✓" else "",
                        fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

private fun hostOptionColor(index: Int): Color = when (index % 4) {
    0 -> Color(0xFFEF4444)
    1 -> Color(0xFF3B82F6)
    2 -> Color(0xFFF59E0B)
    else -> Color(0xFF10B981)
}

@Composable
private fun HostDisclosure(title: String, expanded: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics {
        stateDescription = if (expanded) "Đã mở" else "Đã thu gọn"
    }) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
    }
}

@Composable
private fun HostAnswerProgressRing(progress: HostQuestionProgress?, emptyRoom: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val track = hostBorderColor()
    Box(Modifier.size(184.dp).semantics(mergeDescendants = true) {
        contentDescription = "Tiến độ trả lời câu hiện tại"
        if (progress != null) {
            progressBarRangeInfo = ProgressBarRangeInfo(progress.fraction, 0f..1f)
            stateDescription = "${progress.answered} trên ${progress.total} người đã trả lời"
        } else stateDescription = if (emptyRoom) "Chưa có người chơi đang kết nối" else "Đang đồng bộ tiến độ"
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = Stroke(width = 12.dp.toPx())
            drawCircle(track, style = stroke)
            if (progress != null && progress.fraction > 0f)
                drawArc(primary, -90f, progress.fraction * 360f, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(18.dp)) {
            Text(progress?.let { "${it.answered} / ${it.total}" } ?: "—", fontSize = 30.sp,
                lineHeight = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (progress != null) "Người đã trả lời" else if (emptyRoom) "Chưa có người chơi" else "Đang đồng bộ",
                fontSize = 11.sp, lineHeight = 16.sp, color = hostMutedColor())
            if (progress != null) Text("${progress.percent}%", color = primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HostProgressLine(label: String, count: Int?, fraction: Float?, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = hostMutedColor(), fontSize = 12.sp)
            Text(count?.let { "$it người" } ?: "—", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(hostBorderColor())) {
            if (fraction != null && fraction > 0f)
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
        }
    }
}

@Composable
private fun HostCumulativePlayerRow(row: HostLeaderboardRow) {
    Surface(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant
        else FrontendColors.BrandTint.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("#${row.rank}", fontWeight = FontWeight.Bold)
                Text(row.playerName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("${row.playerScore} điểm", fontWeight = FontWeight.SemiBold)
            }
            Text("Đã làm ${row.answeredCount} câu · Đúng ${row.correctCount} · Sai ${row.wrongCount}",
                fontSize = 12.sp, lineHeight = 17.sp, color = hostMutedColor())
            Text(when (row.status) {
                "connected" -> "Đang kết nối"
                "disconnected" -> "Mất kết nối"
                "finished" -> "Đã hoàn thành"
                "eliminated" -> "Đã bị loại"
                else -> "Chưa xác định trạng thái"
            }, fontSize = 12.sp, color = hostMutedColor())
        }
    }
}

@Composable private fun hostPageColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else FrontendColors.PageBackground
@Composable private fun hostBorderColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border
@Composable private fun hostMutedColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground

private fun hostClassicFixture() = HostGameUiState(
    connection = HostGameConnection.CONNECTED, sessionStatus = SessionStatus.ACTIVE,
    mode = GameMode.CLASSIC, phase = GamePhase.QUESTION_ACTIVE, index = 3, totalQuestions = 15,
    autoAdvance = false, isStateSynchronized = true, isQuestionClockKnown = true,
    showCorrectAnswer = true, showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
    answeredCount = 18, activePlayers = 20, hasAnswerProgress = true,
    question = PublicQuestion(3, 15, 4, "multiple_choice", "Quốc gia nào có diện tích lớn nhất thế giới?",
        answerOptions = listOf(PublicAnswerOption("ca", "Canada"), PublicAnswerOption("ru", "Liên bang Nga"),
            PublicAnswerOption("us", "Hoa Kỳ"), PublicAnswerOption("cn", "Trung Quốc"))),
    hasAnswerKey = true, correctAnswers = listOf("ru"),
    leaderboard = HostLeaderboard(rows = (1..8).map { index ->
        HostLeaderboardRow(index, index.toLong(), "Người chơi $index", 5000 - index * 100, 4, 3, 1)
    }),
)

private fun hostResultsFixture() = hostClassicFixture().copy(
    phase = GamePhase.SHOWING_RESULTS, isAnswerRevealed = true, hasResultsLeaderboard = true,
    results = QuestionResults(3, 4, listOf("ru"), AnswerStats(20, mapOf("ca" to 2, "ru" to 15, "us" to 1, "cn" to 2))),
)

@Composable
private fun HostClassicFixture(state: HostGameUiState = hostClassicFixture(),
    time: HostQuestionTime = HostQuestionTime(8, "Thời gian còn lại"), options: Boolean = false) {
    MyQuizAppTheme {
        HostClassicQuestionScreenContent(state, time, {}, rememberLazyListState(), remember { SnackbarHostState() },
            options, {}, false, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Host - Active ảnh luôn hiện", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable private fun HostManualPreview() = HostClassicFixture(hostClassicFixture().let {
    it.copy(question = it.question?.copy(questionImage = "https://preview.invalid/host-question.png"))
})
@Preview(name = "Host - Mở đáp án chọn nhiều", showBackground = true, widthDp = 390, heightDp = 1350)
@Composable private fun HostOptionsPreview() = HostClassicFixture(hostClassicFixture().let {
    it.copy(isAnswerRevealed = true, correctAnswers = listOf("ru", "cn"), question = it.question?.copy(questionType = "multiple_select"))
}, options = true)
@Preview(name = "Host - Kết quả manual full bảng hạng", showBackground = true, widthDp = 390, heightDp = 1800)
@Composable private fun HostResultsPreview() = HostClassicFixture(hostResultsFixture(),
    HostQuestionTime(label = "Chờ Host chuyển câu"), options = true)
@Preview(name = "Host - Kết quả auto dark", showBackground = true, widthDp = 390, heightDp = 1200,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun HostAutoPreview() = HostClassicFixture(hostResultsFixture().copy(autoAdvance = true),
    HostQuestionTime(2, "Câu tiếp theo sẽ bắt đầu"), options = true)
@Preview(name = "Host - Kết quả tự luận không biểu đồ", showBackground = true, widthDp = 390, heightDp = 1300)
@Composable private fun HostTextPreview() = HostClassicFixture(hostResultsFixture().let {
    it.copy(question = it.question?.copy(questionType = "long_answer", answerOptions = emptyList()),
        correctAnswers = listOf("Liên bang Nga — đáp án mẫu dài để kiểm tra xuống dòng trong cùng card câu hỏi."))
}, HostQuestionTime(label = "Chờ Host chuyển câu"), options = true)
@Preview(name = "Host - Cấu hình ẩn đáp án và bảng hạng", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable private fun HostHiddenPreview() = HostClassicFixture(hostResultsFixture().copy(
    showCorrectAnswer = false, showLeaderboard = ShowLeaderboard.NEVER, isAnswerRevealed = false,
    results = QuestionResults(3, 4, stats = AnswerStats(20))), HostQuestionTime(label = "Chờ Host chuyển câu"), options = true)
@Preview(name = "Host - Reconnect thiếu thống kê", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable private fun HostReconnectPreview() = HostClassicFixture(hostResultsFixture().copy(
    connection = HostGameConnection.RECONNECT_FAILED, isStateSynchronized = false,
    results = null, hasResultsLeaderboard = false, hasAnswerKey = false, isAnswerRevealed = false,
    correctAnswers = emptyList()), HostQuestionTime(label = "Chờ đồng bộ thời gian"), options = true)
@Preview(name = "Host - Phân bố thực sự bằng không", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable private fun HostZeroPreview() = HostClassicFixture(hostResultsFixture().copy(results = QuestionResults(3, 4)),
    HostQuestionTime(label = "Chờ Host chuyển câu"))
@Preview(name = "Host - Pause", showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun HostPausePreview() = HostClassicFixture(hostClassicFixture().copy(sessionStatus = SessionStatus.PAUSED),
    HostQuestionTime(label = "Thời gian đang tạm dừng"))
@Preview(name = "Host - Hẹp chữ lớn nhiều lựa chọn", showBackground = true, widthDp = 320, heightDp = 1500, fontScale = 1.3f)
@Composable private fun HostNarrowPreview() = HostClassicFixture(hostResultsFixture().let {
    it.copy(question = it.question?.let { q -> q.copy(answerOptions = q.answerOptions +
        listOf(PublicAnswerOption("e", "Một lựa chọn có nội dung rất dài"), PublicAnswerOption("f", "Lựa chọn F"))) })
}, HostQuestionTime(label = "Chờ Host chuyển câu"), options = true)
@Preview(name = "Host - Kết quả chọn nhiều theo nhóm backend", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable private fun HostGroupedResultsPreview() = HostClassicFixture(hostResultsFixture().let {
    it.copy(question = it.question?.copy(questionType = "multiple_select"),
        results = QuestionResults(3, 4, listOf("ru"), AnswerStats(4,
            mapOf("ca,ru" to 2, "ru,cn" to 1, "null" to 1))))
}, HostQuestionTime(label = "Chờ Host chuyển câu"))

