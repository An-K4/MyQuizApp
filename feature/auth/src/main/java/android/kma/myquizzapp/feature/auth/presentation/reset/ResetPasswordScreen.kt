package android.kma.myquizzapp.feature.auth.presentation.reset

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ResetPasswordEffect.NavigateToLogin -> onNavigateToLogin()
                is ResetPasswordEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ResetPasswordScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        passwordVisible = passwordVisible,
        confirmPasswordVisible = confirmPasswordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        onConfirmPasswordVisibilityChange = { confirmPasswordVisible = it },
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreenContent(
    uiState: ResetPasswordUiState,
    snackbarHostState: SnackbarHostState,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onConfirmPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (ResetPasswordIntent) -> Unit
) {
    Scaffold(
        snackbarHost = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                SnackbarHost(snackbarHostState, Modifier.padding(top = 8.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Đặt lại mật khẩu") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ResetPasswordIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isCheckingTicket -> {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Text(
                        "Đang kiểm tra phiên đặt lại mật khẩu...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                uiState.ticketError != null -> {
                    Spacer(Modifier.height(48.dp))
                    Text(uiState.ticketError, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Text("Vui lòng quay lại và yêu cầu mã mới.", textAlign = TextAlign.Center)
                }
                else -> ResetPasswordForm(
                    uiState = uiState,
                    passwordVisible = passwordVisible,
                    confirmPasswordVisible = confirmPasswordVisible,
                    onPasswordVisibilityChange = onPasswordVisibilityChange,
                    onConfirmPasswordVisibilityChange = onConfirmPasswordVisibilityChange,
                    onIntent = onIntent
                )
            }
        }
    }
}

@Composable
private fun ResetPasswordForm(
    uiState: ResetPasswordUiState,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onConfirmPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (ResetPasswordIntent) -> Unit
) {
    if (uiState.email.isNotBlank()) {
        Text("Đặt mật khẩu mới cho", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(uiState.email)
    }
    OutlinedTextField(
        value = uiState.newPassword,
        onValueChange = { onIntent(ResetPasswordIntent.PasswordChanged(it)) },
        label = { Text("Mật khẩu mới") },
        isError = uiState.passwordError != null,
        supportingText = { uiState.passwordError?.let { Text(it) } },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        enabled = !uiState.isLoading,
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = { onIntent(ResetPasswordIntent.ConfirmPasswordChanged(it)) },
        label = { Text("Xác nhận mật khẩu") },
        isError = uiState.confirmPasswordError != null,
        supportingText = { uiState.confirmPasswordError?.let { Text(it) } },
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        enabled = !uiState.isLoading,
        trailingIcon = {
            IconButton(onClick = { onConfirmPasswordVisibilityChange(!confirmPasswordVisible) }) {
                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = { onIntent(ResetPasswordIntent.Submit) },
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (uiState.isLoading) CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary
        ) else Text("Đặt lại mật khẩu")
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResetPasswordScreenContentPreview() {
    MyQuizAppTheme {
        ResetPasswordScreenContent(
            uiState = ResetPasswordUiState(email = "user@example.com"),
            snackbarHostState = remember { SnackbarHostState() },
            passwordVisible = false,
            confirmPasswordVisible = false,
            onPasswordVisibilityChange = {},
            onConfirmPasswordVisibilityChange = {},
            onIntent = {}
        )
    }
}
