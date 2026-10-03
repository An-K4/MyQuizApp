package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun FinalResultScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinalResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FinalResultScreenContent(
        state = state,
        onIntent = viewModel::handleIntent,
        onHome = { viewModel.consume(); onHome() },
        modifier = modifier
    )
}

@Composable
fun FinalResultScreenContent(
    state: FinalResultUiState,
    onIntent: (FinalResultIntent) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Kết quả trận đấu", style = MaterialTheme.typography.headlineMedium) }
            if (state.isPractice) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "Chế độ luyện tập không tính điểm. Chuỗi đúng giúp bạn theo dõi tiến bộ.",
                            Modifier.padding(16.dp)
                        )
                    }
                }
            }
            when {
                state.isMissing -> item {
                    Text("Kết quả tạm thời không còn sau khi ứng dụng được khởi động lại.")
                }
                state.isLeaderboardHidden -> item {
                    Text(if (state.isPractice) "Luyện tập không có bảng xếp hạng." else "Host đã ẩn bảng xếp hạng của trận này.")
                }
                else -> {
                    state.currentPlayer?.let { me ->
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Thành tích của bạn", style = MaterialTheme.typography.titleMedium)
                                    Text("Hạng ${me.rank} • ${me.playerScore} điểm")
                                    me.correctAnswersCount?.let { Text("$it câu đúng") }
                                    me.streak?.let { Text("Chuỗi đúng: $it") }
                                }
                            }
                        }
                    }
                    items(state.leaderboard, key = { it.id }) { row ->
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "#${row.rank}  ${row.playerName} — ${row.playerScore} điểm" +
                                    if (row.id == state.playerId) "  (Bạn)" else "",
                                Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            if (state.reviewEnabled && !state.isMissing) {
                item {
                    OutlinedButton(
                        onClick = { onIntent(FinalResultIntent.ToggleReview) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isReviewVisible) "Ẩn phần xem lại" else "Xem lại câu trả lời")
                    }
                }
                if (state.isReviewVisible) {
                    when {
                        state.isReviewLoading -> item { CircularProgressIndicator() }
                        state.reviewError != null -> item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(state.reviewError)
                                    OutlinedButton(onClick = { onIntent(FinalResultIntent.RetryReview) }) {
                                        Text("Thử lại")
                                    }
                                }
                            }
                        }
                        state.review != null -> {
                            item {
                                Text(
                                    "${state.review.correctAnswersCount}/${state.review.totalQuestions} câu đúng • " +
                                        "${state.review.answeredCount} lượt trả lời",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            items(state.reviewItems, key = { "${it.questionIndex}-${it.questionId}" }) { item ->
                                ReviewItemCard(item)
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Về trang chủ")
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(item: GameReviewItem) {
    val status = when {
        !item.answered -> "Bỏ qua"
        item.isCorrect -> "Đúng"
        else -> "Sai"
    }
    val statusColor = when {
        item.isCorrect -> MaterialTheme.colorScheme.primaryContainer
        !item.answered -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Câu ${item.questionIndex + 1}", style = MaterialTheme.typography.titleMedium)
                Text(status)
            }
            Text(item.questionText ?: "Nội dung câu hỏi không còn khả dụng")
            item.questionImage?.let { image ->
                AsyncImage(
                    model = image,
                    contentDescription = "Hình minh họa câu hỏi",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    contentScale = ContentScale.Fit
                )
            }
            if (item.answerOptions.isNotEmpty()) {
                item.answerOptions.forEach { option -> ReviewOptionRow(item, option) }
            } else {
                Text("Bạn trả lời: ${displayKeys(item.yourAnswers)}")
                if (!item.isCorrect) Text("Đáp án đúng: ${displayKeys(item.correctAnswers)}")
            }
            item.explanation?.takeIf { it.isNotBlank() }?.let {
                Text("Giải thích: $it", style = MaterialTheme.typography.bodyMedium)
            }
            val details = buildList {
                add("Điểm: ${item.scoreEarned}")
                if (item.isLate) add("Trả lời muộn")
                item.timeTakenSeconds?.let {
                    add("${String.format(Locale.getDefault(), "%.2f", it)} giây")
                }
            }.joinToString(" • ")
            Text(details, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewOptionRow(item: GameReviewItem, option: PublicAnswerOption) {
    val key = option.id.normalized()
    val isMine = item.yourAnswers.any { it.normalized() == key }
    val isCorrect = item.correctAnswers.any { it.normalized() == key }
    val marker = when {
        isCorrect && isMine -> "✓ Bạn chọn • Đáp án đúng"
        isCorrect -> "✓ Đáp án đúng"
        isMine -> "✕ Bạn chọn"
        else -> ""
    }
    Text("${option.text ?: option.id}${if (marker.isBlank()) "" else " — $marker"}")
}

private fun displayKeys(values: List<String>): String =
    values.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Không có"

private fun String.normalized(): String = trim().lowercase()
