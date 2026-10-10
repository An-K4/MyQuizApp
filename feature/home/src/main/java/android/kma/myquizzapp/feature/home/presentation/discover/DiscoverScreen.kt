package android.kma.myquizzapp.feature.home.presentation.discover

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.ui.components.DiscoveryQuizCard
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverFilter
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverQuery
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverSort
import android.kma.myquizzapp.feature.home.domain.discover.resolveDiscoverRequest
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
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
    // Count, keys and rendered data must share one immutable snapshot. The grid
    // may evaluate an old key lambda after Paging has replaced/shrunk its live list.
    val quizSnapshot = quizzes.itemSnapshotList
    val gridState = rememberLazyGridState()
    val filterScrollState = rememberScrollState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(request) { viewModel.onIntent(DiscoverIntent.Initialize(request)) }
    LaunchedEffect(uiState.selectedFilter, uiState.sort) { gridState.scrollToItem(0) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DiscoverEffect.NavigateToQuizDetail -> onNavigateToQuizDetail(effect.quizId)
            }
        }
    }

    DiscoverScreenContent(
        uiState = uiState,
        itemCount = quizSnapshot.size,
        quizAt = { index ->
            val quiz = quizSnapshot.getOrNull(index)
            // Still notify Paging of visible accesses for prefetch, but never read
            // a stale index (or a different quiz after filter/refresh replacement).
            if (index in 0 until quizzes.itemCount && quizzes.peek(index)?.id == quiz?.id) {
                quizzes[index]
            }
            quiz
        },
        itemKey = { index -> quizSnapshot.getOrNull(index)?.id ?: "quiz-placeholder-$index" },
        loadStates = quizzes.loadState,
        gridState = gridState,
        filterScrollState = filterScrollState,
        sortMenuExpanded = sortMenuExpanded,
        onSortMenuExpandedChange = { sortMenuExpanded = it },
        onIntent = viewModel::onIntent,
        onRetry = quizzes::retry,
        onNavigateBack = onNavigateBack,
        onQuizClick = { viewModel.onIntent(DiscoverIntent.QuizClicked(it)) },
        modifier = modifier
    )
}

