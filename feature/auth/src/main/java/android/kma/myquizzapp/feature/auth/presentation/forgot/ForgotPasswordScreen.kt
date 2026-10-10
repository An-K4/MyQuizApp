package android.kma.myquizzapp.feature.auth.presentation.forgot

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.kma.myquizzapp.feature.auth.presentation.PasswordResetFormField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtpVerification: (String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ForgotPasswordEffect.NavigateBack -> onNavigateBack()
                is ForgotPasswordEffect.NavigateToOtpVerification -> {
                    onNavigateToOtpVerification(effect.email)
                }
                is ForgotPasswordEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    ForgotPasswordScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        scrollState = scrollState,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUiState,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState,
    onIntent: (ForgotPasswordIntent) -> Unit,
) {
    Scaffold(
        snackbarHost = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                SnackbarHost(snackbarHostState, Modifier.padding(top = 8.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Quên mật khẩu") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(ForgotPasswordIntent.NavigateBack) }) {
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
            Text(
                "Nhập địa chỉ email liên kết với tài khoản của bạn. Chúng tôi sẽ gửi mã xác thực OTP 6 số để đặt lại mật khẩu.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PasswordResetFormField(
                label = "Email đã đăng ký", value = uiState.email, placeholder = "email@domain.com",
                onValueChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
                icon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(ForgotPasswordIntent.Submit) }),
                enabled = !uiState.isLoading, error = uiState.emailError,
            )
            Spacer(Modifier.height(20.dp))
            QuizPrimaryButton(
                text = "Gửi mã OTP", onClick = { onIntent(ForgotPasswordIntent.Submit) },
                modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading, loading = uiState.isLoading,
            )
        }
    }
}

@Preview(name = "Forgot Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "Forgot Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForgotPasswordScreenPreview() = ForgotContentPreview(ForgotPasswordUiState())

@Preview(name = "Forgot Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ForgotLoadingPreview() = ForgotContentPreview(ForgotPasswordUiState(email = "user@domain.com", isLoading = true))

@Preview(name = "Forgot Error Large Text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun ForgotErrorPreview() = ForgotContentPreview(ForgotPasswordUiState(email = "invalid", emailError = "Email không hợp lệ"))

@Composable
private fun ForgotContentPreview(state: ForgotPasswordUiState) {
    MyQuizAppTheme {
        ForgotPasswordScreenContent(state, remember { SnackbarHostState() }, rememberScrollState(), {})
    }
}
