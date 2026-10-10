package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

@Preview(name = "Question Image Light", showBackground = true, widthDp = 390)
@Preview(name = "Question Image Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuestionImagePreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("Câu hỏi có ảnh — khung 16:9")
                QuestionImage("https://preview.invalid/question.png", "Ảnh minh họa câu hỏi")
                androidx.compose.material3.Text("Câu hỏi không có ảnh — không chiếm chỗ")
                QuestionImage(null, null)
                androidx.compose.material3.Text("Nội dung tiếp theo")
            }
        }
    }
}

