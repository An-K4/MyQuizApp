package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless monitoring layout. Screen owns scroll/menu/dialog state; no clocks or socket effects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HostSelfPacedScreenContent(
    state: HostGameUiState,
    onIntent: (HostGameIntent) -> Unit,
    onRequestLeave: () -> Unit,
    onRequestEnd: () -> Unit,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else FrontendColors.PageBackground
    val rows = hostMonitorRows(state)
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = page,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Theo dõi tiến độ", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onRequestLeave) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Rời màn điều khiển")
                } },
                actions = {
                    IconButton(onClick = { onMenuExpandedChange(true) }) { Icon(Icons.Filled.MoreVert, "Điều khiển trận") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                        DropdownMenuItem(text = { Text(state.pauseLabel) }, enabled = state.canPauseOrResume,
                            onClick = { onMenuExpandedChange(false); onIntent(HostGameIntent.PauseOrResume) })
                        DropdownMenuItem(text = { Text("Kết thúc trận", color = if (state.canEndGame)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                            enabled = state.canEndGame,
                            onClick = { onMenuExpandedChange(false); onRequestEnd() })
                        DropdownMenuItem(text = { Text("Rời màn điều khiển") },
                            onClick = { onMenuExpandedChange(false); onRequestLeave() })
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = page))
        },
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "mode") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint,
                        shape = RoundedCornerShape(50)) {
                        Text(hostMonitorModeLabel(state), Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge)
                    }
                    if (state.isMarathon) Text("Mỗi người chơi có thời gian riêng.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            hostMonitorNotice(state)?.let { notice -> item(key = "notice") {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(notice, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        if (state.connection == HostGameConnection.RECONNECT_FAILED)
                            QuizSecondaryButton("Kết nối lại", { onIntent(HostGameIntent.Retry) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            } }
            item(key = "overview") { HostMonitorOverview(state) }
            item(key = "players-title") {
                Text(if (state.hasFreshMonitoringData) "Người chơi · ${rows.size}" else "Người chơi",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
            }
            if (rows.isEmpty()) item(key = "empty") {
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!state.hasFreshMonitoringData && state.connection == HostGameConnection.CONNECTED)
                            CircularProgressIndicator(Modifier.size(28.dp))
                        else Icon(Icons.Outlined.Info, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (state.hasFreshMonitoringData) "Chưa có người chơi trong danh sách."
                            else "Chưa có dữ liệu người chơi được đồng bộ.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            items(rows, key = { "player-${it.id}" }) { row -> HostMonitorPlayerCard(row, state) }
        }
    }
}

