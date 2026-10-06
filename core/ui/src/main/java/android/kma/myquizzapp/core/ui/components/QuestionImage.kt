package android.kma.myquizzapp.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme

/**
 * Ảnh minh họa câu hỏi dùng chung cho Host/Player gameplay.
 *
 * Không tạo block khi câu không có ảnh. Khung 16:9 giữ bố cục ổn định trong lúc
 * Coil tải ảnh; Fit giữ trọn nội dung vì ảnh câu hỏi có thể chứa chữ/sơ đồ.
 */
@Composable
fun QuestionImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val url = imageUrl?.takeIf(String::isNotBlank) ?: return
    RemoteImage(
        imageUrl = url,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Fit
    )
}
