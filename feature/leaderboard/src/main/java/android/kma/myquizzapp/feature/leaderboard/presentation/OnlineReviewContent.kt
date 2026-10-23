package android.kma.myquizzapp.feature.leaderboard.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.ui.components.AnswerOptionFeedback
import android.kma.myquizzapp.core.ui.components.AnswerOptionItem
import android.kma.myquizzapp.core.ui.components.QuestionImage
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.leaderboard.presentation.historydetail.historyDetailColors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Stateless Player-only review; the final-result screen retains permissions/loading/scroll. */
@Composable
internal fun OnlineReviewItemCard(item: GameReviewItem, mode: GameMode?, modifier: Modifier = Modifier) {
    val review = item.toOnlineReviewCardUi(mode)
    val foreground = historyDetailColors().foreground
    val statusColor = review.tone.foreground()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = review.tone.background()),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(review.heading, Modifier.weight(1f), color = foreground,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = review.tone.label }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(when (review.tone) {
                            OnlineReviewTone.Correct -> Icons.Outlined.CheckCircle
                            OnlineReviewTone.Incorrect -> Icons.Outlined.Cancel
                            else -> Icons.Outlined.RemoveCircleOutline
                        }, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                        Text(review.tone.label, color = statusColor, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(review.questionText, color = foreground, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            QuestionImage(item.questionImage, "Ảnh minh họa ${review.heading.lowercase()}")
            review.options.forEach { option ->
                AnswerOptionItem(
                    label = option.label,
                    text = option.displayText,
                    selected = false,
                    onClick = {},
                    readOnly = true,
                    showReadOnlyFeedback = true,
                    feedback = when (option.tone) {
                        OnlineReviewTone.Correct -> AnswerOptionFeedback.Correct
                        OnlineReviewTone.Incorrect -> AnswerOptionFeedback.Incorrect
                        else -> AnswerOptionFeedback.None
                    },
                )
            }
            review.responses.forEach { response ->
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, response.tone.foreground().copy(alpha = 0.3f))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(response.label, color = response.tone.foreground(),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(response.text, color = foreground, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            review.notices.forEach { notice ->
                Text(notice, color = foreground, style = MaterialTheme.typography.bodySmall)
            }
            review.explanation?.let { explanation ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Giải thích", color = foreground, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text(explanation, color = foreground, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val details = listOfNotNull(review.scoreText, review.timeText,
                "Trả lời muộn".takeIf { review.isLate }).joinToString(" • ")
            if (details.isNotBlank()) Text(details, color = foreground, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OnlineReviewTone.background(): Color = when (this) {
    OnlineReviewTone.Correct -> historyDetailColors().correctSurface
    OnlineReviewTone.Incorrect -> historyDetailColors().incorrectSurface
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun OnlineReviewTone.foreground(): Color = when (this) {
    OnlineReviewTone.Correct -> historyDetailColors().correct
    OnlineReviewTone.Incorrect -> historyDetailColors().incorrect
    else -> historyDetailColors().muted
}

// Fixtures are Preview-only, never substitutes for missing review data at runtime.
private fun reviewFixture() = GameReviewItem(
    questionIndex = 0, questionId = 10,
    questionText = "Quốc gia có diện tích lớn nhất thế giới?",
    answerOptions = listOf(PublicAnswerOption("a", "Canada"), PublicAnswerOption("b", "Liên bang Nga"),
        PublicAnswerOption("c", "Trung Quốc")),
    answered = true, yourAnswers = listOf("b"), correctAnswers = listOf("b"),
    isCorrect = true, isLate = false, scoreEarned = 950, timeTakenSeconds = 2.35,
    explanation = "Liên bang Nga có diện tích lớn nhất thế giới.",
)

@Composable
private fun ReviewCardPreview(item: GameReviewItem = reviewFixture(), mode: GameMode = GameMode.CLASSIC) {
    MyQuizAppTheme {
        Surface {
            OnlineReviewItemCard(item, mode, Modifier.padding(20.dp))
        }
    }
}

@Preview(name = "Online Review · Đúng", widthDp = 390, showBackground = true)
@Preview(name = "Online Review · Dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnlineReviewCorrectPreview() = ReviewCardPreview()

@Preview(name = "Online Review · Sai, đã chọn", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewIncorrectPreview() = ReviewCardPreview(reviewFixture().copy(yourAnswers = listOf("a"), isCorrect = false, scoreEarned = 0))

@Preview(name = "Online Review · Nhiều lựa chọn, chọn thiếu", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewPartialPreview() = ReviewCardPreview(reviewFixture().copy(
    questionText = "Chọn tất cả các đáp án đúng", correctAnswers = listOf("a", "b"),
    yourAnswers = listOf("a"), isCorrect = false, scoreEarned = 0, explanation = null))

@Preview(name = "Online Review · Bỏ qua", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewSkippedPreview() = ReviewCardPreview(reviewFixture().copy(answered = false,
    yourAnswers = emptyList(), isCorrect = false, scoreEarned = 0, timeTakenSeconds = null))

@Preview(name = "Online Review · Câu nhập chữ", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewTextPreview() = ReviewCardPreview(reviewFixture().copy(answerOptions = emptyList(),
    questionText = "Viết tên quốc gia có diện tích lớn nhất thế giới", yourAnswers = listOf("Canada"),
    correctAnswers = listOf("Liên bang Nga"), isCorrect = false, scoreEarned = 0))

@Preview(name = "Online Review · Practice không điểm", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewPracticePreview() = ReviewCardPreview(mode = GameMode.PRACTICE)

@Preview(name = "Online Review · Marathon lượt 21", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewMarathonPreview() = ReviewCardPreview(reviewFixture().copy(questionIndex = 20), GameMode.MARATHON)

@Preview(name = "Online Review · Ảnh fallback", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewImagePreview() = ReviewCardPreview(reviewFixture().copy(questionImage = "https://preview.invalid/question.png"))

@Preview(name = "Online Review · Thiếu dữ liệu", widthDp = 390, showBackground = true)
@Composable
private fun OnlineReviewMissingPreview() = ReviewCardPreview(reviewFixture().copy(questionText = null,
    yourAnswers = listOf("removed-option"), correctAnswers = emptyList(), isCorrect = false, explanation = null))

@Preview(name = "Online Review · Hẹp, chữ lớn", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun OnlineReviewLargeTextPreview() = ReviewCardPreview(reviewFixture().copy(
    questionText = "Một nội dung câu hỏi rất dài để kiểm tra việc xuống dòng, không cắt mất thông tin trên màn hình nhỏ.",
    answerOptions = reviewFixture().answerOptions.map { it.copy(text = "Nội dung đáp án dài cần hiển thị đầy đủ, kể cả khi tăng cỡ chữ.") }))