@Composable
private fun HostMonitorOverview(state: HostGameUiState) {
    val metrics = hostMonitorMetrics(state)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 280.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEach { metric ->
                        val tone = hostMonitorTone(metric.status)
                        Surface(modifier = Modifier.weight(1f), color = tone.first, contentColor = tone.second,
                            shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(metric.value?.toString() ?: "—", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall)
                                Text(metric.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostMonitorPlayerCard(row: HostLeaderboardRow, state: HostGameUiState) {
    val progress = hostMonitorProgress(row, state)
    val status = hostMonitorStatus(row.status)
    val tone = hostMonitorTone(status)
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val scoreInline = maxWidth >= 300.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics { }) {
                            Text(hostMonitorInitials(row.playerName), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(row.playerName.ifBlank { "Người chơi" }, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Surface(color = tone.first, contentColor = tone.second, shape = RoundedCornerShape(50)) {
                            Text(status.label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (state.monitorShowsScores && scoreInline) Text(hostMonitorScoreLabel(row.playerScore),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (state.monitorShowsScores && !scoreInline) Text(hostMonitorScoreLabel(row.playerScore),
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(progress.label, style = MaterialTheme.typography.bodyMedium)
                    progress.fraction?.let { fraction ->
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(5.dp)
                            .semantics { contentDescription = progress.label })
                    }
                }
                if (row.correctCount >= 0 && row.wrongCount >= 0)
                    Text("Đúng ${row.correctCount} · Sai ${row.wrongCount}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // No lives/streak: the incremental progress event does not keep those fields current.
            }
        }
    }
}

/** Colors supplement explicit status labels; no meaning relies on color alone. */
@Composable
private fun hostMonitorTone(status: HostMonitorStatus): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (status) {
        HostMonitorStatus.PLAYING -> if (dark) Color(0xFF123F32) to Color(0xFF7FE0B2)
            else Color(0xFFE5F8EF) to Color(0xFF006B48)
        HostMonitorStatus.FINISHED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        HostMonitorStatus.DISCONNECTED -> if (dark) Color(0xFF3F2E13) to Color(0xFFFFD98A)
            else Color(0xFFFFF7DF) to Color(0xFF8A4B00)
        HostMonitorStatus.ELIMINATED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        HostMonitorStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// Preview-only fixtures. No ViewModel, network, timers or runtime sample fallbacks.
private fun hostMonitorFixture(mode: GameMode = GameMode.SOLO) = HostGameUiState(
    connection = HostGameConnection.CONNECTED, isStateSynchronized = true, hasMonitoringData = true,
    sessionStatus = SessionStatus.ACTIVE, phase = GamePhase.QUESTION_ACTIVE, mode = mode,
    pacing = Pacing.SELF, totalQuestions = 10,
    leaderboard = HostLeaderboard(rows = listOf(
        HostLeaderboardRow(1, 1, "Minh Anh", 2450, answeredCount = 4, correctCount = 3, wrongCount = 1,
            currentQuestionIndex = 4, totalQuestions = 10, lives = if (mode == GameMode.SURVIVAL) 2 else null),
        HostLeaderboardRow(2, 2, "Quang Huy", 4200, answeredCount = 10, correctCount = 8, wrongCount = 2,
            currentQuestionIndex = 10, totalQuestions = 10, status = "finished"),
        HostLeaderboardRow(3, 3, "Tuấn Kiệt", 1800, answeredCount = 3, correctCount = 2, wrongCount = 1,
            currentQuestionIndex = 3, totalQuestions = 10, status = "disconnected"),
    )),
)
@Composable
private fun HostMonitorPreview(state: HostGameUiState = hostMonitorFixture(), expanded: Boolean = false) {
    MyQuizAppTheme { HostSelfPacedScreenContent(state, {}, {}, {}, rememberLazyListState(),
        remember { SnackbarHostState() }, expanded, {}) }
}
@Preview(name = "Host Solo · Theo dõi tiến độ", widthDp = 390, heightDp = 844)
@Composable private fun HostSoloMonitorPreview() = HostMonitorPreview()
@Preview(name = "Host Survival · Bị loại", widthDp = 390, heightDp = 844)
@Composable private fun HostSurvivalMonitorPreview() = HostMonitorPreview(hostMonitorFixture(GameMode.SURVIVAL).let { state ->
    state.copy(leaderboard = state.leaderboard.copy(rows = state.leaderboard.rows.map {
        if (it.id == 1L) it.copy(status = "eliminated", lives = 0) else it
    }))
})
@Preview(name = "Host Marathon · Lặp ngân hàng câu", widthDp = 390, heightDp = 844)
@Composable private fun HostMarathonMonitorPreview() = HostMonitorPreview(hostMonitorFixture(GameMode.MARATHON).let { state ->
    state.copy(leaderboard = state.leaderboard.copy(rows = state.leaderboard.rows.map {
        it.copy(answeredCount = 18, correctCount = 12, wrongCount = 6, lives = 2)
    }))
})
@Preview(name = "Host Practice · Không điểm hoặc hạng", widthDp = 390, heightDp = 844)
@Composable private fun HostPracticeMonitorPreview() = HostMonitorPreview(hostMonitorFixture(GameMode.PRACTICE))
@Preview(name = "Host Monitor · Hẹp, tên dài, chữ lớn", widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Composable private fun HostMonitorNarrowPreview() = HostMonitorPreview(hostMonitorFixture().let { state ->
    state.copy(leaderboard = state.leaderboard.copy(rows = state.leaderboard.rows.map {
        it.copy(playerName = "Nguyễn Hoàng Minh Anh tên rất dài để kiểm tra xuống dòng", playerScore = 2147483647)
    }))
})
@Preview(name = "Host Monitor · Dark", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun HostMonitorDarkPreview() = HostMonitorPreview()
@Preview(name = "Host Monitor · Loading", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorLoadingPreview() = HostMonitorPreview(hostMonitorFixture().copy(
    hasMonitoringData = false, leaderboard = HostLeaderboard()))
@Preview(name = "Host Monitor · Danh sách rỗng đã đồng bộ", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorEmptyPreview() = HostMonitorPreview(hostMonitorFixture().copy(leaderboard = HostLeaderboard()))
@Preview(name = "Host Monitor · Kết nối lại", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorReconnectPreview() = HostMonitorPreview(hostMonitorFixture().copy(
    connection = HostGameConnection.RECONNECT_FAILED, isStateSynchronized = false))
@Preview(name = "Host Monitor · Đang đồng bộ, khóa lệnh", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorSyncPreview() = HostMonitorPreview(hostMonitorFixture().copy(isStateSynchronized = false))
@Preview(name = "Host Monitor · Tạm dừng", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorPausePreview() = HostMonitorPreview(hostMonitorFixture().copy(sessionStatus = SessionStatus.PAUSED))
@Preview(name = "Host Monitor · Menu điều khiển", widthDp = 390, heightDp = 844)
@Composable private fun HostMonitorMenuPreview() = HostMonitorPreview(expanded = true)
