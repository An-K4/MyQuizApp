package android.kma.myquizzapp.feature.lobby.presentation.joinroom

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.padding
import android.kma.myquizzapp.core.ui.components.RoomCodeEntryCard
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Thế nhập mã phòng, nhúng vào Trang chủ (N19.6).
 *
 * Trước N19.6 đây là một MÀN riêng (`JoinRoomScreen` + `Route.JoinRoom` + một
 * tab ở bottom nav). Bỏ màn đó vì nó chỉ chứa đúng một ô nhập rồi điều hướng
 * đi ngay: không đủ nội dung để làm một điểm đến, lại chiếm ô giữa thanh nav
 * và tạo hai điểm đến cùng "quan trọng nhất" (Trang chủ và Tham gia).
 *
 * Phần logic (tra cứu → phân luồng tài khoản/khách) giữ nguyên ở
 * [JoinRoomViewModel]; chỉ có vỏ UI đổi từ Scaffold sang Card.
 *
 * Vì sao là slot do tầng navigation truyền vào Home chứ không phải Home tự gọi:
 * thế này thuộc `feature:lobby`, nếu `feature:home` gọi trực tiếp thì hai
 * feature phải phụ thuộc nhau chỉ vì một ô nhập.
 *
 * Lỗi hiện INLINE trong thế, không dùng snackbar nữa: một Card không có
 * SnackbarHost của riêng nó, và đi xin host của Home thì lại buộc Home phải
 * biết về luồng join.
 *
 * @param exitMessage lý do bị bật ra khỏi phòng chờ (tầng navigation truyền ngược
 *   về sau khi pop). Trước đây màn Join nhận, giờ Trang chủ là màn đứng sau
 *   lobby nên nó nhận.
 * @param onExitMessageShown báo lại để xóa message, tránh hiện lại khi xoay màn.
 */
@Composable
fun JoinRoomCard(
    onNavigateToPlayerLobby: (gameId: Long, playerId: Long, socketToken: String) -> Unit,
    onNavigateToGuestNickname: (sessionCode: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    exitMessage: String? = null,
    onExitMessageShown: () -> Unit = {},
    viewModel: JoinRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Business text stays in ViewModel; only selection/focus are local visual state.
    var codeSelection by remember { mutableStateOf(TextRange.Zero) }
    var codeFocused by remember { mutableStateOf(false) }
    val codeValue = TextFieldValue(
        uiState.sessionCode,
        TextRange(codeSelection.start.coerceIn(0, uiState.sessionCode.length), codeSelection.end.coerceIn(0, uiState.sessionCode.length)),
    )

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

    JoinRoomCardContent(
        uiState = uiState,
        codeValue = codeValue,
        onCodeValueChange = {
            codeSelection = it.selection
            viewModel.onIntent(JoinRoomIntent.CodeChanged(it.text))
        },
        codeFocused = codeFocused,
        onCodeFocusChange = { codeFocused = it },
        onIntent = viewModel::onIntent,
        modifier = modifier,
        exitMessage = exitMessage,
        onExitMessageShown = onExitMessageShown
    )
}

@Composable
fun JoinRoomCardContent(
    uiState: JoinRoomUiState,
    codeValue: TextFieldValue,
    onCodeValueChange: (TextFieldValue) -> Unit,
    codeFocused: Boolean,
    onCodeFocusChange: (Boolean) -> Unit,
    onIntent: (JoinRoomIntent) -> Unit,
    modifier: Modifier = Modifier,
    exitMessage: String? = null,
    onExitMessageShown: () -> Unit = {},
) {
    RoomCodeEntryCard(
        codeValue = codeValue, onCodeValueChange = onCodeValueChange,
        codeFocused = codeFocused, onCodeFocusChange = onCodeFocusChange,
        onSubmit = { onIntent(JoinRoomIntent.Submit) }, canSubmit = uiState.canSubmit,
        modifier = modifier, isSubmitting = uiState.isSubmitting,
        codeError = uiState.codeError, errorMessage = uiState.errorMessage,
        onErrorDismiss = { onIntent(JoinRoomIntent.ErrorShown) },
        exitMessage = exitMessage, onExitMessageDismiss = onExitMessageShown,
    )
    if (uiState.guestBlocked) {
        GuestBlockedDialog(
            onDismiss = { onIntent(JoinRoomIntent.GuestBlockedDismissed) },
            onLogin = { onIntent(JoinRoomIntent.GuestBlockedLoginClicked) },
        )
    }
}

/**
 * Phòng tắt chế độ cho khách.
 *
 * Không phải báo lỗi suông mà đưa luôn lối đi tiếp (đăng nhập) — đây là trường
 * hợp người dùng hoàn toàn có thể tự xử lý được.
 *
 * Không dùng chung `AuthRequiredDialog` ở `core:ui`: hộp thoại đó nói "việc này
 * cần tài khoản", còn ở đây vấn đề là CẤU HÌNH CỦA PHÒNG — phòng khác vẫn
 * vào được bình thường, nói sai sẽ khiến người dùng tưởng cả app cần đăng nhập.
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

@Preview(name = "Join Room Light", showBackground = true, widthDp = 390)
@Preview(name = "Join Room Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinRoomCardPreview() = JoinRoomPreview(JoinRoomUiState(sessionCode = "AB23CD"))

@Preview(name = "Join Room Error", showBackground = true, widthDp = 390)
@Composable
private fun JoinRoomCardErrorPreview() = JoinRoomPreview(JoinRoomUiState(sessionCode = "AB23CD", codeError = "Không tìm thấy phòng"))

@Preview(name = "Join Room Guest Blocked", showBackground = true, widthDp = 390)
@Composable
private fun JoinRoomGuestBlockedPreview() = JoinRoomPreview(JoinRoomUiState(guestBlocked = true))

@Composable
private fun JoinRoomPreview(state: JoinRoomUiState) {
    MyQuizAppTheme {
        JoinRoomCardContent(
            state, TextFieldValue(state.sessionCode, TextRange(state.sessionCode.length)), {}, false, {}, {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
