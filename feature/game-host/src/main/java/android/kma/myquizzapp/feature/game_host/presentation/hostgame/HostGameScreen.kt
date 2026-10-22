package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.components.GameStartCountdownScreenContent
import android.kma.myquizzapp.core.ui.components.gameStartCountdownSeconds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import android.kma.myquizzapp.core.ui.components.AppDialogVariant
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Màn điều khiển trận của HOST (N21 host-paced, N28.5 self-paced).
 *
 * Classic dùng hai bố cục: tiến độ + câu hỏi khi đang chơi, biểu đồ + bảng hạng
 * khi có kết quả. Ảnh luôn hiện trong card câu hỏi; phần trả lời có thể thu gọn.
 *
 * Toàn trang dùng ĐÚNG MỘT [LazyColumn] (quy ước 3.11): lồng danh sách cuộn trong
 * danh sách cuộn làm Compose ném lỗi đo chiều cao vô hạn.
 */
@Composable
fun HostGameScreen(
    onExit: (String?) -> Unit,
    viewModel: HostGameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HostGameEffect.ExitGame -> onExit(effect.message)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val finalListState = rememberLazyListState()
    var finalBoardExpanded by rememberSaveable { mutableStateOf(true) }
    var finalStatsExpanded by rememberSaveable { mutableStateOf(false) }
    val questionKey = hostQuestionKey(uiState)
    var optionsExpanded by rememberSaveable(questionKey) { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingCloseKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }

    val isCountdown = uiState.phase == GamePhase.COUNTDOWN && !uiState.isFinished
    val isPreparing = uiState.usesStartCountdownLayout
    val needsLeaveConfirmation = (uiState.isClassic || isPreparing) && !uiState.isFinished
    val requestLeave: () -> Unit = {
        menuExpanded = false
        pendingCloseKey = null
        viewModel.onIntent(HostGameIntent.DismissEndDialog)
        if (needsLeaveConfirmation) showLeaveDialog = true
        else viewModel.onIntent(HostGameIntent.LeaveGame)
    }
    BackHandler(enabled = needsLeaveConfirmation || uiState.isFinished) { requestLeave() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(HostGameIntent.ErrorShown)
        }
    }
    LaunchedEffect(uiState.notice) {
        uiState.notice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(HostGameIntent.NoticeShown)
        }
    }
    LaunchedEffect(questionKey, uiState.isShowingQuestionResults) {
        if (uiState.isClassic) listState.scrollToItem(0)
    }
    LaunchedEffect(questionKey, uiState.phase) { menuExpanded = false }
    LaunchedEffect(questionKey, uiState.canCloseClassicQuestion) {
        if (pendingCloseKey != questionKey || !uiState.canCloseClassicQuestion) pendingCloseKey = null
    }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) showLeaveDialog = false }

    if (uiState.isFinished) {
        HostFinalResultScreenContent(
            state = uiState, listState = finalListState,
            boardExpanded = finalBoardExpanded, onToggleBoard = { finalBoardExpanded = !finalBoardExpanded },
            statsExpanded = finalStatsExpanded, onToggleStats = { finalStatsExpanded = !finalStatsExpanded },
            onRetry = { viewModel.onIntent(HostGameIntent.RetryFinalResults) },
            onHome = requestLeave, snackbarHostState = snackbarHostState,
        )
    } else if (isPreparing) {
        val seconds by key(uiState.phase, uiState.countdownTargetEpochMs, uiState.serverOffsetMs,
            uiState.isPaused, uiState.connection, uiState.isStateSynchronized) {
            produceState<Int?>(null) {
                if (!isCountdown || uiState.isPaused || uiState.connection != HostGameConnection.CONNECTED || !uiState.isStateSynchronized) return@produceState
                do {
                    value = gameStartCountdownSeconds(uiState.countdownTargetEpochMs, uiState.serverOffsetMs, System.currentTimeMillis())
                    if (value == null || value == 0) break
                    delay(100)
                } while (true)
            }
        }
        val message = when {
            uiState.connection == HostGameConnection.RECONNECT_FAILED -> "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại."
            uiState.connection == HostGameConnection.CONNECTING -> "Đang kết nối với máy chủ…"
            uiState.connection != HostGameConnection.CONNECTED -> "Đang kết nối lại với máy chủ…"
            !isCountdown -> "Đang đồng bộ trạng thái trận…"
            !uiState.isStateSynchronized -> "Đang đồng bộ trạng thái trận…"
            uiState.isPaused -> "Trận đang tạm dừng. Chọn Tiếp tục trong menu để bắt đầu đếm tiếp."
            seconds == null -> "Đang đồng bộ thời gian bắt đầu…"
            seconds == 0 -> "Đang chờ máy chủ mở câu hỏi đầu tiên…"
            else -> "Câu hỏi đầu tiên sắp xuất hiện. Hãy sẵn sàng!"
        }
        GameStartCountdownScreenContent(seconds, message, "Chuẩn bị vào trận",
            snackbarHost = { SnackbarHost(snackbarHostState) },
            onRetry = if (uiState.connection == HostGameConnection.RECONNECT_FAILED ||
                (uiState.isSelfPaced && uiState.connection == HostGameConnection.CONNECTED &&
                    uiState.isStateSynchronized && !uiState.isPaused && (!isCountdown || seconds == 0)))
                ({ viewModel.onIntent(HostGameIntent.Retry) }) else null,
            actions = {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, "Điều khiển trận") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text(uiState.pauseLabel) }, enabled = isCountdown && uiState.canPauseOrResume,
                        onClick = { menuExpanded = false; viewModel.onIntent(HostGameIntent.PauseOrResume) })
                    DropdownMenuItem(text = { Text("Kết thúc trận") }, enabled = isCountdown && uiState.canEndGame,
                        onClick = { menuExpanded = false; viewModel.onIntent(HostGameIntent.RequestEndGame) })
                    DropdownMenuItem(text = { Text("Rời màn") }, onClick = requestLeave)
                }
            })
    } else if (uiState.usesClassicRoundLayout) {
        val answerSectionExpanded = optionsExpanded || uiState.isAnswerRevealed
        HostClassicQuestionScreenContent(
            state = uiState, time = rememberHostQuestionTime(uiState),
            onIntent = { intent ->
                // Hiding via the fill button keeps the answer section open; collapsing hides both.
                if (intent == HostGameIntent.ToggleAnswerKey) optionsExpanded = true
                viewModel.onIntent(intent)
            }, listState = listState, snackbarHostState = snackbarHostState,
            optionsExpanded = answerSectionExpanded, onToggleOptions = {
                if (answerSectionExpanded) {
                    optionsExpanded = false
                    viewModel.onIntent(HostGameIntent.HideAnswerKey)
                } else optionsExpanded = true
            },
            menuExpanded = menuExpanded, onMenuExpandedChange = { menuExpanded = it },
            onRequestClose = { if (uiState.canCloseClassicQuestion) pendingCloseKey = questionKey },
            onRequestNext = { viewModel.onIntent(HostGameIntent.AdvanceFromResults(uiState.index)) },
            onRequestEnd = {
                menuExpanded = false
                pendingCloseKey = null
                showLeaveDialog = false
                viewModel.onIntent(HostGameIntent.RequestEndGame)
            },
            onRequestLeave = requestLeave,
        )
    } else {
        // Legacy self-paced/other active-game layouts stay unchanged.
        HostGameScreenContent(uiState, viewModel::onIntent, snackbarHostState, requestLeave)
    }

    if (pendingCloseKey == questionKey && uiState.canCloseClassicQuestion) {
        val expectedIndex = uiState.index
        val expectedId = uiState.question?.id
        AppConfirmDialog(
            title = "Chốt câu & hiện kết quả?",
            message = "Câu ${expectedIndex + 1} sẽ đóng ngay. Người chơi chưa trả lời sẽ không thể gửi đáp án; máy chủ sẽ công bố kết quả theo cấu hình phòng.",
            confirmText = "Chốt câu & hiện kết quả", dismissText = "Tiếp tục chờ",
            onConfirm = {
                pendingCloseKey = null
                if (expectedId != null) viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(expectedIndex, expectedId))
            },
            onDismiss = { pendingCloseKey = null },
        )
    }
    if (uiState.isEndDialogOpen) {
        if (uiState.isClassic || isPreparing) AppConfirmDialog(
            title = "Kết thúc trận?",
            message = "Trận sẽ kết thúc cho tất cả người chơi và chuyển sang kết quả cuối trận. Đây không phải thao tác chỉ rời màn Host.",
            confirmText = "Kết thúc trận", dismissText = "Tiếp tục trận", variant = AppDialogVariant.Danger,
            onConfirm = { viewModel.onIntent(HostGameIntent.ConfirmEndGame) },
            onDismiss = { viewModel.onIntent(HostGameIntent.DismissEndDialog) },
        ) else EndGameDialog(
            onConfirm = { viewModel.onIntent(HostGameIntent.ConfirmEndGame) },
            onDismiss = { viewModel.onIntent(HostGameIntent.DismissEndDialog) },
        )
    }
    if (showLeaveDialog && needsLeaveConfirmation) AppConfirmDialog(
        title = "Rời màn điều khiển?",
        message = "Bạn sẽ ngắt kết nối Host và rời màn điều khiển. Trận không kết thúc; nếu đang chờ Host chuyển câu, trận vẫn sẽ chờ.",
        confirmText = "Rời màn", dismissText = "Ở lại điều khiển",
        onConfirm = { showLeaveDialog = false; viewModel.onIntent(HostGameIntent.LeaveGame) },
        onDismiss = { showLeaveDialog = false },
    )
}

