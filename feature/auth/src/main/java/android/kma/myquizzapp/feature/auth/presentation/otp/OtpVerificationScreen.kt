package android.kma.myquizzapp.feature.auth.presentation.otp

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (ticket: String, email: String) -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OtpVerificationEffect.NavigateBack -> onNavigateBack()
                is OtpVerificationEffect.NavigateToResetPassword ->
                    onNavigateToResetPassword(effect.ticket, effect.email)
                is OtpVerificationEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    OtpVerificationScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreenContent(
    uiState: OtpVerificationUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (OtpVerificationIntent) -> Unit
) {
    Scaffold(
        snackbarHost = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                SnackbarHost(snackbarHostState, Modifier.padding(top = 8.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Xác thực OTP") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(OtpVerificationIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Nhập mã OTP đã gửi đến",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(uiState.email, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            OtpInputField(
                otp = uiState.otp,
                onOtpChanged = { onIntent(OtpVerificationIntent.OtpChanged(it)) },
                enabled = !uiState.isLoading
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onIntent(OtpVerificationIntent.Verify) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.otp.length == 6
            ) {
                if (uiState.isLoading) CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                ) else Text("Xác nhận")
            }
            TextButton(
                onClick = { onIntent(OtpVerificationIntent.ResendCode) },
                enabled = !uiState.isLoading && uiState.resendSecondsLeft == 0
            ) {
                Text(if (uiState.resendSecondsLeft > 0) "Gửi lại mã (${uiState.resendSecondsLeft}s)" else "Gửi lại mã")
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Mã OTP có hiệu lực trong 2 phút.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OtpInputField(otp: String, onOtpChanged: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = otp,
        onValueChange = onOtpChanged,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpVerificationScreenContentPreview() {
    MyQuizAppTheme {
        OtpVerificationScreenContent(
            uiState = OtpVerificationUiState(email = "user@example.com", otp = "123456"),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
        )
    }
}
