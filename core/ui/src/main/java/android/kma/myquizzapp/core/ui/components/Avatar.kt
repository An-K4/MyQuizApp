package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Avatar tròn dùng chung cho người dùng (Home header, Profile, v.v.).
 *
 * Coil là chi tiết triển khai nội bờCore:ui — module gọi component này
 * (app, feature:home, ...) không cần tự thêm dependency `coil.compose` của
 * riêng mình, chỉ cần dependency `core:ui` (thường đã có sẵn).
 *
 * Nếu [avatarUrl] null/rỗng, hiển thị icon mặc định (tránh ảnh vỡ khi
 * user chưa có avatar).
 */
@Composable
fun Avatar(
    avatarUrl: String?,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    if (avatarUrl.isNullOrBlank() || LocalInspectionMode.current) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = contentDescription,
            modifier = modifier.size(size)
        )
    } else {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}

@Preview(name = "Avatar States Light", showBackground = true, widthDp = 390)
@Preview(name = "Avatar States Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AvatarPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(null, "Avatar mặc định", size = 34.dp)
                Avatar("", "Avatar mặc định", size = 40.dp)
                Avatar(null, "Avatar lớn", size = 64.dp)
            }
        }
    }
}

