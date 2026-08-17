package android.kma.myquizzapp.auth.presentation.forgot

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtpVerification: (String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ForgotPasswordViewModel.Effect.NavigateBack -> onNavigateBack()
                is ForgotPasswordViewModel.Effect.NavigateToOtpVerification -> {
                    onNavigateToOtpVerification(effect.email)
                }
                is ForgotPasswordViewModel.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ForgotPasswordContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordViewModel.UiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ForgotPasswordViewModel.Intent) -> Unit
) {
    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ForgotPasswordViewModel.Intent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Nhập email để nhận mã đặt lại mật khẩu",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(ForgotPasswordViewModel.Intent.EmailChanged(it)) },
                label = { Text("Email") },
                isError = uiState.emailError != null,
                supportingText = { uiState.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onIntent(ForgotPasswordViewModel.Intent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Gửi mã")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Bạn sẽ nhận được email có mã OTP và link đặt lại mật khẩu. Mã có hiệu lực trong 5 phút.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ForgotPasswordScreenPreview() {
    MyQuizAppTheme {
        ForgotPasswordContent(
            uiState = ForgotPasswordViewModel.UiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
        )
    }
}