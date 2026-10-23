package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.R
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Ảnh từ xa dùng chung; caller quyết định kích thước/clip của khung.
 * Loading, thiếu URL và lỗi tải đều dùng nền trắng + logo app đặt giữa.
 * Giữ nguyên màu/tỉ lệ logo, không có chữ phủ trên ảnh; state vẫn có semantics.
 */
@Composable
fun RemoteImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    RemoteImageContainer(imageUrl, contentDescription, modifier, contentScale) { state ->
        RemoteImagePlaceholder(state)
    }
}

internal enum class RemoteImageLoadState(val label: String) {
    Missing("Chưa có ảnh"),
    Loading("Đang tải ảnh…"),
    Success("Đã tải ảnh"),
    Error("Không tải được ảnh"),
}

/** Chỉ giữ trạng thái kỹ thuật của request, không lưu bằng rememberSaveable. */
@Composable
internal fun RemoteImageContainer(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholder: @Composable (RemoteImageLoadState) -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    // URL mới phải có state mới; callback từ request cũ không thể đổi fallback ảnh mới.
    key(imageUrl, isPreview) {
        var state by remember {
            mutableStateOf(
                if (imageUrl.isNullOrBlank()) RemoteImageLoadState.Missing
                else RemoteImageLoadState.Loading
            )
        }
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics(mergeDescendants = true) {
                    if (!contentDescription.isNullOrBlank()) {
                        this.contentDescription = contentDescription
                        stateDescription = state.label
                    }
                },
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            if (!imageUrl.isNullOrBlank() && !isPreview) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null, // Được đọc một lần ở khung ngoài.
                    contentScale = contentScale,
                    onLoading = { state = RemoteImageLoadState.Loading },
                    onSuccess = { state = RemoteImageLoadState.Success },
                    onError = { state = RemoteImageLoadState.Error },
                )
            }
            if (state != RemoteImageLoadState.Success) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    placeholder(state)
                }
            }
        }
    }
}

@Composable
internal fun RemoteImagePlaceholder(state: RemoteImageLoadState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.White).semantics {
            stateDescription = state.label
        },
        contentAlignment = Alignment.Center,
    ) {
        // Asset có khoảng trong suốt sẵn: Fit giữ nguyên logo, không crop hay tint.
        val logoSize = (minOf(maxWidth, maxHeight) * 0.9f).coerceAtMost(192.dp)
        Image(
            painter = painterResource(R.drawable.quiz_brand_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(logoSize),
        )
    }
}

@Preview(name = "Image Fallback · Light", showBackground = true, widthDp = 390)
@Preview(name = "Image Fallback · Dark", showBackground = true, widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Image Fallback · Narrow, large text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun RemoteImageFallbackPreview() {
    MyQuizAppTheme {
        Surface {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Các state được dựng trực tiếp, Preview không giả vờ tải request mạng.
                RemoteImageLoadState.entries.filter { it != RemoteImageLoadState.Success }.forEach { state ->
                    Text(state.label, style = MaterialTheme.typography.labelMedium)
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                        propagateMinConstraints = true,
                    ) { RemoteImagePlaceholder(state) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center, propagateMinConstraints = true) {
                        RemoteImagePlaceholder(RemoteImageLoadState.Error)
                    }
                    Box(Modifier.size(width = 82.dp, height = 106.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center, propagateMinConstraints = true) {
                        RemoteImagePlaceholder(RemoteImageLoadState.Error)
                    }
                }
            }
        }
    }
}

