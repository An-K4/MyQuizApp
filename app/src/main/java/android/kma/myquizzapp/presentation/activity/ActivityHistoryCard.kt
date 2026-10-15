package android.kma.myquizzapp.presentation.activity

import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
internal fun PlayedActivityCard(item: GameHistoryItem, onClick: () -> Unit) {
    ActivityHistoryCardFrame(item = item, onClick = onClick, showHost = true) { colors ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            ActivityMetric(
                label = "Thứ hạng",
                value = item.rank?.let { rank ->
                    buildString {
                        append("Hạng $rank")
                        if (item.totalPlayers > 0) append("/${item.totalPlayers}")
                        if (rank == 1) append(" 🥇")
                    }
                } ?: "Chưa xếp hạng",
                modifier = Modifier.weight(1f),
                colors = colors,
                accent = item.rank != null
            )
            ActivityMetric(
                label = "Tổng điểm",
                value = item.playerScore?.let { "${NumberFormatInteger.format(it)} điểm" } ?: "—",
                modifier = Modifier.weight(1f),
                colors = colors
            )
        }

        val correct = item.correctAnswersCount
        val total = item.totalQuestions
        if (correct != null && total > 0) {
            val progress = (correct.toFloat() / total).coerceIn(0f, 1f)
            val percentage = (progress * 100).toInt()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "$correct/$total câu đúng ($percentage%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.track)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(FrontendColors.Success)
                    )
                }
            }
        }
    }
}

@Composable
internal fun HostedActivityCard(item: GameHistoryItem, onClick: () -> Unit) {
    ActivityHistoryCardFrame(item = item, onClick = onClick, showHost = false) { colors ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            ActivityMetric(
                label = "Người chơi",
                value = item.totalPlayers.toString(),
                modifier = Modifier.weight(1f),
                colors = colors,
                accent = true
            )
            ActivityMetric(
                label = "Số câu hỏi",
                value = item.totalQuestions.toString(),
                modifier = Modifier.weight(1f),
                colors = colors
            )
        }
    }
}

private data class ActivityCardColors(
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val divider: Color,
    val track: Color,
    val card: Color,
    val modeBackground: Color
)

@Composable
private fun ActivityHistoryCardFrame(
    item: GameHistoryItem,
    onClick: () -> Unit,
    showHost: Boolean,
    content: @Composable (ActivityCardColors) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = ActivityCardColors(
        foreground = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground,
        muted = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground,
        accent = MaterialTheme.colorScheme.primary,
        divider = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f) else FrontendColors.Border,
        track = if (isDark) MaterialTheme.colorScheme.surface else FrontendColors.Border,
        card = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground,
        modeBackground = if (isDark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.quizImage.isNullOrBlank()) {
                    RemoteImage(
                        imageUrl = item.quizImage,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = item.quizName?.takeIf(String::isNotBlank) ?: item.sessionName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(color = colors.modeBackground, shape = CircleShape) {
                    Text(
                        text = item.gameMode.activityLabel(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
                Text(
                    text = formatActivityTime(item.endedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showHost && !item.hostName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(item.hostAvatar, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Host: ${item.hostName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            content(colors)
        }
    }
}

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    modifier: Modifier,
    colors: ActivityCardColors,
    accent: Boolean = false
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (accent) colors.accent else colors.foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun GameMode.activityLabel(): String = when (this) {
    GameMode.CLASSIC -> "Cổ điển"
    GameMode.SOLO -> "Solo"
    GameMode.SURVIVAL -> "Sinh tồn"
    GameMode.MARATHON -> "Marathon"
    GameMode.PRACTICE -> "Luyện tập"
}

private fun formatActivityTime(raw: String): String = runCatching {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.parse(raw).atZone(zone)
    val daysAgo = ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDate.now(zone))
    when {
        daysAgo == 0L -> "Hôm nay, ${activityTimeFormatter.format(dateTime)}"
        daysAgo == 1L -> "Hôm qua"
        daysAgo in 2L..6L -> "$daysAgo ngày trước"
        else -> activityDateFormatter.format(dateTime)
    }
}.getOrDefault(raw)

private val activityTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val activityDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
private val NumberFormatInteger = java.text.NumberFormat.getIntegerInstance(Locale.US)
