package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateRoomScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHostLobby: (gameId: Long, socketToken: String, sessionCode: String) -> Unit,
    onRequireAuthentication: () -> Unit,
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tạo phòng chơi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoadingModes -> LoadingContent(Modifier.padding(innerPadding))
            uiState.modes.isEmpty() -> EmptyModesContent(
                message = uiState.errorMessage,
                onRetry = { onIntent(CreateRoomIntent.RetryLoadModes) },
                modifier = Modifier.padding(innerPadding)
            )
            else -> CreateRoomForm(
                state = uiState,
                onIntent = onIntent,
                modifier = Modifier.padding(innerPadding)
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.sessionName,
                onValueChange = { onIntent(CreateRoomIntent.SessionNameChanged(it)) },
                enabled = !state.isWaitingForHostToken,
                label = { Text("Tên phòng") },
                supportingText = { Text("Từ 2 đến 100 ký tự") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text("Chế độ chơi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.modes, key = { it.mode.name }) { descriptor ->
                    FilterChip(
                        selected = state.selectedMode == descriptor.mode,
                        enabled = !state.isWaitingForHostToken,
                        onClick = { onIntent(CreateRoomIntent.ModeSelected(descriptor.mode)) },
                        label = { Text(modeLabel(descriptor.mode)) }
                    )
                }
            }
            state.selectedDescriptor?.let { descriptor ->
                Text(
                    if (descriptor.pacing == Pacing.HOST) "Host điều khiển nhịp câu hỏi"
                    else "Người chơi tự làm theo nhịp riêng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val mode = state.selectedMode
        val form = state.modeConfig
        if (mode != null && form != null) item(key = mode.name) {
            GameModeConfigEditor(
                mode = mode,
                form = form,
                invalidKeys = state.invalidConfigKeys,
                enabled = !state.isWaitingForHostToken && !state.isSubmitting,
                onIntent = onIntent
            )
        }
        if (state.validationErrors.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.validationErrors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (state.ignoredFields.isNotEmpty()) item {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Máy chủ đã bỏ qua ${state.ignoredFields.size} thiết lập không hợp lệ.",
                    Modifier.padding(12.dp)
                )
            }
        }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = {
                    onIntent(if (state.isWaitingForHostToken) CreateRoomIntent.RetryHostToken else CreateRoomIntent.Submit)
                },
                enabled = !state.isSubmitting && state.modeConfig != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.height(20.dp))
                else Text(if (state.isWaitingForHostToken) "Thử vào phòng" else "Tạo phòng")
            }
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
        modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message ?: "Không tải được chế độ chơi")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
    }
}

private fun modeLabel(mode: GameMode): String = when (mode) {
    GameMode.CLASSIC -> "Cổ điển"
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CreateRoomScreenContentPreview() {
    MyQuizAppTheme {
        CreateRoomScreenContent(
            uiState = CreateRoomUiState(),
            onNavigateBack = {},
            onIntent = {}
        )
    }
}
