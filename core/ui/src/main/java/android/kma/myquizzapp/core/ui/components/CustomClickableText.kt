package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless inline action: only clickableText is a link; surrounding text is not clickable. */
@Composable
fun CustomClickableText(
    clickableText: String,
    onTextClicked: () -> Unit,
    modifier: Modifier = Modifier,
    startText: String? = null,
    endText: String = "",
    clickableTextTag: String = "action",
    textSize: TextUnit = 16.sp,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign = TextAlign.Start,
) {
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f),
        fontWeight = FontWeight.Bold,
    )
    val text = buildAnnotatedString {
        if (!startText.isNullOrBlank()) {
            append(startText)
            append(" ")
        }
        if (enabled) {
            withLink(LinkAnnotation.Clickable(
                tag = clickableTextTag,
                styles = TextLinkStyles(style = linkStyle),
                linkInteractionListener = { onTextClicked() },
            )) { append(clickableText) }
        } else {
            pushStyle(linkStyle)
            append(clickableText)
            pop()
        }
        append(endText)
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = textSize, lineHeight = textSize * 1.45f, letterSpacing = 0.sp,
        ),
    )
}

@Preview(name = "Inline Action Light", showBackground = true, widthDp = 390)
@Preview(name = "Inline Action Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CustomClickableTextPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CustomClickableText("Đăng ký ngay", {}, startText = "Chưa có tài khoản?", textSize = 12.sp)
                CustomClickableText("Đăng ký ngay", {}, startText = "Chưa có tài khoản?", textSize = 12.sp, enabled = false)
                CustomClickableText("Điều khoản sử dụng", {}, endText = " của ứng dụng.")
            }
        }
    }
}
