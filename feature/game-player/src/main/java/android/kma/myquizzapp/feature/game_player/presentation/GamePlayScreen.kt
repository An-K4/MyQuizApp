package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.components.GameStartCountdownScreenContent
import android.kma.myquizzapp.core.ui.components.gameStartCountdownSeconds
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun GamePlayScreen(
    onExit: (String?) -> Unit,
    onNavigateToFinalResult: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is GameEffect.Exit -> onExit(it.message)
                is GameEffect.NavigateToFinalResult -> onNavigateToFinalResult(it.gameId, it.playerId)
            }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val needsExitConfirmation = state.isClassic || state.usesStartCountdownLayout
    val requestExit: () -> Unit = {
        if (needsExitConfirmation) showExitDialog = true else viewModel.onIntent(GameIntent.Leave)
    }
    BackHandler(enabled = needsExitConfirmation) { requestExit() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.onIntent(GameIntent.ErrorShown) }
    }
    LaunchedEffect(state.question?.id, state.question?.index, state.showsClassicSummary) {
        if (state.isClassic) listState.scrollToItem(0)
    }
    LaunchedEffect(state.isClassic, state.phase, state.isAnswerInputEnabled) {
        if (state.isClassic && (state.phase != GamePhaseUi.Question || !state.isAnswerInputEnabled)) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    if (state.usesStartCountdownLayout) {
        val target = (state.phase as? GamePhaseUi.Countdown)?.startsAt
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        val seconds by key(state.phase, target, state.serverOffsetMs, state.isPaused, state.connection, state.isSynchronizing) {
            produceState<Int?>(null) {
                if (state.phase !is GamePhaseUi.Countdown || state.isPaused || state.connection != GameConnection.CONNECTED || state.isSynchronizing) return@produceState
                do {
                    value = gameStartCountdownSeconds(target, state.serverOffsetMs, System.currentTimeMillis())
                    if (value == null || value == 0) break
                    delay(100)
                } while (true)
            }
        }
        val message = when {
            state.connection == GameConnection.RECONNECT_FAILED -> "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại."
            state.connection == GameConnection.CONNECTING -> "Đang kết nối với máy chủ…"
            state.connection != GameConnection.CONNECTED -> "Đang kết nối lại với máy chủ…"
            state.phase == GamePhaseUi.Connecting -> "Đang đồng bộ trạng thái trận…"
            state.isSynchronizing -> "Đang đồng bộ trạng thái trận…"
            state.isPaused -> "Trận đang tạm dừng — chờ Host tiếp tục."
            seconds == null -> "Đang đồng bộ thời gian bắt đầu…"
            seconds == 0 -> "Đang chờ máy chủ mở câu hỏi đầu tiên…"
            else -> "Câu hỏi đầu tiên sắp xuất hiện. Hãy sẵn sàng!"
        }
        GameStartCountdownScreenContent(seconds, message, "Chuẩn bị vào trận", modifier,
            snackbarHost = { SnackbarHost(snackbar) },
            actions = { TextButton(onClick = requestExit) { Text("Rời màn") } },
            onRetry = if (state.connection == GameConnection.RECONNECT_FAILED)
                ({ viewModel.onIntent(GameIntent.Retry) }) else null)
    } else if (state.showsClassicSummary || (state.isClassic && state.phase is GamePhaseUi.Results && state.question == null)) {
        val waitText = rememberClassicResultsWait(state)
        ClassicResultsScreenContent(state, waitText, requestExit, { viewModel.onIntent(GameIntent.Retry) },
            listState, snackbar, modifier)
    } else if (state.usesClassicQuestionLayout) {
        val time = if (state.phase is GamePhaseUi.Results) ClassicQuestionTime() else rememberClassicQuestionTime(state)
        LaunchedEffect(state.question?.id, state.question?.index, state.endsAt, time.expired, state.isPaused) {
            if (time.expired && !state.isPaused) viewModel.onIntent(GameIntent.DeadlineReached)
        }
        ClassicQuestionScreenContent(
            state = state, time = time, onIntent = viewModel::onIntent,
            onRequestExit = requestExit, listState = listState, snackbar = snackbar, modifier = modifier,
        )
    } else {
        // Non-Classic question layouts keep their existing behavior.
        GamePlayScreenContent(
            state = state, onIntent = viewModel::onIntent, onRequestExit = requestExit,
            snackbar = snackbar, modifier = modifier,
        )
    }
    if (showExitDialog && needsExitConfirmation) {
        AppConfirmDialog(
            title = "Rời màn chơi?",
            message = "Bạn sẽ ngắt kết nối và rời màn chơi. Thao tác này không kết thúc trận của cả phòng.",
            confirmText = "Rời màn", dismissText = "Tiếp tục chơi",
            onConfirm = { showExitDialog = false; viewModel.onIntent(GameIntent.Leave) },
            onDismiss = { showExitDialog = false },
        )
    }
}

