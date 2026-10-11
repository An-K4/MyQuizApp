package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.ui.components.LibraryQuizCard
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.ConfirmDeleteQuizDialog
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.collect

/** Stateful boundary: Paging, lifecycle, scroll/menu state and one-off effects live here. */
@Composable
fun QuizManageListScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizManageListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quizzes = viewModel.quizzes.collectAsLazyPagingItems()
    val quizSnapshot = quizzes.itemSnapshotList
    val listState = rememberLazyListState()
    val filterScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var skipFirstResume by rememberSaveable { mutableStateOf(true) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var expandedQuizMenuId by rememberSaveable { mutableStateOf<Long?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipFirstResume) skipFirstResume = false
        else viewModel.onIntent(QuizManageListIntent.Refresh)
    }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QuizManageListEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    QuizManageListScreenContent(
        uiState = uiState,
        itemCount = quizSnapshot.size,
        // Count, key and rendered row all use the SAME captured snapshot.
        quizAt = { index ->
            val quiz = quizSnapshot.getOrNull(index)
            if (index in 0 until quizzes.itemCount && quizzes.peek(index)?.id == quiz?.id) {
                quizzes[index] // Keep Paging access hints / append prefetch, but render the captured row.
            }
            quiz
        },
        itemKey = { index -> quizSnapshot.getOrNull(index)?.id ?: "library-placeholder-$index" },
        refreshState = quizzes.loadState.refresh,
        appendState = quizzes.loadState.append,
        listState = listState,
        filterScrollState = filterScrollState,
        snackbarHostState = snackbarHostState,
        sortMenuExpanded = sortMenuExpanded,
        expandedQuizMenuId = expandedQuizMenuId,
        onSortMenuExpandedChange = { sortMenuExpanded = it },
        onQuizMenuExpandedChange = { expandedQuizMenuId = it },
        onIntent = { intent ->
            expandedQuizMenuId = null
            viewModel.onIntent(intent)
        },
        onRetry = quizzes::retry,
        onNavigateBack = onNavigateBack,
        onNavigateToCreateQuiz = onNavigateToCreateQuiz,
        onNavigateToQuizDetail = onNavigateToQuizDetail,
        onNavigateToEditQuiz = onNavigateToEditQuiz,
        modifier = modifier,
    )
}

