package android.kma.myquizzapp.presentation.profile

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect

@Composable
fun ProfileScreen(
    onNavigateToAuth: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenSecurity: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickerOwner by remember { mutableStateOf<SessionUserToken?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onIntent(ProfileIntent.AvatarPicked(it.toString(), pickerOwner)) }
        pickerOwner = null
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateBack -> onLoggedOut()
                is ProfileEffect.ShowError -> snackbar.showSnackbar(effect.message)
                is ProfileEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
            }
        }
    }
    ProfileScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onSignIn = onNavigateToAuth,
        onPickAvatar = {
            pickerOwner = uiState.sessionToken
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        snackbarHostState = snackbar,
        modifier = modifier,
        onOpenSecurity = onOpenSecurity
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onSignIn: () -> Unit,
    onPickAvatar: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onOpenSecurity: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Hồ sơ") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.isConfirmedGuest -> GuestProfileContent(onSignIn)
                else -> SignedInProfileContent(uiState, onIntent, onPickAvatar, onOpenSecurity)
            }
        }
    }
    if (uiState.draft != null) ProfileEditorDialog(uiState, onIntent)
    if (uiState.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(ProfileIntent.KeepEditing) },
            title = { Text("Bỏ thay đổi?") },
            text = { Text("Thông tin vừa chỉnh sửa chưa được lưu.") },
            confirmButton = { TextButton(onClick = { onIntent(ProfileIntent.DiscardEdits) }) { Text("Bỏ thay đổi") } },
            dismissButton = { TextButton(onClick = { onIntent(ProfileIntent.KeepEditing) }) { Text("Tiếp tục sửa") } }
        )
    }
    if (uiState.selectedAvatarUri != null) AvatarPreviewDialog(uiState, onIntent)
}

@Composable
private fun SignedInProfileContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onPickAvatar: () -> Unit,
    onOpenSecurity: () -> Unit
) {
    val user = state.user ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(user.avatar, contentDescription = "Ảnh đại diện", size = 72.dp)
                Column(Modifier.weight(1f)) {
                    Text(user.fullname, style = MaterialTheme.typography.titleLarge)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            OutlinedButton(onClick = onPickAvatar, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Đổi ảnh đại diện")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Số điện thoại", style = MaterialTheme.typography.labelLarge)
                    Text(user.phone?.takeIf { it.isNotBlank() } ?: "Chưa thêm số điện thoại")
                    Text("Giới thiệu", style = MaterialTheme.typography.labelLarge)
                    Text(user.description?.takeIf { it.isNotBlank() } ?: "Chưa có giới thiệu")
                    Text("Email không thể thay đổi.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Button(
                onClick = { onIntent(ProfileIntent.EditProfile) }, enabled = state.canEdit,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Chỉnh sửa hồ sơ") }
        }
        item {
            OutlinedButton(onClick = onOpenSecurity, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Bảo mật tài khoản")
            }
        }
        item {
            TextButton(
                onClick = { onIntent(ProfileIntent.Logout) }, enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.isLoggingOut) "Đang đăng xuất…" else "Đăng xuất") }
        }
    }
}

@Composable
private fun ProfileEditorDialog(state: ProfileUiState, onIntent: (ProfileIntent) -> Unit) {
    val draft = state.draft ?: return
    val errors = draft.errors()
    AlertDialog(
        onDismissRequest = { onIntent(ProfileIntent.RequestCloseEdit) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Chỉnh sửa hồ sơ") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(state.user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = draft.fullname, onValueChange = { onIntent(ProfileIntent.ChangeFullname(it)) },
                    enabled = !state.isBusy, label = { Text("Họ tên") }, singleLine = true,
                    isError = errors.fullname != null,
                    supportingText = { Text(errors.fullname ?: "${draft.fullname.trim().length}/100") },
                    modifier = Modifier.fillMaxWidth()
                )
                val phoneError = errors.phone ?: state.phoneServerError
                OutlinedTextField(
                    value = draft.phone, onValueChange = { onIntent(ProfileIntent.ChangePhone(it)) },
                    enabled = !state.isBusy, label = { Text("Số điện thoại") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    supportingText = { Text(phoneError ?: "Để trống để xóa số điện thoại") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.description, onValueChange = { onIntent(ProfileIntent.ChangeDescription(it)) },
                    enabled = !state.isBusy, label = { Text("Giới thiệu") }, minLines = 3, maxLines = 5,
                    isError = errors.description != null,
                    supportingText = { Text(errors.description ?: "${draft.description.trim().length}/200") },
                    modifier = Modifier.fillMaxWidth()
                )
                state.profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onIntent(ProfileIntent.SaveProfile) },
                enabled = !state.isBusy && draft.isDirty && !errors.hasErrors && state.phoneServerError == null
            ) {
                if (state.isSavingProfile) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ProfileIntent.RequestCloseEdit) }, enabled = !state.isBusy) { Text("Hủy") }
        }
    )
}

@Composable
private fun AvatarPreviewDialog(state: ProfileUiState, onIntent: (ProfileIntent) -> Unit) {
    val pending = state.pendingAvatar != null
    AlertDialog(
        onDismissRequest = { onIntent(ProfileIntent.CancelAvatar) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Ảnh đại diện mới") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(state.selectedAvatarUri, contentDescription = "Xem trước ảnh đại diện", size = 160.dp)
                Text("Ảnh sẽ được resize/nén trước khi tải lên. Chưa có thao tác crop ảnh.", style = MaterialTheme.typography.bodySmall)
                state.avatarError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onIntent(if (pending) ProfileIntent.VerifyAvatar else ProfileIntent.ConfirmAvatar) },
                enabled = !state.isBusy
            ) {
                if (state.isUpdatingAvatar) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (pending) "Kiểm tra lại" else "Cập nhật ảnh")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onIntent(if (pending) ProfileIntent.Logout else ProfileIntent.CancelAvatar) },
                enabled = !state.isBusy
            ) { Text(if (pending) "Đăng xuất" else "Hủy") }
        }
    )
}

@Composable
private fun GuestProfileContent(onSignIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bạn đang chơi với tư cách khách", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Đăng nhập để tạo quiz, mở phòng chơi và lưu lịch sử những trận đã tham gia.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSignIn) { Text("Đăng ký/Đăng nhập") }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfilePreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            ProfileUiState(session = SessionState.LoggedIn(User(
                id = 1, fullname = "Người chơi", email = "player@example.com", phone = "+84901234567",
                description = "Cùng học và chơi quiz", createdAt = "2026-10-06", updatedAt = "2026-10-06"
            ))), {}, {}, {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Khách")
@Composable
private fun GuestPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(ProfileUiState(session = SessionState.Guest), {}, {}, {}, remember { SnackbarHostState() })
    }
}
