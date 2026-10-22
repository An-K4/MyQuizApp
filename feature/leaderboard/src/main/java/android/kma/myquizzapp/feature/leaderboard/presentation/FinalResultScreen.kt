package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.ui.components.GameFinalResultHero
import android.kma.myquizzapp.core.ui.components.GameFinalResultSummaryCard
import android.kma.myquizzapp.core.ui.components.LeaderboardPlayerCard
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun FinalResultScreen(
    onHome: () -> Unit,
    onResourceMissing: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinalResultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var boardExpanded by rememberSaveable(state.gameId) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FinalResultEffect.ResourceMissing -> {
                    viewModel.consume()
                    onResourceMissing(effect.message)
                }
            }
        }
    }
    LaunchedEffect(state.isReviewVisible) {
        if (state.isReviewVisible && state.reviewEnabled && !state.isMissing) {
            // Summary is always item 0; review heading is item 1 when visible.
            listState.animateScrollToItem(1)
        }
    }
    FinalResultScreenContent(
        state = state,
        onIntent = viewModel::handleIntent,
        onHome = { viewModel.consume(); onHome() },
        listState = listState,
        boardExpanded = boardExpanded,
        onToggleBoard = { boardExpanded = !boardExpanded },
        modifier = modifier
    )
}

/** Pure rendering; saveable disclosure and scroll state belong to the Screen. */
@Composable
fun FinalResultScreenContent(
    state: FinalResultUiState,
    onIntent: (FinalResultIntent) -> Unit,
    onHome: () -> Unit,
    listState: LazyListState,
    boardExpanded: Boolean,
    onToggleBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val standings = finalResultRemainingStandings(state)
    val player = finalResultPlayer(state)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.reviewEnabled && !state.isMissing) {
                        QuizSecondaryButton(
                            text = if (state.isReviewVisible) "Ẩn phần xem lại" else "Xem lại chi tiết từng câu trả lời",
                            onClick = { onIntent(FinalResultIntent.ToggleReview) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    QuizPrimaryButton("Về trang chủ", onHome, Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(Color(0xFF211238), Color(0xFF51318A), Color(0xFF34205F)))
        )) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "finish-summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        GameFinalResultHero(entries = finalResultPodium(state))
                        when {
                            state.isResultLoading -> ResultNotice("Đang tải lại kết quả…", loading = true)
                            state.resultError != null -> ResultNotice(
                                state.resultError,
                                actionLabel = "Thử tải lại kết quả",
                                onAction = { onIntent(FinalResultIntent.RetryResult) }
                            )
                            state.isMissing -> ResultNotice("Không tìm thấy kết quả của trận này.")
                            state.isPractice -> ResultNotice("Chế độ luyện tập không tính điểm và không có bảng xếp hạng.")
                            state.isLeaderboardHidden -> ResultNotice(
                                if (state.leaderboardHiddenByConfig) "Host đã ẩn bảng xếp hạng của trận này."
                                else "Chưa có dữ liệu bảng xếp hạng cho trận này."
                            )
                            player != null -> GameFinalResultSummaryCard(
                                title = "Thành tích của bạn (${player.playerName})",
                                metrics = finalResultMetrics(state)
                            )
                            else -> ResultNotice("Chưa có thành tích cá nhân của bạn trong kết quả này.")
                        }
                    }
                }
                if (state.reviewEnabled && !state.isMissing && state.isReviewVisible) {
                    item(key = "review-heading") {
                        Text("Xem lại câu trả lời", color = Color.White,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    when {
                        state.isReviewLoading -> item(key = "review-loading") {
                            ResultNotice("Đang tải câu trả lời…", loading = true)
                        }
                        state.reviewError != null -> item(key = "review-error") {
                            ResultNotice(state.reviewError, "Thử lại", { onIntent(FinalResultIntent.RetryReview) })
                        }
                        state.review != null -> {
                            item(key = "review-summary") {
                                ResultNotice("${state.review.correctAnswersCount}/${state.review.totalQuestions} câu đúng • " +
                                    "${state.review.answeredCount} lượt trả lời")
                            }
                            if (state.reviewItems.isEmpty()) {
                                item(key = "review-empty") { ResultNotice("Không có chi tiết câu trả lời để hiển thị.") }
                            }
                            items(state.reviewItems, key = { "review-${it.questionIndex}-${it.questionId}" }) {
                                ReviewItemCard(it)
                            }
                        }
                    }
                }
                if (standings.isNotEmpty()) {
                    item(key = "board-toggle") {
                        ResultDisclosure("Bảng xếp hạng", boardExpanded, onToggleBoard)
                    }
                    if (boardExpanded) {
                        items(standings, key = { "player-${it.id}" }) { row ->
                            LeaderboardPlayerCard(
                                rankLabel = "#${row.rank}", playerName = row.playerName,
                                scoreText = "${formatFinalResultScore(row.playerScore)} điểm",
                                initials = row.playerName.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
                                    .take(2).joinToString("") { it.take(1).uppercase() },
                                isCurrentPlayer = row.id == state.playerId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultNotice(
    text: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    loading: Boolean = false
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator()
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null) QuizSecondaryButton(actionLabel, onAction, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultDisclosure(title: String, expanded: Boolean, onClick: () -> Unit) {
    QuizSecondaryButton(
        text = title, onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = if (expanded) "Đang mở" else "Đang thu gọn" },
        trailingIcon = { Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge) }
    )
}

@Composable
internal fun ReviewItemCard(item: GameReviewItem) {
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
internal fun ReviewOptionRow(item: GameReviewItem, option: PublicAnswerOption) {
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


// Preview-only fixtures; never used by the runtime result/review flow.
private fun previewFinalResult() = FinalResultUiState(
    gameId = 91, playerId = 4, mode = GameMode.CLASSIC,
    result = GameEnded(
        leaderboard = listOf(
            LeaderboardRow(1, 1, "Minh Anh", 12450),
            LeaderboardRow(2, 2, "Quang Huy", 11200),
            LeaderboardRow(3, 3, "Tuấn Kiệt", 10850),
            LeaderboardRow(4, 4, "Quốc Tuấn", 9650, correctAnswersCount = 13)
        ),
        reviewEnabled = true
    )
)

@Composable
private fun FinalResultPreview(state: FinalResultUiState = previewFinalResult(), dark: Boolean = false) {
    val listState = rememberLazyListState()
    MyQuizAppTheme(darkTheme = dark) {
        FinalResultScreenContent(state, {}, {}, listState, false, {})
    }
}

@Preview(name = "Player Final · Light", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PlayerFinalLightPreview() = FinalResultPreview()

@Preview(name = "Player Final · Dark", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PlayerFinalDarkPreview() = FinalResultPreview(dark = true)

@Preview(name = "Player Final · Narrow & large text", widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Composable
private fun PlayerFinalNarrowPreview() {
    val state = previewFinalResult()
    FinalResultPreview(state.copy(result = state.result!!.copy(leaderboard = state.leaderboard.map {
        it.copy(playerName = "Nguyễn Hoàng Minh Anh tên rất dài")
    })))
}

@Preview(name = "Player Final · One player", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalOnePlayerPreview() {
    val state = previewFinalResult()
    FinalResultPreview(state.copy(playerId = 1, result = state.result!!.copy(leaderboard = state.leaderboard.take(1))))
}

@Preview(name = "Player Final · Hidden", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalHiddenPreview() = FinalResultPreview(previewFinalResult().copy(leaderboardHiddenByConfig = true))

@Preview(name = "Player Final · Loading", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalLoadingPreview() = FinalResultPreview(previewFinalResult().copy(result = null, isResultLoading = true))

@Preview(name = "Player Final · Result error", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalErrorPreview() = FinalResultPreview(previewFinalResult().copy(result = null, resultError = "Không thể tải kết quả. Kiểm tra kết nối rồi thử lại."))

@Preview(name = "Player Final · Review", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalReviewPreview() = FinalResultPreview(previewFinalResult().copy(
    isReviewVisible = true,
    review = GameReview(playerScore = 9650, correctAnswersCount = 13, totalQuestions = 15, answeredCount = 15,
        items = listOf(GameReviewItem(questionIndex = 0, questionId = 10, questionText = "Quốc gia có diện tích lớn nhất thế giới?",
            answerOptions = listOf(PublicAnswerOption("A", "Canada"), PublicAnswerOption("B", "Liên bang Nga")),
            answered = true, yourAnswers = listOf("B"), correctAnswers = listOf("B"),
            isCorrect = true, isLate = false, scoreEarned = 950)))
))

@Preview(name = "Player Final · Review unavailable", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalReviewErrorPreview() = FinalResultPreview(previewFinalResult().copy(
    isReviewVisible = true, reviewError = "Phiên xem lại không còn sau khi ứng dụng được khởi động lại."
))

@Preview(name = "Player Final · Network practice", widthDp = 390, heightDp = 844)
@Composable
private fun PlayerFinalPracticePreview() = FinalResultPreview(previewFinalResult().copy(mode = GameMode.PRACTICE))
