package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/** History-only review card. Final Result keeps its existing ReviewItemCard. */
@Composable
internal fun HistoryReviewQuestionCard(item: GameReviewItem, modifier: Modifier = Modifier) {
    val colors = historyDetailColors()
    val review = item.toHistoryReviewUi()
    val statusColor = when (review.tone) {
        HistoryAnswerTone.Correct -> colors.correct
        HistoryAnswerTone.Incorrect -> colors.incorrect
        HistoryAnswerTone.Neutral -> colors.muted
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Câu ${item.questionIndex + 1}: ${item.questionText?.takeIf(String::isNotBlank) ?: "Nội dung câu hỏi không còn khả dụng"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground
            )
            // Directly below the question text; no image means no placeholder block.
            QuestionImage(
                imageUrl = item.questionImage,
                contentDescription = "Ảnh minh họa câu ${item.questionIndex + 1}",
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (review.tone) {
                        HistoryAnswerTone.Correct -> Icons.Outlined.CheckCircle
                        HistoryAnswerTone.Incorrect -> Icons.Outlined.Cancel
                        HistoryAnswerTone.Neutral -> Icons.Outlined.RemoveCircleOutline
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(review.statusText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = statusColor)
            }
            review.answers.forEach { answer -> HistoryReviewAnswer(answer, colors) }
            review.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.muted) }
            item.explanation?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
            val details = buildList {
                if (item.isLate) add("Trả lời muộn")
                item.timeTakenSeconds?.let { add("${String.format(Locale.getDefault(), "%.2f", it)} giây") }
            }.joinToString(" • ")
            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
    }
}

@Composable
private fun HistoryReviewAnswer(answer: HistoryAnswerUi, colors: HistoryDetailColors) {
    val foreground = when (answer.tone) {
        HistoryAnswerTone.Correct -> colors.correct
        HistoryAnswerTone.Incorrect -> colors.incorrect
        HistoryAnswerTone.Neutral -> colors.foreground
    }
    val background = when (answer.tone) {
        HistoryAnswerTone.Correct -> colors.correctSurface
        HistoryAnswerTone.Incorrect -> colors.incorrectSurface
        HistoryAnswerTone.Neutral -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            stateDescription = when (answer.tone) {
                HistoryAnswerTone.Correct -> "Đáp án đúng"
                HistoryAnswerTone.Incorrect -> "Câu trả lời sai"
                HistoryAnswerTone.Neutral -> "Chưa có đáp án đối chiếu"
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = background
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                answer.caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = foreground) }
                Text(
                    text = answer.optionLabel?.let { "$it. ${answer.text}" } ?: answer.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground
                )
            }
            if (answer.tone != HistoryAnswerTone.Neutral) {
                Icon(
                    if (answer.tone == HistoryAnswerTone.Correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = foreground
                )
            }
        }
    }
}

@Preview(name = "Review — chọn một, đúng, có ảnh", showBackground = true, widthDp = 390)
@Preview(name = "Review — tối", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistorySingleCorrectPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[0])

@Preview(name = "Review — chọn một, sai", showBackground = true, widthDp = 390)
@Composable
private fun HistorySingleWrongPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[0].copy(
    questionImage = null, questionText = "Ai là người phát minh ra động cơ hơi nước?",
    answerOptions = listOf(PublicAnswerOption("a", "Isaac Newton"), PublicAnswerOption("b", "Thomas Edison"), PublicAnswerOption("c", "James Watt")),
    yourAnswers = listOf("b"), correctAnswers = listOf("c"), isCorrect = false, scoreEarned = 0,
    explanation = "James Watt đã hoàn thiện động cơ hơi nước năm 1769."
))

@Preview(name = "Review — chọn nhiều, sai một lựa chọn", showBackground = true, widthDp = 390)
@Composable
private fun HistoryMultipleWrongPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[1])

@Preview(name = "Review — chọn nhiều, thiếu lựa chọn đúng", showBackground = true, widthDp = 390)
@Composable
private fun HistoryMultipleIncompletePreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[1].copy(yourAnswers = listOf("2")))

@Preview(name = "Review — trả lời ngắn, đúng", showBackground = true, widthDp = 390)
@Composable
private fun HistoryShortAnswerPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[2])

@Preview(name = "Review — trả lời dài, sai, màn hẹp", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun HistoryLongAnswerPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[3])

@Preview(name = "Review — chưa trả lời", showBackground = true, widthDp = 390)
@Composable
private fun HistorySkippedPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[4])

@Preview(name = "Review — thiếu đáp án đối chiếu", showBackground = true, widthDp = 390)
@Composable
private fun HistoryMissingKeyPreview() = HistoryReviewPreview(HistoryDetailPreviewData.items[0].copy(
    questionImage = null, correctAnswers = emptyList(), isCorrect = false, scoreEarned = 0
))

@Composable
private fun HistoryReviewPreview(item: GameReviewItem) {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistoryReviewQuestionCard(item, Modifier.padding(20.dp))
        }
    }
}
