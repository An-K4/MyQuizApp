package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Màn phòng chờ của NGƯỜI CHƠI (stateful).
 *
 * @param onExit được gọi khi rời phòng. `message` khác null là bị buộc rời — tầng
 *   navigation quyết định hiển thị thông báo ở đâu sau khi pop.
 */
@Composable
fun PlayerLobbyScreen(
    onExit: (message: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerLobbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlayerLobbyEffect.ExitLobby -> onExit(effect.message)
            }
        }
    }

    PlayerLobbyScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLobbyScreenContent(
    uiState: PlayerLobbyUiState,
    onIntent: (PlayerLobbyIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onIntent(PlayerLobbyIntent.ErrorShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Phòng chờ") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PlayerLobbyIntent.LeaveRoom) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Rời phòng"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            WaitingCard(sessionStatus = uiState.sessionStatus)

            Spacer(Modifier.height(12.dp))

            ConnectionBanner(
                status = uiState.connection,
                onRetry = { onIntent(PlayerLobbyIntent.Retry) }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Người chơi (${uiState.playerCount})",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            when {
                !uiState.hasLobbySnapshot -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.players, key = { it.id }) { player ->
                        PlayerRow(
                            player = player,
                            isMe = player.id == uiState.myPlayerId,
                            showLives = uiState.showLives
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingCard(sessionStatus: SessionStatus?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (sessionStatus) {
                    SessionStatus.ACTIVE -> "Trận đang diễn ra"
                    SessionStatus.PAUSED -> "Trận đang tạm dừng"
                    else -> "Đang chờ chủ phòng bắt đầu"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Giữ màn hình này để không bị lỡ lượt chơi.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ConnectionBanner(
    status: ConnectionStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        ConnectionStatus.CONNECTED -> Text(
            text = "Đã kết nối",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.fillMaxWidth()
        )

        ConnectionStatus.CONNECTING -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Text("Đang kết nối...", style = MaterialTheme.typography.bodyMedium)
        }

        ConnectionStatus.RECONNECTING -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Mất kết nối — đang thử lại",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onRetry) { Text("Kết nối lại") }
        }
    }
}

@Composable
private fun PlayerRow(
    player: LobbyPlayer,
    isMe: Boolean,
    showLives: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        // Tô nền dòng của chính mình: phòng đông tới 100 người, không có dấu hiệu
        // này thì người chơi rất khó tự tìm mình trong danh sách.
        colors = if (isMe) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayerAvatar(name = player.playerName, avatarUrl = player.playerAvatar)

            Text(
                text = if (isMe) "${player.playerName} (bạn)" else player.playerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            // lives chỉ có nghĩa ở mode survival; mode khác backend trả null nên ẩn hẳn
            // thay vì hiện "0 mạng" gây hiểu nhầm là đã bị loại.
            if (showLives && player.lives != null) {
                Text(
                    text = "❤️ ${player.lives}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = if (player.status == STATUS_DISCONNECTED) "Mất kết nối" else "Sẵn sàng",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Avatar trong lobby.
 *
 * Khách không có ảnh đại diện, mà [Avatar] của core:ui lại rơi về icon người
 * chung chung — cả phòng toàn khách sẽ thành một dãy icon giống hệt nhau. Ở đây
 * dùng chữ cái đầu để mỗi người vẫn khác biệt. Giữ cục bộ trong package này vì
 * mới chỉ lobby cần; khi màn chơi / bảng điểm cần nữa thì chuyển lên core:ui.
 */
@Composable
private fun PlayerAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (avatarUrl != null) {
        Avatar(avatarUrl = avatarUrl, contentDescription = name, size = size, modifier = modifier)
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/** Giá trị status do backend gửi (chuỗi, không phải enum — xem LobbyPlayerDto). */
private const val STATUS_DISCONNECTED = "disconnected"

@Preview(showBackground = true)
@Composable
private fun PlayerLobbyScreenContentPreview() {
    MyQuizAppTheme {
        PlayerLobbyScreenContent(
            uiState = PlayerLobbyUiState(
                sessionStatus = SessionStatus.LOBBY,
                connection = ConnectionStatus.CONNECTED,
                myPlayerId = 2,
                players = listOf(
                    LobbyPlayer(id = 1, playerName = "Kiro", playerScore = 0, status = "connected"),
                    LobbyPlayer(
                        id = 2,
                        playerName = "Khách vui tính",
                        playerScore = 0,
                        status = "connected"
                    ),
                    LobbyPlayer(
                        id = 3,
                        playerName = "Lan",
                        playerScore = 0,
                        status = "disconnected"
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerLobbyScreenContentConnectingPreview() {
    MyQuizAppTheme {
        PlayerLobbyScreenContent(uiState = PlayerLobbyUiState(), onIntent = {})
    }
}