/** Stateless UI: Paging collection, effects and all local UI state belong to the wrapper. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenContent(
    uiState: DiscoverUiState,
    itemCount: Int,
    quizAt: (Int) -> QuizCard?,
    itemKey: (Int) -> Any,
    loadStates: CombinedLoadStates,
    gridState: LazyGridState,
    filterScrollState: ScrollState,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onIntent: (DiscoverIntent) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mutedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khám phá", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Tất cả các bộ Quiz công khai",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor
            )
            DiscoverFilterBar(
                uiState = uiState,
                filterScrollState = filterScrollState,
                sortMenuExpanded = sortMenuExpanded,
                onSortMenuExpandedChange = onSortMenuExpandedChange,
                onQuerySelected = { onIntent(DiscoverIntent.QueryChanged(it)) },
                onSortSelected = { onIntent(DiscoverIntent.SortChanged(it)) }
            )
            QuizPagingContent(
                initialized = uiState.initialized,
                itemCount = itemCount, quizAt = quizAt, itemKey = itemKey,
                loadStates = loadStates, gridState = gridState,
                onRetry = onRetry, onQuizClick = onQuizClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DiscoverFilterBar(
    uiState: DiscoverUiState,
    filterScrollState: ScrollState,
    sortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onQuerySelected: (DiscoverQuery) -> Unit,
    onSortSelected: (DiscoverSort) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val softColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val mutedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val foregroundColor = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(filterScrollState)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.filters.forEach { filter ->
                FilterChip(
                    selected = filter == uiState.selectedFilter,
                    onClick = { onQuerySelected(DiscoverQuery(filter, uiState.sort)) },
                    label = {
                        Text(
                            when (filter) {
                                DiscoverFilter.All -> "Tất cả"
                                is DiscoverFilter.Category -> filter.label
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                        )
                    },
                    shape = RoundedCornerShape(50),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = softColor,
                        labelColor = mutedColor,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
            // No total-result label: this Paging source does not expose backend total.
            Box {
                TextButton(onClick = { onSortMenuExpandedChange(true) }) {
                    Text(uiState.sort.label(), color = foregroundColor,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Sắp xếp",
                        tint = foregroundColor)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { onSortMenuExpandedChange(false) }
                ) {
                    DiscoverSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label()) },
                            trailingIcon = { if (sort == uiState.sort) Text("✓") },
                            onClick = {
                                onSortSelected(sort)
                                onSortMenuExpandedChange(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizPagingContent(
    initialized: Boolean,
    itemCount: Int,
    quizAt: (Int) -> QuizCard?,
    itemKey: (Int) -> Any,
    loadStates: CombinedLoadStates,
    gridState: LazyGridState,
    onRetry: () -> Unit,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        !initialized || (loadStates.refresh is LoadState.Loading && itemCount == 0) ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        loadStates.refresh is LoadState.Error && itemCount == 0 -> ErrorState(
            message = "Không thể tải quiz. Vui lòng thử lại.", onRetry = onRetry,
            modifier = modifier.fillMaxSize()
        )
        itemCount == 0 -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có quiz phù hợp", modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center)
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(2), state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loadStates.refresh is LoadState.Error) item(
                key = "refresh_error", span = { GridItemSpan(maxLineSpan) }
            ) {
                ErrorState("Không thể cập nhật quiz. Vui lòng thử lại.", onRetry)
            }
            items(count = itemCount, key = itemKey, contentType = { "quiz" }) { index ->
                quizAt(index)?.let { quiz ->
                    DiscoveryQuizCard(
                        title = quiz.quizName,
                        categoryText = quiz.quizCategory,
                        metadataText = "${quiz.questionCount} câu • ${quiz.playCount} lượt chơi",
                        imageUrl = quiz.quizImage,
                        onClick = { onQuizClick(quiz.id) },
                        modifier = Modifier.fillMaxWidth(), uniformGrid = true
                    )
                }
            }
            when (loadStates.append) {
                is LoadState.Loading -> item(key = "append_loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadState.Error -> item(key = "append_error", span = { GridItemSpan(maxLineSpan) }) {
                    ErrorState("Không thể tải thêm quiz. Vui lòng thử lại.", onRetry)
                }
                else -> Unit
            }
        }
    }
}

private fun DiscoverSort.label(): String = when (this) {
    DiscoverSort.NEWEST -> "Mới nhất"
    DiscoverSort.OLDEST -> "Cũ nhất"
    DiscoverSort.MOST_PLAYED -> "Chơi nhiều nhất"
    DiscoverSort.TRENDING -> "Xu hướng"
    DiscoverSort.NAME_ASC -> "Tên A-Z"
    DiscoverSort.NAME_DESC -> "Tên Z-A"
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("Thử lại") }
    }
}

private val discoverPreviewQuizzes = listOf(
    "Đố vui vũ trụ" to "Khoa học", "Ngữ pháp N3 tiếng Nhật" to "Ngoại ngữ",
    "Các thương hiệu nổi tiếng" to "Giải trí", "Công nghệ quanh ta" to "Công nghệ"
).mapIndexed { index, (title, category) ->
    QuizCard(
        id = index.toLong() + 1, quizName = title, quizCategory = category,
        quizLanguage = "vi", quizOwnerId = 1, questionCount = 12 + index * 6,
        playCount = 350 + index * 70, completionRate = 0.0, createdAt = "2026-10-10T00:00:00Z"
    )
}

@Composable
private fun DiscoverPreview(
    quizzes: List<QuizCard> = discoverPreviewQuizzes,
    refresh: LoadState = LoadState.NotLoading(false),
    append: LoadState = LoadState.NotLoading(true)
) {
    val request = resolveDiscoverRequest(null, null, null)
    val source = LoadStates(refresh, LoadState.NotLoading(true), append)
    MyQuizAppTheme {
        DiscoverScreenContent(
            uiState = DiscoverUiState(initialized = true, filters = request.filters),
            itemCount = quizzes.size, quizAt = { quizzes[it] }, itemKey = { quizzes[it].id },
            loadStates = CombinedLoadStates(refresh, source.prepend, append, source),
            gridState = rememberLazyGridState(), filterScrollState = rememberScrollState(),
            sortMenuExpanded = false, onSortMenuExpandedChange = {}, onIntent = {},
            onRetry = {}, onNavigateBack = {}, onQuizClick = {}
        )
    }
}

@Preview(name = "Discover Grid Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Discover Grid Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Discover Grid Narrow", showBackground = true, widthDp = 320, heightDp = 720)
@Preview(name = "Discover Large Text", showBackground = true, widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
private fun DiscoverGridPreview() = DiscoverPreview()

@Preview(name = "Discover Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverLoadingPreview() = DiscoverPreview(emptyList(), refresh = LoadState.Loading)

@Preview(name = "Discover Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverEmptyPreview() = DiscoverPreview(emptyList())

@Preview(name = "Discover Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverErrorPreview() = DiscoverPreview(emptyList(), refresh = LoadState.Error(IllegalStateException("Preview")))

@Preview(name = "Discover Append Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DiscoverAppendErrorPreview() = DiscoverPreview(append = LoadState.Error(IllegalStateException("Preview")))
