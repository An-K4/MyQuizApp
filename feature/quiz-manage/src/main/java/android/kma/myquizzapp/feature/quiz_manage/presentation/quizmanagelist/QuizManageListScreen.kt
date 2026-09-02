package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.flowOf

@Composable
fun QuizManageListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizManageListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quizzes = viewModel.quizzes.collectAsLazyPagingItems()
    var skipFirstResume by rememberSaveable { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipFirstResume) skipFirstResume = false
        else viewModel.onIntent(QuizManageListIntent.Refresh)
    }

    QuizManageListScreenContent(
        uiState = uiState,
        quizzes = quizzes,
        sortMenuExpanded = sortMenuExpanded,
        onSortMenuExpandedChange = { sortMenuExpanded = it },
        onIntent = viewModel::onIntent,
        onRetry = quizzes::retry,
        onNavigateBack = onNavigateBack,
        onNavigateToCreateQuiz = onNavigateToCreateQuiz,
        onNavigateToQuizDetail = onNavigateToQuizDetail,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizManageListScreenContent(
    uiState: QuizManageListUiState,
    quizzes: LazyPagingItems<QuizSummary>,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onIntent: (QuizManageListIntent) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
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
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = { onIntent(QuizManageListIntent.KeywordChanged(it)) },
                placeholder = { Text("Tìm quiz của tôi...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibilityChip("Tất cả", uiState.visibility == MyQuizzesVisibility.ALL) {
                    onIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.ALL))
                }
                VisibilityChip("Công khai", uiState.visibility == MyQuizzesVisibility.PUBLIC) {
                    onIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.PUBLIC))
                }
                VisibilityChip("Riêng tư", uiState.visibility == MyQuizzesVisibility.PRIVATE) {
                    onIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.PRIVATE))
                }
                Spacer(Modifier.weight(1f))
                SortMenuButtonContent(
                    expanded = sortMenuExpanded,
                    onExpandedChange = onSortMenuExpandedChange,
                    onSortSelected = { onIntent(QuizManageListIntent.SortChanged(it)) }
                )
            }
            Spacer(Modifier.height(4.dp))
            QuizPagingContent(
                quizzes = quizzes,
                onRetry = onRetry,
                onQuizClick = onNavigateToQuizDetail
            )
        }
    }
}

@Composable
private fun QuizPagingContent(
    quizzes: LazyPagingItems<QuizSummary>,
    onRetry: () -> Unit,
    onQuizClick: (Long) -> Unit
) {
    when {
        quizzes.loadState.refresh is LoadState.Loading && quizzes.itemCount == 0 ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        quizzes.loadState.refresh is LoadState.Error && quizzes.itemCount == 0 ->
            CenterMessage("Không tải được danh sách quiz của bạn.", onRetry)
        quizzes.itemCount == 0 ->
            CenterMessage("Bạn chưa có quiz nào. Nhấn + để tạo quiz đầu tiên.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count = quizzes.itemCount, key = quizzes.itemKey { it.id }) { index ->
                quizzes[index]?.let { quiz ->
                    QuizManageListItem(quiz = quiz, onClick = { onQuizClick(quiz.id) })
                }
            }
            if (quizzes.loadState.append is LoadState.Loading) item {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            if (quizzes.loadState.append is LoadState.Error) item {
                CenterMessage("Tải thêm thất bại.", onRetry, Modifier.fillMaxWidth().padding(12.dp))
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        onRetry?.let {
            Spacer(Modifier.height(12.dp))
            Button(onClick = it) { Text("Thử lại") }
        }
    }
}

@Composable
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SortMenuButtonContent(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortSelected: (MyQuizzesSort) -> Unit
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Filled.Sort, contentDescription = "Sắp xếp")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            listOf(
                MyQuizzesSort.RECENTLY_UPDATED to "Cập nhật gần đây",
                MyQuizzesSort.NEWEST to "Mới nhất",
                MyQuizzesSort.OLDEST to "Cũ nhất",
                MyQuizzesSort.NAME_ASC to "Tên A-Z"
            ).forEach { (sort, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSortSelected(sort)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizManageListItem(quiz: QuizSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = quiz.quizImage,
                contentDescription = quiz.quizName,
                modifier = Modifier.size(56.dp),
                fallback = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column(Modifier.weight(1f)) {
                Text(quiz.quizName)
                Text("${quiz.questionCount} câu hỏi • ${quiz.playCount} lần chơi")
            }
            AssistChip(onClick = {}, label = { Text(if (quiz.isPublic) "Công khai" else "Riêng tư") })
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizManageListScreenContentPreview() {
    val quizzes = flowOf(PagingData.empty<QuizSummary>()).collectAsLazyPagingItems()
    MyQuizAppTheme {
        QuizManageListScreenContent(
            uiState = QuizManageListUiState(),
            quizzes = quizzes,
            sortMenuExpanded = false,
            onSortMenuExpandedChange = {},
            onIntent = {},
            onRetry = {},
            onNavigateBack = {},
            onNavigateToCreateQuiz = {},
            onNavigateToQuizDetail = {}
        )
    }
}
