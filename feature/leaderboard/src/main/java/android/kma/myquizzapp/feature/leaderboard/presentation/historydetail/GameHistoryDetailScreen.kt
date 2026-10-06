package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.feature.leaderboard.presentation.QuestionStatCard
import android.kma.myquizzapp.feature.leaderboard.presentation.ReviewItemCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GameHistoryDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameHistoryDetailScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryDetailScreenContent(
    state: GameHistoryDetailUiState,
    onIntent: (GameHistoryDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết trận") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Quay lại") } }
            )
        }
    ) { padding ->
        when {
            state.isSummaryLoading && state.summary == null -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Đang tải chi tiết trận…", Modifier.padding(top = 12.dp))
            }
            state.summaryError != null && state.summary == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.summaryError, color = MaterialTheme.colorScheme.error)
                OutlinedButton(
                    onClick = { onIntent(GameHistoryDetailIntent.RetrySummary) },
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("Thử lại") }
            }
            state.summary != null -> HistoryDetailContent(
                state = state,
                topPadding = padding.calculateTopPadding(),
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun HistoryDetailContent(
    state: GameHistoryDetailUiState,
    topPadding: androidx.compose.ui.unit.Dp,
    onIntent: (GameHistoryDetailIntent) -> Unit
) {
    val summary = state.summary ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = topPadding).padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.quiz?.let { quiz ->
                        quiz.image?.let { image ->
                            RemoteImage(
                                imageUrl = image,
                                contentDescription = quiz.name,
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Text(
                        summary.quiz?.name?.takeIf(String::isNotBlank) ?: "Quiz đã bị xóa",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    summary.quiz?.description?.takeIf(String::isNotBlank)?.let { Text(it) }
                    Text("${summary.gameMode.label()} • ${summary.sessionStatus.label()}")
                    Text("Phòng: ${summary.sessionName}")
                    summary.hostName?.takeIf(String::isNotBlank)?.let { Text("Host: $it") }
                    summary.finishedAt?.let { Text("Kết thúc: ${formatHistoryTime(it)}") }
                    Text("${summary.totalPlayers} người chơi • ${summary.totalQuestions} câu hỏi")
                    if (state.isHost) Text("Bạn đã tổ chức trận này")
                }
            }
        }

        state.currentPlayer?.let { me ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Thành tích của bạn", style = MaterialTheme.typography.titleMedium)
                        Text("Hạng ${me.rank} • ${me.playerScore} điểm")
                        me.correctAnswersCount?.let { Text("$it/${summary.totalQuestions} câu đúng") }
                        me.streak?.let { Text("Chuỗi đúng: $it") }
                    }
                }
            }
        }

        if (summary.leaderboardHiddenByConfig) {
            item { Text("Host đã ẩn bảng xếp hạng của trận này.") }
        } else if (summary.leaderboard.isNotEmpty()) {
            item { Text("Bảng xếp hạng", style = MaterialTheme.typography.titleLarge) }
            items(summary.leaderboard, key = { it.id }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "#${row.rank}  ${row.playerName} — ${row.playerScore} điểm" +
                            if (row.id == state.playerId) "  (Bạn)" else "",
                        Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (state.isHost && state.questionStats.isNotEmpty()) {
            item { Text("Thống kê từng câu", style = MaterialTheme.typography.titleLarge) }
            items(state.questionStats, key = { "${it.questionIndex}-${it.questionId}" }) {
                QuestionStatCard(it)
            }
        }

        if (state.playerId != null) {
            item { Text("Câu trả lời của bạn", style = MaterialTheme.typography.titleLarge) }
            when {
                state.isAnswersLoading -> item { CircularProgressIndicator() }
                state.reviewDisabled -> item { Text("Phòng này không cho xem lại đáp án.") }
                state.answersError != null -> item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(state.answersError)
                            OutlinedButton(onClick = { onIntent(GameHistoryDetailIntent.RetryAnswers) }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
                state.review != null -> {
                    item {
                        Text(
                            "${state.review.correctAnswersCount}/${state.review.totalQuestions} câu đúng • " +
                                "${state.review.answeredCount} lượt trả lời"
                        )
                    }
                    items(state.reviewItems, key = { "${it.questionIndex}-${it.questionId}" }) {
                        ReviewItemCard(it)
                    }
                }
            }
        }
    }
}

private fun GameMode.label(): String = when (this) {
    GameMode.CLASSIC -> "Classic"
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
}

private fun SessionStatus.label(): String = when (this) {
    SessionStatus.FINISHED -> "Đã kết thúc"
    SessionStatus.CANCELLED -> "Đã hủy"
    SessionStatus.LOBBY -> "Phòng chờ"
    SessionStatus.ACTIVE -> "Đang chơi"
    SessionStatus.PAUSED -> "Tạm dừng"
}

private val historyDetailDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private fun formatHistoryTime(raw: String): String = runCatching {
    historyDetailDateFormatter.format(Instant.parse(raw).atZone(ZoneId.systemDefault()))
}.getOrDefault(raw)
