package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Hộp thoại "cần đăng nhập" dùng chung cho mọi chỗ gác đăng nhập (N19.6).
 *
 * Vì sao ở `core:ui` và vì sao KHÔNG biết gì về phiên đăng nhập: component này
 * chỉ nhận `message` + 2 callback, không đọc [SessionState] và không tự quyết
 * định khi nào hiện. Nếu nó tự đi đọc trạng thái thì `core:ui` sẽ phải phụ
 * thuộc vào tầng dự liệu, và mỗi nơi gọi lại có thêm một nguồn sự thật — đúng
 * cái bệnh N19.6 đang đi sửa ở lượt 1.
 *
 * Quyết định "có hiện hay không" nằm ở chỗ gác (AppNavGraph), xem `requireAuth`.
 *
 * @param message việc người dùng vừa định làm, diễn đạt theo ngợ cảnh — hộp thoại
 *   chung nhưng lời nhắn phải riêng, "Cần đăng nhập" trơn không cho người dùng
 *   biết họ vừa bị chặn ở đâu.
 */
@Composable
fun AuthRequiredDialog(
    message: String,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    title: String = "Cần đăng nhập",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onSignIn) { Text("Đăng ký/Đăng nhập") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Để sau") }
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthRequiredDialogPreview() {
    MyQuizAppTheme {
        AuthRequiredDialog(
            message = "Đăng nhập để xem và quản lý thư viện quiz của bạn.",
            onDismiss = {},
            onSignIn = {},
        )
    }
}
