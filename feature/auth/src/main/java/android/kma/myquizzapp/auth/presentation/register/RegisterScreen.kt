package android.kma.myquizzapp.auth.presentation.register

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.kma.myquizzapp.core.ui.style.AppTextStyles
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme

/**
 * Stateful wrapper for Register screen
 * Manages ViewModel, state, and side effects
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegisterViewModel.Effect.NavigateToHostHome -> onRegisterSuccess()
                is RegisterViewModel.Effect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        passwordVisible = passwordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        onIntent = viewModel::onIntent,
        onBackToLogin = onBackToLogin,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Stateless composable for Register screen UI
 * Pure UI component without ViewModel - Preview-friendly
 */
@Composable
fun RegisterScreenContent(
    uiState: RegisterViewModel.UiState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (RegisterViewModel.Intent) -> Unit,
    onBackToLogin: () -> Unit,
    snackbarHostState: SnackbarHostState,
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            // Title
            Text(
                text = "Đăng ký tài khoản",
                style = AppTextStyles.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Tạo tài khoản mới để bắt đầu",
                style = AppTextStyles.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(RegisterViewModel.Intent.EmailChanged(it)) },
                label = { Text("Email") },
                isError = uiState.emailError != null,
                supportingText = { uiState.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))

            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onIntent(RegisterViewModel.Intent.PasswordChanged(it)) },
                label = { Text("Mật khẩu") },
                isError = uiState.passwordError != null,
                supportingText = { uiState.passwordError?.let { Text(it) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
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
            Spacer(Modifier.height(8.dp))

            // Fullname field
            OutlinedTextField(
                value = uiState.fullname,
                onValueChange = { onIntent(RegisterViewModel.Intent.FullnameChanged(it)) },
                label = { Text("Họ và tên") },
                isError = uiState.fullnameError != null,
                supportingText = { uiState.fullnameError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(8.dp))

            // Phone field
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = { onIntent(RegisterViewModel.Intent.PhoneChanged(it)) },
                label = { Text("Số điện thoại (tuỳ chọn)") },
                isError = uiState.phoneError != null,
                supportingText = { uiState.phoneError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(24.dp))

            // Register button
            Button(
                onClick = { onIntent(RegisterViewModel.Intent.Submit) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Đăng ký", style = AppTextStyles.buttonText)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Back to login link
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã có tài khoản? ",
                    style = AppTextStyles.bodyMedium
                )
                Text(
                    text = "Đăng nhập",
                    style = AppTextStyles.linkText,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreviewLight() {
    MyQuizAppTheme {
        RegisterScreenContent(
            uiState = RegisterViewModel.UiState(),
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onBackToLogin = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegisterScreenPreviewDark() {
    MyQuizAppTheme {
        RegisterScreenContent(
            uiState = RegisterViewModel.UiState(),
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onBackToLogin = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
