package android.kma.myquizzapp.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.kma.myquizzapp.core.ui.components.Avatar
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider

/**
 * Màn Profile. Yêu cằu đăng nhập — chỉ đạt tới đây qua avatar ở Home
 * (Route.Profile) khi đã đăng nhập.
 *
 * Hiển thị thông tin user (avatar, tên, email) và danh sách hành động:
 * - "Quiz của tôi" → sang Route.MyQuizzes (QuizManageListScreen, N13-14).
 *   Mục này trước đây là tab "Của tôi" ở Home, chuyển về đây vì nó là
 *   điều hướng sang màn khác (không phải nội dung tab tại chỗ).
 * - "Đăng xuất" → gọi LogoutUseCase rồi quay về Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMyQuizzes: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLoggedOut()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Header: avatar + tên + email
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar dùng component chung của core:ui — Coil là chi tiết nội bộ core:ui,
                // module app không cần tự khai dependency coil.compose.
                Avatar(
                    avatarUrl = uiState.user?.avatar,
                    contentDescription = null,
                    size = 64.dp
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Column {
                    Text(
                        text = uiState.user?.fullname ?: "—",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = uiState.user?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // "Quiz của tôi" — nav trigger sang QuizManageListScreen (không qua ViewModel)
            ListItem(
                headlineContent = { Text("Quiz của tôi") },
                supportingContent = { Text("Xem và quản lý các quiz bạn đã tạo") },
                leadingContent = {
                    Icon(Icons.Default.Assignment, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToMyQuizzes)
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // "Đăng xuất" — gọi LogoutUseCase qua ViewModel, rỚi onLoggedOut() qua LaunchedEffect ứng với isLoggedOut
            ListItem(
                headlineContent = { Text("Đăng xuất") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { viewModel.logout() })
            )
        }
    }
}
