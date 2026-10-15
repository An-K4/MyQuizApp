package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.LeaderboardPlayerCard
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameHistoryDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameHistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    GameHistoryDetailScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        listState = listState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistoryDetailScreenContent(
    state: GameHistoryDetailUiState,
    onIntent: (GameHistoryDetailIntent) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        "Chi tiết trận đấu", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
                // No duplicate "Quay lại" action on the right.
            )
        }
    ) { padding ->
        when {
            state.summary != null -> HistoryDetailContent(
                state = state, listState = listState,
                modifier = Modifier.fillMaxSize().padding(padding), onIntent = onIntent
            )
            state.summaryError != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.summaryError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                QuizSecondaryButton(
                    text = "Thử lại", onClick = { onIntent(GameHistoryDetailIntent.RetrySummary) },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Đang tải chi tiết trận…", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HistoryDetailContent(
    state: GameHistoryDetailUiState,
    listState: LazyListState,
    modifier: Modifier,
    onIntent: (GameHistoryDetailIntent) -> Unit
) {
    val summary = state.summary ?: return
    val colors = historyDetailColors()
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "summary") { HistorySummaryCard(summary, state.currentPlayer) }
        if (state.isHost) {
            item(key = "leaderboard-heading") { HistorySectionHeading("Bảng xếp hạng") }
            when {
                summary.leaderboardHiddenByConfig -> item(key = "leaderboard-hidden") {
                    HistoryNoticeCard("Bảng xếp hạng không khả dụng.")
                }
                summary.leaderboard.isEmpty() -> item(key = "leaderboard-empty") {
                    HistoryNoticeCard("Chưa có dữ liệu xếp hạng.")
                }
                else -> items(summary.leaderboard, key = { "leaderboard:${it.id}" }) { row ->
                    LeaderboardPlayerCard(
                        rankLabel = row.rank.toString(), playerName = row.playerName,
                        scoreText = "${formatHistoryNumber(row.playerScore)} điểm",
                        initials = historyPlayerInitials(row.playerName)
                    )
                }
            }
            item(key = "stats-heading") { HistorySectionHeading("Thống kê từng câu") }
            if (state.questionStats.isEmpty()) {
                item(key = "stats-empty") { HistoryNoticeCard("Chưa có thống kê từng câu.") }
            } else {
                items(state.questionStats, key = { "stat:${it.questionIndex}:${it.questionId}" }) {
                    HistoryQuestionStatCard(it)
                }
            }
        } else if (state.playerId != null) {
            item(key = "review-heading") { HistorySectionHeading("Xem lại câu trả lời") }
            val review = state.review
            when {
                state.isAnswersLoading -> item(key = "answers-loading") {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Đang tải câu trả lời…", style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                    }
                }
                state.reviewDisabled -> item(key = "review-disabled") {
                    HistoryNoticeCard("Phòng này không cho xem lại đáp án.")
                }
                state.answersError != null -> item(key = "answers-error") {
                    HistoryNoticeCard(
                        state.answersError, isError = true,
                        onRetry = { onIntent(GameHistoryDetailIntent.RetryAnswers) }
                    )
                }
                review != null -> {
                    item(key = "review-overview") {
                        Text(
                            "${review.correctAnswersCount}/${review.totalQuestions} câu đúng • ${review.answeredCount} lượt trả lời",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted
                        )
                    }
                    if (state.reviewItems.isEmpty()) {
                        item(key = "review-empty") { HistoryNoticeCard("Chưa có câu hỏi để xem lại.") }
                    } else {
                        items(state.reviewItems, key = { "review:${it.questionIndex}:${it.questionId}" }) {
                            HistoryReviewQuestionCard(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySectionHeading(text: String) {
    Text(
        text, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold, color = historyDetailColors().foreground
    )
}

@Composable
private fun HistoryNoticeCard(text: String, isError: Boolean = false, onRetry: (() -> Unit)? = null) {
    val colors = historyDetailColors()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.card) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (isError) colors.incorrect else colors.muted)
            if (onRetry != null) QuizSecondaryButton("Thử lại", onRetry)
        }
    }
}

@Preview(name = "Chi tiết trận — Player", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Chi tiết trận — tối", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryDetailPlayerPreview() = HistoryScreenPreview(HistoryDetailPreviewData.state())

@Preview(name = "Chi tiết trận — Host", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HistoryDetailHostPreview() = HistoryScreenPreview(HistoryDetailPreviewData.state(host = true))

@Preview(name = "Chi tiết trận — hẹp/chữ lớn, đã hủy", showBackground = true, widthDp = 320, heightDp = 844, fontScale = 1.3f)
@Composable
private fun HistoryDetailNarrowPreview() {
    val state = HistoryDetailPreviewData.state()
    HistoryScreenPreview(state.copy(summary = state.summary?.copy(
        sessionStatus = SessionStatus.CANCELLED, sessionCode = null,
        quiz = state.summary?.quiz?.copy(image = null)
    )))
}

@Preview(name = "Chi tiết trận — đang tải tổng quan", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailLoadingPreview() = HistoryScreenPreview(GameHistoryDetailUiState(gameId = 91))

@Preview(name = "Chi tiết trận — lỗi tổng quan", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailSummaryErrorPreview() = HistoryScreenPreview(GameHistoryDetailUiState(
    gameId = 91, isSummaryLoading = false, summaryError = "Không thể tải chi tiết trận. Vui lòng thử lại."
))

@Preview(name = "Chi tiết trận — đang tải câu trả lời", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailAnswersLoadingPreview() = HistoryScreenPreview(HistoryDetailPreviewData.state().copy(
    isAnswersLoading = true, review = null
))

@Preview(name = "Chi tiết trận — review bị tắt", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailReviewDisabledPreview() = HistoryScreenPreview(HistoryDetailPreviewData.state().copy(
    reviewDisabled = true, review = null
))

@Preview(name = "Chi tiết trận — lỗi câu trả lời", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailAnswersErrorPreview() = HistoryScreenPreview(HistoryDetailPreviewData.state().copy(
    answersError = "Chưa tải được câu trả lời. Tổng quan trận vẫn được giữ lại.", review = null
))

@Preview(name = "Chi tiết trận — Host chưa có thống kê", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailEmptyHostPreview() {
    val state = HistoryDetailPreviewData.state(host = true)
    HistoryScreenPreview(state.copy(summary = state.summary?.copy(leaderboard = emptyList(), perQuestion = emptyList())))
}

@Preview(name = "Chi tiết trận — danh sách câu hỏi rỗng", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryDetailEmptyReviewPreview() {
    val state = HistoryDetailPreviewData.state()
    HistoryScreenPreview(state.copy(review = state.review?.copy(items = emptyList())))
}

@Composable
private fun HistoryScreenPreview(state: GameHistoryDetailUiState) {
    MyQuizAppTheme {
        GameHistoryDetailScreenContent(state, {}, {}, listState = rememberLazyListState())
    }
}
