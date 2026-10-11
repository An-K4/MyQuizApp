package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.AppConfirmDialog
import android.kma.myquizzapp.core.ui.components.AppConfirmDialogContent
import android.kma.myquizzapp.core.ui.components.AppDialogVariant
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Quiz-specific copy only. Shared window/layout/actions live in core:ui; mutation state stays in VM. */
@Composable
fun ConfirmDeleteQuizDialog(
    quizName: String,
    deleteError: String?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppConfirmDialog(
        title = "Xóa quiz này?",
        message = deleteQuizWarning(quizName),
        confirmText = "Xóa vĩnh viễn",
        dismissText = "Giữ lại",
        variant = AppDialogVariant.Danger,
        isLoading = isDeleting,
        loadingText = "Đang xóa...",
        errorText = deleteError,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

private fun deleteQuizWarning(quizName: String): String =
    "\"$quizName\" và toàn bộ câu hỏi sẽ bị xóa vĩnh viễn, kèm kết quả của mọi phòng đã chơi quiz này. Không thể hoàn tác."

/** Preview plain content rather than a separate Dialog window. */
@Composable
private fun DeleteQuizPreviewContent(errorText: String? = null) {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppConfirmDialogContent(
                title = "Xóa quiz này?", message = deleteQuizWarning("Khám phá Văn hóa Thế giới"),
                confirmText = "Xóa vĩnh viễn", dismissText = "Giữ lại", variant = AppDialogVariant.Danger,
                errorText = errorText, scrollState = rememberScrollState(),
                onConfirm = {}, onDismiss = {}, modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview(name = "Delete Quiz Light", showBackground = true, widthDp = 390)
@Preview(name = "Delete Quiz Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConfirmDeleteQuizDialogPreview() = DeleteQuizPreviewContent()

@Preview(name = "Delete Quiz Error", showBackground = true, widthDp = 390)
@Composable
private fun ConfirmDeleteQuizDialogErrorPreview() = DeleteQuizPreviewContent("Không có kết nối mạng. Vui lòng thử lại.")
