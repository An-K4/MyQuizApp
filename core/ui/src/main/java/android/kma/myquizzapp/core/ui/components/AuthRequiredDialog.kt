package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shared sign-in warning. AppNavGraph owns the auth gate and navigation;
 * this recipe only supplies copy/callbacks to the normal app confirmation dialog.
 * It does not read session state or decide when sign-in is required.
 */
@Composable
fun AuthRequiredDialog(
    message: String,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    title: String = "Cần đăng nhập",
) {
    AppConfirmDialog(
        title = title,
        message = message,
        confirmText = "Đăng ký/Đăng nhập",
        dismissText = "Để sau",
        onConfirm = onSignIn,
        onDismiss = onDismiss,
    )
}

@Preview(name = "Auth Required Light", showBackground = true, widthDp = 390)
@Preview(name = "Auth Required Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthRequiredDialogPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppConfirmDialogContent(
                title = "Cần đăng nhập",
                message = "Đăng nhập để xem và quản lý thư viện quiz của bạn.",
                confirmText = "Đăng ký/Đăng nhập", dismissText = "Để sau",
                scrollState = rememberScrollState(), onConfirm = {}, onDismiss = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
