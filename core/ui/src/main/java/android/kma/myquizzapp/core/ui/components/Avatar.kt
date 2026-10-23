package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Avatar tròn; thiếu URL, đang tải hoặc tải lỗi đều dùng icon người, không chèn chữ. */
@Composable
fun Avatar(
    avatarUrl: String?,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    RemoteImageContainer(
        imageUrl = avatarUrl,
        contentDescription = contentDescription,
        modifier = modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop,
    ) { AvatarPlaceholder() }
}

@Composable
private fun AvatarPlaceholder() {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxSize(0.8f),
    )
}

@Preview(name = "Avatar Fallback · Light", showBackground = true, widthDp = 390)
@Preview(name = "Avatar Fallback · Dark", showBackground = true, widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Avatar Fallback · Large text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun AvatarFallbackPreview() {
    MyQuizAppTheme {
        Surface {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Avatar(null, "Avatar chưa được chọn", size = 34.dp)
                    Avatar("", "Avatar mặc định", size = 40.dp)
                    Avatar("https://preview.invalid/avatar.png", "Avatar đang tải", size = 64.dp)
                }
                // Cùng placeholder mà nhánh onError dùng, không cần mạng trong Preview.
                Box(Modifier.size(64.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    AvatarPlaceholder()
                }
            }
        }
    }
}

