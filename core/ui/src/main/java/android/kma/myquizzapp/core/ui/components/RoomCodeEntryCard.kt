package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless visual card. Room lookup, guest policy and navigation stay in feature:lobby. */
@Composable
fun RoomCodeEntryCard(
    codeValue: TextFieldValue,
    onCodeValueChange: (TextFieldValue) -> Unit,
    codeFocused: Boolean,
    onCodeFocusChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    codeError: String? = null,
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {},
    exitMessage: String? = null,
    onExitMessageDismiss: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ComponentColors.selectedSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Tham gia ván đấu ngay", color = ComponentColors.foreground,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Nhập mã phòng 6 ký tự được chủ phòng chia sẻ.", color = ComponentColors.muted,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp),
            )
            Spacer(Modifier.height(12.dp))
            if (exitMessage != null) {
                RoomCodeMessage(exitMessage, "Đã hiểu", onExitMessageDismiss)
                Spacer(Modifier.height(12.dp))
            }
            SixCharacterCodeFieldContent(
                value = codeValue, onValueChange = onCodeValueChange, kind = SixCharacterCodeKind.Room,
                focused = codeFocused, onFocusChange = onCodeFocusChange,
                modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting, errorMessage = codeError,
                imeAction = ImeAction.Go,
                keyboardActions = KeyboardActions(onGo = { if (canSubmit && !isSubmitting) onSubmit() }),
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                RoomCodeMessage(errorMessage, "Đóng", onErrorDismiss)
            }
            Spacer(Modifier.height(12.dp))
            QuizPrimaryButton(
                "Vào phòng", onSubmit, Modifier.fillMaxWidth(), enabled = canSubmit && !isSubmitting,
                loading = isSubmitting,
                trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun RoomCodeMessage(message: String, actionText: String, onDismiss: () -> Unit) {
    Row {
        Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onDismiss) { Text(actionText) }
    }
}

@Preview(name = "Room Code Light", showBackground = true, widthDp = 390)
@Preview(name = "Room Code Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoomCodeEntryCardPreview() = RoomCodePreview("AB23", false)

@Preview(name = "Room Code Loading", showBackground = true, widthDp = 390)
@Composable
private fun RoomCodeLoadingPreview() = RoomCodePreview("AB23CD", true)

@Preview(name = "Room Code Error Large Text", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun RoomCodeErrorPreview() = RoomCodePreview("AB23CD", false, "Không tìm thấy phòng")

@Composable
private fun RoomCodePreview(code: String, loading: Boolean, error: String? = null) {
    MyQuizAppTheme {
        Surface {
            RoomCodeEntryCard(
                TextFieldValue(code, TextRange(code.length)), {}, true, {}, {}, code.length == 6,
                modifier = Modifier.padding(20.dp), isSubmitting = loading, codeError = error,
            )
        }
    }
}
