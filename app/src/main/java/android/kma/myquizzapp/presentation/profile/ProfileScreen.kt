package android.kma.myquizzapp.presentation.profile

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMyQuizzes: () -> Unit,
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
        onNavigateBack = onNavigateBack,
        onNavigateToMyQuizzes = onNavigateToMyQuizzes,
        onLogout = viewModel::logout,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit,
    onNavigateToMyQuizzes: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
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

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            ListItem(
                headlineContent = { Text("Quiz của tôi") },
                supportingContent = { Text("Xem và quản lý các quiz bạn đã tạo") },
                leadingContent = { Icon(Icons.Default.Assignment, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToMyQuizzes)
            )
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            ListItem(
                headlineContent = { Text("Đăng xuất") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenContentPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(isLoading = false),
            onNavigateBack = {},
            onNavigateToMyQuizzes = {},
            onLogout = {}
        )
    }
}