/** Cover the lobby-to-game join/sync gap before the countdown event or first question arrives. */
internal val GameUiState.usesStartCountdownLayout: Boolean
    get() = phase == GamePhaseUi.Connecting || phase is GamePhaseUi.Countdown

/** Stateful clock adapter lives at the Screen boundary; the Classic content only renders values. */
@Composable
private fun rememberClassicQuestionTime(state: GameUiState): ClassicQuestionTime {
    val time by key(state.question?.id, state.question?.index, state.endsAt, state.serverOffsetMs,
        state.timeLimitSeconds, state.isPaused, state.remainingSeconds) {
        produceState(initialValue = classicQuestionTime(state, System.currentTimeMillis())) {
            do {
                value = classicQuestionTime(state, System.currentTimeMillis())
                if (state.isPaused || value.seconds == null || value.expired) break
                delay(200)
            } while (true)
        }
    }
    return time
}

@Composable
private fun rememberClassicResultsWait(state: GameUiState): String {
    val text by key(state.endsAt, state.serverOffsetMs, state.isPaused, state.remainingSeconds,
        state.connection, state.isSynchronizing, state.autoAdvance, state.question?.id, state.question?.index) {
        produceState(initialValue = classicResultsWaitText(state, System.currentTimeMillis())) {
            do {
                value = classicResultsWaitText(state, System.currentTimeMillis())
                if (state.isPaused || state.connection != GameConnection.CONNECTED || state.isSynchronizing || !state.autoAdvance || state.endsAt == null) break
                delay(200)
            } while (true)
        }
    }
    return text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreenContent(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onRequestExit: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Đang chơi") },
                actions = { TextButton(onClick = onRequestExit) { Text("Thoát") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ConnectionBanner(state.connection, onIntent) }
            if (state.isPaused) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text("Trận đang tạm dừng — chờ Host tiếp tục", Modifier.padding(16.dp))
                    }
                }
            }
            item { PhaseHeader(state, onIntent) }
            if (
                state.isSelfPaced && !state.isPersonallyDone &&
                (state.lives != null || state.hasMatchBudget)
            ) {
                item { SelfPacedStatus(state, onIntent) }
            }
            when (state.phase) {
                GamePhaseUi.Eliminated -> item {
                    PersonalCompletionCard(
                        title = "Bạn đã hết mạng",
                        message = "Kết quả đã được ghi nhận. Đang chờ những người chơi còn lại.",
                        score = state.totalScore ?: state.playerScore
                    )
                }
                GamePhaseUi.PlayerFinished -> item {
                    PersonalCompletionCard(
                        title = if (state.matchTimeExpired) "Đã hết thời gian" else "Bạn đã hoàn thành",
                        message = "Đang chờ trận đấu kết thúc để xem kết quả cuối cùng.",
                        score = state.totalScore ?: state.playerScore
                    )
                }
                else -> Unit
            }
            if (state.answeredCount != null && state.activePlayers != null) {
                item { Text("Đã trả lời: ${state.answeredCount}/${state.activePlayers}") }
            }
            state.question?.takeUnless { state.isPersonallyDone || state.phase == GamePhaseUi.Finished }?.let { question ->
                item {
                    Text("Câu ${question.index + 1}/${question.total}", style = MaterialTheme.typography.labelLarge)
                    Text(question.questionText, style = MaterialTheme.typography.headlineSmall)
                    QuestionImage(
                        imageUrl = question.questionImage,
                        contentDescription = "Ảnh minh họa câu hỏi",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    question.questionHint?.takeIf(String::isNotBlank)?.let { Text("Gợi ý: $it") }
                }
                when (question.questionType) {
                    "multiple_choice" -> items(question.answerOptions, key = { it.id }) { option ->
                        SingleOption(option, state.selectedOptionId == option.id, state.isAnswerInputEnabled) {
                            onIntent(GameIntent.SelectOption(option.id))
                        }
                    }
                    "multiple_select" -> items(question.answerOptions, key = { it.id }) { option ->
                        MultipleOption(option, option.id in state.selectedOptionIds, state.isAnswerInputEnabled) {
                            onIntent(GameIntent.ToggleOption(option.id))
                        }
                    }
                    "short_answer", "long_answer" -> item {
                        OutlinedTextField(
                            value = state.textAnswer,
                            onValueChange = { onIntent(GameIntent.ChangeText(it)) },
                            enabled = state.isAnswerInputEnabled,
                            minLines = if (question.questionType == "long_answer") 4 else 1,
                            label = { Text("Câu trả lời") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Button(
                        onClick = { onIntent(GameIntent.Submit) },
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSubmitting) CircularProgressIndicator() else Text("Gửi đáp án")
                    }
                }
            }
            if (state.phase is GamePhaseUi.Results) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (state.phase.restoredWithoutDetails) {
                                Text("Đang chờ câu tiếp theo")
                                Text("Kết quả chi tiết không thể phát lại sau khi kết nối lại.")
                            } else {
                                Text(when (state.outcome) {
                                    QuestionOutcome.CORRECT -> "Chính xác!"
                                    QuestionOutcome.INCORRECT -> "Chưa chính xác"
                                    QuestionOutcome.HIDDEN, null -> "Kết quả đã được ghi nhận"
                                }, style = MaterialTheme.typography.titleMedium)
                                if (state.showCorrectAnswer == true) {
                                    state.results?.correctAnswers?.takeIf { it.isNotEmpty() }?.let {
                                        Text("Đáp án đúng: ${it.joinToString()}")
                                    }
                                    state.results?.stats?.distribution?.takeIf { it.isNotEmpty() }?.let { distribution ->
                                        Text("Phân bố: " + distribution.entries.joinToString { "${it.key}: ${it.value}" })
                                    }
                                }
                                if (state.isSelfPaced) {
                                    if (state.timedOut) {
                                        Text("Đã hết thời gian cho câu này.")
                                    } else if (state.wasLate) {
                                        Text("Câu trả lời được ghi nhận sau thời hạn.")
                                    }
                                    state.scoreEarned?.let { Text("Điểm câu này: $it") }
                                    state.totalScore?.let { Text("Tổng điểm: $it") }
                                    state.streak?.let { Text("Chuỗi đúng: $it") }
                                }
                                if (state.shouldShowNextAction) {
                                    Button(
                                        onClick = { onIntent(GameIntent.NextQuestion) },
                                        enabled = !state.isRequestingNext && state.connection == GameConnection.CONNECTED,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (state.isRequestingNext) {
                                            CircularProgressIndicator()
                                        } else {
                                            Text("Câu tiếp theo")
                                        }
                                    }
                                }
                                if (state.canShowLiveLeaderboard && state.playerRank != null && state.playerScore != null) {
                                    Text("Bạn đang hạng ${state.playerRank} • ${state.playerScore} điểm")
                                }
                                state.results?.nextQuestionAt?.let { next ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Câu tiếp theo sau: ")
                                        DeadlineCountdown(next, state.serverOffsetMs, null) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (state.canShowLiveLeaderboard) {
                item { Text("Bảng xếp hạng", style = MaterialTheme.typography.titleMedium) }
                items(state.leaderboard, key = { "leaderboard-${it.id}" }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "#${row.rank}  ${row.playerName} — ${row.playerScore} điểm" +
                                if (row.id == state.playerId) "  (Bạn)" else "",
                            Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfPacedStatus(state: GameUiState, onIntent: (GameIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.lives?.let { Text("Mạng còn lại: $it", style = MaterialTheme.typography.titleMedium) }
            state.matchEndsAt?.let { deadline ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (state.matchTimeExpired) "Đã hết thời gian" else "Thời gian toàn trận")
                    DeadlineCountdown(deadline, state.serverOffsetMs, null) {
                        onIntent(GameIntent.MatchDeadlineReached)
                    }
                }
                if (state.matchTimeExpired && !state.isPersonallyDone) {
                    Text("Đang chờ máy chủ chốt kết quả…")
                }
            }
        }
    }
}

@Composable
private fun PersonalCompletionCard(title: String, message: String, score: Int?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message)
            score?.let { Text("Tổng điểm: $it") }
        }
    }
}

@Composable
private fun ConnectionBanner(
    connection: GameConnection,
    onIntent: (GameIntent) -> Unit
) {
    when (connection) {
        GameConnection.CONNECTED -> Unit
        GameConnection.CONNECTING -> Text("Đang kết nối…")
        GameConnection.RECONNECTING -> Text(
            "Mất kết nối — đang đồng bộ lại…",
            color = MaterialTheme.colorScheme.error
        )
        GameConnection.RECONNECT_FAILED -> Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại.",
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { onIntent(GameIntent.Retry) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kết nối lại")
                }
            }
        }
    }
}

