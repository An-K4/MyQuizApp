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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                QuizDetailContent(
                    quiz = uiState.quiz!!,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun QuizDetailContent(
    quiz: Quiz,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
