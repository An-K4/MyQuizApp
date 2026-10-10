package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Horizontal card; metadata/rating are caller-provided display values, never fabricated here. */
@Composable
fun QuizListCard(
    title: String,
    metadataText: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ratingText: String? = null,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(20.dp), color = ComponentColors.softSurface,
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
            RemoteImage(imageUrl, contentDescription = null,
                modifier = Modifier.width(82.dp).height(106.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = ComponentColors.foreground,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp))
                Text(metadataText, color = ComponentColors.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp))
                if (!ratingText.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ratingText, color = ComponentColors.warning,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Icon(Icons.Default.Star, contentDescription = "Đánh giá", tint = ComponentColors.warning, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Preview(name = "Quiz Card Light", showBackground = true, widthDp = 390)
@Preview(name = "Quiz Card Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizListCardPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Null image uses the local placeholder: previews do not fetch network images.
                QuizListCard("100 Câu Tiếng Anh Giao Tiếp Thông Dụng", "20 câu hỏi • 500 người đã chơi • Tạo bởi Anh Thư", null, {}, ratingText = "4.9")
                QuizListCard("Quiz chưa có đánh giá", "10 câu hỏi • Tạo bởi Minh Anh", null, {})
                QuizListCard("Quiz tạm thời không thể mở", "20 câu hỏi", null, {}, enabled = false)
            }
        }
    }
}