/** Initial UNKNOWN is the join/snapshot gap, not a legacy gameplay screen. */
internal val HostGameUiState.usesStartCountdownLayout: Boolean
    get() = !isFinished && (phase == GamePhase.UNKNOWN || phase == GamePhase.COUNTDOWN)

/** The clock belongs to the stateful Screen boundary, never Preview/content. */
@Composable
private fun rememberHostQuestionTime(state: HostGameUiState): HostQuestionTime {
    val time by key(hostQuestionKey(state), state.deadlineEpochMs, state.serverOffsetMs,
        state.isPaused, state.connection, state.isStateSynchronized, state.isQuestionClockKnown, state.phase,
        state.autoAdvance, state.results?.nextQuestionAt) {
        produceState(initialValue = hostQuestionTime(state, System.currentTimeMillis())) {
            do {
                value = hostQuestionTime(state, System.currentTimeMillis())
                if (value.seconds == null || value.seconds == 0) break
                delay(250)
            } while (true)
        }
    }
    return time
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostGameScreenContent(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onRequestLeave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.questionNumberLabel)
                },
                actions = {
                    TextButton(onClick = onRequestLeave) {
                        Text("Rời màn")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ConnectionBanner(state = state, onIntent = onIntent) }

            if (state.isSelfPaced) {
                item { SelfPacedOverviewCard(state) }
                item { ControlRow(state = state, onIntent = onIntent) }
            } else {
                item { TimerCard(state) }
                item { QuestionCard(state = state, onIntent = onIntent) }
                item { ControlRow(state = state, onIntent = onIntent) }
            }

            if (state.leaderboard.rows.isNotEmpty()) {
                item {
                    Text(
                        text = if (state.isSelfPaced) "Tiến độ người chơi" else "Bảng theo dõi",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.leaderboard.rows, key = { it.id }) { row ->
                    if (state.isSelfPaced) {
                        SelfPacedPlayerRow(row, state)
                    } else {
                        HostLeaderboardRowItem(row)
                    }
                }
            } else if (!state.hasSnapshot) {
                item {
                    Text(
                        text = "Đang đợi dữ liệu trận từ server...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ConnectionBanner(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit
) {
    when (state.connection) {
        HostGameConnection.CONNECTED -> if (state.isPaused) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Trận đang tạm dừng — đồng hồ của người chơi cũng đứng",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        HostGameConnection.CONNECTING -> Text(
            text = "Đang kết nối...",
            style = MaterialTheme.typography.bodyMedium
        )

        HostGameConnection.RECONNECTING -> Text(
            text = "Mất kết nối — đang tự động thử lại",
            style = MaterialTheme.typography.bodyMedium
        )

        HostGameConnection.RECONNECT_FAILED -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Không thể kết nối lại",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onIntent(HostGameIntent.Retry) }) { Text("Kết nối lại") }
        }
    }
}

/**
 * Đồng hồ đếm ngược cho cả giai đoạn đếm trước trận và câu đang mở.
 *
 * Tick mỗi nửa giây rồi TÍNH LẠI từ mốc tuyệt đối đã bù lệch đồng hồ, chứ không
 * trừ dần một biến đếm. Trừ dần sẽ trôi mỗi khi coroutine bị hoãn hay màn hình
 * tạm dừng, và host sẽ đọc số khác với máy người chơi — rất dễ thành tranh cãi.
 */
@Composable
private fun TimerCard(state: HostGameUiState) {
    val target = if (state.phase == GamePhase.COUNTDOWN) {
        state.countdownTargetEpochMs
    } else {
        state.deadlineEpochMs
    }

    val remaining by produceState(
        initialValue = remainingSeconds(target, state.serverOffsetMs),
        target,
        state.serverOffsetMs
    ) {
        while (true) {
            value = remainingSeconds(target, state.serverOffsetMs)
            delay(TICK_MS)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = phaseLabel(state), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // "—" khi câu không giới hạn thời gian: không được hiện "0".
                    text = remaining?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Text(text = state.answeredLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private const val TICK_MS = 500L

private fun remainingSeconds(target: Long?, offsetMs: Long): Int? {
    if (target == null) return null
    val left = target - (System.currentTimeMillis() + offsetMs)
    return if (left <= 0L) 0 else ((left + 999L) / 1000L).toInt()
}

private fun phaseLabel(state: HostGameUiState): String = when (state.phase) {
    GamePhase.COUNTDOWN -> "Sắp bắt đầu"
    GamePhase.QUESTION_ACTIVE -> "Đang trả lời"
    GamePhase.QUESTION_LOCKED -> when (state.lockReason) {
        QuestionLockReason.ALL_ANSWERED -> "Đã đóng — mọi người đã trả lời"
        QuestionLockReason.TIME_UP -> "Đã đóng — hết thời gian"
        else -> "Đã đóng câu"
    }
    GamePhase.SHOWING_RESULTS -> "Đang hiện kết quả"
    GamePhase.FINISHED -> "Trận đã xong"
    GamePhase.UNKNOWN -> "Đang đồng bộ"
}

@Composable
private fun QuestionCard(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit
) {
    val question = state.question
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = question?.questionText ?: "Đang chờ câu hỏi từ server...",
                style = MaterialTheme.typography.titleLarge
            )
            QuestionImage(
                imageUrl = question?.questionImage,
                contentDescription = "Ảnh minh họa câu hỏi",
                modifier = Modifier.padding(top = 12.dp)
            )

            if (question != null && question.answerOptions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                question.answerOptions.forEachIndexed { position, option ->
                    AnswerOptionRow(
                        position = position,
                        option = option,
                        // Chỉ đánh dấu đúng/sai khi host đã chủ động mở đáp án.
                        isCorrect = state.isAnswerRevealed && option.id in state.correctAnswers,
                        answerCount = state.results
                            ?.takeIf { state.isAnswerRevealed }
                            ?.stats
                            ?.distribution
                            ?.get(option.id)
                    )
                }
            } else if (question != null && state.isAnswerRevealed) {
                // Câu tự luận không có lựa chọn nào để in đậm, nên đáp án phải hiện
                // thành chữ. Thiếu nhánh này thì bấm "Xem đáp án" trông y hệt nút hỏng.
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (state.correctAnswers.isEmpty()) {
                        "Câu này không kèm đáp án mẫu"
                    } else {
                        // Nhiều đáp án được chấp nhận thì liệt kê hết, host còn biết đường chấm tay.
                        "Đáp án: " + state.correctAnswers.joinToString("  /  ")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            if (state.canRevealAnswer) {
                OutlinedButton(onClick = { onIntent(HostGameIntent.ToggleAnswerKey) }) {
                    Text(if (state.isAnswerRevealed) "Ẩn đáp án" else "Xem đáp án")
                }
            } else {
                // Nói thật lý do thay vì hiện nút bấm vào không ra gì: sau khi kết nối lại
                // giữa câu, backend không phát lại đáp án cho host nữa.
                Text(
                    text = "Chưa có đáp án cho câu này — sẽ có lại từ câu tiếp theo",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AnswerOptionRow(
    position: Int,
    option: PublicAnswerOption,
    isCorrect: Boolean,
    answerCount: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${optionLetter(position)}.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            // Lựa chọn luôn là chữ (CSDL chỉ lưu `option_text`), nên fallback chỉ còn
            // cho trường hợp dữ liệu hỏng thật sự, không phải cho lựa chọn ảnh.
            text = option.text ?: "(lựa chọn trống)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isCorrect) {
            Text(text = "Đúng", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
        }
        answerCount?.let {
            Text(text = "$it", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun optionLetter(position: Int): String =
    if (position in 0..LETTERS.lastIndex) LETTERS[position] else "${position + 1}"

private val LETTERS = listOf("A", "B", "C", "D", "E", "F")

@Composable
private fun ControlRow(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nút chuyển câu chỉ tồn tại khi phòng KHÔNG tự chuyển câu; còn lại backend sẽ
        // trả 409 GAME_ADVANCE_NOT_ALLOWED nên hiện nút là mời host bấm để nhận lỗi.
        if (state.isManualAdvanceVisible) {
            Button(
                onClick = { onIntent(HostGameIntent.AdvanceQuestion) },
                enabled = state.canAdvance
            ) {
                Text(state.advanceLabel)
            }
        }

        OutlinedButton(
            onClick = { onIntent(HostGameIntent.PauseOrResume) },
            enabled = state.canPauseOrResume
        ) {
            Text(state.pauseLabel)
        }

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = { onIntent(HostGameIntent.RequestEndGame) },
            enabled = state.canEndGame
        ) {
            Text("Kết thúc")
        }
    }
}

@Composable
private fun SelfPacedOverviewCard(state: HostGameUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(modeLabel(state), style = MaterialTheme.typography.titleLarge)
            Text(
                "Đang chơi ${state.playingPlayers} · Mất kết nối ${state.disconnectedPlayers} · " +
                    "Hoàn thành ${state.finishedPlayers} · Bị loại ${state.eliminatedPlayers}"
            )
            if (state.isMarathon) {
                Text(
                    "Mỗi người có đồng hồ riêng; Host theo dõi số câu đã làm thay vì một timer chung.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun modeLabel(state: HostGameUiState): String = when (state.mode) {
    GameMode.SOLO -> "Solo — tiến độ riêng"
    GameMode.SURVIVAL -> "Survival — sinh tồn"
    GameMode.MARATHON -> "Marathon — chạy đua thời gian"
    GameMode.PRACTICE -> "Practice — luyện tập"
    else -> "Self-paced — tiến độ riêng"
}

@Composable
private fun SelfPacedPlayerRow(row: HostLeaderboardRow, state: HostGameUiState) {
    val total = row.totalQuestions.takeIf { it > 0 } ?: state.totalQuestions
    val progress = when {
        state.isMarathon -> "Đã làm ${row.answeredCount} câu"
        row.status == "finished" || row.status == "eliminated" -> {
            if (total > 0) "Đã làm ${row.answeredCount}/$total câu" else "Đã làm ${row.answeredCount} câu"
        }
        total > 0 -> "Đang ở câu ${(row.currentQuestionIndex + 1).coerceAtMost(total)}/$total"
        else -> "Đã làm ${row.answeredCount} câu"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.playerName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${row.playerScore} điểm", fontWeight = FontWeight.Bold)
            }
            Text("$progress · ${playerStatusLabel(row.status)}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Đúng ${row.correctCount} · Sai ${row.wrongCount}", style = MaterialTheme.typography.bodySmall)
                row.lives?.let { Text("Mạng $it", style = MaterialTheme.typography.bodySmall) }
                if (row.streak > 0) Text("Chuỗi ${row.streak}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun playerStatusLabel(status: String): String = when (status) {
    "connected" -> "Đang chơi"
    "disconnected" -> "Mất kết nối"
    "finished" -> "Đã hoàn thành"
    "eliminated" -> "Đã bị loại"
    else -> status
}

@Composable
private fun HostLeaderboardRowItem(row: HostLeaderboardRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${row.rank}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.playerName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Đúng ${row.correctCount} · Sai ${row.wrongCount} · Chưa ${row.unansweredCount}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "${row.playerScore}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EndGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kết thúc trận?") },
        text = { Text("Người chơi sẽ dừng ngay và chuyển sang bảng kết quả.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Kết thúc") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để tiếp tục") } }
    )
}
