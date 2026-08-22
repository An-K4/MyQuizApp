package android.kma.myquizzapp.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.HomeSectionRow

/**
 * Home screen - browse quiz sections via scroll.
 *
 * Features:
 * - Search icon → navigate to SearchScreen
 * - Auth-aware component cạnh nút tìm kiếm: chưa đăng nhập → nút
 *   "Đăng nhập" (điều hướng sang AuthGraph); đã đăng nhập → avatar (async
 *   image) bấm vào để sang màn Profile.
 * - Vertical scroll of horizontal sections (nội dung "Khám phá" duy nhất,
 *   không còn TabRow "Của tôi" — mục đó đã chuyển sang màn Profile vì
 *   nó là điều hướng sang màn khác, không phải nội dung tab tại chỗ).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // AuthRepository không có Flow phản ứng realtime, nên mọi lần Home resume
    // (ví dụ quay lại từ màn Đăng nhập hoặc Profile sau khi đăng xuất) cần
    // kiểm tra lại trạng thái đăng nhập để cập nhật nút đăng nhập/avatar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.handleIntent(HomeIntent.CheckAuthState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyQuizz") },
                actions = {
                    // Search icon → navigate to SearchScreen
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }

                    // Auth-aware component: nút đăng nhập (guest) hoặc avatar (đã đăng nhập)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ExploreTabContent(
                uiState = uiState,
                onQuizClick = onNavigateToQuizDetail,
                onRetry = { viewModel.handleIntent(HomeIntent.Retry) }
            )
        }
    }
}

/**
 * Component cạnh nút tìm kiếm: chưa đăng nhập → nút "Đăng ký/Đăng nhập";
 * đã đăng nhập → avatar (bấm vào để sang Profile).
 */
@Composable
private fun AuthHeaderAction(
    currentUser: User?,
    onNavigateToAuth: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    if (currentUser == null) {
        TextButton(onClick = onNavigateToAuth) {
            Text("Đăng ký/Đăng nhập")
        }
    } else {
        // Avatar dùng component chung của core:ui — Coil là chi tiết nội bộ core:ui,
        // feature:home không cần tự khai dependency coil.compose.
        Avatar(
            avatarUrl = currentUser.avatar,
            contentDescription = "Hồ sơ của tôi",
            size = 36.dp,
            modifier = Modifier
                .padding(end = 12.dp)
                .clickable(onClick = onNavigateToProfile)
        )
    }
}

/**
 * Nội dung "Khám phá" - vertical scroll of horizontal sections.
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
