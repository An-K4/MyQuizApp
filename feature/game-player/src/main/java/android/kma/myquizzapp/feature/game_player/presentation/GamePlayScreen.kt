package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.ui.components.QuestionImage
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
    GamePlayScreenContent(state, viewModel::onIntent, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreenContent(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); onIntent(GameIntent.ErrorShown) }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Đang chơi") },
                actions = { TextButton(onClick = { onIntent(GameIntent.Leave) }) { Text("Thoát") } }
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
            onIntent = {}
        )
    }
}
