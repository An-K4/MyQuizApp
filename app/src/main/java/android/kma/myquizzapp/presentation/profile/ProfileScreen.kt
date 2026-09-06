package android.kma.myquizzapp.presentation.profile

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateBack -> onLoggedOut()
                is ProfileEffect.ShowError -> {
                    // TODO: Show toast/snackbar with effect.message
                }
            }
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        onLogout = viewModel::logout,
        onSignIn = onNavigateToAuth,
        modifier = modifier
    )
}

/**
 * Màn Hồ sơ có BA trạng thái, không phải hai (N19.6).
 *
 * Trước đây màn này chỉ có "đang tải" và "có user", nên khi là khách thì nó
 * hiện một hồ sơ rỗng với tên "—" và một nút Đăng xuất vô nghĩa. Giờ trạng
 * thái "đã biết chắc là khách" có UI riêng.
 *
 * Vì sao Hồ sơ dùng empty state chứ không dùng hộp thoại gác như Thư viện: nó
 * là một tab của bottom nav. Chặn ngay khi bấm tab thì tab đó không bao giờ
 * mở được, thanh nav sẽ có một ô bấm vào là hiện hộp thoại — về sau tab này
 * còn chứa cài đặt và giới thiệu ứng dụng, là thứ khách xem được.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Là tab cấp cao nhất (N19.5) nên không có nút back.
            TopAppBar(title = { Text("Hồ sơ") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.isConfirmedGuest -> GuestProfileContent(onSignIn = onSignIn)

                else -> SignedInProfileContent(uiState = uiState, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun SignedInProfileContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // "Quiz của tôi" đã chuyển thành tab Thư viện ở bottom nav (N19.5).
        // Profile về sau chỉ hiển thị thông tin + cài đặt, không chứa navigation.
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        ListItem(
            headlineContent = { Text("Đăng xuất") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout)
        )
    }
}

@Composable
private fun GuestProfileContent(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bạn đang chơi với tư cách khách",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Đăng nhập để tạo quiz của riêng bạn, mở phòng chơi và lưu lại " +
                "lịch sử những trận đã tham gia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSignIn) { Text("Đăng ký/Đăng nhập") }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenContentPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(),
            onLogout = {},
            onSignIn = {}
        )
    }
}

@Preview(showBackground = true, name = "Khách")
@Preview(showBackground = true, name = "Khách (tối)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenGuestPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(session = SessionState.Guest),
            onLogout = {},
            onSignIn = {}
        )
    }
}
