package android.kma.myquizzapp.feature.game_player.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact personal summary: no duplicated question, answer sheet or room leaderboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassicResultsScreenContent(
    state: GameUiState,
    waitText: String,
    onRequestExit: () -> Unit,
    onRetry: () -> Unit,
    listState: LazyListState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val page = if (dark) MaterialTheme.colorScheme.background else FrontendColors.PageBackground
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = page,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("Kết quả câu hỏi", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onRequestExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Rời màn chơi")
                } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = page))
        },
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Timing is first so even a short results window or a large font keeps it visible.
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.question?.let { Text("Câu ${it.index + 1}/${it.total} · Cổ điển",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
                        Text(waitText, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        })
                        if (state.connection == GameConnection.RECONNECT_FAILED) {
                            QuizPrimaryButton("Kết nối lại", onRetry, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (!state.skipsClassicSummary) {
                item { ClassicOutcomeCard(state) }
                if (state.showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS) item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Thành tích của bạn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (!state.isResultsLeaderboardFresh) {
                                Text("Đang chờ máy chủ cập nhật thứ hạng và tổng điểm…",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (state.playerRank == null || state.playerScore == null) {
                                Text("Chưa có thứ hạng của bạn trong dữ liệu vừa nhận.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("#${state.playerRank}", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                        Text("Thứ hạng", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${state.playerScore}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                        Text("Tổng điểm", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Gameplay does not supply explanation, score delta or streak. Do not invent them.
        }
    }
}

@Composable
internal fun ClassicOutcomeCard(state: GameUiState, compact: Boolean = false) {
    val kind = classicResultKind(state)
    val dark = isSystemInDarkTheme()
    val background = when (kind) {
        ClassicResultKind.CORRECT -> if (dark) Color(0xFF123F32) else Color(0xFFE5F8EF)
        ClassicResultKind.INCORRECT -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when (kind) {
        ClassicResultKind.CORRECT -> if (dark) Color(0xFF7FE0B2) else Color(0xFF006B48)
        ClassicResultKind.INCORRECT -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (kind) {
        ClassicResultKind.CORRECT -> Icons.Outlined.CheckCircle
        ClassicResultKind.INCORRECT -> Icons.Outlined.Cancel
        else -> Icons.Outlined.Info
    }
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.padding(if (compact) 16.dp else 24.dp),
            horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!compact) Icon(icon, null, Modifier.size(52.dp))
            Text(classicResultTitle(state), style = if (compact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(classicResultDescription(state), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun classicResultFixture() = GameUiState(connection = GameConnection.CONNECTED,
    mode = GameMode.CLASSIC, phase = GamePhaseUi.Results(), showCorrectAnswer = true,
    showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS, isInputLocked = true,
    question = PublicQuestion(3, 15, 4, "multiple_choice", "Không lặp lại câu hỏi trong tổng kết"),
    results = QuestionResults(3, 4, correctAnswers = listOf("b")), outcome = QuestionOutcome.CORRECT,
    acceptedAnswerKeys = listOf("b"), hasAnswerAttempt = true,
    isResultsLeaderboardFresh = true, playerRank = 4, playerScore = 3420)

@Composable
private fun ClassicResultFixture(state: GameUiState) {
    MyQuizAppTheme { ClassicResultsScreenContent(state, "Câu tiếp theo sẽ bắt đầu sau: 1s", {}, {},
        rememberLazyListState(), remember { SnackbarHostState() }) }
}
@Preview(name = "Kết quả - Đúng", widthDp = 390, showBackground = true)
@Preview(name = "Kết quả - Đúng dark", widthDp = 390, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun ClassicCorrectResultPreview() = ClassicResultFixture(classicResultFixture())
@Preview(name = "Kết quả - Sai, hẹp, chữ lớn", widthDp = 320, fontScale = 1.3f, showBackground = true)
@Composable private fun ClassicIncorrectResultPreview() = ClassicResultFixture(classicResultFixture().copy(outcome = QuestionOutcome.INCORRECT))
@Preview(name = "Kết quả - Chưa trả lời", widthDp = 390, showBackground = true)
@Composable private fun ClassicUnansweredResultPreview() = ClassicResultFixture(classicResultFixture().copy(
    acceptedAnswerKeys = null, hasAnswerAttempt = false, outcome = QuestionOutcome.HIDDEN))
@Preview(name = "Kết quả - Ẩn đáp án, chờ hạng", widthDp = 390, showBackground = true)
@Composable private fun ClassicHiddenResultPreview() = ClassicResultFixture(classicResultFixture().copy(
    showCorrectAnswer = false, outcome = QuestionOutcome.HIDDEN, results = QuestionResults(3), isResultsLeaderboardFresh = false))
@Preview(name = "Kết quả - Reconnect thiếu chi tiết", widthDp = 390, showBackground = true)
@Composable private fun ClassicRestoredResultPreview() = ClassicResultFixture(classicResultFixture().copy(
    phase = GamePhaseUi.Results(restoredWithoutDetails = true), results = null, outcome = null))
