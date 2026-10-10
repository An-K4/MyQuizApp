package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Vertical card for Home/Discover. QuizListCard is the horizontal Search/Library variant. */
@Composable
fun DiscoveryQuizCard(
    title: String,
    categoryText: String?,
    metadataText: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(169.dp),
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier,
        shape = RoundedCornerShape(20.dp), color = ComponentColors.softSurface,
    ) {
        Column {
            RemoteImage(
                imageUrl = imageUrl, contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(20.dp)),
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!categoryText.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(50), color = ComponentColors.selectedSurface) {
                        Text(
                            categoryText, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    title, color = ComponentColors.foreground,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    metadataText, color = ComponentColors.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
                )
            }
        }
    }
}

@Preview(name = "Discovery Quiz Light", showBackground = true, widthDp = 390)
@Preview(name = "Discovery Quiz Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscoveryQuizCardPreview() {
    MyQuizAppTheme {
        Surface {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiscoveryQuizCard("Đố vui vũ trụ", "Khoa học", "12 câu • 350 lượt chơi", null, {})
                DiscoveryQuizCard("Một tên quiz dài để kiểm tra bố cục", null, "20 câu • 500 lượt chơi", null, {}, enabled = false)
            }
        }
    }
}

@Preview(name = "Discovery Quiz Home Width", showBackground = true, widthDp = 260)
@Preview(name = "Discovery Quiz Large Text", showBackground = true, widthDp = 260, fontScale = 1.5f)
@Composable
private fun DiscoveryQuizCardHomePreview() {
    MyQuizAppTheme {
        Surface {
            DiscoveryQuizCard("Đố vui vũ trụ", "Khoa học", "12 câu • 350 lượt chơi", null, {}, Modifier.padding(20.dp).width(220.dp))
        }
    }
}
