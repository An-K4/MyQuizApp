package android.kma.myquizzapp.feature.auth.presentation.register

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme

import android.kma.myquizzapp.core.ui.components.CustomClickableText
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

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
                RegisterEffect.NavigateToHostHome -> onRegisterSuccess()
                is RegisterEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    RegisterScreenContent(
        uiState = uiState,
        passwordVisible = passwordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        onIntent = viewModel::onIntent,
        onBackToLogin = onBackToLogin,
        snackbarHostState = snackbarHostState,
        scrollState = rememberScrollState(),
    )
}

/** Stateless register UI; values, visibility and scroll state are supplied by the caller. */
@Composable
fun RegisterScreenContent(
    uiState: RegisterUiState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onIntent: (RegisterIntent) -> Unit,
    onBackToLogin: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scrollState: ScrollState,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val foreground = if (isDark) scheme.onBackground else FrontendColors.Foreground
    val muted = if (isDark) scheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border = if (isDark) scheme.outlineVariant else FrontendColors.Border
    val inputStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp, lineHeight = 20.3.sp, letterSpacing = 0.sp,
    )
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
                "Bắt đầu hành trình 🚀",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp, lineHeight = 36.sp,
                    letterSpacing = 0.sp, fontWeight = FontWeight.Bold,
                ),
                color = foreground,
            )
            Spacer(Modifier.height(8.dp))
            Text("Tạo tài khoản để thi đấu và chia sẻ bộ câu hỏi", style = inputStyle, color = muted)
            Spacer(Modifier.height(20.dp))

            RegisterFormField(
                label = "Họ và tên",
                value = uiState.fullname,
                placeholder = "Nguyễn Văn A",
                onValueChange = { onIntent(RegisterIntent.FullnameChanged(it)) },
                error = uiState.fullnameError,
                icon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                enabled = !uiState.isLoading,
                foreground = foreground, inputStyle = inputStyle, colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            RegisterFormField(
                label = "Email",
                value = uiState.email,
                placeholder = "example@email.com",
                onValueChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                error = uiState.emailError,
                icon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                enabled = !uiState.isLoading,
                foreground = foreground, inputStyle = inputStyle, colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            RegisterFormField(
                label = "Mật khẩu",
                value = uiState.password,
                placeholder = "Tối thiểu 8 ký tự",
                onValueChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                error = uiState.passwordError,
                icon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                enabled = !uiState.isLoading,
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = onPasswordVisibilityChange,
                foreground = foreground, inputStyle = inputStyle, colors = fieldColors,
            )
            Spacer(Modifier.height(12.dp))
            RegisterFormField(
                label = "Số điện thoại (tùy chọn)",
                value = uiState.phone,
                placeholder = "Số điện thoại",
                onValueChange = { onIntent(RegisterIntent.PhoneChanged(it)) },
                error = uiState.phoneError,
                icon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!uiState.isLoading) onIntent(RegisterIntent.Submit)
                }),
                enabled = !uiState.isLoading,
                foreground = foreground, inputStyle = inputStyle, colors = fieldColors,
            )
            Spacer(Modifier.height(20.dp))
            QuizPrimaryButton(
                text = "Đăng ký",
                onClick = { onIntent(RegisterIntent.Submit) },
                loading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            CustomClickableText(
                startText = "Đã có tài khoản?",
                clickableText = "Đăng nhập",
                clickableTextTag = "login",
                onTextClicked = onBackToLogin,
                textSize = 12.sp,
                enabled = !uiState.isLoading,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Private stateless field with an external label, matching the register design. */
@Composable
private fun RegisterFormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    error: String?,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
    enabled: Boolean,
    foreground: Color,
    inputStyle: TextStyle,
    colors: TextFieldColors,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    passwordVisible: Boolean? = null,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
) {
    Column {
        Text(label, color = foreground, style = inputStyle.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = inputStyle) },
            leadingIcon = { Icon(icon, null, Modifier.size(20.dp)) },
            trailingIcon = if (passwordVisible != null) {
                {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }, enabled = enabled) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            Modifier.size(20.dp),
                        )
                    }
                }
            } else null,
            visualTransformation = if (passwordVisible == false) PasswordVisualTransformation() else VisualTransformation.None,
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = inputStyle,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
                .semantics { contentDescription = label },
            colors = colors,
        )
    }
}

@Preview(name = "Register Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Composable
fun RegisterScreenPreviewLight() = RegisterContentPreview(RegisterUiState())

@Preview(name = "Register Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegisterScreenPreviewDark() = RegisterContentPreview(RegisterUiState())

@Preview(name = "Register Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RegisterLoadingPreview() = RegisterContentPreview(
    RegisterUiState(fullname = "Nguyễn Văn A", email = "preview@example.com", password = "preview-only", isLoading = true),
)

@Preview(name = "Register Validation Errors", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RegisterErrorsPreview() = RegisterContentPreview(
    RegisterUiState(
        fullnameError = "Vui lòng nhập họ và tên",
        email = "email", emailError = "Email không hợp lệ",
        passwordError = "Mật khẩu phải có ít nhất 8 ký tự",
        phone = "123", phoneError = "Số điện thoại không hợp lệ",
    ),
)

@Preview(name = "Register Narrow Large Text", showBackground = true, widthDp = 320, heightDp = 740, fontScale = 1.5f)
@Composable
private fun RegisterLargeTextPreview() = RegisterContentPreview(RegisterUiState())

@Composable
private fun RegisterContentPreview(uiState: RegisterUiState) {
    MyQuizAppTheme {
        RegisterScreenContent(
            uiState = uiState,
            passwordVisible = false,
            onPasswordVisibilityChange = {},
            onIntent = {},
            onBackToLogin = {},
            snackbarHostState = remember { SnackbarHostState() },
            scrollState = rememberScrollState(),
        )
    }
}

