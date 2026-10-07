package android.kma.myquizzapp.presentation.profile

import kotlinx.coroutines.flow.collect

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.domain.security.SecurityAccount
import android.kma.myquizzapp.core.common.model.SessionUserToken
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bảo mật tài khoản") }, navigationIcon = {
                TextButton(onClick = { onIntent(AccountSecurityIntent.Back) }, enabled = state.busy == null && !state.cleanupPending) { Text("Quay lại") }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when {
                state.account.loading -> item { CircularProgressIndicator() }
                state.account.token == null -> item { Text("Bạn cần đăng nhập để quản lý bảo mật tài khoản.") }
                state.account.googleOnly -> item {
                    Card(Modifier.fillMaxWidth()) {
                        Text("Tài khoản này dùng Google và chưa có mật khẩu riêng. Backend hiện chưa hỗ trợ đổi mật khẩu hoặc vô hiệu hóa cho tài khoản Google-only.", Modifier.padding(16.dp))
                    }
                }
                else -> {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Đổi mật khẩu", style = MaterialTheme.typography.titleMedium)
                                Text("Đổi mật khẩu không tự đăng xuất các thiết bị khác.", style = MaterialTheme.typography.bodySmall)
                                SecretField("Mật khẩu hiện tại", state.oldPassword, { onIntent(AccountSecurityIntent.OldPassword(it)) }, state.canEdit, state.passwordErrors.current)
                                SecretField("Mật khẩu mới", state.newPassword, { onIntent(AccountSecurityIntent.NewPassword(it)) }, state.canEdit, state.passwordErrors.new)
                                SecretField("Xác nhận mật khẩu mới", state.confirmPassword, { onIntent(AccountSecurityIntent.ConfirmPassword(it)) }, state.canEdit, state.passwordErrors.confirm)
                                Button(onClick = { onIntent(AccountSecurityIntent.ChangePassword) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (state.busy == SecurityAction.CHANGE_PASSWORD) "Đang đổi mật khẩu…" else "Đổi mật khẩu")
                                }
                            }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Vô hiệu hóa tài khoản", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                Text("Bạn sẽ không thể đăng nhập bằng tài khoản này. Đây không phải thao tác xóa toàn bộ dữ liệu; ứng dụng chưa có chức năng tự khôi phục.")
                                SecretField("Mật khẩu xác nhận", state.deactivationPassword, { onIntent(AccountSecurityIntent.DeactivationPassword(it)) }, state.canEdit, state.deactivationError)
                                OutlinedButton(onClick = { onIntent(AccountSecurityIntent.RequestDeactivation) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (state.busy == SecurityAction.DEACTIVATE) "Đang xử lý…" else "Vô hiệu hóa tài khoản", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (state.cleanupPending) item {
                Button(onClick = { onIntent(AccountSecurityIntent.RetryLocalCleanup) }, enabled = state.busy == null) { Text("Dọn phiên trên thiết bị") }
            }
        }
    }
    if (state.showConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(AccountSecurityIntent.CancelDeactivation) },
            title = { Text("Vô hiệu hóa tài khoản?") },
            text = { Text("Tài khoản sẽ không thể đăng nhập. Bạn sẽ được đưa về Trang chủ với tư cách khách. Chỉ tiếp tục khi bạn chắc chắn.") },
            confirmButton = { TextButton(onClick = { onIntent(AccountSecurityIntent.ConfirmDeactivation) }) { Text("Xác nhận vô hiệu hóa", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { onIntent(AccountSecurityIntent.CancelDeactivation) }) { Text("Hủy") } }
        )
    }
}

/** Convenience wrapper owns visibility only; value/business state remains hoisted. */
@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean, error: String?) {
    var visible by remember { mutableStateOf(false) }
    SecretFieldContent(label, value, onChange, enabled, error, visible, { visible = !visible })
}

@Composable
private fun SecretFieldContent(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean, error: String?, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = onToggle, enabled = enabled) { Text(if (visible) "Ẩn" else "Hiện") } },
        supportingText = { if (error != null) Text(error) }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityPreview() {
    MyQuizAppTheme {
        AccountSecurityScreenContent(AccountSecurityUiState(account = SecurityAccount(SessionUserToken(7, 1), false, false)), {}, remember { SnackbarHostState() })
    }
}

@Preview(showBackground = true, name = "Google-only")
@Composable
private fun GoogleSecurityPreview() {
    MyQuizAppTheme {
        AccountSecurityScreenContent(AccountSecurityUiState(account = SecurityAccount(SessionUserToken(7, 1), false, true)), {}, remember { SnackbarHostState() })
    }
}
