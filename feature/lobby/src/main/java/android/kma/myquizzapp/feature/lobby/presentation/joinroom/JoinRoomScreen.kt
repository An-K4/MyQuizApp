package android.kma.myquizzapp.feature.lobby.presentation.joinroom

import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Màn nhập mã phòng (stateful).
 *
 * Ba đường ra khác nhau nên tách thành ba callback thay vì một callback chung:
 * vào thẳng phòng chờ (đã đăng nhập), sang màn nhập tên (khách), hoặc sang
 * màn đăng nhập (phòng chặn khách).
 *
 * @param exitMessage lý do bị bật ra khỏi phòng chờ (do tầng navigation truyền
 *   ngược về sau khi pop). Màn này là nơi duy nhất còn sống sau khi lobby đóng
 *   nên nó chịu trách nhiệm hiển thị — trả nợ TODO N18.
 * @param onExitMessageShown báo lại để xóa message, tránh hiện lại khi xoay màn.
 */
@Composable
fun JoinRoomScreen(
    onNavigateToPlayerLobby: (gameId: Long, playerId: Long, socketToken: String) -> Unit,
    onNavigateToGuestNickname: (sessionCode: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    exitMessage: String? = null,
    onExitMessageShown: () -> Unit = {},
    viewModel: JoinRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is JoinRoomEffect.NavigateToPlayerLobby -> onNavigateToPlayerLobby(
                    effect.gameId,
                    effect.playerId,
                    effect.socketToken
                )

                is JoinRoomEffect.NavigateToGuestNickname ->
                    onNavigateToGuestNickname(effect.sessionCode)

                JoinRoomEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    JoinRoomScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
        exitMessage = exitMessage,
        onExitMessageShown = onExitMessageShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreenContent(
    uiState: JoinRoomUiState,
    onIntent: (JoinRoomIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    exitMessage: String? = null,
    onExitMessageShown: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exitMessage) {
        val message = exitMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onExitMessageShown()
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onIntent(JoinRoomIntent.ErrorShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tham gia phòng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nhập mã phòng chủ phòng chia sẻ để vào trận.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.sessionCode,
                onValueChange = { onIntent(JoinRoomIntent.CodeChanged(it)) },
                label = { Text("Mã phòng") },
                singleLine = true,
                isError = uiState.codeError != null,
                supportingText = uiState.codeError?.let { { Text(it) } },
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Go
                ),
                // Enter trên bàn phím = bấm "Vào phòng": gõ mã xong vào luôn, không
                // bắt người dùng đóng bàn phím rồi mới tìm nút.
                keyboardActions = KeyboardActions(
                    onGo = { if (uiState.canSubmit) onIntent(JoinRoomIntent.Submit) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onIntent(JoinRoomIntent.Submit) },
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(0.dp))
                } else {
                    Text("Vào phòng")
                }
            }
        }
    }

    if (uiState.guestBlocked) {
        GuestBlockedDialog(
            onDismiss = { onIntent(JoinRoomIntent.GuestBlockedDismissed) },
            onLogin = { onIntent(JoinRoomIntent.GuestBlockedLoginClicked) }
        )
    }
}

/**
 * Phòng tắt chế độ cho khách.
 *
 * Không phải báo lỗi suông mà đưa luôn lối đi tiếp (đăng nhập) — đây là trường
 * hợp người dùng hoàn toàn có thể tự xử lý được.
 */
@Composable
private fun GuestBlockedDialog(onDismiss: () -> Unit, onLogin: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Phòng không nhận khách") },
        text = {
            Text("Chủ phòng yêu cầu người chơi phải đăng nhập. Đăng nhập rồi vào lại nhé.")
        },
        confirmButton = { TextButton(onClick = onLogin) { Text("Đăng nhập") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để sau") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun JoinRoomScreenContentPreview() {
    MyQuizAppTheme {
        JoinRoomScreenContent(
            uiState = JoinRoomUiState(sessionCode = "4829AB"),
            onIntent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinRoomScreenContentErrorPreview() {
    MyQuizAppTheme {
        JoinRoomScreenContent(
            uiState = JoinRoomUiState(
                sessionCode = "000000",
                codeError = "Không tìm thấy phòng với mã này"
            ),
            onIntent = {},
            onBack = {}
        )
    }
}
