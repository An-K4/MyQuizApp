package android.kma.myquizzapp.auth.presentation.otp

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (email: String, otp: String) -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OtpVerificationViewModel.Effect.NavigateBack -> onNavigateBack()
                is OtpVerificationViewModel.Effect.NavigateToResetPassword -> {
                    onNavigateToResetPassword(effect.email, effect.otp)
                }
                is OtpVerificationViewModel.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    OtpVerificationContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtpVerificationContent(
    uiState: OtpVerificationViewModel.UiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (OtpVerificationViewModel.Intent) -> Unit
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
                title = { Text("Xác thực OTP") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(OtpVerificationViewModel.Intent.NavigateBack) }) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Nhập mã xác thực",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Subtitle with email
            Text(
                text = "Chúng tôi đã gửi mã 6 chữ số đến",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = uiState.email,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // OTP Input (6 boxes)
            OtpInputField(
                value = uiState.otp,
                onValueChange = { onIntent(OtpVerificationViewModel.Intent.OtpChanged(it)) },
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Verify button
            Button(
                onClick = { onIntent(OtpVerificationViewModel.Intent.Verify) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.otp.length == 6
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Xác nhận")
                }
            }

            // Resend code link
            TextButton(
                onClick = { onIntent(OtpVerificationViewModel.Intent.ResendCode) },
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = "Gửi lại mã",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Helper text
            Text(
                text = "Mã OTP có hiệu lực trong 5 phút",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(6) { index ->
            val digit = value.getOrNull(index)?.toString() ?: ""
            
            OutlinedTextField(
                value = digit,
                onValueChange = { newValue ->
                    if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                        val newOtp = value.toMutableList().apply {
                            if (index < size) {
                                this[index] = newValue.firstOrNull() ?: ' '
                            } else {
                                add(newValue.firstOrNull() ?: ' ')
                            }
                        }.joinToString("").trim()
                        
                        onValueChange(newOtp)
                        
                        // Auto-focus next box
                        if (newValue.isNotEmpty() && index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else if (newValue.isEmpty() && index > 0) {
                        // Handle backspace: clear current and focus previous
                        val newOtp = value.take(index) + value.drop(index + 1)
                        onValueChange(newOtp)
                        focusRequesters[index - 1].requestFocus()
                    }
                },
                modifier = Modifier
                    .width(48.dp)
                    .focusRequester(focusRequesters[index]),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }

    // Auto-focus first box on composition
    LaunchedEffect(Unit) {
        if (value.isEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }
}
