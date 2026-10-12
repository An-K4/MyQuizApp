package android.kma.myquizzapp.presentation.profile

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.ui.components.QuizDangerButton
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.domain.security.SecurityAccount
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect

@Composable
fun AccountSecurityScreen(
    onBack: () -> Unit,
    onSessionEnded: (String) -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    BackHandler { viewModel.onIntent(AccountSecurityIntent.Back) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountSecurityEffect.Back -> onBack()
                is AccountSecurityEffect.Message -> snackbar.showSnackbar(effect.text)
                is AccountSecurityEffect.SessionEnded -> onSessionEnded(effect.text)
            }
        }
    }
    AccountSecurityScreenContent(state, viewModel::onIntent, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecurityScreenContent(
    state: AccountSecurityUiState,
    onIntent: (AccountSecurityIntent) -> Unit,
    snackbar: SnackbarHostState
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardSurface = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val dangerSurface = if (isDark) MaterialTheme.colorScheme.errorContainer else FrontendColors.Danger.copy(alpha = 0.08f)
    val dangerForeground = if (isDark) MaterialTheme.colorScheme.onErrorContainer else FrontendColors.Danger
    val dangerBorder = if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else FrontendColors.Danger.copy(alpha = 0.28f)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        "Bảo mật tài khoản",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onIntent(AccountSecurityIntent.Back) },
                        enabled = state.busy == null && !state.cleanupPending
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                state.account.loading -> item { CircularProgressIndicator() }
                state.account.token == null -> item { Text("Bạn cần đăng nhập để quản lý bảo mật tài khoản.") }
                state.account.googleOnly -> item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface)
                    ) {
                        Text(
                            "Tài khoản này dùng Google và chưa có mật khẩu riêng. Backend hiện chưa hỗ trợ đổi mật khẩu hoặc vô hiệu hóa cho tài khoản Google-only.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Đổi mật khẩu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Đổi mật khẩu không tự đăng xuất các thiết bị khác.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SecretField(
                                "Mật khẩu hiện tại", state.oldPassword,
                                { onIntent(AccountSecurityIntent.OldPassword(it)) },
                                state.canEdit, state.passwordErrors.current
                            )
                            SecretField(
                                "Mật khẩu mới", state.newPassword,
                                { onIntent(AccountSecurityIntent.NewPassword(it)) },
                                state.canEdit, state.passwordErrors.new
                            )
                            SecretField(
                                "Xác nhận mật khẩu mới", state.confirmPassword,
                                { onIntent(AccountSecurityIntent.ConfirmPassword(it)) },
                                state.canEdit, state.passwordErrors.confirm
                            )
                            QuizPrimaryButton(
                                text = if (state.busy == SecurityAction.CHANGE_PASSWORD) "Đang cập nhật…" else "Cập nhật mật khẩu",
                                onClick = { onIntent(AccountSecurityIntent.ChangePassword) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canEdit,
                                loading = state.busy == SecurityAction.CHANGE_PASSWORD
                            )
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = dangerSurface),
                            border = BorderStroke(1.dp, dangerBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = dangerForeground,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Vô hiệu hóa tài khoản",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = dangerForeground
                                    )
                                }
                                Text(
                                    "Bạn sẽ không thể đăng nhập bằng tài khoản này. Đây không phải thao tác xóa toàn bộ dữ liệu; ứng dụng chưa có chức năng tự khôi phục.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                QuizDangerButton(
                                    text = if (state.busy == SecurityAction.DEACTIVATE) "Đang xử lý…" else "Vô hiệu hóa tài khoản",
                                    onClick = { onIntent(AccountSecurityIntent.RequestDeactivation) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = state.canEdit,
                                    loading = state.busy == SecurityAction.DEACTIVATE
                                )
                            }
                        }
                    }
                }
            }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (state.cleanupPending) item {
                QuizPrimaryButton(
                    text = "Dọn phiên trên thiết bị",
                    onClick = { onIntent(AccountSecurityIntent.RetryLocalCleanup) },
                    enabled = state.busy == null,
                    loading = state.busy == SecurityAction.DEACTIVATE
                )
            }
        }
    }
    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(AccountSecurityIntent.CancelDeactivation) },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Vô hiệu hóa tài khoản?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tài khoản sẽ không thể đăng nhập. Vô hiệu hóa không xóa toàn bộ dữ liệu; chỉ tiếp tục nếu bạn chắc chắn.")
                    SecretField(
                        "Mật khẩu xác nhận", state.deactivationPassword,
                        { onIntent(AccountSecurityIntent.DeactivationPassword(it)) },
                        state.canEdit, state.deactivationError
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuizSecondaryButton(
                        text = "Hủy",
                        onClick = { onIntent(AccountSecurityIntent.CancelDeactivation) },
                        enabled = state.busy == null,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    QuizDangerButton(
                        text = if (state.busy == SecurityAction.DEACTIVATE) "Đang xử lý…" else "Xác nhận",
                        onClick = { onIntent(AccountSecurityIntent.ConfirmDeactivation) },
                        enabled = state.canEdit,
                        loading = state.busy == SecurityAction.DEACTIVATE,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        )
    }
}

/** Each field owns only its visibility toggle; password values stay in state. */
@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean, error: String?) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                )
            }
        },
        supportingText = { if (error != null) Text(error) }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityPreview() {
    MyQuizAppTheme {
        AccountSecurityScreenContent(
            AccountSecurityUiState(account = SecurityAccount(SessionUserToken(7, 1), false, false)),
            {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Vô hiệu hóa — yêu cầu xác nhận")
@Composable
private fun DeactivationConfirmationPreview() {
    MyQuizAppTheme {
        AccountSecurityScreenContent(
            AccountSecurityUiState(
                account = SecurityAccount(SessionUserToken(7, 1), false, false),
                showConfirmation = true
            ),
            {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Google-only")
@Composable
private fun GoogleSecurityPreview() {
    MyQuizAppTheme {
        AccountSecurityScreenContent(
            AccountSecurityUiState(account = SecurityAccount(SessionUserToken(7, 1), false, true)),
            {}, remember { SnackbarHostState() }
        )
    }
}
