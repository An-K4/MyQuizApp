package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun QuizDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var skipFirstResume by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is QuizDetailEffect.QuizDeleted) onNavigateBack()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipFirstResume) skipFirstResume = false
        else viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
    }

    QuizDetailScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToCreateRoom = onNavigateToCreateRoom,
        onNavigateToEditQuiz = onNavigateToEditQuiz,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreenContent(
    uiState: QuizDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    onIntent: (QuizDetailIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val error = uiState.error
    val quiz = uiState.quiz
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(quiz?.quizName ?: "Chi tiết quiz") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        bottomBar = {
            quiz?.let { currentQuiz ->
                Button(
                    onClick = { onNavigateToCreateRoom(currentQuiz.id) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
                ) { Text("Tạo phòng chơi") }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> CenterContent(Modifier.padding(innerPadding)) { CircularProgressIndicator() }
            error != null -> CenterContent(Modifier.padding(innerPadding)) {
                Text(error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onIntent(QuizDetailIntent.Retry) }) { Text("Thử lại") }
            }
            quiz != null -> QuizDetailContent(
                quiz = quiz,
                isOwner = uiState.isOwner,
                onEditClick = { onNavigateToEditQuiz(quiz.id) },
                onDeleteClick = { onIntent(QuizDetailIntent.DeleteQuizClicked) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (uiState.isConfirmingDelete) {
        DeleteQuizDialog(
            quizName = quiz?.quizName.orEmpty(),
            deleteError = uiState.deleteError,
            isDeleting = uiState.isDeleting,
            onConfirm = { onIntent(QuizDetailIntent.DeleteQuizConfirmed) },
            onDismiss = { onIntent(QuizDetailIntent.DeleteQuizDismissed) }
        )
    }
}

@Composable
private fun CenterContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
private fun DeleteQuizDialog(
    quizName: String,
    deleteError: String?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa quiz này?") },
        text = {
            Column {
                Text("\"$quizName\" và toàn bộ câu hỏi sẽ bị xóa vĩnh viễn, kèm kết quả của mọi phòng đã chơi quiz này. Không thể hoàn tác.")
                deleteError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(
                    if (isDeleting) "Đang xóa..." else "Xóa vĩnh viễn",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Giữ lại") }
        }
    )
}

@Composable
private fun QuizDetailContent(
    quiz: Quiz,
    isOwner: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AsyncImage(
                model = quiz.quizImage,
                contentDescription = quiz.quizName,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                fallback = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
        }
        item { Text(quiz.quizName) }
        item { Text("Tạo bởi: ${quiz.owner?.fullname ?: quiz.quizOwner}") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quiz.quizCategory?.let { category ->
                    AssistChip(onClick = {}, label = { Text(category) })
                }
                AssistChip(onClick = {}, label = { Text(quiz.quizLanguage) })
            }
        }
        item { Text("${quiz.questionCount} câu hỏi • ${quiz.playCount} lần chơi") }
        quiz.quizDescription?.takeIf { it.isNotBlank() }?.let { description ->
            item { Text(description) }
        }
        if (isOwner) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                    Text("Chỉnh sửa")
                }
                TextButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa quiz") }
            }
        }
        items(quiz.questions) { QuestionItem(it) }
    }
}

@Composable
private fun QuestionItem(question: Question) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(question.questionText)
            Spacer(Modifier.height(8.dp))
            question.answerOptions?.forEach { Text("• ${it.optionText}") }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizDetailScreenContentPreview() {
    MyQuizAppTheme {
        QuizDetailScreenContent(
            uiState = QuizDetailUiState(isLoading = true),
            onNavigateBack = {},
            onNavigateToCreateRoom = {},
            onNavigateToEditQuiz = {},
            onIntent = {}
        )
    }
}
