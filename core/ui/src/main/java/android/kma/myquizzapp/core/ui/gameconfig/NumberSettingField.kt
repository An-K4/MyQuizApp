package android.kma.myquizzapp.core.ui.gameconfig

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Ô nhập số của form cấu hình.
 *
 * Giá trị là String chứ không phải Int? — có chủ ý. Nhiều field cho phép null
 * (`perQuestionSeconds`, `totalMatchSeconds`, `lives`) và ô rỗng chính là cách
 * người dùng biểu đạt null. Nếu ép sang Int? ngay tại đây thì không phân biệt
 * được "đang xóa để gõ lại" với "chọn null", và con trỏ sẽ nhảy khi gõ dở.
 */
@Composable
fun NumberSettingField(
    title: String,
    state: NumberSettingUiState,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    description: String? = null
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = onValueChange,
        enabled = enabled && state.editable,
        isError = isError,
        label = { Text(title) },
        supportingText = {
            val range = listOfNotNull(state.min, state.max).joinToString("\u2013")
            val support = listOfNotNull(
                range.takeIf { it.isNotBlank() },
                description,
                state.note?.takeUnless { description != null }
            ).joinToString(" \u2022 ")
            if (support.isNotBlank()) Text(support)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
