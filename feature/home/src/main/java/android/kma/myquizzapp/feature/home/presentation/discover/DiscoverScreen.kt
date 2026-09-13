package android.kma.myquizzapp.feature.home.presentation.discover

import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.ui.components.QuizCardItem
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverFilter
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverQuery
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverSort
import android.kma.myquizzapp.feature.home.domain.discover.resolveDiscoverRequest
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun DiscoverScreen(
    sectionType: String?,
    title: String?,
    topic: String?,
    onNavigateBack: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val request = remember(sectionType, title, topic) {
        resolveDiscoverRequest(sectionType, title, topic)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quizzes = viewModel.quizzes.collectAsLazyPagingItems()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(request) { viewModel.onIntent(DiscoverIntent.Initialize(request)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DiscoverEffect.NavigateToQuizDetail -> onNavigateToQuizDetail(effect.quizId)
            }
        }
    }

    DiscoverScreenContent(
        uiState = uiState,
        quizzes = quizzes,
        sortMenuExpanded = sortMenuExpanded,
        onSortMenuExpandedChange = { sortMenuExpanded = it },
        onIntent = viewModel::onIntent,
        onRetry = quizzes::retry,
        onNavigateBack = onNavigateBack,
        onQuizClick = { viewModel.onIntent(DiscoverIntent.QuizClicked(it)) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenContent(
    uiState: DiscoverUiState,
    quizzes: LazyPagingItems<QuizCard>,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onIntent: (DiscoverIntent) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khám phá") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DiscoverFilterBar(
                uiState = uiState,
                sortMenuExpanded = sortMenuExpanded,
                onSortMenuExpandedChange = onSortMenuExpandedChange,
                onQuerySelected = { onIntent(DiscoverIntent.QueryChanged(it)) },
                onSortSelected = { onIntent(DiscoverIntent.SortChanged(it)) }
            )
            QuizPagingContent(
                initialized = uiState.initialized,
                quizzes = quizzes,
                onRetry = onRetry,
                onQuizClick = onQuizClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DiscoverFilterBar(
    uiState: DiscoverUiState,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onQuerySelected: (DiscoverQuery) -> Unit,
    onSortSelected: (DiscoverSort) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedFilter == DiscoverFilter.All &&
                    uiState.sort !in QUICK_SORTS,
                onClick = {
                    onQuerySelected(DiscoverQuery(DiscoverFilter.All, DiscoverSort.NEWEST))
                },
                label = { Text("Tất cả") }
            )
            FilterChip(
                selected = uiState.selectedFilter == DiscoverFilter.All &&
                    uiState.sort == DiscoverSort.MOST_PLAYED,
                onClick = {
                    onQuerySelected(DiscoverQuery(DiscoverFilter.All, DiscoverSort.MOST_PLAYED))
                },
                label = { Text("Chơi nhiều nhất") }
            )
            FilterChip(
                selected = uiState.selectedFilter == DiscoverFilter.All &&
                    uiState.sort == DiscoverSort.TRENDING,
                onClick = {
                    onQuerySelected(DiscoverQuery(DiscoverFilter.All, DiscoverSort.TRENDING))
                },
                label = { Text("Xu hướng") }
            )
        }
        Box {
            IconButton(onClick = { onSortMenuExpandedChange(true) }) {
                Icon(Icons.Filled.Sort, contentDescription = "Lọc và sắp xếp")
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { onSortMenuExpandedChange(false) }
            ) {
                MENU_SORTS.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label()) },
                        trailingIcon = {
                            if (sort == uiState.sort && uiState.selectedFilter == DiscoverFilter.All) {
                                Text("✓")
                            }
                        },
                        onClick = {
                            onSortSelected(sort)
                            onSortMenuExpandedChange(false)
                        }
                    )
                }
                HorizontalDivider()
                uiState.filters.filterIsInstance<DiscoverFilter.Category>().forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.label) },
                        trailingIcon = {
                            if (filter == uiState.selectedFilter) Text("✓")
                        },
                        onClick = {
                            onQuerySelected(DiscoverQuery(filter, DiscoverSort.TRENDING))
                            onSortMenuExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizPagingContent(
    initialized: Boolean,
    quizzes: LazyPagingItems<QuizCard>,
    onRetry: () -> Unit,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        !initialized || quizzes.loadState.refresh is LoadState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        quizzes.loadState.refresh is LoadState.Error -> ErrorState(
            message = (quizzes.loadState.refresh as LoadState.Error).error.message
                ?: "Không thể tải nội dung",
            onRetry = onRetry,
            modifier = modifier.fillMaxSize()
        )
        quizzes.itemCount == 0 ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có quiz phù hợp")
            }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count = quizzes.itemCount) { index ->
                quizzes[index]?.let { quiz ->
                    QuizCardItem(
                        quiz = quiz,
                        onClick = { onQuizClick(quiz.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            when (val append = quizzes.loadState.append) {
                is LoadState.Loading -> item { LoadingMore() }
                is LoadState.Error -> item {
                    ErrorState(append.error.message ?: "Không thể tải thêm", onRetry)
                }
                else -> Unit
            }
        }
    }
}

private val QUICK_SORTS = setOf(DiscoverSort.MOST_PLAYED, DiscoverSort.TRENDING)
private val MENU_SORTS = listOf(
    DiscoverSort.NEWEST,
    DiscoverSort.OLDEST,
    DiscoverSort.NAME_ASC,
    DiscoverSort.NAME_DESC
)

private fun DiscoverSort.label(): String = when (this) {
    DiscoverSort.NEWEST -> "Mới nhất"
    DiscoverSort.OLDEST -> "Cũ nhất"
    DiscoverSort.MOST_PLAYED -> "Chơi nhiều nhất"
    DiscoverSort.TRENDING -> "Xu hướng"
    DiscoverSort.NAME_ASC -> "Tên A-Z"
    DiscoverSort.NAME_DESC -> "Tên Z-A"
}

@Composable
private fun LoadingMore() = Box(
    Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center
) { CircularProgressIndicator() }

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Thử lại") }
    }
}
