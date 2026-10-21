package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pure clock arithmetic. Zero means wait for the server, never start a question locally. */
fun gameStartCountdownSeconds(targetEpochMs: Long?, offsetMs: Long, nowMillis: Long): Int? {
    if (targetEpochMs == null) return null
    val left = targetEpochMs - (nowMillis + offsetMs)
    return if (left <= 0L) 0 else ((left + 999L) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

@Composable
private fun countdownPageColor() =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else FrontendColors.PageBackground

/** Convenience wrapper owns only scrolling; the overload below is stateless. */
@Composable
fun GameStartCountdownContent(
    seconds: Int?,
    message: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onRetry: (() -> Unit)? = null,
) = GameStartCountdownContent(seconds, message, rememberScrollState(), modifier, subtitle, onRetry)

/** Shared presentation only: no timer, navigation, socket or automatic phase transition. */
@Composable
fun GameStartCountdownContent(
    seconds: Int?,
    message: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().background(countdownPageColor()), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(scrollState).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Chuẩn bị bắt đầu", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Surface(
                shape = CircleShape,
                color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint,
            ) {
                Box(Modifier.sizeIn(minWidth = 160.dp, minHeight = 160.dp).padding(24.dp),
                    contentAlignment = Alignment.Center) {
                    Text(seconds?.takeIf { it > 0 }?.toString() ?: "—",
                        color = MaterialTheme.colorScheme.primary, fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (onRetry != null) QuizPrimaryButton("Kết nối lại", onRetry)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameStartCountdownScreenContent(
    seconds: Int?,
    message: String,
    title: String,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    onRetry: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(), containerColor = countdownPageColor(),
        snackbarHost = snackbarHost,
        topBar = { TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = countdownPageColor())) },
    ) { padding ->
        GameStartCountdownContent(seconds, message,
            Modifier.padding(padding).consumeWindowInsets(padding), onRetry = onRetry)
    }
}

@Preview(name = "Countdown", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CountdownPreview() = MyQuizAppTheme {
    GameStartCountdownScreenContent(3, "Câu hỏi đầu tiên sắp xuất hiện. Hãy sẵn sàng!", "Chuẩn bị vào trận")
}

@Preview(name = "Countdown paused dark", showBackground = true, widthDp = 390, heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CountdownPausedPreview() = MyQuizAppTheme {
    GameStartCountdownScreenContent(null, "Trận đang tạm dừng — chờ Host tiếp tục.", "Chuẩn bị vào trận")
}

@Preview(name = "Countdown retry large font", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun CountdownRetryPreview() = MyQuizAppTheme {
    GameStartCountdownScreenContent(null, "Không thể kết nối lại. Hãy kiểm tra mạng rồi thử lại.",
        "Chuẩn bị vào trận", onRetry = {})
}

@Preview(name = "Countdown waiting for server", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CountdownWaitingPreview() = MyQuizAppTheme {
    GameStartCountdownScreenContent(0, "Đang chờ máy chủ mở câu hỏi đầu tiên…", "Chuẩn bị vào trận")
}
