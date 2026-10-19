package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import android.kma.myquizzapp.core.ui.components.AppConfirmDialogContent
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.gameconfig.BooleanSettingUiState
import android.kma.myquizzapp.core.ui.gameconfig.ChoiceSettingUiState
import android.kma.myquizzapp.core.ui.gameconfig.GameModeConfigEditor
import android.kma.myquizzapp.core.ui.gameconfig.NumberSettingUiState
import android.kma.myquizzapp.core.ui.gameconfig.RoomConfigForm
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Stateful boundary: navigation, platform actions, effects, dialogs and scroll/sheet state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreen(
    onExit: (message: String?) -> Unit,
    onNavigateToHostGame: (gameId: Long, socketToken: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostLobbyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val configScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showEmptyRoomWarning by rememberSaveable { mutableStateOf(false) }
    var showLeaveConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HostLobbyEffect.ExitLobby -> onExit(effect.message)
                is HostLobbyEffect.NavigateToHostGame ->
                    onNavigateToHostGame(effect.gameId, effect.socketToken)
            }
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onIntent(HostLobbyIntent.ErrorShown)
    }
    LaunchedEffect(uiState.configNotice) {
        val message = uiState.configNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onIntent(HostLobbyIntent.ConfigNoticeShown)
    }
    LaunchedEffect(uiState.isConfigSheetOpen) {
        if (uiState.isConfigSheetOpen) configScrollState.scrollTo(0)
    }
    LaunchedEffect(uiState.sessionStatus, uiState.isStarting) {
        if (uiState.isStarting || uiState.sessionStatus != SessionStatus.LOBBY) {
            showEmptyRoomWarning = false
        }
    }

    // Let the modal sheet handle Back while it is open; otherwise match the toolbar back action.
    BackHandler(enabled = !uiState.isConfigSheetOpen) { showLeaveConfirmation = true }

    HostLobbyScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        gridState = gridState,
        onBack = { showLeaveConfirmation = true },
        onStart = {
            if (uiState.canStartGame) {
                if (uiState.players.isEmpty()) showEmptyRoomWarning = true
                else viewModel.onIntent(HostLobbyIntent.StartGame)
            }
        },
        onCopyCode = {
            clipboard.setText(AnnotatedString(uiState.sessionCode))
            scope.launch { snackbarHostState.showSnackbar("Đã sao chép mã phòng.") }
        },
        onCopyLink = {
            clipboard.setText(AnnotatedString(uiState.shareLink))
            scope.launch { snackbarHostState.showSnackbar("Đã sao chép link tham gia phòng.") }
        },
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )

    if (uiState.isConfigSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(HostLobbyIntent.DismissConfigSheet) },
            sheetState = sheetState,
            containerColor = HostLobbyColors.surface,
            contentColor = HostLobbyColors.foreground,
        ) {
            ConfigSheetContent(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                scrollState = configScrollState,
                maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f,
            )
        }
    }
    if (showEmptyRoomWarning) {
        AppConfirmDialog(
            title = EMPTY_ROOM_TITLE,
            message = EMPTY_ROOM_MESSAGE,
            confirmText = "Vẫn bắt đầu",
            dismissText = "Chờ thêm",
            onConfirm = {
                showEmptyRoomWarning = false
                viewModel.onIntent(HostLobbyIntent.StartGame)
            },
            onDismiss = { showEmptyRoomWarning = false },
        )
    }
    if (showLeaveConfirmation) {
        AppConfirmDialog(
            title = LEAVE_ROOM_TITLE,
            message = LEAVE_ROOM_MESSAGE,
            confirmText = "Rời phòng",
            dismissText = "Ở lại",
            onConfirm = {
                showLeaveConfirmation = false
                viewModel.onIntent(HostLobbyIntent.LeaveRoom)
            },
            onDismiss = { showLeaveConfirmation = false },
        )
    }
}

