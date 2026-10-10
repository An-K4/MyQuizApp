package android.kma.myquizzapp.feature.auth.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless labeled field shared by forgot/reset. Validation belongs to the ViewModel. */
@Composable
internal fun PasswordResetFormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    enabled: Boolean,
    error: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    passwordVisible: Boolean? = null,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val foreground = if (isDark) scheme.onBackground else FrontendColors.Foreground
    val muted = if (isDark) scheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border = if (isDark) scheme.outlineVariant else FrontendColors.Border
    val inputStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp,
    )
    Column {
        Text(label, color = foreground, style = inputStyle.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = inputStyle) },
            leadingIcon = { Icon(icon, null, Modifier.size(20.dp)) },
            trailingIcon = if (passwordVisible != null) {
                {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }, enabled = enabled) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            Modifier.size(20.dp),
                        )
                    }
                }
            } else null,
            visualTransformation = if (passwordVisible == false) PasswordVisualTransformation() else VisualTransformation.None,
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = inputStyle,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
                .semantics { contentDescription = label },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = foreground, unfocusedTextColor = foreground,
                focusedBorderColor = scheme.primary, unfocusedBorderColor = border,
                focusedLeadingIconColor = muted, unfocusedLeadingIconColor = muted,
                focusedTrailingIconColor = muted, unfocusedTrailingIconColor = muted,
                focusedPlaceholderColor = muted, unfocusedPlaceholderColor = muted,
                cursorColor = scheme.primary,
            ),
        )
    }
}

@Preview(name = "Reset Field Light", showBackground = true, widthDp = 390)
@Preview(name = "Reset Field Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PasswordResetFormFieldPreview() {
    MyQuizAppTheme {
        Surface {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PasswordResetFormField("Email đã đăng ký", "", "email@domain.com", {}, Icons.Outlined.Email, KeyboardOptions.Default, true)
                PasswordResetFormField("Email đã đăng ký", "invalid", "email@domain.com", {}, Icons.Outlined.Email, KeyboardOptions.Default, true, error = "Email không hợp lệ")
            }
        }
    }
}
