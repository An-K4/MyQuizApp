package android.kma.myquizzapp.core.ui.components

import android.kma.myquizzapp.core.ui.theme.ComponentColors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stateless row: ranks 1–3 render medals; ranking/scoring remains owned by the caller. */
@Composable
fun LeaderboardPlayerCard(
    rankLabel: String,
    playerName: String,
    scoreText: String?,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    initials: String = "",
    scoreDeltaText: String? = null,
    isCurrentPlayer: Boolean = false,
) {
    Surface(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        shape = RoundedCornerShape(16.dp), color = ComponentColors.softSurface) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val rank = when (rankLabel.trim()) {
                "1", "🥇" -> 1
                "2", "🥈" -> 2
                "3", "🥉" -> 3
                else -> null
            }
            val displayRank = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> rankLabel
            }
            Text(
                displayRank,
                modifier = Modifier.widthIn(min = 20.dp).clearAndSetSemantics {
                    contentDescription = "Hạng ${rank ?: rankLabel}"
                },
                color = ComponentColors.foreground,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (avatarUrl.isNullOrBlank()) {
                Surface(shape = CircleShape, color = ComponentColors.selectedSurface, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initials.take(2), color = ComponentColors.selectedForeground,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            } else Avatar(avatarUrl, size = 34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(playerName + if (isCurrentPlayer) " (Bạn)" else "", color = ComponentColors.foreground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold))
                if (!scoreText.isNullOrBlank()) Text(scoreText, color = ComponentColors.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp))
            }
            if (!scoreDeltaText.isNullOrBlank()) Text(scoreDeltaText, color = ComponentColors.successForeground,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Preview(name = "Leaderboard Row Light", showBackground = true, widthDp = 390)
@Preview(name = "Leaderboard Row Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LeaderboardPlayerCardPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LeaderboardPlayerCard("1", "Minh Anh", "4,120 điểm", initials = "MA", scoreDeltaText = "(+950)")
                LeaderboardPlayerCard("2", "Anh Thư", "3,200 điểm", initials = "AT", isCurrentPlayer = true)
                LeaderboardPlayerCard("3", "Tên người chơi dài để kiểm tra bố cục và xuống dòng", null, initials = "TN")
                LeaderboardPlayerCard("4", "Hoàng Nam", "2,100 điểm", initials = "HN")
            }
        }
    }
}

