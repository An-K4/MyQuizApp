package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.gameconfig.GameModeConfigEditor
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Màn phòng chờ của HOST (stateful).
 *
 * @param onExit được gọi khi phải rời phòng. `message` khác null là trường hợp
 *   bị buộc rời (token sai, phòng không còn...) — tầng navigation quyết định
 *   hiển thị thông báo ở đâu sau khi pop.
 * @param onNavigateToHostGame trận đã bắt đầu thật (đã nhận `game:started`).
 */
@Composable
fun HostLobbyScreen(
    onExit: (message: String?) -> Unit,
    onNavigateToHostGame: (gameId: Long, socketToken: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostLobbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HostLobbyEffect.ExitLobby -> onExit(effect.message)
                is HostLobbyEffect.NavigateToHostGame ->
                    onNavigateToHostGame(effect.gameId, effect.socketToken)
            }
        }
    }

    HostLobbyScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreenContent(
    uiState: HostLobbyUiState,
    onIntent: (HostLobbyIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Cảnh báo "phòng chưa có ai" là trạng thái của UI chứ không phải của phòng,
    // nên giữ ở đây. rememberSaveable để quay máy không làm dialog biến mất.
    var showEmptyRoomWarning by rememberSaveable { mutableStateOf(false) }

    // Lỗi không chết người (VD một lệnh bị từ chối) hiện bằng snackbar rồi xóa
    // khỏi state, tránh hiện lại mỗi lần recompose.
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onIntent(HostLobbyIntent.ErrorShown)
    }

    LaunchedEffect(uiState.configNotice) {
        val message = uiState.configNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onIntent(HostLobbyIntent.ConfigNoticeShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Phòng chờ") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(HostLobbyIntent.LeaveRoom) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Thoát phòng"
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
            SessionCodeCard(
                sessionCode = uiState.sessionCode,
                onCopyCode = { clipboard.setText(AnnotatedString(uiState.sessionCode)) },
                onCopyLink = { clipboard.setText(AnnotatedString(uiState.shareLink)) }
            )

            Spacer(Modifier.height(12.dp))

            ConnectionBanner(
                status = uiState.connection,
                onRetry = { onIntent(HostLobbyIntent.Retry) }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onIntent(HostLobbyIntent.OpenConfigSheet) },
                enabled = uiState.canEditConfig && !uiState.isStarting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.canEditConfig) {
                        "Sửa cấu hình phòng"
                    } else {
                        "Trận đã bắt đầu — không sửa được cấu hình"
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Người chơi (${uiState.playerCount})",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            when {
                // Chưa có snapshot nào: không biết phòng rỗng hay chưa kịp nhận dữ liệu
                // — hiện loading thay vì báo "chưa có ai" (dễ gây hiểu sai).
                !uiState.hasLobbySnapshot -> LoadingPlayers()
                uiState.players.isEmpty() -> EmptyPlayers()
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.players, key = { it.id }) { player ->
                        PlayerRow(player)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            StartGameButton(
                uiState = uiState,
                onStart = {
                    if (uiState.players.isEmpty()) {
                        showEmptyRoomWarning = true
                    } else {
                        onIntent(HostLobbyIntent.StartGame)
                    }
                }
            )

            Spacer(Modifier.height(12.dp))
        }

        if (uiState.isConfigSheetOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onIntent(HostLobbyIntent.DismissConfigSheet) },
                sheetState = sheetState
            ) {
                ConfigSheetContent(uiState = uiState, onIntent = onIntent)
            }
        }

        if (showEmptyRoomWarning) {
            AlertDialog(
                onDismissRequest = { showEmptyRoomWarning = false },
                title = { Text("Phòng chưa có ai") },
                text = {
                    Text(
                        "Chưa có người chơi nào vào phòng. Bắt đầu bây giờ thì những người " +
                            "vào sau chỉ tham gia được nếu phòng bật cho vào giữa trận."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showEmptyRoomWarning = false
                        onIntent(HostLobbyIntent.StartGame)
                    }) { Text("Vẫn bắt đầu") }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyRoomWarning = false }) { Text("Để chờ thêm") }
                }
            )
        }
    }
}

/**
 * Nội dung bảng sửa cấu hình.
 *
 * Dùng Column + verticalScroll chứ không LazyColumn: nội dung lồng trong bottom
 * sheet đã có sẵn chỉ vài chục dòng và quy ước của dự án là mỗi trang chỉ một
 * LazyColumn — danh sách người chơi đã giữ chỗ đó.
 */
