package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import android.kma.myquizzapp.core.ui.components.AppConfirmDialogContent
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.core.ui.theme.Primary
import android.kma.myquizzapp.feature.lobby.R
import android.kma.myquizzapp.feature.lobby.presentation.components.LobbyColors
import android.kma.myquizzapp.feature.lobby.presentation.components.LobbyConnectionBadge
import android.kma.myquizzapp.feature.lobby.presentation.components.LobbyPlayerAvatar
import android.kma.myquizzapp.feature.lobby.presentation.components.LobbyPlayerStatusBadge
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Stateful boundary: socket effects/navigation, snackbar, scroll and leave confirmation. */
@Composable
fun PlayerLobbyScreen(
    onExit: (message: String?) -> Unit,
    onNavigateToGame: (gameId: Long, playerId: Long, socketToken: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerLobbyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            showLeaveConfirmation = false
            when (effect) {
                is PlayerLobbyEffect.ExitLobby -> onExit(effect.message)
                is PlayerLobbyEffect.NavigateToGame -> onNavigateToGame(
                    effect.gameId, effect.playerId, effect.socketToken,
                )
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onIntent(PlayerLobbyIntent.ErrorShown)
    }
    BackHandler { showLeaveConfirmation = true }

    PlayerLobbyScreenContent(
        uiState = uiState,
        gridState = gridState,
        onBack = { showLeaveConfirmation = true },
        onRetry = { viewModel.onIntent(PlayerLobbyIntent.Retry) },
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
    if (showLeaveConfirmation) {
        AppConfirmDialog(
            title = LEAVE_ROOM_TITLE,
            message = LEAVE_ROOM_MESSAGE,
            confirmText = "Rời phòng",
            dismissText = "Ở lại",
            onConfirm = {
                showLeaveConfirmation = false
                // Keep the existing leaveLobby-before-disconnect path; never pop directly.
                viewModel.onIntent(PlayerLobbyIntent.LeaveRoom)
            },
            onDismiss = { showLeaveConfirmation = false },
        )
    }
}

/** Pure UI: header, identity card and roster scroll together; connection footer stays visible. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLobbyScreenContent(
    uiState: PlayerLobbyUiState,
    gridState: LazyGridState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LobbyColors.surface,
        contentColor = LobbyColors.foreground,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = {
                    Text("Phòng chờ", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Rời phòng chờ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LobbyColors.surface,
                    titleContentColor = LobbyColors.foreground,
                    navigationIconContentColor = LobbyColors.foreground,
                ),
            )
        },
        bottomBar = { PlayerConnectionFooter(uiState.connection, onRetry) },
    ) { innerPadding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            // At most two columns. Small screens/large text get one readable column.
            val minimumCellWidth = 100.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val columns = if (maxWidth >= minimumCellWidth * 2 + 50.dp) 2 else 1
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "waiting-header", span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        WaitingHero(uiState.sessionStatus, uiState.hasLobbySnapshot)
                        MyPlayerCard(uiState.myPlayer, uiState.hasLobbySnapshot)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (uiState.hasLobbySnapshot) "Người chơi cùng phòng (${uiState.playerCount} người chơi)"
                                else "Người chơi cùng phòng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when {
                                    !uiState.hasLobbySnapshot -> "Đang tải danh sách người chơi..."
                                    uiState.connection == ConnectionStatus.CONNECTED ->
                                        "${uiState.connectedPlayerCount} người đang kết nối"
                                    else -> "${uiState.connectedPlayerCount} người kết nối ở lần đồng bộ gần nhất"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LobbyColors.muted,
                            )
                        }
                    }
                }
                when {
                    !uiState.hasLobbySnapshot -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = Primary)
                        }
                    }
                    uiState.players.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Chưa có thông tin người chơi trong phòng.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LobbyColors.muted,
                        )
                    }
                    else -> items(uiState.players, key = { "player-${it.id}" }) { player ->
                        PlayerLobbyCard(player, isMe = player.id == uiState.myPlayerId)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingHero(sessionStatus: SessionStatus?, hasSnapshot: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.gamepad_lobby),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = when {
                !hasSnapshot -> "Đang kết nối tới phòng..."
                sessionStatus == SessionStatus.ACTIVE -> "Trận đang diễn ra"
                sessionStatus == SessionStatus.PAUSED -> "Trận đang tạm dừng"
                else -> "Đang chờ chủ phòng bắt đầu trận đấu..."
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LobbyColors.foreground,
            textAlign = TextAlign.Center,
        )
        Text(
            "Giữ màn hình này để không bỏ lỡ lúc trận đấu bắt đầu.",
            style = MaterialTheme.typography.bodyMedium,
            color = LobbyColors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Identity only: no '(Bạn)' suffix, accent name or special highlight in this wide card. */
@Composable
private fun MyPlayerCard(player: LobbyPlayer?, hasSnapshot: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = LobbyColors.softSurface,
    ) {
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (player != null) LobbyPlayerAvatar(player, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bạn là:", style = MaterialTheme.typography.bodySmall, color = LobbyColors.muted)
                Text(
                    player?.playerName ?: if (hasSnapshot) "Chưa có thông tin của bạn" else "Đang tải thông tin...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LobbyColors.foreground,
                )
            }
        }
    }
}

