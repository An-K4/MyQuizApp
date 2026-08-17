package android.kma.myquizzapp.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.kma.myquizzapp.core.ui.components.HomeSectionRow

/**
 * Home screen - browse quiz sections via scroll.
 * 
 * Features:
 * - Search icon → navigate to SearchScreen
 * - Tab switching: "Khám phá" (explore) / "Của tôi" (my quizzes)
 * - Vertical scroll of horizontal sections (for "Khám phá" tab)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyQuizz") },
                actions = {
                    // Search icon → navigate to SearchScreen
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                    
                    // TODO: Auth button (guest) or Avatar (authenticated)
                    // Will implement after auth state management is ready
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row: "Khám phá" / "Của tôi"
            TabRow(selectedTabIndex = uiState.currentTab.ordinal) {
                Tab(
                    selected = uiState.currentTab == HomeTab.EXPLORE,
                    onClick = { viewModel.handleIntent(HomeIntent.TabChanged(HomeTab.EXPLORE)) },
                    text = { Text("Khám phá") }
                )
                Tab(
                    selected = uiState.currentTab == HomeTab.MY_QUIZZES,
                    onClick = { viewModel.handleIntent(HomeIntent.TabChanged(HomeTab.MY_QUIZZES)) },
                    text = { Text("Của tôi") }
                )
            }
            
            // Content based on selected tab
            when (uiState.currentTab) {
                HomeTab.EXPLORE -> ExploreTabContent(
                    uiState = uiState,
                    onQuizClick = onNavigateToQuizDetail,
                    onRetry = { viewModel.handleIntent(HomeIntent.Retry) }
                )
                HomeTab.MY_QUIZZES -> MyQuizzesTabContent(
                    uiState = uiState
                )
            }
        }
    }
}

/**
 * "Khám phá" tab content - vertical scroll of horizontal sections.
 */
@Composable
private fun ExploreTabContent(
    uiState: HomeUiState,
    onQuizClick: (Long) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isLoadingHome -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        uiState.homeError != null -> {
            // Error state
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
                        text = "Không thể tải nội dung",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = uiState.homeError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("Thử lại")
                    }
                }
            }
        }
        
        uiState.homeSections.isEmpty() -> {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có nội dung",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        else -> {
            // Sections content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = uiState.homeSections,
                    key = { it.sectionKey }
                ) { section ->
                    HomeSectionRow(
                        section = section,
                        onQuizClick = onQuizClick
                    )
                }
            }
        }
    }
}

/**
 * "Của tôi" tab content - user's created quizzes.
 * 
 * TODO: Implement in N13-14 (quiz-manage feature)
 */
@Composable
private fun MyQuizzesTabContent(
    uiState: HomeUiState
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Sắp ra mắt",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Quản lý quiz của bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
