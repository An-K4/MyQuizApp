package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.ui.components.QuestionImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun QuizPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizPreviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                QuizPreviewEffect.Exit -> onNavigateBack()
                is QuizPreviewEffect.EditQuiz -> onNavigateToEditQuiz(effect.quizId)
            }
        }
    }
    QuizPreviewScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPreviewScreenContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tự chơi thử") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Thoát chơi thử")
                    }
                },
                actions = {
                    if (!state.isLoading && state.quiz != null) {
                        TextButton(onClick = { onIntent(QuizPreviewIntent.Exit) }) { Text("Thoát") }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Centered(Modifier.padding(padding)) { CircularProgressIndicator() }
            state.errorMessage != null -> Centered(Modifier.padding(padding)) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onIntent(QuizPreviewIntent.Retry) }) { Text("Thử lại") }
                TextButton(onClick = onNavigateBack) { Text("Quay lại") }
            }
            state.quiz?.questions.isNullOrEmpty() -> Centered(Modifier.padding(padding)) {
                Text("Quiz này chưa có câu hỏi để chơi thử.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onNavigateBack) { Text("Quay lại quiz") }
            }
            else -> when (state.phase) {
                PreviewPhase.COUNTDOWN -> CountdownContent(state, Modifier.padding(padding))
                PreviewPhase.QUESTION -> QuestionContent(state, onIntent, Modifier.padding(padding))
                PreviewPhase.FEEDBACK -> FeedbackContent(state, onIntent, Modifier.padding(padding))
                PreviewPhase.FINISHED -> SummaryContent(state, onIntent, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun CountdownContent(state: QuizPreviewUiState, modifier: Modifier) {
    Centered(modifier) {
        Text("Chuẩn bị", style = MaterialTheme.typography.titleLarge)
        Text("${state.countdownSeconds}", style = MaterialTheme.typography.displayLarge)
        state.quiz?.let { Text(it.quizName, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun QuestionContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    modifier: Modifier
) {
    val question = state.currentQuestion ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Câu ${state.currentIndex + 1}/${state.quiz?.questions?.size}")
                Text(
                    if (state.isLate) "Đã hết giờ" else "${state.remainingSeconds}s",
                    color = if (state.isLate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        item {
            Text(question.questionText, style = MaterialTheme.typography.headlineSmall)
            QuestionImage(
                imageUrl = question.questionImage,
                contentDescription = "Ảnh minh họa câu hỏi",
                modifier = Modifier.padding(top = 12.dp)
            )
            question.questionHint?.takeIf(String::isNotBlank)?.let {
                Text("Gợi ý: $it", modifier = Modifier.padding(top = 8.dp))
            }
            if (state.isLate) {
                Text(
                    "Bạn vẫn có thể trả lời, nhưng câu đúng chỉ nhận 90% điểm cơ bản.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE -> items(question.answerOptions.orEmpty(), key = { it.id }) { option ->
                Card(
                    onClick = { onIntent(QuizPreviewIntent.SelectOption(option.id)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = option.id in state.selectedOptionIds, onClick = null)
                        Text(option.optionText)
                    }
                }
            }
            QuestionType.MULTIPLE_SELECT -> items(question.answerOptions.orEmpty(), key = { it.id }) { option ->
                Card(
                    onClick = { onIntent(QuizPreviewIntent.ToggleOption(option.id)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = option.id in state.selectedOptionIds, onCheckedChange = null)
                        Text(option.optionText)
                    }
                }
            }
            QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER -> item {
                OutlinedTextField(
                    value = state.textAnswer,
                    onValueChange = { onIntent(QuizPreviewIntent.ChangeText(it)) },
                    label = { Text("Câu trả lời") },
                    minLines = if (question.questionType == QuestionType.LONG_ANSWER) 4 else 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (question.questionType != QuestionType.MULTIPLE_CHOICE) {
            item {
                Button(
                    onClick = { onIntent(QuizPreviewIntent.Submit) },
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Gửi đáp án") }
            }
        }
        item {
            TextButton(
                onClick = { onIntent(QuizPreviewIntent.Skip) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bỏ qua câu này") }
        }
    }
}

@Composable
private fun FeedbackContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    modifier: Modifier
) {
    val result = state.currentResult ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Câu ${state.currentIndex + 1}/${state.quiz?.questions?.size}") }
        item {
            Text(result.question.questionText, style = MaterialTheme.typography.headlineSmall)
            QuestionImage(
                imageUrl = result.question.questionImage,
                contentDescription = "Ảnh minh họa câu hỏi",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (result.isCorrect) {
                            true -> "Chính xác!"
                            false -> if (result.isAnswered) "Chưa chính xác" else "Bạn đã bỏ qua câu này"
                            null -> "Không thể chấm câu hỏi này"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (result.scoreEarned > 0) Text("+${result.scoreEarned} điểm")
                    if (result.isLate) Text("Đáp án được gửi sau thời hạn.")
                    if (result.isCorrect != true) {
                        result.correctAnswer?.let { Text("Đáp án đúng: $it") }
                    }
                    result.question.explanation?.takeIf(String::isNotBlank)?.let {
                        Text("Giải thích: $it")
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onIntent(QuizPreviewIntent.Next) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.currentIndex + 1 >= (state.quiz?.questions?.size ?: 0)) "Xem tổng kết" else "Câu tiếp theo")
            }
        }
    }
}

@Composable
private fun SummaryContent(
    state: QuizPreviewUiState,
    onIntent: (QuizPreviewIntent) -> Unit,
    modifier: Modifier
) {
    val ordered = state.results.sortedBy {
        when {
            it.isCorrect == false && it.isAnswered -> 0
            !it.isAnswered -> 1
            it.isCorrect == null -> 2
            else -> 3
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hoàn thành lượt chơi thử", style = MaterialTheme.typography.headlineSmall)
            Text("Không có phòng hoặc kết quả nào được lưu lên máy chủ.")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Điểm ước tính: ${state.totalScore}", style = MaterialTheme.typography.titleLarge)
                    Text("Đúng: ${state.correctCount}/${state.quiz?.questions?.size}")
                    Text("Đã trả lời: ${state.answeredCount}/${state.quiz?.questions?.size}")
                    Text("Độ chính xác: ${state.accuracy}%")
                    Text("Thời gian: ${state.elapsedSeconds}s")
                    Text(
                        "Điểm được tính cục bộ theo cấu hình Classic mặc định và có thể khác trận thật.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item { Text("Xem lại câu trả lời", style = MaterialTheme.typography.titleMedium) }
        items(ordered, key = { it.question.id }) { result -> PreviewReviewItem(result) }
        item {
            Button(
                onClick = { onIntent(QuizPreviewIntent.Restart) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Chơi lại") }
        }
        if (state.isOwner) {
            item {
                OutlinedButton(
                    onClick = { onIntent(QuizPreviewIntent.EditQuiz) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Chỉnh sửa quiz") }
            }
        }
        item {
            TextButton(
                onClick = { onIntent(QuizPreviewIntent.Exit) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Thoát chơi thử") }
        }
    }
}

@Composable
private fun PreviewReviewItem(result: PreviewQuestionResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.question.questionText, style = MaterialTheme.typography.titleSmall)
            Text(
                when (result.isCorrect) {
                    true -> "Đúng • +${result.scoreEarned} điểm"
                    false -> if (result.isAnswered) "Sai" else "Bỏ qua"
                    null -> "Không thể chấm"
                }
            )
            result.submittedAnswer?.let { Text("Bạn trả lời: $it") }
            result.correctAnswer?.let { Text("Đáp án đúng: $it") }
            result.question.explanation?.takeIf(String::isNotBlank)?.let { Text(it) }
        }
    }
}
