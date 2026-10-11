package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.AppConfirmDialogContent
import android.kma.myquizzapp.core.ui.components.AppDialogVariant
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.tooling.preview.Preview
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Feature-local light design tokens with Material fallbacks for the existing dark theme. */
object EditorColors {
    val isDark: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground @Composable get() = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted @Composable get() = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val soft @Composable get() = if (isDark) MaterialTheme.colorScheme.surfaceContainer else FrontendColors.SoftBackground
    val surface @Composable get() = if (isDark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val border @Composable get() = if (isDark) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border
    val tint @Composable get() = if (isDark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
}

/** Stateless image selection block: the Screen owns the platform picker. */
@Composable
fun ImagePickerSection(
    label: String,
    imageModel: Any?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCover: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (imageModel != null) {
            RemoteImage(
                imageUrl = imageModel.toString(), contentDescription = label,
                contentScale = if (isCover) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .clipEditorImage(),
            )
            Row {
                TextButton(onClick = onPick, enabled = enabled) { Text("Đổi ảnh") }
                TextButton(onClick = onRemove, enabled = enabled) { Text("Xóa ảnh", color = MaterialTheme.colorScheme.error) }
            }
        } else if (isCover) {
            val border = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            Surface(
                onClick = onPick, enabled = enabled, color = EditorColors.soft,
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Box {
                    Canvas(Modifier.matchParentSize()) {
                        val inset = 1.dp.toPx()
                        drawRoundRect(
                            color = border, topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                            style = Stroke(width = inset, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))),
                        )
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Text("Chạm để tải ảnh bìa Quiz", color = EditorColors.foreground)
                        Text("Khuyên dùng 16:9", style = MaterialTheme.typography.bodySmall, color = EditorColors.muted)
                        Surface(color = EditorColors.tint, shape = RoundedCornerShape(50)) {
                            Text("Chọn ảnh", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = onPick, enabled = enabled, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Thêm ảnh minh họa cho câu hỏi")
            }
        }
    }
}

private fun Modifier.clipEditorImage() = this.then(
    Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
)

/** Render plain dialog content, not a separate popup window, in Android Studio Preview. */
@Preview(name = "Quiz Editor - Discard Changes", showBackground = true, widthDp = 390)
@Preview(name = "Quiz Editor - Discard Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizEditorDiscardPreview() {
    MyQuizAppTheme {
        Box(Modifier.padding(20.dp)) {
            AppConfirmDialogContent(
                title = "Bỏ thay đổi?",
                message = "Bạn có thay đổi chưa được lưu. Thoát sẽ mất toàn bộ thay đổi này.",
                confirmText = "Bỏ thay đổi", dismissText = "Ở lại", variant = AppDialogVariant.Danger,
                onConfirm = {}, onDismiss = {}, scrollState = rememberScrollState(),
            )
        }
    }
}
