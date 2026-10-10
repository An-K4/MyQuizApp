package android.kma.myquizzapp.feature.home.presentation.search

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.model.QuizOwner
import android.kma.myquizzapp.core.ui.components.QuizListCard
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(listState, uiState.results.size, uiState.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.results.size - 3 &&
                    uiState.canLoadMore
                ) {
                    viewModel.onIntent(SearchIntent.LoadMore)
                }
            }
    }

    SearchScreenContent(
        uiState = uiState,
        focusRequester = focusRequester,
        listState = listState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack,
        onNavigateToQuizDetail = onNavigateToQuizDetail,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    focusRequester: FocusRequester,
    listState: LazyListState,
    onIntent: (SearchIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val searchFieldColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
                        placeholder = { Text("Tìm kiếm quiz...") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = searchFieldColor,
                            unfocusedContainerColor = searchFieldColor,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onIntent(SearchIntent.SubmitSearch) }),
                        trailingIcon = {
                            if (uiState.hasQuery) {
                                IconButton(onClick = { onIntent(SearchIntent.ClearSearch) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa từ khóa")
                                }
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { onIntent(SearchIntent.SubmitSearch) }) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isSearching && !uiState.hasResults -> LoadingSearchContent()
                uiState.error != null && !uiState.hasResults -> SearchErrorContent(
                    message = uiState.error,
                    onRetry = { onIntent(SearchIntent.Retry) }
                )
                !uiState.hasQuery -> SearchHint("Nhập từ khóa để tìm kiếm quiz")
                uiState.hasResults -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results, key = { it.id }) { quiz ->
                        QuizListCard(
                            title = quiz.quizName,
                            metadataText = buildString {
                                append("${quiz.questionCount} câu hỏi • ${quiz.playCount} lượt chơi")
                                quiz.owner?.fullname?.takeIf { it.isNotBlank() }?.let {
                                    append(" • Tạo bởi $it")
                                }
                            },
                            imageUrl = quiz.quizImage,
                            onClick = { onNavigateToQuizDetail(quiz.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uiState.error != null) item(key = "append_error") {
                        SearchErrorMessage(uiState.error, onRetry = { onIntent(SearchIntent.Retry) })
                    }
                    if (uiState.isLoadingMore) item(key = "append_loading") {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                uiState.shouldShowNoResults -> SearchHint(
                    title = "Không tìm thấy kết quả",
                    subtitle = "Thử từ khóa khác"
                )
                else -> SearchHint("Nhấn tìm kiếm để xem kết quả")
            }
        }
    }
}

@Composable
private fun LoadingSearchContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun SearchErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SearchErrorMessage(message, onRetry)
    }
}

@Composable
private fun SearchErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) { Text("Thử lại") }
    }
}

@Composable
private fun SearchHint(title: String, subtitle: String? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Preview fixtures are presentation-only; no Hilt, Flow, network images or navigation.
private val searchPreviewQuizzes = listOf(
    "100 Câu Tiếng Anh Giao Tiếp Thông Dụng", "Từ vựng tiếng Anh mỗi ngày", "Luyện nghe cơ bản"
).mapIndexed { index, title ->
    QuizCard(
        id = index.toLong() + 1, quizName = title, quizLanguage = "vi",
        quizOwnerId = 1, owner = QuizOwner(1, "Anh Thư"), questionCount = 20,
        playCount = 500, completionRate = 0.0, createdAt = "2026-10-10T00:00:00Z"
    )
}

private fun searchPreviewState() = SearchUiState(
    query = "Tiếng Anh giao tiếp", submittedQuery = "Tiếng Anh giao tiếp",
    hasCompletedSearch = true, results = searchPreviewQuizzes, hasMore = false
)

@Composable
private fun SearchPreview(state: SearchUiState) {
    MyQuizAppTheme {
        SearchScreenContent(
            uiState = state,
            focusRequester = remember { FocusRequester() },
            listState = rememberLazyListState(),
            onIntent = {}, onNavigateBack = {}, onNavigateToQuizDetail = {}
        )
    }
}

@Preview(name = "Search Results Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Search Results Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Search Results Narrow", showBackground = true, widthDp = 320, heightDp = 720)
@Composable
private fun SearchResultsPreview() = SearchPreview(searchPreviewState())

@Preview(name = "Search Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SearchLoadingPreview() = SearchPreview(SearchUiState(query = "Tiếng Anh", isSearching = true))

@Preview(name = "Search Empty Results", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SearchEmptyPreview() = SearchPreview(searchPreviewState().copy(results = emptyList()))

@Preview(name = "Search Initial Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SearchErrorPreview() = SearchPreview(SearchUiState(query = "Tiếng Anh", error = "Không thể kết nối. Vui lòng thử lại."))

@Preview(name = "Search Append Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SearchAppendErrorPreview() = SearchPreview(searchPreviewState().copy(
    error = "Không thể tải thêm kết quả. Vui lòng thử lại.", hasMore = true, nextCursor = "preview-next"
))

@Preview(name = "Search Initial Hint", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SearchHintPreview() = SearchPreview(SearchUiState())
