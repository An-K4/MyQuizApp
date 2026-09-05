package android.kma.myquizzapp.feature.home.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.HomeSectionRow
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    // Tạm thời: lối vào màn nhập mã phòng đặt ở top bar để test N19. Khi có
    // Bottom Navigation thật (N19.5) thì chuyển xuống tab "Tham gia" và bỏ nút này.
    onNavigateToJoinRoom: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToQuizDetail -> onNavigateToQuizDetail(effect.quizId)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(HomeIntent.CheckAuthState)
    }

    HomeScreenContent(
        uiState = uiState,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToAuth = onNavigateToAuth,
        onNavigateToQuizDetail = onNavigateToQuizDetail,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToJoinRoom = onNavigateToJoinRoom,
        onRetry = { viewModel.onIntent(HomeIntent.Retry) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToJoinRoom: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyQuizz") },
                actions = {
                    TextButton(onClick = onNavigateToJoinRoom) { Text("Vào phòng") }
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                    AuthHeaderAction(
                        currentUser = uiState.currentUser,
                        onNavigateToAuth = onNavigateToAuth,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        ExploreTabContent(
            uiState = uiState,
            onQuizClick = onNavigateToQuizDetail,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        )
    }
}

@Composable
private fun AuthHeaderAction(
    currentUser: User?,
    onNavigateToAuth: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    if (currentUser == null) {
        TextButton(onClick = onNavigateToAuth) { Text("Đăng ký/Đăng nhập") }
    } else {
        Avatar(
            avatarUrl = currentUser.avatar,
            contentDescription = "Hồ sơ của tôi",
            size = 36.dp,
            modifier = Modifier.padding(end = 12.dp).clickable(onClick = onNavigateToProfile)
        )
    }
}

@Composable
private fun ExploreTabContent(
    uiState: HomeUiState,
    onQuizClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeError = uiState.homeError
    when {
        uiState.isLoadingHome -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        homeError != null -> Box(modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Không thể tải nội dung", style = MaterialTheme.typography.bodyLarge)
                Text(
                    homeError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onRetry) { Text("Thử lại") }
            }
        }
        uiState.homeSections.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
            Text("Không có nội dung", style = MaterialTheme.typography.bodyLarge)
        }
        else -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(uiState.homeSections, key = { it.sectionKey }) { section ->
                HomeSectionRow(section = section, onQuizClick = onQuizClick)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentPreview() {
    MyQuizAppTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onNavigateToSearch = {},
            onNavigateToAuth = {},
            onNavigateToQuizDetail = {},
            onNavigateToProfile = {},
            onRetry = {}
        )
    }
}