/** Stateless content; previews supply plain rows and load states without collecting Paging flows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizManageListScreenContent(
    uiState: QuizManageListUiState,
    itemCount: Int,
    quizAt: (Int) -> QuizSummary?,
    itemKey: (Int) -> Any,
    refreshState: LoadState,
    appendState: LoadState,
    listState: LazyListState,
    filterScrollState: ScrollState,
    snackbarHostState: SnackbarHostState,
    sortMenuExpanded: Boolean,
    expandedQuizMenuId: Long?,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onQuizMenuExpandedChange: (Long?) -> Unit,
    onIntent: (QuizManageListIntent) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onNavigateToCreateQuiz: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground = if (isDark) MaterialTheme.colorScheme.onBackground else FrontendColors.Foreground
    val border = if (isDark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text("Thư viện của tôi", color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    onNavigateBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateQuiz,
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tạo Quiz mới", fontWeight = FontWeight.Bold) },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = { onIntent(QuizManageListIntent.KeywordChanged(it)) },
                placeholder = { Text("Tìm quiz của tôi...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.keyword.isNotEmpty()) {
                        IconButton(onClick = { onIntent(QuizManageListIntent.KeywordChanged("")) }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa từ khóa")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = border),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MyQuizzesVisibility.entries.forEach { visibility ->
                        val label = when (visibility) {
                            MyQuizzesVisibility.ALL -> "Tất cả"
                            MyQuizzesVisibility.PUBLIC -> "Công khai"
                            MyQuizzesVisibility.PRIVATE -> "Riêng tư"
                        }
                        FilterChip(
                            selected = uiState.visibility == visibility,
                            onClick = { onIntent(QuizManageListIntent.VisibilityChanged(visibility)) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
                SortMenuButtonContent(
                    selected = uiState.sort,
                    expanded = sortMenuExpanded,
                    onExpandedChange = onSortMenuExpandedChange,
                    onSortSelected = { onIntent(QuizManageListIntent.SortChanged(it)) },
                )
            }
            QuizPagingContent(
                uiState = uiState,
                itemCount = itemCount,
                quizAt = quizAt,
                itemKey = itemKey,
                refreshState = refreshState,
                appendState = appendState,
                listState = listState,
                expandedQuizMenuId = expandedQuizMenuId,
                onQuizMenuExpandedChange = onQuizMenuExpandedChange,
                onRetry = onRetry,
                onQuizClick = onNavigateToQuizDetail,
                onEditClick = onNavigateToEditQuiz,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
    }
    uiState.deleteTarget?.let { target ->
        ConfirmDeleteQuizDialog(
            quizName = target.title,
            deleteError = uiState.deleteError,
            isDeleting = uiState.isDeleting,
            onConfirm = { onIntent(QuizManageListIntent.DeleteQuizConfirmed) },
            onDismiss = { onIntent(QuizManageListIntent.DeleteQuizDismissed) },
        )
    }
}

@Composable
private fun QuizPagingContent(
    uiState: QuizManageListUiState,
    itemCount: Int,
    quizAt: (Int) -> QuizSummary?,
    itemKey: (Int) -> Any,
    refreshState: LoadState,
    appendState: LoadState,
    listState: LazyListState,
    expandedQuizMenuId: Long?,
    onQuizMenuExpandedChange: (Long?) -> Unit,
    onRetry: () -> Unit,
    onQuizClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onIntent: (QuizManageListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        refreshState is LoadState.Loading && itemCount == 0 ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        refreshState is LoadState.Error && itemCount == 0 ->
            CenterMessage("Không tải được danh sách quiz của bạn.", onRetry, modifier.fillMaxSize())
        itemCount == 0 -> {
            val filtered = uiState.keyword.isNotBlank() || uiState.visibility != MyQuizzesVisibility.ALL
            CenterMessage(
                if (filtered) "Không có quiz phù hợp với từ khóa hoặc bộ lọc này."
                else "Bạn chưa có quiz nào. Nhấn Tạo Quiz mới để bắt đầu.",
                modifier = modifier.fillMaxSize(),
            )
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            // Leave space for the extended FAB; it must not cover the last card's menu.
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (refreshState is LoadState.Loading) item(key = "refresh-progress") {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (refreshState is LoadState.Error) item(key = "refresh-error") {
                CenterMessage("Không thể cập nhật danh sách. Đang hiển thị dữ liệu đã tải.", onRetry,
                    Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
            items(count = itemCount, key = itemKey) { index ->
                quizAt(index)?.let { quiz ->
                    LibraryQuizCard(
                        title = quiz.quizName,
                        metadataText = "${quiz.questionCount} câu hỏi • ${quiz.playCount} lượt chơi",
                        imageUrl = quiz.quizImage,
                        isPublic = quiz.isPublic,
                        menuExpanded = expandedQuizMenuId == quiz.id,
                        onMenuExpandedChange = { onQuizMenuExpandedChange(if (it) quiz.id else null) },
                        onClick = { onQuizClick(quiz.id) },
                        onEditClick = { onEditClick(quiz.id) },
                        onDeleteClick = { onIntent(QuizManageListIntent.DeleteQuizClicked(QuizDeleteTarget(quiz.id, quiz.quizName))) },
                        enabled = !uiState.isDeleting,
                    )
                }
            }
            if (appendState is LoadState.Loading) item(key = "append-progress") {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            if (appendState is LoadState.Error) item(key = "append-error") {
                CenterMessage("Tải thêm thất bại.", onRetry, Modifier.fillMaxWidth().padding(12.dp))
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onRetry?.let {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = it) { Text("Thử lại") }
        }
    }
}

@Composable
private fun SortMenuButtonContent(
    selected: MyQuizzesSort,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSortSelected: (MyQuizzesSort) -> Unit,
) {
    val options = listOf(
        MyQuizzesSort.RECENTLY_UPDATED to "Cập nhật gần đây",
        MyQuizzesSort.NEWEST to "Mới nhất",
        MyQuizzesSort.OLDEST to "Cũ nhất",
        MyQuizzesSort.NAME_ASC to "Tên A–Z",
    )
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.Sort, contentDescription = "Sắp xếp: ${options.first { it.first == selected }.second}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { (sort, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (sort == selected) Icon(Icons.Default.Check, contentDescription = "Đang chọn") },
                    onClick = {
                        onSortSelected(sort)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

private fun libraryPreviewQuizzes(): List<QuizSummary> = listOf(
    Triple("Luyện thi trắc nghiệm Sinh học 12", 10, true),
    Triple("Quiz vui cuối tuần", 15, false),
    Triple("Khám phá Văn hóa Thế giới", 15, true),
    Triple("Thử thách tư duy mỗi ngày", 10, false),
).mapIndexed { index, (title, count, isPublic) ->
    QuizSummary(
        id = index + 1L, quizOwner = 1L, quizName = title, quizLanguage = "vi", isPublic = isPublic,
        questionCount = count, playCount = 0, completionRate = 0.0,
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z",
    )
}

/** Preview adapter only: no ViewModel, Hilt, network images or Paging Flow. */
@Composable
private fun LibraryPreviewContent(
    quizzes: List<QuizSummary> = libraryPreviewQuizzes(),
    uiState: QuizManageListUiState = QuizManageListUiState(),
    refreshState: LoadState = LoadState.NotLoading(false),
    appendState: LoadState = LoadState.NotLoading(true),
    expandedQuizMenuId: Long? = null,
) {
    MyQuizAppTheme {
        QuizManageListScreenContent(
            uiState = uiState,
            itemCount = quizzes.size,
            quizAt = quizzes::getOrNull,
            itemKey = { quizzes[it].id },
            refreshState = refreshState,
            appendState = appendState,
            listState = rememberLazyListState(),
            filterScrollState = rememberScrollState(),
            snackbarHostState = remember { SnackbarHostState() },
            sortMenuExpanded = false,
            expandedQuizMenuId = expandedQuizMenuId,
            onSortMenuExpandedChange = {},
            onQuizMenuExpandedChange = {},
            onIntent = {},
            onRetry = {},
            onNavigateBack = null,
            onNavigateToCreateQuiz = {},
            onNavigateToQuizDetail = {},
            onNavigateToEditQuiz = {},
        )
    }
}

