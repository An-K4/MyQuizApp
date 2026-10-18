package android.kma.myquizzapp.core.ui.gameconfig

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Stateless field shared by the room name and numeric configuration controls. */
@Composable
fun RoomConfigTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ComponentColors.foreground.copy(alpha = if (enabled) 1f else 0.55f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
            enabled = enabled,
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon,
            supportingText = supportingText?.let { text -> { Text(text) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ComponentColors.foreground,
                unfocusedTextColor = ComponentColors.foreground,
                disabledTextColor = ComponentColors.muted,
                focusedContainerColor = ComponentColors.surface,
                unfocusedContainerColor = ComponentColors.surface,
                disabledContainerColor = ComponentColors.softSurface,
                unfocusedBorderColor = ComponentColors.border,
                disabledBorderColor = ComponentColors.border,
                focusedSupportingTextColor = ComponentColors.muted,
                unfocusedSupportingTextColor = ComponentColors.muted,
                focusedLeadingIconColor = ComponentColors.muted,
                unfocusedLeadingIconColor = ComponentColors.muted,
            ),
        )
    }
}

@Preview(name = "Room fields Light", showBackground = true, widthDp = 390)
@Preview(name = "Room fields Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoomConfigTextFieldPreview() {
    MyQuizAppTheme {
        Surface(color = ComponentColors.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RoomConfigTextField("Tên phòng", "Phòng chơi của tôi", {}, supportingText = "Từ 2 đến 100 ký tự")
                RoomConfigTextField("Thời gian mỗi câu (giây)", "20", {})
                RoomConfigTextField("Số người chơi tối đa", "0", {}, isError = true, supportingText = "Kiểm tra lại giới hạn cho phép")
                RoomConfigTextField("Thiết lập bị khóa", "100", {}, enabled = false)
            }
        }
    }
}
