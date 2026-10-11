package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Visual treatment only. The caller decides permissions, visibility and business operations. */
enum class AppDialogVariant { Normal, Danger }

/** Window/scroll convenience wrapper; AppConfirmDialogContent is the stateless, previewable API. */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Hủy",
    variant: AppDialogVariant = AppDialogVariant.Normal,
    isLoading: Boolean = false,
    loadingText: String = "Đang xử lý...",
    errorText: String? = null,
) {
    val scrollState = rememberScrollState()
    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp - 48.dp).coerceAtLeast(160.dp)
    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
        ),
    ) {
        AppConfirmDialogContent(
            title = title, message = message, confirmText = confirmText,
            onConfirm = onConfirm, onDismiss = onDismiss, scrollState = scrollState,
            dismissText = dismissText, variant = variant, isLoading = isLoading,
            loadingText = loadingText, errorText = errorText, maxHeight = maxHeight,
        )
    }
}

/** Plain content, not a popup: previews can inspect normal, danger, loading and error states. */
@Composable
fun AppConfirmDialogContent(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    dismissText: String = "Hủy",
    variant: AppDialogVariant = AppDialogVariant.Normal,
    isLoading: Boolean = false,
    loadingText: String = "Đang xử lý...",
    errorText: String? = null,
    maxHeight: Dp = 560.dp,
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight),
        shape = RoundedCornerShape(24.dp),
        color = ComponentColors.surface,
    ) {
        Column(Modifier.padding(20.dp)) {
            // Long warnings/errors scroll; the action row stays reachable.
            Column(Modifier.weight(1f, fill = false).verticalScroll(scrollState)) {
                Text(
                    title, modifier = Modifier.semantics { heading() },
                    color = ComponentColors.foreground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(12.dp))
                Text(message, color = ComponentColors.muted, style = MaterialTheme.typography.bodyMedium)
                errorText?.takeIf { it.isNotBlank() }?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuizSecondaryButton(
                    text = dismissText,
                    onClick = { if (!isLoading) onDismiss() },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    // Existing neutral token: grey text + the shared faint secondary border.
                    contentColor = if (variant == AppDialogVariant.Danger) ComponentColors.muted else null,
                )
                when (variant) {
                    AppDialogVariant.Normal -> QuizPrimaryButton(
                        text = if (isLoading) loadingText else confirmText,
                        onClick = { if (!isLoading) onConfirm() },
                        enabled = !isLoading, loading = isLoading,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    AppDialogVariant.Danger -> QuizDangerButton(
                        text = if (isLoading) loadingText else confirmText,
                        onClick = { if (!isLoading) onConfirm() },
                        enabled = !isLoading, loading = isLoading,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDialogPreviewContent(
    variant: AppDialogVariant,
    isLoading: Boolean = false,
    errorText: String? = null,
) {
    val danger = variant == AppDialogVariant.Danger
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppConfirmDialogContent(
                title = if (danger) "Xóa quiz này?" else "Cần đăng nhập",
                message = if (danger)
                    "Quiz và toàn bộ câu hỏi sẽ bị xóa vĩnh viễn, kèm kết quả của mọi phòng đã chơi quiz này. Không thể hoàn tác."
                else "Đăng nhập để xem và quản lý thư viện quiz của bạn.",
                confirmText = if (danger) "Xóa vĩnh viễn" else "Đăng nhập",
                dismissText = if (danger) "Giữ lại" else "Để sau",
                variant = variant, isLoading = isLoading,
                loadingText = if (danger) "Đang xóa..." else "Đang xử lý...",
                errorText = errorText, scrollState = rememberScrollState(),
                onConfirm = {}, onDismiss = {}, modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview(name = "App Dialog Normal Light", showBackground = true, widthDp = 390)
@Preview(name = "App Dialog Normal Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppDialogNormalPreview() = AppDialogPreviewContent(AppDialogVariant.Normal)

@Preview(name = "App Dialog Danger Light", showBackground = true, widthDp = 390)
@Preview(name = "App Dialog Danger Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "App Dialog Danger Large Text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun AppDialogDangerPreview() = AppDialogPreviewContent(AppDialogVariant.Danger)

@Preview(name = "App Dialog Danger Loading", showBackground = true, widthDp = 390)
@Composable
private fun AppDialogLoadingPreview() = AppDialogPreviewContent(AppDialogVariant.Danger, isLoading = true)

@Preview(name = "App Dialog Danger Error", showBackground = true, widthDp = 390)
@Composable
private fun AppDialogErrorPreview() = AppDialogPreviewContent(AppDialogVariant.Danger, errorText = "Không có kết nối mạng. Vui lòng thử lại.")