@Composable
private fun PhaseHeader(state: GameUiState, onIntent: (GameIntent) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (state.phase) {
                    GamePhaseUi.Connecting -> "Đang tải trận"
                    is GamePhaseUi.Countdown -> "Chuẩn bị"
                    GamePhaseUi.Question -> "Chọn câu trả lời"
                    GamePhaseUi.Submitted -> if (state.isConfirming) "Đang xác nhận câu trả lời" else "Đã gửi câu trả lời"
                    GamePhaseUi.Locked -> "Câu hỏi đã khóa"
                    is GamePhaseUi.Results -> "Kết quả"
                    GamePhaseUi.Eliminated -> "Đã bị loại"
                    GamePhaseUi.PlayerFinished -> "Đã hoàn thành phần chơi"
                    GamePhaseUi.Finished -> "Trận đã kết thúc"
                },
                style = MaterialTheme.typography.titleMedium
            )
            val target = (state.phase as? GamePhaseUi.Countdown)?.startsAt ?: state.endsAt
            DeadlineCountdown(target, state.serverOffsetMs, state.remainingSeconds) {
                onIntent(GameIntent.DeadlineReached)
            }
        }
    }
}

@Composable
private fun DeadlineCountdown(endsAt: String?, offsetMs: Long, fallback: Int?, onExpired: () -> Unit) {
    if (endsAt == null) return
    var seconds by remember(endsAt, offsetMs) { mutableIntStateOf(fallback ?: 0) }
    LaunchedEffect(endsAt, offsetMs) {
        val deadline = runCatching { Instant.parse(endsAt).toEpochMilli() }.getOrNull() ?: return@LaunchedEffect
        while (true) {
            seconds = ((deadline - (System.currentTimeMillis() + offsetMs)).coerceAtLeast(0L) / 1000L).toInt()
            if (seconds == 0) {
                onExpired()
                break
            }
            delay(250)
        }
    }
    Text("${seconds}s", style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun SingleOption(option: PublicAnswerOption, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
            Text(option.text ?: option.id)
        }
    }
}

@Composable
private fun MultipleOption(option: PublicAnswerOption, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onClick() }, enabled = enabled)
            Text(option.text ?: option.id)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewGame() {
    MyQuizAppTheme {
        GamePlayScreenContent(
            state = GameUiState(
                connection = GameConnection.CONNECTED,
                phase = GamePhaseUi.Question,
                question = PublicQuestion(
                    index = 0,
                    total = 4,
                    id = 1,
                    questionType = "multiple_choice",
                    questionText = "Thủ đô Việt Nam?",
                    questionImage = "https://example.com/question.png",
                    answerOptions = listOf(
                        PublicAnswerOption("0", "Hà Nội"), PublicAnswerOption("1", "Huế")
                    )
                ),
                isInputLocked = false
            ),
            onIntent = {},
            onRequestExit = {},
            snackbar = remember { SnackbarHostState() },
        )
    }
}
