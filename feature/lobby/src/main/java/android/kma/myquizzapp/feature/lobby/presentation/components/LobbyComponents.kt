package android.kma.myquizzapp.feature.lobby.presentation.components

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.Primary
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun LobbyConnectionBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
    connectedLabel: String = "Đã kết nối",
) {
    LobbyStatusBadge(
        label = when (status) {
            ConnectionStatus.CONNECTED -> connectedLabel
            ConnectionStatus.CONNECTING -> "Đang kết nối"
            ConnectionStatus.RECONNECTING -> "Đang nối lại"
            ConnectionStatus.RECONNECT_FAILED -> "Mất kết nối"
        },
        tone = when (status) {
            ConnectionStatus.CONNECTED -> LobbyBadgeTone.Success
            ConnectionStatus.RECONNECT_FAILED -> LobbyBadgeTone.Danger
            else -> LobbyBadgeTone.Neutral
        },
        modifier = modifier,
    )
}

internal fun lobbyPlayerStatusLabel(status: String): String = when (status) {
    "connected" -> "Đã kết nối"
    "disconnected" -> "Mất kết nối"
    "eliminated" -> "Đã bị loại"
    "finished" -> "Đã hoàn thành"
    else -> "Không rõ"
}

@Composable
internal fun LobbyPlayerStatusBadge(player: LobbyPlayer, modifier: Modifier = Modifier) {
    LobbyStatusBadge(
        label = lobbyPlayerStatusLabel(player.status),
        tone = if (player.status == "connected") LobbyBadgeTone.Success else LobbyBadgeTone.Neutral,
        modifier = modifier,
    )
}

@Composable
internal fun LobbyPlayerAvatar(
    player: LobbyPlayer,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    if (!player.playerAvatar.isNullOrBlank() && !LocalInspectionMode.current) {
        Avatar(player.playerAvatar, contentDescription = null, size = size, modifier = modifier)
    } else {
        val accents = listOf(Primary, FrontendColors.Info, FrontendColors.Warning, FrontendColors.SuccessForeground)
        val accent = accents[Math.floorMod(player.id, accents.size.toLong()).toInt()]
        Surface(modifier.size(size), shape = CircleShape, color = accent.copy(alpha = 0.08f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    player.playerName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        .takeLast(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" },
                    color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal enum class LobbyBadgeTone { Success, Neutral, Danger }

/** Shared visual language for the host connection badge and each player's status badge. */
@Composable
internal fun LobbyStatusBadge(label: String, tone: LobbyBadgeTone, modifier: Modifier = Modifier) {
    val foreground = when (tone) {
        LobbyBadgeTone.Success -> LobbyColors.successForeground
        LobbyBadgeTone.Neutral -> LobbyColors.muted
        LobbyBadgeTone.Danger -> MaterialTheme.colorScheme.error
    }
    val background = when (tone) {
        LobbyBadgeTone.Success -> LobbyColors.successSurface
        LobbyBadgeTone.Neutral -> LobbyColors.neutralSurface
        LobbyBadgeTone.Danger -> MaterialTheme.colorScheme.errorContainer
    }
    Row(
        modifier = modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(foreground, CircleShape))
        Text(label, modifier = Modifier.weight(1f, fill = false), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Approved light tokens; dark roles remain provisional, as on the other polished screens. */
internal object LobbyColors {
    private val dark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val softSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val foreground: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    val neutralSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.Border.copy(alpha = 0.55f)
    val successSurface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.CorrectAnswerBackground
    val successForeground: Color
        @Composable get() = if (dark) FrontendColors.Success else FrontendColors.SuccessForeground
}
