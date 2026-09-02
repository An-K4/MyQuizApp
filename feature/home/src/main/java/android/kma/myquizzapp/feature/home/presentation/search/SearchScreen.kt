package android.kma.myquizzapp.feature.home.presentation.search

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.QuizCardItem
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.input.ImeAction
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
    LaunchedEffect(listState, uiState.results.size, uiState.isLoadingMore, uiState.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.results.size - 3 &&
                    !uiState.isLoadingMore &&
                    uiState.hasMore
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
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onIntent(SearchIntent.SubmitSearch) }),
                        trailingIcon = {
                            if (uiState.hasQuery) {
                                IconButton(onClick = { onIntent(SearchIntent.ClearSearch) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa")
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.results, key = { it.id }) { quiz ->
                        QuizCardItem(
                            quiz = quiz,
                            onClick = { onNavigateToQuizDetail(quiz.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (uiState.isLoadingMore) item {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Không thể tìm kiếm", style = MaterialTheme.typography.bodyLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Thử lại") }
        }
    }
}

@Composable
private fun SearchHint(title: String, subtitle: String? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchScreenContentPreview() {
    MyQuizAppTheme {
        SearchScreenContent(
            uiState = SearchUiState(),
            focusRequester = remember { FocusRequester() },
            listState = rememberLazyListState(),
            onIntent = {},
            onNavigateBack = {},
            onNavigateToQuizDetail = {}
        )
    }
}
