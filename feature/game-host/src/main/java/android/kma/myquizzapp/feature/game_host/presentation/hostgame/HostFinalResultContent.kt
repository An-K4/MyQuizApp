package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.GameFinalResultHero
import android.kma.myquizzapp.core.ui.components.LeaderboardPlayerCard
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Only final result data. Never append the stale in-game tracker to this screen. */
@Composable
internal fun HostFinalResultScreenContent(
    state: HostGameUiState,
    listState: LazyListState,
    boardExpanded: Boolean,
    onToggleBoard: () -> Unit,
    statsExpanded: Boolean,
    onToggleStats: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val standings = hostFinalRemainingStandings(state)
    val stats = hostFinalQuestionStats(state)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface {
                QuizPrimaryButton("Về trang chủ", onHome,
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(Color(0xFF211238), Color(0xFF51318A), Color(0xFF34205F)))
        )) {
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "final-hero") {
                    GameFinalResultHero(hostFinalPodium(state),
                        subtitle = if (state.sessionStatus == SessionStatus.CANCELLED) "Trận đã bị hủy"
                        else "Tổng kết trận đấu của bạn")
                }
                when {
                    state.isFinalResultLoading -> item(key = "final-loading") {
                        HostFinalNotice("Đang tải kết quả cuối trận…", loading = true)
                    }
                    state.finalResultError != null -> item(key = "final-error") {
                        HostFinalNotice(state.finalResultError, onRetry = onRetry)
                    }
                    state.ended == null -> item(key = "final-missing") {
                        HostFinalNotice("Chưa nhận được kết quả cuối trận. Bạn có thể tải lại.", onRetry = onRetry)
                    }
                    else -> {
                        if (state.mode == GameMode.PRACTICE) {
                            item(key = "final-practice") { HostFinalNotice("Chế độ luyện tập không tính điểm và không có bảng xếp hạng.") }
                        } else if (standings.isNotEmpty()) {
                            item(key = "final-board-toggle") {
                                HostFinalDisclosure("Bảng xếp hạng", boardExpanded, onToggleBoard)
                            }
                            if (boardExpanded) {
                                items(standings, key = { "final-player-${it.id}" }) { row ->
                                    LeaderboardPlayerCard(
                                        rankLabel = "#${row.rank}", playerName = row.playerName,
                                        scoreText = "${hostFinalScore(row.playerScore)} điểm",
                                        initials = row.playerName.trim().split(Regex("\\s+"))
                                            .filter(String::isNotEmpty).takeLast(2)
                                            .joinToString("") { it.take(1).uppercase() },
                                    )
                                }
                            }
                        } else if (hostFinalStandings(state).isEmpty()) {
                            item(key = "final-board-empty") { HostFinalNotice("Không có dữ liệu bảng xếp hạng cho trận này.") }
                        }
                        item(key = "final-stats-toggle") {
                            HostFinalDisclosure("Thống kê từng câu", statsExpanded, onToggleStats)
                        }
                        if (statsExpanded) {
                            item(key = "final-stats-note") {
                                Text(if (state.isMarathon)
                                    "Thống kê theo lượt trả lời; các lần gặp lại cùng một câu được gộp chung. Số câu là vị trí xuất hiện đầu tiên."
                                else "Thống kê trên các lượt trả lời đã ghi nhận; không bao gồm người bỏ qua.",
                                    color = Color(0xFFE6DDF5), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (stats.isEmpty()) {
                                item(key = "final-stats-empty") { HostFinalNotice("Chưa có lượt trả lời để thống kê.") }
                            }
                            items(stats, key = { "final-stat-${it.questionId}-${it.questionIndex}" }) {
                                HostFinalQuestionStatCard(it)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Matches the wide Player-lobby identity card, but contains aggregate answer counts only. */
@Composable
internal fun HostFinalQuestionStatCard(stat: QuestionStat, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground = if (dark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val summary = hostFinalQuestionSummary(stat)
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground) {
        Row(Modifier.padding(20.dp).semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp),
                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("${stat.questionIndex + 1}", color = foreground, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = "Câu ${stat.questionIndex + 1}" })
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary.text, color = foreground, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { summary.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                        .clearAndSetSemantics { },
                    color = if (dark) FrontendColors.Success else FrontendColors.SuccessForeground,
                    trackColor = if (dark) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border,
                )
            }
        }
    }
}

@Composable
private fun HostFinalDisclosure(title: String, expanded: Boolean, onClick: () -> Unit) {
    QuizSecondaryButton(title, onClick,
        Modifier.fillMaxWidth().semantics { stateDescription = if (expanded) "Đang mở" else "Đang thu gọn" },
        trailingIcon = { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null) })
}

@Composable
private fun HostFinalNotice(text: String, onRetry: (() -> Unit)? = null, loading: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator()
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (onRetry != null) QuizSecondaryButton("Tải lại kết quả", onRetry, Modifier.fillMaxWidth())
        }
    }
}

