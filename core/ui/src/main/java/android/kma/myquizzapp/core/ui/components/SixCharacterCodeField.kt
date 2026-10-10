package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Convenience boundary keeps only transient focus state; the caller owns text/selection. */
@Composable
fun SixCharacterCodeField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    kind: SixCharacterCodeKind,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
    label: String = if (kind == SixCharacterCodeKind.Otp) "Mã OTP, 6 chữ số" else "Mã phòng, 6 ký tự chữ hoặc số",
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var focused by remember { mutableStateOf(false) }
    SixCharacterCodeFieldContent(
        value, onValueChange, kind, focused, { focused = it }, modifier,
        enabled, errorMessage, label, imeAction, keyboardActions,
    )
}

/** One real editable field: supports paste, backspace, selection, IME and TalkBack as one control. */
@Composable
fun SixCharacterCodeFieldContent(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    kind: SixCharacterCodeKind,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorMessage: String? = null,
    label: String = if (kind == SixCharacterCodeKind.Otp) "Mã OTP, 6 chữ số" else "Mã phòng, 6 ký tự chữ hoặc số",
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val surface = ComponentColors.surface
    val border = ComponentColors.border
    val tint = ComponentColors.selectedSurface
    val foreground = ComponentColors.foreground
    val danger = ComponentColors.dangerForeground
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { incoming ->
                val text = normalizeSixCharacterCode(incoming.text, kind)
                val from = normalizeSixCharacterCode(incoming.text.take(incoming.selection.start), kind).length.coerceAtMost(text.length)
                val to = normalizeSixCharacterCode(incoming.text.take(incoming.selection.end), kind).length.coerceAtMost(text.length)
                onValueChange(TextFieldValue(text, TextRange(from, to)))
            },
            enabled = enabled, singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (kind == SixCharacterCodeKind.Otp) KeyboardType.NumberPassword else KeyboardType.Ascii,
                capitalization = if (kind == SixCharacterCodeKind.Room) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                autoCorrectEnabled = false, imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) }.semantics {
                contentDescription = label
                if (errorMessage != null) error(errorMessage)
            },
            decorationBox = { innerTextField ->
                Box {
                    // Native editing/selection remains available underneath the visual cells.
                    Box(Modifier.matchParentSize()) { innerTextField() }
                    Row(
                        Modifier.fillMaxWidth().clearAndSetSemantics { },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(6) { index ->
                            val active = focused && index == value.selection.end.coerceIn(0, 5)
                            Surface(
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                color = if (active) tint else surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(if (active) 2.dp else 1.dp, when {
                                    errorMessage != null -> danger
                                    active -> Primary
                                    else -> border
                                }),
                            ) {
                                Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        value.text.getOrNull(index)?.toString().orEmpty(),
                                        color = foreground.copy(alpha = if (enabled) 1f else 0.45f),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
        if (errorMessage != null) {
            Text(errorMessage, color = danger, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Preview(name = "OTP Light", showBackground = true, widthDp = 390)
@Preview(name = "OTP Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpCodeFieldPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SixCharacterCodeFieldContent(TextFieldValue(), {}, SixCharacterCodeKind.Otp, false, {})
                SixCharacterCodeFieldContent(TextFieldValue("482", TextRange(2)), {}, SixCharacterCodeKind.Otp, true, {})
                SixCharacterCodeFieldContent(TextFieldValue("123456"), {}, SixCharacterCodeKind.Otp, false, {}, enabled = false)
                SixCharacterCodeFieldContent(TextFieldValue("123"), {}, SixCharacterCodeKind.Otp, false, {}, errorMessage = "Vui lòng nhập đủ 6 chữ số")
            }
        }
    }
}

@Preview(name = "Room Code Light", showBackground = true, widthDp = 390)
@Preview(name = "Room Code Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoomCodeFieldPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SixCharacterCodeFieldContent(TextFieldValue("AB2", TextRange(3)), {}, SixCharacterCodeKind.Room, true, {})
                SixCharacterCodeFieldContent(TextFieldValue("AB23CD"), {}, SixCharacterCodeKind.Room, false, {})
            }
        }
    }
}

