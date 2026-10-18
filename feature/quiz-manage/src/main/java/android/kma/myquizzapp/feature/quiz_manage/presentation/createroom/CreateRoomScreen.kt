package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.gameconfig.GameModeConfigEditor
import android.kma.myquizzapp.core.ui.gameconfig.RoomConfigTextField
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateRoomScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHostLobby: (gameId: Long, socketToken: String, sessionCode: String) -> Unit,
    onRequireAuthentication: () -> Unit,
    onResourceMissing: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateRoomEffect.NavigateToHostLobby ->
                    onNavigateToHostLobby(effect.gameId, effect.socketToken, effect.sessionCode)
                CreateRoomEffect.RequireAuthentication -> onRequireAuthentication()
                is CreateRoomEffect.ResourceMissing -> onResourceMissing(effect.message)
            }
        }
    }

    CreateRoomScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreenContent(
    uiState: CreateRoomUiState,
    onNavigateBack: () -> Unit,
    onIntent: (CreateRoomIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasForm = !uiState.isLoadingModes && uiState.modes.isNotEmpty()
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = RoomColors.surface,
        contentColor = RoomColors.foreground,
        topBar = {
            TopAppBar(
                title = { Text("Tạo phòng chơi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RoomColors.surface,
                    titleContentColor = RoomColors.foreground,
                    navigationIconContentColor = RoomColors.foreground,
                ),
            )
        },
        bottomBar = {
            if (hasForm) CreateRoomActions(uiState, onIntent)
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        when {
            uiState.isLoadingModes -> LoadingContent(contentModifier)
            uiState.modes.isEmpty() -> EmptyModesContent(
                message = uiState.errorMessage,
                onRetry = { onIntent(CreateRoomIntent.RetryLoadModes) },
                modifier = contentModifier,
            )
            else -> CreateRoomForm(
                state = uiState,
                onIntent = onIntent,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun CreateRoomForm(
    state: CreateRoomUiState,
    onIntent: (CreateRoomIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val editable = !state.isSubmitting && !state.isWaitingForHostToken
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "name") {
            RoomConfigTextField(
                title = "Tên phòng",
                value = state.sessionName,
                onValueChange = { onIntent(CreateRoomIntent.SessionNameChanged(it)) },
                enabled = editable,
                supportingText = "Từ 2 đến 100 ký tự",
                leadingIcon = { Icon(Icons.Outlined.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp)) },
            )
        }
        item(key = "modes") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chọn chế độ chơi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.modes, key = { it.mode.name }) { descriptor ->
                        val selected = state.selectedMode == descriptor.mode
                        FilterChip(
                            selected = selected,
                            enabled = editable,
                            onClick = { onIntent(CreateRoomIntent.ModeSelected(descriptor.mode)) },
                            label = { Text(modeLabel(descriptor.mode), fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) Primary else RoomColors.border),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = RoomColors.surface,
                                labelColor = RoomColors.foreground,
                                selectedContainerColor = RoomColors.selectedSurface,
                                selectedLabelColor = Primary,
                            ),
                        )
                    }
                }
                state.selectedDescriptor?.let { descriptor ->
                    Text(
                        if (descriptor.pacing == Pacing.HOST) "Host điều khiển nhịp câu hỏi"
                        else "Người chơi tự làm theo nhịp riêng",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoomColors.muted,
                    )
                }
            }
        }
        val mode = state.selectedMode
        val form = state.modeConfig
        if (mode != null && form != null) item(key = mode.name) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cấu hình nâng cao", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GameModeConfigEditor(
                    mode = mode,
                    form = form,
                    invalidKeys = state.invalidConfigKeys,
                    enabled = editable,
                    onToggle = { key, checked -> onIntent(CreateRoomIntent.ToggleChanged(key, checked)) },
                    onNumberChange = { key, value -> onIntent(CreateRoomIntent.NumberChanged(key, value)) },
                    onChoiceChange = { key, value -> onIntent(CreateRoomIntent.ChoiceChanged(key, value)) },
                )
            }
        }
        if (state.ignoredFields.isNotEmpty()) item(key = "ignored") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = RoomColors.selectedSurface,
            ) {
                Text(
                    "Máy chủ đã bỏ qua ${state.ignoredFields.size} thiết lập không hợp lệ.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

    }
}

@Composable
private fun CreateRoomActions(state: CreateRoomUiState, onIntent: (CreateRoomIntent) -> Unit) {
    Surface(color = RoomColors.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = RoomColors.border)
            // The action is fixed: feedback must stay visible without scrolling to the form end.
            if (state.validationErrors.isNotEmpty() || state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.validationErrors.forEach {
                        Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            QuizPrimaryButton(
                text = if (state.isWaitingForHostToken) "Thử vào phòng" else "Tạo phòng",
                onClick = {
                    onIntent(if (state.isWaitingForHostToken) CreateRoomIntent.RetryHostToken else CreateRoomIntent.Submit)
                },
                enabled = !state.isSubmitting && state.modeConfig != null,
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyModesContent(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message ?: "Không tải được chế độ chơi", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        QuizPrimaryButton(text = "Thử lại", onClick = onRetry)
    }
}

private fun modeLabel(mode: GameMode): String = when (mode) {
    GameMode.CLASSIC -> "Cổ điển"
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
}

/** Use the approved light tokens, with provisional Material roles for dark. */
private object RoomColors {
    private val dark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val foreground: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    val selectedSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
}

@Preview(name = "Create room Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Create room Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CreateRoomScreenContentPreview(
    @PreviewParameter(CreateRoomPreviewProvider::class) state: CreateRoomUiState,
) {
    MyQuizAppTheme {
        CreateRoomScreenContent(uiState = state, onNavigateBack = {}, onIntent = {})
    }
}

@Preview(name = "Create room Small", showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
private fun CreateRoomSmallPreview() {
    MyQuizAppTheme {
        CreateRoomScreenContent(uiState = createRoomPreviewState(GameMode.CLASSIC), onNavigateBack = {}, onIntent = {})
    }
}
