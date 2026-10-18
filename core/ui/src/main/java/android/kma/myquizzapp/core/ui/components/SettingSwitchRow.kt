package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pattern cài đặt phổ biến; không phụ thuộc model hay intent của feature. */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ComponentColors.foreground.copy(alpha = if (enabled) 1f else 0.55f),
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = ComponentColors.muted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = ComponentColors.surface,
                uncheckedTrackColor = ComponentColors.border,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Preview(name = "Setting Switch States Light", showBackground = true, widthDp = 390)
@Preview(name = "Setting Switch States Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingSwitchRowPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitchRow("Âm thanh", true, {}, description = "Phát âm thanh trong khi chơi")
                SettingSwitchRow("Rung", false, {})
                SettingSwitchRow("Thông báo", false, {}, enabled = false,
                    description = "Mô tả dài để kiểm tra bố cục nhiều dòng khi tùy chọn bị vô hiệu hóa")
            }
        }
    }
}

