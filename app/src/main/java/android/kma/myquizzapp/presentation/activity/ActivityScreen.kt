package android.kma.myquizzapp.presentation.activity

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityScreen(
    onOpenHistory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Bottom navigation giữ ViewModel/back stack của tab. Khi quay lại từ một trận
    // vừa kết thúc, state cũ (kể cả empty state) vẫn còn nên phải nạp lại server.
    // ViewModel tự bỏ qua nếu initial load đang chạy, tránh request kép lần đầu mở.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ActivityIntent.Refresh)
    }

    ActivityScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onOpenHistory = onOpenHistory,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreenContent(
    uiState: ActivityUiState,
    onIntent: (ActivityIntent) -> Unit,
    onOpenHistory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Hoạt động") }) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.showRoleTabs) {
                val roles = listOf(GameHistoryRole.PLAYED, GameHistoryRole.HOSTED)
                TabRow(selectedTabIndex = roles.indexOf(uiState.selectedRole)) {
                    roles.forEach { role ->
                        Tab(
                            selected = uiState.selectedRole == role,
                            onClick = { onIntent(ActivityIntent.SelectRole(role)) },
                            text = { Text(if (role == GameHistoryRole.PLAYED) "Đã chơi" else "Đã tổ chức") }
                        )
                    }
                }
            }

            if (uiState.session is SessionState.Unknown) {
                LoadingState()
            } else {
                HistoryList(
                    page = uiState.currentList,
                    role = uiState.selectedRole,
                    onRetry = { onIntent(ActivityIntent.Retry) },
                    onLoadMore = { onIntent(ActivityIntent.LoadMore) },
                    onOpenHistory = onOpenHistory
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    page: HistoryListState,
    role: GameHistoryRole,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenHistory: (Long) -> Unit
) {
    when {
        page.isInitialLoading && page.items.isEmpty() -> LoadingState()
        page.errorMessage != null && page.items.isEmpty() -> ErrorState(page.errorMessage, onRetry)
        page.hasLoaded && page.items.isEmpty() -> EmptyState(role)
        else -> {
            val listState = rememberLazyListState()
            val shouldLoadMore by remember(page.items.size, page.hasMore, page.isLoadingMore) {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    page.hasMore && !page.isLoadingMore && lastVisible >= page.items.lastIndex - 2
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) onLoadMore()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(page.items, key = { it.sessionId }) { item ->
                    HistoryCard(
                        item = item,
                        role = role,
                        onClick = { onOpenHistory(item.sessionId) }
                    )
                }
                if (page.isLoadingMore) {
                    item { Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                page.appendErrorMessage?.let { message ->
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            TextButton(onClick = onRetry) { Text("Thử lại") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: GameHistoryItem,
    role: GameHistoryRole,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            RemoteImage(
                imageUrl = item.quizImage,
                contentDescription = item.quizName ?: item.sessionName,
                modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.medium)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.quizName?.takeIf(String::isNotBlank) ?: item.sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.gameMode.label()} • ${item.sessionStatus.label()} • ${formatEndedAt(item.endedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.hostName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(item.hostAvatar, size = 22.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Host: ${item.hostName}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (role == GameHistoryRole.PLAYED) {
                    Text(
                        text = buildString {
                            append("Điểm ${item.playerScore ?: 0}")
                            item.correctAnswersCount?.let { append(" • Đúng $it/${item.totalQuestions}") }
                            item.rank?.let { append(" • Hạng $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "${item.totalPlayers} người chơi • ${item.totalQuestions} câu hỏi",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun EmptyState(role: GameHistoryRole) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (role == GameHistoryRole.PLAYED) "Bạn chưa chơi trận nào" else "Bạn chưa tổ chức trận nào",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
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

private val historyDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private fun formatEndedAt(raw: String): String = runCatching {
    historyDateFormatter.format(Instant.parse(raw).atZone(ZoneId.systemDefault()))
}.getOrDefault(raw)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActivityScreenPreview() {
    MyQuizAppTheme {
        ActivityScreenContent(
            uiState = ActivityUiState(
                session = SessionState.Guest,
                played = HistoryListState(
                    hasLoaded = true,
                    items = listOf(
                        GameHistoryItem(
                            sessionId = 12,
                            sessionName = "Phòng tối thứ sáu",
                            gameMode = GameMode.CLASSIC,
                            sessionStatus = SessionStatus.FINISHED,
                            totalPlayers = 8,
                            totalQuestions = 10,
                            endedAt = "2026-10-03T14:30:00.000Z",
                            quizId = 2,
                            quizName = "Kiến thức Android",
                            quizImage = null,
                            hostName = "Kiro",
                            hostAvatar = null,
                            playerScore = 830,
                            correctAnswersCount = 8,
                            rank = 2
                        )
                    )
                )
            ),
            onIntent = {},
            onOpenHistory = {}
        )
    }
}
