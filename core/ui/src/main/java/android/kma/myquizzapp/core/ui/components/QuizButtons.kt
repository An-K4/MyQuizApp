package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizPrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) = QuizActionButton(text, onClick, modifier, enabled, loading, ActionStyle.Primary, trailingIcon)

/** White outlined secondary action, not an opaque pale-purple filled button. */
@Composable
fun QuizSecondaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    contentColor: Color? = null,
) = QuizActionButton(
    text, onClick, modifier, enabled, loading, ActionStyle.Secondary, trailingIcon,
    leadingIcon = leadingIcon, contentColorOverride = contentColor,
)

/** Only forwards a click. Confirmation and destructive business logic belong to the caller. */
@Composable
fun QuizDangerButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) = QuizActionButton(text, onClick, modifier, enabled, loading, ActionStyle.Danger, trailingIcon)

private enum class ActionStyle { Primary, Secondary, Danger }

@Composable
private fun QuizActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier,
    enabled: Boolean, loading: Boolean, style: ActionStyle,
    trailingIcon: (@Composable () -> Unit)?,
    leadingIcon: (@Composable () -> Unit)? = null,
    contentColorOverride: Color? = null,
) {
    val active = enabled && !loading
    val foreground = contentColorOverride ?: when (style) {
        ActionStyle.Primary -> FrontendColors.PageBackground
        ActionStyle.Secondary -> Primary
        ActionStyle.Danger -> ComponentColors.dangerForeground
    }
    val background = if (style == ActionStyle.Primary) Primary else ComponentColors.surface
    val buttonModifier = modifier.heightIn(min = 52.dp).semantics {
        if (loading) stateDescription = "Đang xử lý"
    }
    val colors = ButtonDefaults.buttonColors(
        containerColor = background,
        contentColor = foreground,
        disabledContainerColor = background.copy(alpha = 0.45f),
        disabledContentColor = foreground.copy(alpha = 0.55f),
    )
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = foreground, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 14.sp, lineHeight = 20.sp,
            letterSpacing = 0.sp, fontWeight = FontWeight.Bold,
        ))
        if (trailingIcon != null) {
            Spacer(Modifier.width(4.dp))
            trailingIcon()
        }
    }
    if (style == ActionStyle.Primary) {
        Button(
            onClick = onClick, modifier = buttonModifier, enabled = active,
            shape = RoundedCornerShape(16.dp), colors = colors,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            content = content,
        )
    } else {
        val borderColor = if (style == ActionStyle.Danger) ComponentColors.dangerForeground else ComponentColors.border
        OutlinedButton(
            onClick = onClick, modifier = buttonModifier, enabled = active,
            shape = RoundedCornerShape(16.dp), colors = colors,
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (active) 1f else 0.45f)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            content = content,
        )
    }
}

@Preview(name = "Primary Light", showBackground = true, widthDp = 390)
@Preview(name = "Primary Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizPrimaryButtonPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizPrimaryButton("Đăng nhập", {}, Modifier.fillMaxWidth())
                QuizPrimaryButton("Đang đăng nhập", {}, Modifier.fillMaxWidth(), loading = true)
                QuizPrimaryButton("Đăng nhập", {}, Modifier.fillMaxWidth(), enabled = false)
            }
        }
    }
}

@Preview(name = "Secondary Light", showBackground = true, widthDp = 390)
@Preview(name = "Secondary Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizSecondaryButtonPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizSecondaryButton("Tiếp tục với tư cách Khách", {}, Modifier.fillMaxWidth())
                QuizSecondaryButton("Đang xử lý", {}, Modifier.fillMaxWidth(), loading = true)
                QuizSecondaryButton("Tiếp tục", {}, Modifier.fillMaxWidth(), enabled = false)
            }
        }
    }
}

@Preview(name = "Danger Light", showBackground = true, widthDp = 390)
@Preview(name = "Danger Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizDangerButtonPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizDangerButton("Đăng xuất", {}, Modifier.fillMaxWidth(), trailingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, Modifier.size(16.dp))
                })
                QuizDangerButton("Đang xử lý", {}, Modifier.fillMaxWidth(), loading = true)
                QuizDangerButton("Vô hiệu hóa tài khoản", {}, Modifier.fillMaxWidth(), enabled = false)
            }
        }
    }
}

