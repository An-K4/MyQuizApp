package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Owned-quiz card, separate from SearchQuizCard. All menu state/actions are hoisted. */
@Composable
fun LibraryQuizCard(
    title: String,
    metadataText: String,
    imageUrl: String?,
    isPublic: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val danger = if (isDark) MaterialTheme.colorScheme.error else FrontendColors.Danger
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
        color = ComponentColors.softSurface,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RemoteImage(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = ComponentColors.foreground,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    ),
                )
                Text(
                    text = metadataText,
                    color = ComponentColors.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPublic) ComponentColors.successSurface
                        else if (isDark) MaterialTheme.colorScheme.surface else FrontendColors.Border.copy(alpha = 0.65f),
                ) {
                    Text(
                        text = if (isPublic) "Công khai" else "Riêng tư",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (isPublic) ComponentColors.successForeground else ComponentColors.muted,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            Box {
                IconButton(onClick = { onMenuExpandedChange(true) }, enabled = enabled) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn cho $title",
                        tint = ComponentColors.foreground,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded && enabled,
                    onDismissRequest = { onMenuExpandedChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onMenuExpandedChange(false)
                            onEditClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa quiz", color = danger) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = danger) },
                        onClick = {
                            onMenuExpandedChange(false)
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "Library Cards Light", showBackground = true, widthDp = 390)
@Preview(name = "Library Cards Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Library Cards Large Text", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun LibraryQuizCardPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryQuizCard("Luyện thi trắc nghiệm Sinh học 12", "10 câu hỏi", null, true, false, {}, {}, {}, {})
                LibraryQuizCard("Quiz vui cuối tuần", "15 câu hỏi", null, false, false, {}, {}, {}, {})
                LibraryQuizCard("Quiz đang được xử lý", "10 câu hỏi", null, true, false, {}, {}, {}, {}, enabled = false)
            }
        }
    }
}
