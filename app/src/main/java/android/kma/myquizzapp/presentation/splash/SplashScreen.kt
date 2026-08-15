package android.kma.myquizzapp.presentation.splash

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.kma.myquizzapp.R
import com.example.ui.theme.MyQuizAppTheme

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToGuest: () -> Unit,
    onNavigateToHost: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Điều hướng là side-effect → làm trong LaunchedEffect, KHÔNG gọi navController
    // ngay trong thân composable (composition có thể chạy/hủy nhiều lần).
    LaunchedEffect(uiState) {
        when (uiState) {
            SplashViewModel.UiState.ShowAuth -> onNavigateToAuth()
            SplashViewModel.UiState.ShowGuest -> onNavigateToGuest()
            SplashViewModel.UiState.ShowHost -> onNavigateToHost()
            SplashViewModel.UiState.Loading -> Unit
        }
    }

    SplashScreenContent()
}

/**
 * Stateless composable for Splash screen UI
 * Pure UI component without ViewModel - Preview-friendly
 */
@Composable
fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Logo ở giữa màn hình
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        )

        // Loading indicator ở gần dưới cùng
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreviewLight() {
    MyQuizAppTheme {
        SplashScreenContent()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SplashScreenPreviewDark() {
    MyQuizAppTheme {
        SplashScreenContent()
    }
}