@Composable
private fun ConfigSheetContent(
    uiState: HostLobbyUiState,
    onIntent: (HostLobbyIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = uiState.mode
    val form = uiState.configForm

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cấu hình phòng", style = MaterialTheme.typography.titleLarge)

        when {
            uiState.isLoadingSpec -> Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Text("Đang tải thiết lập của chế độ chơi...")
            }

            mode == null || form == null -> Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Chưa tải được thiết lập của phòng này.")
                Button(onClick = { onIntent(HostLobbyIntent.RetryLoadSpec) }) { Text("Thử lại") }
            }

            else -> {
                GameModeConfigEditor(
                    mode = mode,
                    form = form,
                    invalidKeys = uiState.invalidConfigKeys,
                    enabled = uiState.isConfigFormEnabled,
                    onToggle = { key, checked ->
                        onIntent(HostLobbyIntent.ToggleChanged(key, checked))
                    },
                    onNumberChange = { key, value ->
                        onIntent(HostLobbyIntent.NumberChanged(key, value))
                    },
                    onChoiceChange = { key, value ->
                        onIntent(HostLobbyIntent.ChoiceChanged(key, value))
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onIntent(HostLobbyIntent.SaveConfig) },
                        enabled = uiState.isConfigFormEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isSavingConfig) {
                            CircularProgressIndicator(Modifier.height(20.dp))
                        } else {
                            Text("Lưu")
                        }
                    }
                    OutlinedButton(
                        onClick = { onIntent(HostLobbyIntent.DismissConfigSheet) },
                        enabled = !uiState.isSavingConfig,
                        modifier = Modifier.weight(1f)
                    ) { Text("Đóng") }
                }
            }
        }
    }
}

@Composable
private fun StartGameButton(
    uiState: HostLobbyUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onStart,
        enabled = uiState.canStartGame,
        modifier = modifier.fillMaxWidth()
    ) {
        if (uiState.isStarting) {
            CircularProgressIndicator(Modifier.height(20.dp))
        } else {
            Text(if (uiState.canEditConfig) "Bắt đầu" else "Trận đã bắt đầu")
        }
    }
}

/**
 * Thẻ mã phòng với hai cách chia sẻ.
 *
 * Hai nút riêng vì hai ngữ cảnh khác nhau: mã trần để đọc cho người ngồi cạnh
 * nhập vào app, còn link để dán vào chat cho người mở bằng web.
 */
@Composable
private fun SessionCodeCard(
    sessionCode: String,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mã phòng",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = sessionCode,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Chia sẻ mã này để người khác vào phòng",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopyCode) { Text("Copy mã") }
                OutlinedButton(onClick = onCopyLink) { Text("Copy link") }
            }
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
        ConnectionStatus.CONNECTED -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đã kết nối",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ConnectionStatus.CONNECTING -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.height(16.dp))
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
            // Sau khi socket.io cạn số lần tự retry thì chỉ còn cách này để vào lại.
            TextButton(onClick = onRetry) { Text("Kết nối lại") }
        }
    }
}

@Composable
private fun PlayerRow(player: LobbyPlayer, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = player.playerName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (player.status == STATUS_DISCONNECTED) "Mất kết nối" else "Sẵn sàng",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LoadingPlayers(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyPlayers(modifier: Modifier = Modifier) {
    Text(
        text = "Chưa có ai vào phòng. Hãy chia sẻ mã phòng ở trên.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth()
    )
}

/** Giá trị status do backend gửi (chuỗi, không phải enum — xem LobbyPlayerDto). */
private const val STATUS_DISCONNECTED = "disconnected"

@Preview(showBackground = true)
@Composable
private fun HostLobbyScreenContentPreview() {
    MyQuizAppTheme {
        HostLobbyScreenContent(
            uiState = HostLobbyUiState(
                sessionCode = "482913",
                sessionStatus = SessionStatus.LOBBY,
                connection = ConnectionStatus.CONNECTED,
                mode = GameMode.CLASSIC,
                players = listOf(
                    LobbyPlayer(id = 1, playerName = "Kiro", playerScore = 0, status = "connected"),
                    LobbyPlayer(id = 2, playerName = "Lan", playerScore = 0, status = "disconnected")
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HostLobbyScreenContentConnectingPreview() {
    MyQuizAppTheme {
        HostLobbyScreenContent(uiState = HostLobbyUiState(sessionCode = "482913"), onIntent = {})
    }
}
