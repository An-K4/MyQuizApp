package android.kma.myquizzapp.core.ui.gameconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Ô chọn một-trong-nhiều của form cấu hình.
 *
 * Danh sách lựa chọn KHÔNG hardcode ở client mà lấy từ `state.options`, tức từ
 * constraint của backend. Ví dụ mode survival chỉ cho `never|end_only`; nếu client
 * tự liệt kê đủ ba lựa chọn thì host chọn được giá trị mà server sẽ âm thầm
 * normalize lại — đúng cái bẫy phải tránh.
 *
 * [optionLabel] tách khỏi component vì nhãn tiếng Việt là chuyện của từng nhóm
 * cấu hình, còn component này chỉ biết về chuỗi wire.
 */
@Composable
fun ChoiceSettingField(
    title: String,
    state: ChoiceSettingUiState,
    enabled: Boolean,
    optionLabel: (String) -> String,
    onValueChange: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(title)
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && state.editable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(optionLabel(state.value))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    }
                )
            }
        }
    }
}

/** Nhãn tiếng Việt cho `flow.showLeaderboard`; chuỗi lạ thì hiện nguyên bản. */
fun leaderboardOptionLabel(value: String): String = when (value) {
    "never" -> "Không bao giờ"
    "between_questions" -> "Giữa các câu"
    "end_only" -> "Chỉ cuối trận"
    else -> value
}
