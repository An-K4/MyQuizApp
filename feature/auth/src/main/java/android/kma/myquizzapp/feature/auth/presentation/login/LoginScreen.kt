package android.kma.myquizzapp.feature.auth.presentation.login

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.kma.myquizzapp.core.common.util.hashSha256
import android.kma.myquizzapp.feature.auth.R
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

import android.kma.myquizzapp.core.ui.components.CustomClickableText
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToForgotPassword: () -> Unit,
    onPlayAsGuest: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Google One Tap launcher (kept in UI layer - needs Context)
    fun launchGoogleOneTap() {
        scope.launch {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = rawNonce.hashSha256()

                val serverClientId = "808588686055-l416uahi6qvb0o8n8q0h7mee85avutmc.apps.googleusercontent.com"
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    viewModel.onIntent(LoginIntent.GoogleTokenReceived(credential.idToken))
                } else {
                    Timber.e("Invalid credential type: ${credential::class.java.name}")
                    snackbarHostState.showSnackbar("Loại credential không hợp lệ")
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled - silent fail
            } catch (e: NoCredentialException) {
                Timber.e("NoCredentialException: ${e.message}")
                snackbarHostState.showSnackbar("Không tìm thấy tài khoản Google")
            } catch (e: GetCredentialException) {
                Timber.e("GetCredentialException: ${e.message}")
                snackbarHostState.showSnackbar("Lỗi đăng nhập Google: ${e.message}")
            } catch (e: Exception) {
                Timber.e("Unexpected exception in Google One Tap: ${e.message}")
                snackbarHostState.showSnackbar("Lỗi không xác định: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToHostHome -> onLoginSuccess()
                LoginEffect.NavigateToGuestHome -> onPlayAsGuest()
                LoginEffect.NavigateToForgotPassword -> onGoToForgotPassword()
                is LoginEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        passwordVisible = passwordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        onIntent = viewModel::onIntent,
        onGoogleSignIn = { launchGoogleOneTap() },
        onGoToRegister = onGoToRegister,
        snackbarHostState = snackbarHostState,
        scrollState = rememberScrollState(),
    )
}

/** Stateless login UI; integration and transient UI state belong to LoginScreen. */
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (LoginIntent) -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoToRegister: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val foreground = if (isDark) scheme.onBackground else FrontendColors.Foreground
    val muted = if (isDark) scheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border = if (isDark) scheme.outlineVariant else FrontendColors.Border
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = foreground,
        unfocusedTextColor = foreground,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = border,
        focusedLeadingIconColor = muted,
        unfocusedLeadingIconColor = muted,
        focusedTrailingIconColor = muted,
        unfocusedTrailingIconColor = muted,
        focusedPlaceholderColor = muted,
        unfocusedPlaceholderColor = muted,
        cursorColor = scheme.primary,
    )
    val inputStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp,
    )

    Scaffold(
        containerColor = scheme.background,
        snackbarHost = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                SnackbarHost(snackbarHostState, Modifier.padding(top = 8.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
        ) {
            Text(
                "Chào mừng trở lại! 👋",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp, lineHeight = 36.sp,
                    letterSpacing = 0.sp, fontWeight = FontWeight.Bold,
                ),
                color = foreground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Đăng nhập để lưu lịch sử và quản lý quiz của bạn",
                style = inputStyle,
                color = muted,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                placeholder = { Text("Nhập email của bạn", style = inputStyle) },
                leadingIcon = { Icon(Icons.Outlined.Email, "Email", Modifier.size(20.dp)) },
                isError = uiState.emailError != null,
                supportingText = if (uiState.emailError != null) { { Text(uiState.emailError) } } else null,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                textStyle = inputStyle,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                placeholder = { Text("Nhập mật khẩu", style = inputStyle) },
                leadingIcon = { Icon(Icons.Outlined.Lock, "Mật khẩu", Modifier.size(20.dp)) },
                isError = uiState.passwordError != null,
                supportingText = if (uiState.passwordError != null) { { Text(uiState.passwordError) } } else null,
                enabled = !uiState.isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!uiState.isLoading) onIntent(LoginIntent.Submit)
                }),
                singleLine = true,
                textStyle = inputStyle,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(
                        onClick = { onPasswordVisibilityChange(!passwordVisible) },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            Modifier.size(20.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
            )
            TextButton(
                onClick = { onIntent(LoginIntent.GoToForgotPassword) },
                enabled = !uiState.isLoading,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Quên mật khẩu?", style = inputStyle.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(4.dp))
            QuizPrimaryButton(
                text = "Đăng nhập",
                onClick = { onIntent(LoginIntent.Submit) },
                loading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            QuizSecondaryButton(
                text = "Tiếp tục với tư cách Khách",
                onClick = { onIntent(LoginIntent.PlayAsGuest) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = border)
                Text("Hoặc đăng nhập với", color = muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp))
                HorizontalDivider(Modifier.weight(1f), color = border)
            }
            Spacer(Modifier.height(20.dp))
            QuizSecondaryButton(
                text = "Google",
                onClick = onGoogleSignIn,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                contentColor = foreground,
                leadingIcon = {
                    Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
                },
            )
            Spacer(Modifier.height(20.dp))
            CustomClickableText(
                startText = "Chưa có tài khoản?",
                clickableText = "Đăng ký ngay",
                clickableTextTag = "register",
                onTextClicked = onGoToRegister,
                textSize = 12.sp,
                enabled = !uiState.isLoading,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Login Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Composable
fun LoginScreenPreviewLight() = LoginContentPreview(LoginUiState())

@Preview(name = "Login Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginScreenPreviewDark() = LoginContentPreview(LoginUiState())

@Preview(name = "Login Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginLoadingPreview() = LoginContentPreview(
    LoginUiState(email = "preview@example.com", password = "preview-only", isLoading = true),
)

@Preview(name = "Login Validation Errors", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginErrorsPreview() = LoginContentPreview(
    LoginUiState(email = "email", emailError = "Email không hợp lệ", passwordError = "Vui lòng nhập mật khẩu"),
)

@Preview(name = "Login Narrow Large Text", showBackground = true, widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Composable
private fun LoginLargeTextPreview() = LoginContentPreview(LoginUiState())

@Composable
private fun LoginContentPreview(uiState: LoginUiState) {
    MyQuizAppTheme {
        LoginScreenContent(
            uiState = uiState,
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onGoogleSignIn = {},
            onGoToRegister = {},
            snackbarHostState = remember { SnackbarHostState() },
            scrollState = rememberScrollState(),
        )
    }
}

