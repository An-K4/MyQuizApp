package android.kma.myquizzapp.auth.presentation.reset

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyQuizAppTheme

@Composable
fun ResetPasswordScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ResetPasswordViewModel.Effect.NavigateToLogin -> onNavigateToLogin()
                is ResetPasswordViewModel.Effect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ResetPasswordContent(
        uiState = uiState,
        newPasswordVisible = newPasswordVisible,
        confirmPasswordVisible = confirmPasswordVisible,
        onNewPasswordVisibilityChange = { newPasswordVisible = it },
        onConfirmPasswordVisibilityChange = { confirmPasswordVisible = it },
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordContent(
    uiState: ResetPasswordViewModel.UiState,
    newPasswordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onNewPasswordVisibilityChange: (Boolean) -> Unit,
    onConfirmPasswordVisibilityChange: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    onIntent: (ResetPasswordViewModel.Intent) -> Unit
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
                title = { Text("Đặt lại mật khẩu") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ResetPasswordViewModel.Intent.NavigateBack) }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.isTokenFlow) 
                    "Nhập mật khẩu mới của bạn" 
                else 
                    "Nhập email, mã OTP và mật khẩu mới",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Token Flow: Hidden token field (pre-filled from deep link)
            if (uiState.isTokenFlow) {
                Text(
                    text = "Token từ email đã được xác nhận ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // OTP Flow: Email + OTP fields
            if (!uiState.isTokenFlow) {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { onIntent(ResetPasswordViewModel.Intent.EmailChanged(it)) },
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

                OutlinedTextField(
                    value = uiState.otp,
                    onValueChange = { onIntent(ResetPasswordViewModel.Intent.OtpChanged(it)) },
                    label = { Text("Mã OTP (6 chữ số)") },
                    isError = uiState.otpError != null,
                    supportingText = { uiState.otpError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            }

            // Password fields (both flows)
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = { onIntent(ResetPasswordViewModel.Intent.PasswordChanged(it)) },
                label = { Text("Mật khẩu mới") },
                isError = uiState.passwordError != null,
                supportingText = { uiState.passwordError?.let { Text(it) } },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !uiState.isLoading,
                trailingIcon = {
                    IconButton(onClick = { onNewPasswordVisibilityChange(!newPasswordVisible) }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (newPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { onIntent(ResetPasswordViewModel.Intent.ConfirmPasswordChanged(it)) },
                label = { Text("Xác nhận mật khẩu") },
                isError = uiState.confirmPasswordError != null,
                supportingText = { uiState.confirmPasswordError?.let { Text(it) } },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !uiState.isLoading,
                trailingIcon = {
                    IconButton(onClick = { onConfirmPasswordVisibilityChange(!confirmPasswordVisible) }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                },
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
                onClick = { onIntent(ResetPasswordViewModel.Intent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Đặt lại mật khẩu")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle flow button
            TextButton(
                onClick = { onIntent(ResetPasswordViewModel.Intent.ToggleFlow) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = if (uiState.isTokenFlow) 
                        "Nhập mã OTP thủ công?" 
                    else 
                        "Có link từ email?",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (uiState.isTokenFlow)
                    "Nếu link không hoạt động, hãy chọn 'Nhập mã OTP thủ công'"
                else
                    "Mã OTP có hiệu lực trong 5 phút",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ResetPasswordScreenPreview() {
    MyQuizAppTheme {
        ResetPasswordContent(
            uiState = ResetPasswordViewModel.UiState(),
            newPasswordVisible = false,
            confirmPasswordVisible = false,
            onNewPasswordVisibilityChange = {},
            onConfirmPasswordVisibilityChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
        )
    }
}