@Preview(name = "Library Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Library Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Library Large Text", showBackground = true, widthDp = 320, heightDp = 844, fontScale = 1.5f)
@Composable
private fun QuizManageListScreenContentPreview() = LibraryPreviewContent()

@Preview(name = "Library Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryLoadingPreview() = LibraryPreviewContent(quizzes = emptyList(), refreshState = LoadState.Loading)

@Preview(name = "Library Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryEmptyPreview() = LibraryPreviewContent(quizzes = emptyList())

@Preview(name = "Library Filter Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryFilteredEmptyPreview() = LibraryPreviewContent(quizzes = emptyList(), uiState = QuizManageListUiState(keyword = "Toán"))

@Preview(name = "Library Load Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryErrorPreview() = LibraryPreviewContent(quizzes = emptyList(), refreshState = LoadState.Error(IllegalStateException("Preview")))

@Preview(name = "Library Menu", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryMenuPreview() = LibraryPreviewContent(expandedQuizMenuId = 1L)

@Preview(name = "Library Delete Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryDeleteErrorPreview() = LibraryPreviewContent(uiState = QuizManageListUiState(
    deleteTarget = QuizDeleteTarget(1L, "Luyện thi trắc nghiệm Sinh học 12"),
    deleteError = "Không có kết nối mạng. Vui lòng thử lại.",
))
