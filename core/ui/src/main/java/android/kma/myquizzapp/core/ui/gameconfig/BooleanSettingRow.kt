package android.kma.myquizzapp.core.ui.gameconfig

import android.kma.myquizzapp.core.ui.components.SettingSwitchRow
import androidx.compose.runtime.Composable

/**
 * Một dòng công tắc của form cấu hình.
 *
 * `enabled` cuối cùng là phép AND của hai điều kiện khác nhau về bản chất:
 * [enabled] là "cả form có đang cho sửa không" (đang lưu, phòng đã bắt đầu...),
 * còn `state.editable` là "mode này có cho sửa field này không" (locked từ
 * backend). Giữ tách biệt vì locked là vĩnh viễn, còn enabled là tạm thời.
 */
@Composable
fun BooleanSettingRow(
    title: String,
    state: BooleanSettingUiState,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingSwitchRow(
        title = title,
        checked = state.value,
        enabled = enabled && state.editable,
        onCheckedChange = onCheckedChange
    )
}
