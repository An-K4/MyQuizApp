package android.kma.myquizzapp.feature.lobby.presentation.joinroom

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
        onIntent = viewModel::onIntent,
        modifier = modifier,
        exitMessage = exitMessage,
        onExitMessageShown = onExitMessageShown
    )
}

@Composable
fun JoinRoomCardContent(
    uiState: JoinRoomUiState,
    onIntent: (JoinRoomIntent) -> Unit,
    modifier: Modifier = Modifier,
    exitMessage: String? = null,
    onExitMessageShown: () -> Unit = {}
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nhập mã phòng gồm 6 ký tự được chủ phòng chia sẻ để vào trận.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Lý do vừa bị rời phòng — đặt ngay trên ô nhập vì việc tiếp theo của
            // người dùng thường là nhập lại mã đó.
            exitMessage?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onExitMessageShown) { Text("Đã hiểu") }
                }
            }

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

            // Lỗi không thuộc ô nhập (mất mạng, lỗi server, chưa xác định được phiên).
            uiState.errorMessage?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onIntent(JoinRoomIntent.ErrorShown) }) { Text("Đóng") }
                }
            }

            Button(
                onClick = { onIntent(JoinRoomIntent.Submit) },
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinRoomCardPreview() {
    MyQuizAppTheme {
        JoinRoomCardContent(
            uiState = JoinRoomUiState(sessionCode = "4829AB"),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinRoomCardErrorPreview() {
    MyQuizAppTheme {
        JoinRoomCardContent(
            uiState = JoinRoomUiState(
                sessionCode = "000000",
                codeError = "Không tìm thấy phòng với mã này"
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinRoomCardExitMessagePreview() {
    MyQuizAppTheme {
        JoinRoomCardContent(
            uiState = JoinRoomUiState(),
            onIntent = {},
            exitMessage = "Chủ phòng đã hủy phòng này"
        )
    }
}
