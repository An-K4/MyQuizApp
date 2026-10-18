package android.kma.myquizzapp.core.ui.gameconfig

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType

/** Keep raw text: blank nullable fields and zero have different configuration meanings. */
@Composable
fun NumberSettingField(
    title: String,
    state: NumberSettingUiState,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    description: String? = null
) {
    val range = listOfNotNull(state.min, state.max).joinToString("\u2013")
    val support = listOfNotNull(
        range.takeIf { it.isNotBlank() },
        description,
        state.note?.takeUnless { description != null }
    ).joinToString(" \u2022 ")
    RoomConfigTextField(
        title = title,
        value = state.value,
        onValueChange = onValueChange,
        enabled = enabled && state.editable,
        isError = isError,
        supportingText = support.takeIf { it.isNotBlank() },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
