package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Host aggregate only. The endpoint does not supply question text, answers or explanation. */
@Composable
internal fun HistoryQuestionStatCard(stat: QuestionStat, modifier: Modifier = Modifier) {
    val colors = historyDetailColors()
    val accuracy = historyAccuracy(stat.correctCount, stat.answerCount)
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.card) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = colors.summary) {
                Box(
                    modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${stat.questionIndex + 1}",
                        modifier = Modifier.clearAndSetSemantics { contentDescription = "Câu ${stat.questionIndex + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(accuracy.text, style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                val progress = accuracy.progress
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .then(if (progress != null) Modifier.progressSemantics(progress) else Modifier)
                        .clip(CircleShape).background(colors.track)
                ) {
                    if (progress != null && progress > 0f) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(FrontendColors.Success))
                    }
                }
            }
        }
    }
}

@Preview(name = "Thống kê Host — đúng, sai, chưa có lượt", showBackground = true, widthDp = 390)
@Preview(name = "Thống kê Host — tối", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Thống kê Host — hẹp/chữ lớn", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun HistoryQuestionStatsPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HistoryQuestionStatCard(QuestionStat(101, 0, 100, 59))
                HistoryQuestionStatCard(QuestionStat(102, 1, 15, 0))
                HistoryQuestionStatCard(QuestionStat(103, 2, 0, 0))
            }
        }
    }
}
