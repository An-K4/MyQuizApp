package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility
import android.kma.myquizzapp.core.common.model.QuizSummary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizManageListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizManageListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val quizzes = viewModel.quizzes.collectAsLazyPagingItems()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Quiz của tôi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateQuiz) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo quiz mới")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = { viewModel.handleIntent(QuizManageListIntent.KeywordChanged(it)) },
                placeholder = { Text("Tìm quiz của tôi...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibilityChip(
                    label = "Tất cả",
                    selected = uiState.visibility == MyQuizzesVisibility.ALL,
                    onClick = { viewModel.handleIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.ALL)) }
                )
                VisibilityChip(
                    label = "Công khai",
                    selected = uiState.visibility == MyQuizzesVisibility.PUBLIC,
                    onClick = { viewModel.handleIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.PUBLIC)) }
                )
                VisibilityChip(
                    label = "Riêng tư",
                    selected = uiState.visibility == MyQuizzesVisibility.PRIVATE,
                    onClick = { viewModel.handleIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.PRIVATE)) }
                )
                Spacer(modifier = Modifier.weight(1f))
                SortMenuButton(
                    sort = uiState.sort,
                    onSortSelected = { viewModel.handleIntent(QuizManageListIntent.SortChanged(it)) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                quizzes.loadState.refresh is LoadState.Loading && quizzes.itemCount == 0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                quizzes.loadState.refresh is LoadState.Error && quizzes.itemCount == 0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Không tải được danh sách quiz của bạn.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { quizzes.retry() }) { Text("Thử lại") }
                    }
                }
                quizzes.itemCount == 0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Bạn chưa có quiz nào. Nhấn + để tạo quiz đầu tiên.")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = quizzes.itemCount,
                            key = quizzes.itemKey { it.id }
                        ) { index ->
                            val quiz = quizzes[index]
                            if (quiz != null) {
                                QuizManageListItem(
                                    quiz = quiz,
                                    onClick = { onNavigateToQuizDetail(quiz.id) }
                                )
                            }
                        }
                        if (quizzes.loadState.append is LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        if (quizzes.loadState.append is LoadState.Error) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Tải thêm thất bại.")
                                    Button(onClick = { quizzes.retry() }) { Text("Thử lại") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SortMenuButton(sort: MyQuizzesSort, onSortSelected: (MyQuizzesSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = "Sắp xếp")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Cập nhật gần đây") },
                onClick = { onSortSelected(MyQuizzesSort.RECENTLY_UPDATED); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Mới nhất") },
                onClick = { onSortSelected(MyQuizzesSort.NEWEST); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Cũ nhất") },
                onClick = { onSortSelected(MyQuizzesSort.OLDEST); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Tên A-Z") },
                onClick = { onSortSelected(MyQuizzesSort.NAME_ASC); expanded = false }
            )
        }
    }
}

@Composable
private fun QuizManageListItem(quiz: QuizSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = quiz.quizImage,
                contentDescription = quiz.quizName,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quiz.quizName)
                Text(text = "${quiz.questionCount} câu hỏi • ${quiz.playCount} lần chơi")
            }
            AssistChip(
                onClick = {},
                label = { Text(if (quiz.isPublic) "Công khai" else "Riêng tư") }
            )
        }
    }
}