/** Pure UI: a single scrolling grid includes the header; the start action stays in the footer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostLobbyScreenContent(
    uiState: HostLobbyUiState,
    onIntent: (HostLobbyIntent) -> Unit,
    gridState: LazyGridState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HostLobbyColors.surface,
        contentColor = HostLobbyColors.foreground,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = {
                    Text("Phòng chờ Host", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Rời phòng chờ")
                    }
                },
                actions = {
                    ConnectionBadge(uiState.connection, Modifier.padding(end = 20.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HostLobbyColors.surface,
                    titleContentColor = HostLobbyColors.foreground,
                    navigationIconContentColor = HostLobbyColors.foreground,
                ),
            )
        },
        bottomBar = { HostLobbyActions(uiState, onStart) },
    ) { innerPadding ->
        // Increase the minimum cell width with font scale instead of squeezing names/badges.
        val minCellWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCellWidth),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "room-header", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SessionCodeCard(uiState.sessionCode, onCopyCode, onCopyLink)
                    if (uiState.connection != ConnectionStatus.CONNECTED) {
                        ConnectionNotice(uiState.connection, onRetry = { onIntent(HostLobbyIntent.Retry) })
                    }
                    QuizSecondaryButton(
                        text = if (uiState.sessionStatus == null || uiState.canEditConfig) "Sửa cấu hình phòng"
                            else "Không thể sửa cấu hình",
                        onClick = { onIntent(HostLobbyIntent.OpenConfigSheet) },
                        enabled = uiState.canEditConfig && !uiState.isStarting,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Outlined.Settings, null, Modifier.size(18.dp)) },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (uiState.hasLobbySnapshot) "Người chơi (${uiState.playerCount})" else "Người chơi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when {
                                !uiState.hasLobbySnapshot -> "Đang tải danh sách người chơi..."
                                uiState.connection != ConnectionStatus.CONNECTED ->
                                    "${uiState.connectedPlayerCount} đang kết nối ở lần đồng bộ gần nhất"
                                else -> "${uiState.connectedPlayerCount} đang kết nối"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = HostLobbyColors.muted,
                        )
                    }
                }
            }
            when {
                !uiState.hasLobbySnapshot -> item(key = "players-loading", span = { GridItemSpan(maxLineSpan) }) {
                    LoadingPlayers()
                }
                uiState.players.isEmpty() -> item(key = "players-empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyPlayers()
                }
                else -> items(uiState.players, key = { "player-${it.id}" }) { player ->
                    HostLobbyPlayerCard(player)
                }
            }
        }
    }
}

@Composable
private fun SessionCodeCard(
    sessionCode: String,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(FrontendColors.ResultGradientMiddle, Primary)))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "MÃ PHÒNG", color = FrontendColors.PageBackground.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                sessionCode, color = FrontendColors.PageBackground,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 5.sp),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                RoomCopyButton("Copy mã", onCopyCode, isLink = false, modifier = Modifier.weight(1f))
                RoomCopyButton("Copy link", onCopyLink, isLink = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomCopyButton(text: String, onClick: () -> Unit, isLink: Boolean, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FrontendColors.PageBackground.copy(alpha = 0.16f),
            contentColor = FrontendColors.PageBackground,
        ),
        elevation = null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Icon(if (isLink) Icons.Outlined.Link else Icons.Outlined.ContentCopy, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HostLobbyActions(state: HostLobbyUiState, onStart: () -> Unit) {
    Surface(color = HostLobbyColors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = HostLobbyColors.border)
            QuizPrimaryButton(
                text = when {
                    state.isStarting -> "Đang bắt đầu..."
                    state.hasLobbySnapshot && !state.canEditConfig -> "Trận đã bắt đầu"
                    else -> "Bắt đầu trò chơi"
                },
                onClick = onStart,
                enabled = state.canStartGame,
                loading = state.isStarting,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                trailingIcon = {
                    if (!state.isStarting) Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                },
            )
        }
    }
}

@Composable
private fun ConnectionBadge(status: ConnectionStatus, modifier: Modifier = Modifier) {
    LobbyStatusBadge(
        label = when (status) {
            ConnectionStatus.CONNECTED -> "Đã kết nối"
            ConnectionStatus.CONNECTING -> "Đang kết nối"
            ConnectionStatus.RECONNECTING -> "Đang nối lại"
            ConnectionStatus.RECONNECT_FAILED -> "Mất kết nối"
        },
        tone = when (status) {
            ConnectionStatus.CONNECTED -> LobbyBadgeTone.Success
            ConnectionStatus.RECONNECT_FAILED -> LobbyBadgeTone.Danger
            else -> LobbyBadgeTone.Neutral
        },
        modifier = modifier,
    )
}

@Composable
private fun ConnectionNotice(status: ConnectionStatus, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = HostLobbyColors.softSurface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                when (status) {
                    ConnectionStatus.CONNECTING -> "Đang kết nối tới phòng..."
                    ConnectionStatus.RECONNECTING -> "Mất kết nối — đang tự động thử lại. Danh sách chưa được cập nhật."
                    ConnectionStatus.RECONNECT_FAILED -> "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại."
                    ConnectionStatus.CONNECTED -> "Đã kết nối"
                },
                style = MaterialTheme.typography.bodySmall, color = HostLobbyColors.muted,
            )
            if (status == ConnectionStatus.RECONNECT_FAILED) {
                QuizSecondaryButton("Kết nối lại", onRetry)
            }
        }
    }
}

@Composable
private fun HostLobbyPlayerCard(player: LobbyPlayer, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = HostLobbyColors.softSurface) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LobbyPlayerAvatar(player)
            Text(
                player.playerName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = HostLobbyColors.foreground,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            LobbyStatusBadge(
                label = when (player.status) {
                    "connected" -> "Đã kết nối"
                    "disconnected" -> "Mất kết nối"
                    "eliminated" -> "Đã bị loại"
                    "finished" -> "Đã hoàn thành"
                    else -> "Không rõ"
                },
                tone = if (player.status == "connected") LobbyBadgeTone.Success else LobbyBadgeTone.Neutral,
            )
        }
    }
}

@Composable
private fun LobbyPlayerAvatar(player: LobbyPlayer) {
    if (!player.playerAvatar.isNullOrBlank() && !LocalInspectionMode.current) {
        Avatar(player.playerAvatar, size = 42.dp)
    } else {
        val accents = listOf(Primary, FrontendColors.Info, FrontendColors.Warning, FrontendColors.SuccessForeground)
        val accent = accents[Math.floorMod(player.id, accents.size.toLong()).toInt()]
        Surface(Modifier.size(42.dp), shape = CircleShape, color = accent.copy(alpha = 0.08f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    player.playerName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        .takeLast(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" },
                    color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private enum class LobbyBadgeTone { Success, Neutral, Danger }

/** Shared visual language for the host connection badge and each player's status badge. */
@Composable
private fun LobbyStatusBadge(label: String, tone: LobbyBadgeTone, modifier: Modifier = Modifier) {
    val foreground = when (tone) {
        LobbyBadgeTone.Success -> HostLobbyColors.successForeground
        LobbyBadgeTone.Neutral -> HostLobbyColors.muted
        LobbyBadgeTone.Danger -> MaterialTheme.colorScheme.error
    }
    val background = when (tone) {
        LobbyBadgeTone.Success -> HostLobbyColors.successSurface
        LobbyBadgeTone.Neutral -> HostLobbyColors.neutralSurface
        LobbyBadgeTone.Danger -> MaterialTheme.colorScheme.errorContainer
    }
    Row(
        modifier = modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(foreground, CircleShape))
        Text(label, color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingPlayers() {
    Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun EmptyPlayers() {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = HostLobbyColors.softSurface) {
        Column(
            Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Chưa có người chơi", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "Chia sẻ mã phòng hoặc link ở trên để mời mọi người tham gia.",
                style = MaterialTheme.typography.bodySmall, color = HostLobbyColors.muted, textAlign = TextAlign.Center,
            )
        }
    }
}

/** Stateless body of the modal sheet; scrolling belongs to the Screen. */
@Composable
private fun ConfigSheetContent(
    uiState: HostLobbyUiState,
    onIntent: (HostLobbyIntent) -> Unit,
    scrollState: ScrollState,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val mode = uiState.mode
    val form = uiState.configForm
    Column(modifier.fillMaxWidth().heightIn(max = maxHeight).imePadding()) {
        Text(
            "Cấu hình phòng", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = HostLobbyColors.foreground,
        )
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(scrollState).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                uiState.isLoadingSpec -> Column(
                    Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Primary)
                    Text("Đang tải thiết lập của chế độ chơi...", color = HostLobbyColors.muted)
                }
                mode == null || form == null -> {
                    Text("Chưa tải được thiết lập của phòng này.", color = HostLobbyColors.muted)
                    QuizSecondaryButton("Thử lại", onClick = { onIntent(HostLobbyIntent.RetryLoadSpec) })
                }
                else -> {
                    GameModeConfigEditor(
                        mode = mode, form = form, invalidKeys = uiState.invalidConfigKeys,
                        enabled = uiState.isConfigFormEnabled,
                        onToggle = { key, checked -> onIntent(HostLobbyIntent.ToggleChanged(key, checked)) },
                        onNumberChange = { key, value -> onIntent(HostLobbyIntent.NumberChanged(key, value)) },
                        onChoiceChange = { key, value -> onIntent(HostLobbyIntent.ChoiceChanged(key, value)) },
                    )
                    if (uiState.invalidConfigKeys.isNotEmpty()) {
                        Text(
                            "Kiểm tra các thiết lập được đánh dấu.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = HostLobbyColors.border)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuizSecondaryButton(
                "Đóng", onClick = { onIntent(HostLobbyIntent.DismissConfigSheet) },
                enabled = !uiState.isSavingConfig, modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            QuizPrimaryButton(
                "Lưu", onClick = { onIntent(HostLobbyIntent.SaveConfig) },
                enabled = mode != null && form != null && uiState.isConfigFormEnabled,
                loading = uiState.isSavingConfig, modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

private const val EMPTY_ROOM_TITLE = "Phòng chưa có ai"
private const val EMPTY_ROOM_MESSAGE = "Chưa có người chơi nào vào phòng. Bắt đầu bây giờ thì những người vào sau chỉ tham gia được nếu phòng bật cho vào giữa trận."
private const val LEAVE_ROOM_TITLE = "Rời phòng chờ?"
private const val LEAVE_ROOM_MESSAGE = "Bạn sẽ ngắt kết nối và rời màn phòng chờ. Thao tác này không hủy phòng chơi."

/** Approved light tokens; dark roles remain provisional, as on the other polished screens. */
private object HostLobbyColors {
    private val dark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val softSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val foreground: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    val neutralSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.Border.copy(alpha = 0.55f)
    val successSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.CorrectAnswerBackground
    val successForeground: Color
        @Composable get() = if (dark) FrontendColors.Success else FrontendColors.SuccessForeground
}

// Six fixed previews only: do not multiply combinations through parameter providers.
// Local samples have no ViewModel, socket, clipboard or production effects.
private fun hostLobbyPreviewState(): HostLobbyUiState {
    val names = listOf("Minh Anh", "Hoàng Long", "Thảo Vy", "Quang Huy", "Tuấn Kiệt", "Quốc Tuấn", "Mai Lan", "Đức Anh", "Ngọc Hà")
    return HostLobbyUiState(
        sessionCode = "582910", sessionStatus = SessionStatus.LOBBY,
        connection = ConnectionStatus.CONNECTED, mode = GameMode.CLASSIC,
        players = List(18) { index ->
            LobbyPlayer(
                id = index.toLong() + 1, playerName = names[index % names.size], playerScore = 0,
                status = if (index == 2 || index == 7) "disconnected" else "connected",
            )
        },
    )
}

@Composable
private fun HostLobbyPreviewContent(state: HostLobbyUiState) {
    HostLobbyScreenContent(
        uiState = state, onIntent = {}, gridState = rememberLazyGridState(),
        onBack = {}, onStart = {}, onCopyCode = {}, onCopyLink = {},
    )
}

@Preview(name = "Host lobby Main", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostLobbyScreenContentPreview() {
    MyQuizAppTheme { HostLobbyPreviewContent(hostLobbyPreviewState()) }
}

@Preview(name = "Host lobby Small / large text", showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
private fun HostLobbySmallPreview() {
    MyQuizAppTheme { HostLobbyPreviewContent(hostLobbyPreviewState()) }
}

@Preview(name = "Host lobby Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostLobbyEmptyPreview() {
    MyQuizAppTheme { HostLobbyPreviewContent(hostLobbyPreviewState().copy(players = emptyList())) }
}

@Preview(name = "Host lobby Connection failed", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostLobbyReconnectPreview() {
    MyQuizAppTheme {
        HostLobbyPreviewContent(hostLobbyPreviewState().copy(connection = ConnectionStatus.RECONNECT_FAILED))
    }
}

/** Illustrative editor values, not authoritative mode constraints. */
private fun hostLobbyPreviewForm(): RoomConfigForm {
    val toggle = BooleanSettingUiState(false, true)
    return RoomConfigForm(
        perQuestionSeconds = NumberSettingUiState("20", 0, 600, true, null, true),
        autoAdvance = toggle, totalMatchSeconds = NumberSettingUiState("600", 1, 3600, true, null, true),
        maxPlayers = NumberSettingUiState("100", 1, 200, false, null, true),
        allowLateJoin = toggle, allowGuests = toggle.copy(value = true),
        showCorrectAnswer = toggle.copy(value = true),
        showLeaderboard = ChoiceSettingUiState("end_only", listOf("never", "end_only"), true),
        lives = NumberSettingUiState("3", 1, 10, true, null, true),
        allowAnswerLate = toggle, shuffleQuestions = toggle, shuffleOptions = toggle,
        showHint = toggle, reviewMode = toggle.copy(value = true), speedBonus = toggle.copy(value = true),
        negativeMarking = toggle,
    )
}

@Preview(name = "Host lobby Config sheet", showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun HostLobbyConfigSheetPreview() {
    MyQuizAppTheme {
        Surface(color = HostLobbyColors.surface) {
            ConfigSheetContent(
                hostLobbyPreviewState().copy(configForm = hostLobbyPreviewForm()),
                {}, rememberScrollState(), maxHeight = 620.dp,
            )
        }
    }
}

@Preview(name = "Host lobby Leave confirmation", showBackground = true, widthDp = 390)
@Composable
private fun HostLobbyLeaveDialogPreview() {
    MyQuizAppTheme {
        AppConfirmDialogContent(
            title = LEAVE_ROOM_TITLE, message = LEAVE_ROOM_MESSAGE,
            confirmText = "Rời phòng", dismissText = "Ở lại", onConfirm = {}, onDismiss = {},
            scrollState = rememberScrollState(), modifier = Modifier.padding(20.dp),
        )
    }
}
