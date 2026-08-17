package android.kma.myquizzapp.feature.home.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.kma.myquizzapp.core.ui.components.QuizCardItem

/**
 * Search screen - dedicated screen for searching public quizzes.
 * 
 * Features:
 * - Search bar with auto-focus
 * - Real-time query input
 * - Search results with pagination (infinite scroll)
 * - Empty/Loading/Error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
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
                        onValueChange = { viewModel.handleIntent(SearchIntent.QueryChanged(it)) },
                        placeholder = { Text("Tìm kiếm quiz...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            if (uiState.hasQuery) {
                                IconButton(
                                    onClick = { viewModel.handleIntent(SearchIntent.ClearSearch) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa")
                                }
                            }
                        }
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isSearching && !uiState.hasResults -> {
                    // Initial search loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null && !uiState.hasResults -> {
                    // Error state (no results yet)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Không thể tìm kiếm",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.handleIntent(SearchIntent.Retry) }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
                
                !uiState.hasQuery -> {
                    // Empty query hint
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nhập từ khóa để tìm kiếm quiz",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                uiState.hasResults -> {
                    // Search results
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.results,
                            key = { it.id }
                        ) { quiz ->
                            QuizCardItem(
                                quiz = quiz,
                                onClick = { viewModel.handleIntent(SearchIntent.QuizCardClicked(quiz.id)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Load more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                    
                    // Detect scroll to bottom for pagination
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastVisibleIndex ->
                                if (lastVisibleIndex != null &&
                                    lastVisibleIndex >= uiState.results.size - 3 &&
                                    !uiState.isLoadingMore &&
                                    uiState.hasMore
                                ) {
                                    viewModel.handleIntent(SearchIntent.LoadMore)
                                }
                            }
                    }
                }
                
                else -> {
                    // No results for query
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Không tìm thấy kết quả",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Thử từ khóa khác",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Auto-focus search field on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
