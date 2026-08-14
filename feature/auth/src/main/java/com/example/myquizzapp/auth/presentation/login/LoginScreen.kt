package com.example.myquizzapp.auth.presentation.login

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.common.util.hashSha256
import com.example.myquizzapp.feature.auth.R
import com.example.ui.style.AppTextStyles
import com.example.ui.theme.GoogleButtonGray
import com.example.ui.theme.MyQuizAppTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
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
        Timber.d("🚀 launchGoogleOneTap: Starting Google One Tap flow")
        scope.launch {
            try {
                // Step 1: Generate nonce
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = rawNonce.hashSha256()
                Timber.d("🔐 Nonce generated - raw: ${rawNonce.take(10)}..., hashed: ${hashedNonce.take(10)}...")

                // Step 2: Build Google ID option
                val serverClientId = "808588686055-l416uahi6qvb0o8n8q0h7mee85avutmc.apps.googleusercontent.com"
                Timber.d("⚙️ Building GoogleIdOption with:")
                Timber.d("   - filterByAuthorizedAccounts: false")
                Timber.d("   - serverClientId: $serverClientId")
                Timber.d("   - nonce: ${hashedNonce.take(20)}...")
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setNonce(hashedNonce)
                    .build()
                Timber.d("✅ GoogleIdOption built successfully")

                // Step 3: Build credential request
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                Timber.d("📋 GetCredentialRequest built, calling credentialManager.getCredential()...")

                // Step 4: Request credentials
                val result = credentialManager.getCredential(context, request)
                Timber.d("✅ Credential received! Type: ${result.credential::class.java.simpleName}")

                // Step 5: Process credential
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    Timber.d("🎫 Valid GoogleIdTokenCredential received")
                    Timber.d("   - ID: ${credential.id}")
                    Timber.d("   - displayName: ${credential.displayName}")
                    Timber.d("   - idToken length: ${credential.idToken.length}")
                    viewModel.onIntent(LoginViewModel.Intent.GoogleTokenReceived(credential.idToken))
                } else {
                    Timber.e("❌ Invalid credential type: ${credential::class.java.name}")
                    snackbarHostState.showSnackbar("Loại credential không hợp lệ")
                }
            } catch (e: GetCredentialCancellationException) {
                Timber.w("⚠️ User cancelled Google One Tap: ${e.message}")
                // User cancelled - silent fail
            } catch (e: NoCredentialException) {
                Timber.e("❌ NoCredentialException caught!")
                Timber.e("   - Message: ${e.message}")
                Timber.e("   - Type: ${e.type}")
                Timber.e("   - Cause: ${e.cause}")
                Timber.e("   - Stack trace: ", e)
                Timber.e("⚠️ POSSIBLE CAUSES:")
                Timber.e("   1. No Google accounts on device")
                Timber.e("   2. Wrong/unconfigured Server Client ID in Firebase Console")
                Timber.e("   3. Google Play Services not updated")
                Timber.e("   4. SHA-1 fingerprint not added to Firebase Console")
                snackbarHostState.showSnackbar("Không tìm thấy tài khoản Google")
            } catch (e: GetCredentialException) {
                Timber.e("❌ GetCredentialException caught!")
                Timber.e("   - Message: ${e.message}")
                Timber.e("   - Type: ${e.type}")
                Timber.e("   - ErrorMessage: ${e.errorMessage}")
                Timber.e("   - Stack trace: ", e)
                snackbarHostState.showSnackbar("Lỗi đăng nhập Google: ${e.message}")
            } catch (e: Exception) {
                Timber.e("❌ Unexpected exception in launchGoogleOneTap!")
                Timber.e("   - Type: ${e::class.java.name}")
                Timber.e("   - Message: ${e.message}")
                Timber.e("   - Stack trace: ", e)
                snackbarHostState.showSnackbar("Lỗi không xác định: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LoginViewModel.Effect.NavigateToHostHome -> onLoginSuccess()
                is LoginViewModel.Effect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
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
        onPlayAsGuest = onPlayAsGuest,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Stateless composable for Login screen UI
 * Pure UI component without ViewModel - Preview-friendly
 */
@Composable
fun LoginScreenContent(
    uiState: LoginViewModel.UiState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (LoginViewModel.Intent) -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoToRegister: () -> Unit,
    onPlayAsGuest: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val density = LocalDensity.current

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // Title
            Text(
                text = "Đăng nhập",
                style = AppTextStyles.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Chào mừng bạn quay trở lại ứng dụng",
                style = AppTextStyles.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onIntent(LoginViewModel.Intent.EmailChanged(it)) },
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

            // Password field with visibility toggle
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onIntent(LoginViewModel.Intent.PasswordChanged(it)) },
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

            // Forgot password link
            Text(
                text = "Quên mật khẩu?",
                style = AppTextStyles.linkText,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* TODO */ }.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Login button
            Button(
                onClick = { onIntent(LoginViewModel.Intent.Submit) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Đăng nhập", style = AppTextStyles.buttonText)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Divider text
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(0.5F))
                Text(
                    text = "hoặc",
                    style = AppTextStyles.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(0.5F))
            }
            Spacer(Modifier.height(16.dp))

            // Google button with gray background
            Button(
                onClick = onGoogleSignIn,
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleButtonGray,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.height(
                            with(density) { AppTextStyles.buttonText.fontSize.toDp() }
                        )
                    )
                    Text(
                        text = "Tiếp tục với Google",
                        style = AppTextStyles.buttonText
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Guest login link
            TextButton(
                onClick = onPlayAsGuest,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Đăng nhập với tư cách khách", style = AppTextStyles.linkText)
            }

            // Create account link
            TextButton(
                onClick = onGoToRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Tạo tài khoản mới", style = AppTextStyles.linkText)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewLight() {
    MyQuizAppTheme {
        LoginScreenContent(
            uiState = LoginViewModel.UiState(),
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onGoogleSignIn = {},
            onGoToRegister = {},
            onPlayAsGuest = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginScreenPreviewDark() {
    MyQuizAppTheme {
        LoginScreenContent(
            uiState = LoginViewModel.UiState(),
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onGoogleSignIn = {},
            onGoToRegister = {},
            onPlayAsGuest = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}