package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.Quiz
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage

/**
 * Màn Chi tiết quiz. N16 bổ sung:
 * - Nút Chỉnh sửa / Xóa quiz (chỉ hiện với owner, xem QuizDetailViewModel.isOwner).
 * - Dialog xác nhận xóa — hard delete, cảnh báo mất cả lịch sử phòng chơi.
 * - Reload khi ON_RESUME (quay lại từ màn Sửa quiz thấy bản mới ngay).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // N16: quiz đã xóa xong → rời màn detail (trang này chỉ còn 404 nếu ở lại).
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QuizDetailEffect.QuizDeleted -> onNavigateBack()
            }
        }
    }

    // N16: quay lại từ màn Sửa quiz → reload để hiển thị bản mới. Bỏ qua lần resume
    // đầu tiên vì init{} của ViewModel đã load rồi (tránh gọi mạng 2 lần lúc mở màn).
    // Flag phải là rememberSaveable: Navigation dispose composition khi sang màn khác,
    // remember thường bị reset mỗi lần quay lại → reload không bao giờ chạy (bug N16).
    var skipFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipFirstResume) {
            skipFirstResume = false
        } else {
            viewModel.handleIntent(QuizDetailIntent.LoadQuizDetail)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.quiz?.quizName ?: "Chi tiết quiz") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        bottomBar = {
            val quiz = uiState.quiz
            if (quiz != null) {
                Button(
                    onClick = { onNavigateToCreateRoom(quiz.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Text("Tạo phòng chơi")
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = uiState.error ?: "Đã có lỗi xảy ra")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.handleIntent(QuizDetailIntent.Retry) }) {
                        Text("Thử lại")
                    }
                }
            }
            uiState.quiz != null -> {
                val quiz = uiState.quiz!!
                QuizDetailContent(
                    quiz = quiz,
                    isOwner = uiState.isOwner,
                    onEditClick = { onNavigateToEditQuiz(quiz.id) },
                    onDeleteClick = { viewModel.handleIntent(QuizDetailIntent.DeleteQuizClicked) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // N16: dialog xác nhận xóa — HARD DELETE (backend quiz.repository.ts): xóa hẳn
    // quiz + toàn bộ câu hỏi + lịch sử mọi phòng đã chơi. Không thể hoàn tác.
    if (uiState.isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(QuizDetailIntent.DeleteQuizDismissed) },
            title = { Text("Xóa quiz này?") },
            text = {
                Column {
                    Text(
                        "\"${uiState.quiz?.quizName ?: ""}\" và toàn bộ câu hỏi sẽ bị xóa vĩnh viễn, " +
                            "kèm kết quả của mọi phòng đã chơi quiz này. Không thể hoàn tác."
                    )
                    uiState.deleteError?.let { deleteError ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = deleteError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(QuizDetailIntent.DeleteQuizConfirmed) },
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        text = if (uiState.isDeleting) "Đang xóa..." else "Xóa vĩnh viễn",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(QuizDetailIntent.DeleteQuizDismissed) },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Giữ lại")
                }
            }
        )
    }
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
            // N16: fallback/error placeholder — quiz không có cover không hiện ô trống.
            // TODO(polish): thay bằng ảnh mặc định trong res/.
            AsyncImage(
                model = quiz.quizImage,
                contentDescription = quiz.quizName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                fallback = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
        }
        item {
            Text(text = quiz.quizName)
        }
        item {
            Text(text = "Tạo bởi: ${quiz.owner?.fullname ?: quiz.quizOwner}")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { quiz.quizCategory?.let { Text(it) } })
                AssistChip(onClick = {}, label = { Text(quiz.quizLanguage) })
            }
        }
        item {
            Text(text = "${quiz.questionCount} câu hỏi • ${quiz.playCount} lần chơi")
        }
        if (!quiz.quizDescription.isNullOrBlank()) {
            item {
                Text(text = quiz.quizDescription!!)
            }
        }
        // N16: chỉ chủ quiz mới thấy Sửa/Xóa (backend cũng tự chặn non-owner bằng 404).
        if (isOwner) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                        Text("Chỉnh sửa")
                    }
                    TextButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Xóa quiz")
                    }
                }
            }
        }
        items(quiz.questions) { question ->
            QuestionItem(question = question)
        }
    }
}

@Composable
private fun QuestionItem(question: Question) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = question.questionText)
            Spacer(modifier = Modifier.height(8.dp))
            question.answerOptions?.forEach { option ->
                Text(text = "• ${option.optionText}")
            }
        }
    }
}
