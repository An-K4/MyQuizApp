package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun HistorySummaryCard(
    summary: GameHistorySummary,
    currentPlayer: LeaderboardRow?,
    modifier: Modifier = Modifier
) {
    val colors = historyDetailColors()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.summary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary.quiz?.image?.takeIf(String::isNotBlank)?.let { image ->
                RemoteImage(
                    imageUrl = image,
                    contentDescription = summary.quiz?.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                )
            }
            Text(
                text = summary.quiz?.name?.takeIf(String::isNotBlank)
                    ?: summary.sessionName.takeIf(String::isNotBlank) ?: "Trận đấu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )
            summary.quiz?.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    summary.historyRoomLabel()?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.background) {
                    Text(
                        summary.gameMode.historyModeLabel(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }
            formatHistoryTime(summary.finishedAt)?.let {
                // The DTO may fall back to created_at for a cancelled room: no "Kết thúc" label.
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
            summary.sessionStatus.historyStatusLabel()?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = colors.incorrect)
            }
            if (summary.viewer.isHost) {
                HorizontalDivider(color = colors.border)
                Text("Bạn đã tổ chức trận này", style = MaterialTheme.typography.bodyMedium, color = colors.accent)
            } else if (currentPlayer != null) {
                HorizontalDivider(color = colors.border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Thứ hạng", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        val rank = when {
                            currentPlayer.rank <= 0 -> "Chưa xếp hạng"
                            summary.totalPlayers > 0 -> "Hạng ${currentPlayer.rank}/${summary.totalPlayers}"
                            else -> "Hạng ${currentPlayer.rank}"
                        }
                        Text(rank, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.accent)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tổng điểm", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                        Text(
                            "${formatHistoryNumber(currentPlayer.playerScore)} điểm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground
                        )
                    }
                }
                val details = buildList {
                    currentPlayer.correctAnswersCount?.let { add("$it/${summary.totalQuestions} câu đúng") }
                    currentPlayer.streak?.let { add("Chuỗi đúng: $it") }
                }.joinToString(" • ")
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
        }
    }
}

@Preview(name = "Tổng quan trận — Player", showBackground = true, widthDp = 390)
@Preview(name = "Tổng quan trận — tối", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistorySummaryPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistorySummaryCard(
                HistoryDetailPreviewData.summary(), HistoryDetailPreviewData.summary().viewerResult,
                Modifier.padding(20.dp)
            )
        }
    }
}

@Preview(name = "Tổng quan — Host, thiếu mã/ảnh, đã hủy", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun HistorySummaryFallbackPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val summary = HistoryDetailPreviewData.summary(host = true).copy(
                sessionCode = null, sessionStatus = SessionStatus.CANCELLED,
                quiz = HistoryDetailPreviewData.summary().quiz?.copy(image = null)
            )
            HistorySummaryCard(summary, null, Modifier.padding(20.dp))
        }
    }
}
