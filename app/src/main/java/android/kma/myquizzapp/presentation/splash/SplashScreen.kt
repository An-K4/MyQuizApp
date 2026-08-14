package android.kma.myquizzapp.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashScreen(
    onLoggedIn: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Điều hướng là side-effect → làm trong LaunchedEffect, KHÔNG gọi navController
    // ngay trong thân composable (composition có thể chạy/hủy nhiều lần).
    LaunchedEffect(uiState) {
        when (uiState) {
            SplashViewModel.UiState.LoggedIn -> onLoggedIn()
            SplashViewModel.UiState.LoggedOut -> onLoggedOut()
            SplashViewModel.UiState.Loading -> Unit
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator() // thay bằng logo app sau
    }
}