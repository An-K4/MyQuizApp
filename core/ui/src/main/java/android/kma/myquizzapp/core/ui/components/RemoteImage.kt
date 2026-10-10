package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Ảnh từ xa dùng chung; module gọi không cần phụ thuộc trực tiếp vào Coil. */
@Composable
fun RemoteImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (imageUrl.isNullOrBlank() || LocalInspectionMode.current) {
        ImagePlaceholder(modifier)
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = contentScale,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery),
            fallback = painterResource(android.R.drawable.ic_menu_gallery),
            error = painterResource(android.R.drawable.ic_menu_gallery)
        )
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.4f)
        )
    }
}

@Preview(name = "Remote Image Light", showBackground = true, widthDp = 390)
@Preview(name = "Remote Image Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemoteImagePreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteImage(null, "Ảnh chưa được chọn", Modifier.fillMaxWidth().height(140.dp))
                RemoteImage("https://preview.invalid/image.png", "Khung ảnh từ xa — placeholder trong Preview",
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f), ContentScale.Fit)
            }
        }
    }
}

