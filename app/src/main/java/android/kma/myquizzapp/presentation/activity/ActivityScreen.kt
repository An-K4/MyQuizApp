package android.kma.myquizzapp.presentation.activity

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        "Hoạt động",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.showRoleTabs) {
                val roles = listOf(GameHistoryRole.PLAYED, GameHistoryRole.HOSTED)
                TabRow(selectedTabIndex = roles.indexOf(uiState.selectedRole)) {
                    roles.forEach { role ->
                        Tab(
                            selected = uiState.selectedRole == role,
                            onClick = { onIntent(ActivityIntent.SelectRole(role)) },
                            text = {
                                val list = if (role == GameHistoryRole.PLAYED) uiState.played else uiState.hosted
                                val title = if (role == GameHistoryRole.PLAYED) "Đã tham gia chơi" else "Đã tổ chức Host"
                                Text(
                                    text = list.totalCount?.let { "$title ($it)" } ?: title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(page.items, key = { it.sessionId }) { item ->
                    val onClick = { onOpenHistory(item.sessionId) }
                    when (role) {
                        GameHistoryRole.PLAYED -> PlayedActivityCard(item, onClick)
                        GameHistoryRole.HOSTED -> HostedActivityCard(item, onClick)
                    }
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

@Preview(name = "Hoạt động — đã chơi", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Hoạt động — đã chơi tối", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActivityPlayedPreview() {
    MyQuizAppTheme {
        ActivityScreenContent(
            uiState = ActivityUiState(
                session = SessionState.LoggedIn(previewUser()),
                played = HistoryListState(
                    hasLoaded = true,
                    hasMore = true,
                    nextCursor = "preview-next",
                    totalCount = 24,
                    items = previewPlayedItems()
                ),
                hosted = HistoryListState(hasLoaded = true, totalCount = 6)
            ),
            onIntent = {},
            onOpenHistory = {}
        )
    }
}

@Preview(name = "Hoạt động — Host", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActivityHostedPreview() {
    MyQuizAppTheme {
        ActivityScreenContent(
            uiState = ActivityUiState(
                session = SessionState.LoggedIn(previewUser()),
                selectedRole = GameHistoryRole.HOSTED,
                played = HistoryListState(hasLoaded = true, totalCount = 24),
                hosted = HistoryListState(
                    hasLoaded = true,
                    totalCount = 6,
                    items = listOf(
                        previewHistoryItem(
                            sessionId = 24,
                            title = "Từ vựng tiếng Anh B2",
                            endedAt = "2026-10-10T07:30:00Z",
                            totalPlayers = 20,
                            totalQuestions = 15,
                            playerScore = null,
                            correctAnswersCount = null,
                            rank = null
                        )
                    )
                )
            ),
            onIntent = {},
            onOpenHistory = {}
        )
    }
}

@Preview(name = "Hoạt động — khách chưa chơi", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActivityGuestEmptyPreview() {
    MyQuizAppTheme {
        ActivityScreenContent(
            uiState = ActivityUiState(
                session = SessionState.Guest,
                played = HistoryListState(hasLoaded = true)
            ),
            onIntent = {},
            onOpenHistory = {}
        )
    }
}

@Preview(name = "Hoạt động — đang tải", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActivityLoadingPreview() {
    MyQuizAppTheme {
        ActivityScreenContent(ActivityUiState(), {}, {})
    }
}

private fun previewUser() = User(
    id = 7,
    fullname = "Kiro",
    email = "kiro@example.com",
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z"
)

private fun previewPlayedItems() = listOf(
    previewHistoryItem(
        sessionId = 12,
        title = "Đố vui Lịch sử Việt Nam",
        endedAt = "2026-10-11T07:30:00Z",
        totalPlayers = 15,
        totalQuestions = 15,
        playerScore = 8500,
        correctAnswersCount = 12,
        rank = 2,
        quizImage = "https://preview.invalid/history.png"
    ),
    previewHistoryItem(
        sessionId = 13,
        title = "Từ vựng tiếng Anh B2",
        endedAt = "2026-10-10T07:30:00Z",
        totalPlayers = 20,
        totalQuestions = 20,
        playerScore = 6200,
        correctAnswersCount = null,
        rank = 5
    ),
    previewHistoryItem(
        sessionId = 14,
        title = "IQ Logic Toán học",
        endedAt = "2026-10-08T07:30:00Z",
        totalPlayers = 10,
        totalQuestions = 10,
        playerScore = 9000,
        correctAnswersCount = 10,
        rank = 1
    )
)

private fun previewHistoryItem(
    sessionId: Long,
    title: String,
    endedAt: String,
    totalPlayers: Int,
    totalQuestions: Int,
    playerScore: Int?,
    correctAnswersCount: Int?,
    rank: Int?,
    quizImage: String? = null
) = GameHistoryItem(
    sessionId = sessionId,
    sessionName = title,
    gameMode = GameMode.CLASSIC,
    sessionStatus = SessionStatus.FINISHED,
    totalPlayers = totalPlayers,
    totalQuestions = totalQuestions,
    endedAt = endedAt,
    quizId = sessionId,
    quizName = title,
    quizImage = quizImage,
    hostName = "Minh Anh",
    hostAvatar = null,
    playerScore = playerScore,
    correctAnswersCount = correctAnswersCount,
    rank = rank
)