@Composable
private fun PlayerLobbyCard(player: LobbyPlayer, isMe: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = LobbyColors.softSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LobbyPlayerAvatar(player, size = 42.dp)
            Text(
                text = if (isMe) "${player.playerName} (Bạn)" else player.playerName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = LobbyColors.foreground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            LobbyPlayerStatusBadge(player)
            player.lives?.let { lives ->
                Surface(shape = RoundedCornerShape(50), color = LobbyColors.neutralSurface) {
                    Text(
                        "$lives mạng",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = LobbyColors.muted,
                    )
                }
            }
        }
    }
}

/** Always outside the scrolling grid; connection loss never discards the last roster. */
@Composable
private fun PlayerConnectionFooter(status: ConnectionStatus, onRetry: () -> Unit) {
    Surface(color = LobbyColors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = LobbyColors.border)
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LobbyConnectionBadge(status, connectedLabel = "Đã kết nối máy chủ")
                if (status != ConnectionStatus.CONNECTED) {
                    Text(
                        when (status) {
                            ConnectionStatus.CONNECTING -> "Đang kết nối tới phòng..."
                            ConnectionStatus.RECONNECTING -> "Đang tự động kết nối lại. Danh sách chưa được cập nhật."
                            ConnectionStatus.RECONNECT_FAILED -> "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại."
                            ConnectionStatus.CONNECTED -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LobbyColors.muted,
                    )
                }
                if (status == ConnectionStatus.RECONNECT_FAILED) {
                    QuizSecondaryButton("Kết nối lại", onRetry)
                }
            }
        }
    }
}

private const val LEAVE_ROOM_TITLE = "Rời phòng chờ?"
private const val LEAVE_ROOM_MESSAGE = "Bạn sẽ rời phòng chơi và quay về màn trước. Bạn có thể nhập mã phòng để tham gia lại nếu phòng còn cho phép."

// Six fixed, network-free previews. No parameter-provider combinations.
private fun playerLobbyPreviewState(): PlayerLobbyUiState {
    val names = listOf("Minh Anh", "Hoàng Long", "Thảo Vy", "Quang Huy", "Tuấn Kiệt", "Quốc Tuấn", "Mai Lan", "Đức Anh", "Ngọc Hà")
    return PlayerLobbyUiState(
        sessionStatus = SessionStatus.LOBBY,
        connection = ConnectionStatus.CONNECTED,
        myPlayerId = 6,
        players = List(18) { index ->
            LobbyPlayer(
                id = index.toLong() + 1,
                playerName = names[index % names.size],
                playerScore = 0,
                status = if (index == 2 || index == 7) "disconnected" else "connected",
            )
        },
    )
}

@Composable
private fun PlayerLobbyPreviewContent(state: PlayerLobbyUiState) {
    PlayerLobbyScreenContent(
        uiState = state, gridState = rememberLazyGridState(), onBack = {}, onRetry = {},
    )
}

@Preview(name = "Player lobby Main", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerLobbyMainPreview() {
    MyQuizAppTheme { PlayerLobbyPreviewContent(playerLobbyPreviewState()) }
}

@Preview(name = "Player lobby Small / large text", showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
private fun PlayerLobbySmallPreview() {
    MyQuizAppTheme { PlayerLobbyPreviewContent(playerLobbyPreviewState()) }
}

@Preview(name = "Player lobby Connecting", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerLobbyConnectingPreview() {
    MyQuizAppTheme { PlayerLobbyPreviewContent(PlayerLobbyUiState(myPlayerId = 6)) }
}

@Preview(name = "Player lobby Connection failed", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerLobbyConnectionFailedPreview() {
    MyQuizAppTheme {
        PlayerLobbyPreviewContent(playerLobbyPreviewState().copy(connection = ConnectionStatus.RECONNECT_FAILED))
    }
}

@Preview(name = "Player lobby Lives / missing identity", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerLobbyLivesPreview() {
    val state = playerLobbyPreviewState()
    MyQuizAppTheme {
        PlayerLobbyPreviewContent(
            state.copy(
                myPlayerId = 100,
                players = listOf(
                    state.players[0].copy(playerName = "Người chơi có biệt danh dài để kiểm tra bố cục", lives = 3),
                    state.players[1].copy(lives = 0, status = "eliminated"),
                    state.players[2].copy(lives = 1, status = "finished"),
                    state.players[3].copy(status = "future-status"),
                ),
            ),
        )
    }
}

@Preview(name = "Player lobby Leave confirmation", showBackground = true, widthDp = 390)
@Composable
private fun PlayerLobbyLeavePreview() {
    MyQuizAppTheme {
        AppConfirmDialogContent(
            title = LEAVE_ROOM_TITLE, message = LEAVE_ROOM_MESSAGE,
            confirmText = "Rời phòng", dismissText = "Ở lại",
            onConfirm = {}, onDismiss = {}, scrollState = rememberScrollState(),
            modifier = Modifier.padding(20.dp),
        )
    }
}
