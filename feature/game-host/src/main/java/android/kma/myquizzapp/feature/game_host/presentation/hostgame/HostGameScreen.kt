package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.QuestionLockReason
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
 * Màn điều khiển trận của HOST (N21, chỉ mode host-paced).
 *
 * Nguyên tắc bố cục: **câu hỏi luôn hiện, đáp án ẩn sau nút**. Màn này hay được
 * chiếu lên màn lớn hoặc đặt giữa bàn, nên hiện đáp án mặc định là hỏng trận;
 * nhưng ẩn luôn cả câu hỏi (như bản web hiện tại) thì host mất ngữ cảnh để đọc to
 * và điều phối.
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

    HostGameScreenContent(state = uiState, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostGameScreenContent(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(HostGameIntent.ErrorShown)
        }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(HostGameIntent.NoticeShown)
        }
    }

    if (state.isEndDialogOpen) {
        EndGameDialog(
            onConfirm = { onIntent(HostGameIntent.ConfirmEndGame) },
            onDismiss = { onIntent(HostGameIntent.DismissEndDialog) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isFinished) "Kết quả trận" else state.questionNumberLabel)
                },
                actions = {
                    TextButton(onClick = { onIntent(HostGameIntent.LeaveGame) }) {
                        Text(if (state.isFinished) "Thoát" else "Rời màn")
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

            if (state.isFinished) {
                item { FinishedCard(state) }
            } else {
                item { TimerCard(state) }
                item { QuestionCard(state = state, onIntent = onIntent) }
                item { ControlRow(state = state, onIntent = onIntent) }
            }

            if (state.leaderboard.rows.isNotEmpty()) {
                item {
                    Text(
                        text = "Bảng theo dõi",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.leaderboard.rows, key = { it.id }) { row ->
                    HostLeaderboardRowItem(row)
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

        HostGameConnection.RECONNECTING -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mất kết nối — đang thử lại",
                style = MaterialTheme.typography.bodyMedium,
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

/**
 * Trận xong. `game:ended` đã mang sẵn bảng xếp hạng cuối nên không gọi thêm REST.
 * Màn kết quả đầy đủ thuộc N24; ở đây chỉ hiện gọn rồi để host tự thoát.
 */
@Composable
private fun FinishedCard(state: HostGameUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = "Trận đã kết thúc", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            val ended = state.ended
            if (ended == null || ended.leaderboard.isEmpty()) {
                Text(
                    text = "Không có kết quả nào để hiện",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ended.leaderboard.take(TOP_ROWS).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = "${row.rank}. ${row.playerName}", modifier = Modifier.weight(1f))
                        Text(text = "${row.playerScore}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private const val TOP_ROWS = 10

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
