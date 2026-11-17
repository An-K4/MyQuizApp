package android.kma.myquizzapp.feature.auth.presentation.reset

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.auth.presentation.PasswordResetFormField
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollState = rememberScrollState()
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
        scrollState = scrollState,
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
    scrollState: ScrollState,
    onIntent: (ResetPasswordIntent) -> Unit,
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
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scrollState)
                .padding(horizontal = 20.dp).padding(top = 32.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            when {
                uiState.isCheckingTicket -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Đang kiểm tra phiên đặt lại mật khẩu...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                uiState.ticketError != null -> {
                    Text(uiState.ticketError, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Vui lòng quay lại và yêu cầu mã mới.")
                }

                else -> ResetPasswordForm(
                    uiState, passwordVisible, confirmPasswordVisible,
                    onPasswordVisibilityChange, onConfirmPasswordVisibilityChange, onIntent,
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
    onIntent: (ResetPasswordIntent) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        if (uiState.email.isNotBlank()) {
            Text(
                "Đặt lại mật khẩu cho ${uiState.email}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.3.sp,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
        PasswordResetFormField(
            label = "Mật khẩu",
            value = uiState.newPassword,
            placeholder = "Tối thiểu 8 ký tự",
            onValueChange = { onIntent(ResetPasswordIntent.PasswordChanged(it)) },
            icon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            enabled = !uiState.isLoading,
            error = uiState.passwordError,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = onPasswordVisibilityChange,
        )
        Spacer(Modifier.height(16.dp))
        PasswordResetFormField(
            label = "Xác nhận mật khẩu",
            value = uiState.confirmPassword,
            placeholder = "Nhập lại mật khẩu",
            onValueChange = { onIntent(ResetPasswordIntent.ConfirmPasswordChanged(it)) },
            icon = Icons.Outlined.Lock,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onIntent(ResetPasswordIntent.Submit) }),
            enabled = !uiState.isLoading,
            error = uiState.confirmPasswordError,
            passwordVisible = confirmPasswordVisible,
            onPasswordVisibilityChange = onConfirmPasswordVisibilityChange,
        )
        Spacer(Modifier.height(24.dp))
        QuizPrimaryButton(
            text = "Đặt lại mật khẩu",
            onClick = { onIntent(ResetPasswordIntent.Submit) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            loading = uiState.isLoading,
        )
    }
}

@Preview(
    name = "Reset Light",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Preview(
    name = "Reset Dark",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ResetPasswordScreenContentPreview() =
    ResetContentPreview(ResetPasswordUiState(email = "user@domain.com"))

@Preview(name = "Reset Validation Errors", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResetErrorsPreview() = ResetContentPreview(
    ResetPasswordUiState(
        email = "user@domain.com",
        newPassword = "short",
        confirmPassword = "different",
        passwordError = "Mật khẩu tối thiểu 8 ký tự",
        confirmPasswordError = "Mật khẩu xác nhận không khớp"
    )
)

@Preview(name = "Reset Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResetLoadingPreview() = ResetContentPreview(
    ResetPasswordUiState(
        email = "user@domain.com",
        newPassword = "preview-only",
        confirmPassword = "preview-only",
        isLoading = true
    )
)

@Preview(name = "Reset Checking Ticket", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResetCheckingPreview() =
    ResetContentPreview(ResetPasswordUiState(isCheckingTicket = true))

@Preview(name = "Reset Expired Ticket", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ResetExpiredPreview() =
    ResetContentPreview(ResetPasswordUiState(ticketError = "Phiên đặt lại mật khẩu đã hết hạn."))

@Preview(
    name = "Reset Narrow Large Text",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun ResetNarrowPreview() =
    ResetContentPreview(ResetPasswordUiState(email = "long.email.address@domain.com"))

@Composable
private fun ResetContentPreview(state: ResetPasswordUiState) {
    MyQuizAppTheme {
        ResetPasswordScreenContent(
            state,
            remember { SnackbarHostState() },
            false,
            false,
            {},
            {},
            rememberScrollState(),
            {})
    }
}
