package android.kma.myquizzapp.feature.auth.presentation.otp

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
import android.kma.myquizzapp.core.ui.components.CustomClickableText
import android.kma.myquizzapp.core.ui.components.SixCharacterCodeFieldContent
import android.kma.myquizzapp.core.ui.components.SixCharacterCodeKind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (ticket: String, email: String) -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Text belongs to the ViewModel; only selection/focus are local display state.
    var otpSelection by remember { mutableStateOf(TextRange.Zero) }
    var otpFocused by remember { mutableStateOf(false) }
    val otpValue = TextFieldValue(
        uiState.otp,
        TextRange(otpSelection.start.coerceIn(0, uiState.otp.length), otpSelection.end.coerceIn(0, uiState.otp.length)),
    )

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
        scrollState = scrollState,
        otpValue = otpValue,
        onOtpValueChange = {
            otpSelection = it.selection
            viewModel.onIntent(OtpVerificationIntent.OtpChanged(it.text))
        },
        otpFocused = otpFocused,
        onOtpFocusChange = { otpFocused = it },
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreenContent(
    uiState: OtpVerificationUiState,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState,
    otpValue: TextFieldValue,
    onOtpValueChange: (TextFieldValue) -> Unit,
    otpFocused: Boolean,
    onOtpFocusChange: (Boolean) -> Unit,
    onIntent: (OtpVerificationIntent) -> Unit,
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
                "Mã 6 chữ số đã được gửi tới email ${uiState.email}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            SixCharacterCodeFieldContent(
                value = otpValue, onValueChange = onOtpValueChange, kind = SixCharacterCodeKind.Otp,
                focused = otpFocused, onFocusChange = onOtpFocusChange,
                modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading,
                keyboardActions = KeyboardActions(onDone = { onIntent(OtpVerificationIntent.Verify) }),
            )
            Spacer(Modifier.height(24.dp))
            val seconds = uiState.resendSecondsLeft.coerceAtLeast(0)
            val countdown = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
            QuizPrimaryButton(
                text = "Xác nhận mã", onClick = { onIntent(OtpVerificationIntent.Verify) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.otp.length == 6, loading = uiState.isLoading,
            )
            Spacer(Modifier.height(20.dp))
            CustomClickableText(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                startText = "Gửi lại mã sau $countdown.",
                clickableText = "Gửi lại mã OTP", onTextClicked = { onIntent(OtpVerificationIntent.ResendCode) },
                textSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = !uiState.isLoading && seconds == 0,
            )
        }
    }
}

@Preview(name = "OTP Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "OTP Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpVerificationScreenContentPreview() = OtpContentPreview(OtpVerificationUiState(email = "user@domain.com", otp = "482", resendSecondsLeft = 54), focused = true)

@Preview(name = "OTP Resend Ready", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OtpReadyPreview() = OtpContentPreview(OtpVerificationUiState(email = "user@domain.com", otp = "123456", resendSecondsLeft = 0))

@Preview(name = "OTP Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OtpLoadingPreview() = OtpContentPreview(OtpVerificationUiState(email = "user@domain.com", otp = "123456", isLoading = true))

@Preview(name = "OTP Narrow Large Text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun OtpNarrowPreview() = OtpContentPreview(OtpVerificationUiState(email = "long.email.address@domain.com"))

@Composable
private fun OtpContentPreview(state: OtpVerificationUiState, focused: Boolean = false) {
    MyQuizAppTheme {
        OtpVerificationScreenContent(
            state, remember { SnackbarHostState() }, rememberScrollState(),
            TextFieldValue(state.otp, TextRange(state.otp.length)), {}, focused, {}, {},
        )
    }
}