private fun hostFinalPreviewState() = HostGameUiState(
    connection = HostGameConnection.CONNECTED, phase = GamePhase.FINISHED,
    sessionStatus = SessionStatus.FINISHED, mode = GameMode.CLASSIC,
    ended = GameEnded(leaderboard = listOf(
        LeaderboardRow(1, 1, "Minh Anh", 12450), LeaderboardRow(2, 2, "Quang Huy", 11200),
        LeaderboardRow(3, 3, "Tuấn Kiệt", 10850), LeaderboardRow(4, 4, "Quốc Tuấn", 9650),
        LeaderboardRow(5, 5, "Mai Lan", 8000)),
        perQuestion = listOf(QuestionStat(10, 0, 20, 15), QuestionStat(11, 1, 20, 8), QuestionStat(12, 2, 0, 0)))
)

@Composable
private fun HostFinalPreview(
    state: HostGameUiState = hostFinalPreviewState(), board: Boolean = true, stats: Boolean = false,
    dark: Boolean = false,
) {
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    MyQuizAppTheme(darkTheme = dark) {
        HostFinalResultScreenContent(state, listState, board, {}, stats, {}, {}, {}, snackbar)
    }
}

@Preview(name = "Host Final · Default", widthDp = 390, heightDp = 1000)
@Composable
private fun HostFinalDefaultPreview() = HostFinalPreview()

@Preview(name = "Host Final · Stats open", widthDp = 390, heightDp = 1100)
@Composable
private fun HostFinalStatsPreview() = HostFinalPreview(board = false, stats = true)

@Preview(name = "Host Final · Narrow & large text", widthDp = 320, heightDp = 1200, fontScale = 1.5f)
@Composable
private fun HostFinalNarrowPreview() = HostFinalPreview(board = false, stats = true)

@Preview(name = "Host Final · Dark", widthDp = 390, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HostFinalDarkPreview() = HostFinalPreview(board = false, stats = true, dark = true)

@Preview(name = "Host Final · One player", widthDp = 360, heightDp = 900)
@Composable
private fun HostFinalOnePreview() {
    val state = hostFinalPreviewState()
    HostFinalPreview(state.copy(ended = state.ended!!.copy(leaderboard = state.ended.leaderboard.take(1))))
}

@Preview(name = "Host Final · Loading recovery", widthDp = 390, heightDp = 844)
@Composable
private fun HostFinalLoadingPreview() = HostFinalPreview(hostFinalPreviewState().copy(ended = null, isFinalResultLoading = true))

@Preview(name = "Host Final · Recovery error", widthDp = 390, heightDp = 844)
@Composable
private fun HostFinalErrorPreview() = HostFinalPreview(hostFinalPreviewState().copy(ended = null, finalResultError = "Không thể tải kết quả. Kiểm tra kết nối rồi thử lại."))

@Preview(name = "Host Final · Empty", widthDp = 390, heightDp = 844)
@Composable
private fun HostFinalEmptyPreview() = HostFinalPreview(hostFinalPreviewState().copy(ended = GameEnded()), stats = true)

@Preview(name = "Host Final · Marathon", widthDp = 390, heightDp = 1100)
@Composable
private fun HostFinalMarathonPreview() = HostFinalPreview(hostFinalPreviewState().copy(mode = GameMode.MARATHON), board = false, stats = true)

@Preview(name = "Host Final · Question stat card", widthDp = 390, heightDp = 130)
@Preview(name = "Host Final · Stat card narrow & large text", widthDp = 320, heightDp = 180, fontScale = 1.5f)
@Composable
private fun HostFinalStatCardPreview() {
    MyQuizAppTheme(darkTheme = false) {
        Box(Modifier.fillMaxWidth().padding(20.dp)) { HostFinalQuestionStatCard(QuestionStat(10, 3, 20, 15)) }
    }
}
