package android.kma.myquizzapp.presentation.profile

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.domain.profile.ProfileDraft
import android.kma.myquizzapp.domain.profile.ProfileStats
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var pickerOwner by remember { mutableStateOf<SessionUserToken?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onIntent(ProfileIntent.AvatarPicked(it.toString(), pickerOwner)) }
        pickerOwner = null
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onIntent(ProfileIntent.RefreshStats)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Hồ sơ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.isConfirmedGuest -> GuestProfileContent(onSignIn)
                else -> SignedInProfileContent(uiState, onIntent, onOpenSecurity)
            }
        }
    }
    if (uiState.draft != null) ProfileEditorDialog(uiState, onIntent, onPickAvatar)
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
    onOpenSecurity: () -> Unit
) {
    val user = state.user ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(user.avatar, contentDescription = "Ảnh đại diện", size = 88.dp)
                Spacer(Modifier.height(8.dp))
                Text(user.fullname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { ProfileStatsCard(state, onRetry = { onIntent(ProfileIntent.RefreshStats) }) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    ProfileActionRow(
                        icon = Icons.Outlined.PersonOutline,
                        label = "Chỉnh sửa thông tin cá nhân",
                        enabled = state.canEdit,
                        onClick = { onIntent(ProfileIntent.EditProfile) }
                    )
                    ProfileActionRow(
                        icon = Icons.Outlined.Security,
                        label = "Bảo mật tài khoản & Đổi mật khẩu",
                        enabled = state.canEdit,
                        onClick = onOpenSecurity
                    )
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { onIntent(ProfileIntent.Logout) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.isLoggingOut) "Đang đăng xuất…" else "Đăng xuất", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileStatsCard(state: ProfileUiState, onRetry: () -> Unit) {
    val stats = state.profileStats
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileStat(
                    value = stats?.participatedSessionCount,
                    label = "Lượt tham gia",
                    isLoading = state.isLoadingStats,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier.padding(vertical = 4.dp).width(1.dp).height(42.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                ProfileStat(
                    value = stats?.createdQuizCount,
                    label = "Quiz đã tạo",
                    isLoading = state.isLoadingStats,
                    modifier = Modifier.weight(1f)
                )
            }
            if (state.profileStatsError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chưa tải được thống kê",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) { Text("Thử lại") }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: Int?, label: String, isLoading: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLoading && value == null) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = value?.toString() ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    onPickAvatar: () -> Unit
) {
    val draft = state.draft ?: return
    val errors = draft.errors()
    AlertDialog(
        onDismissRequest = { onIntent(ProfileIntent.RequestCloseEdit) },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Chỉnh sửa thông tin cá nhân") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(state.user?.avatar, contentDescription = "Ảnh đại diện hiện tại", size = 64.dp)
                    TextButton(onClick = onPickAvatar, enabled = state.canEdit) { Text("Đổi ảnh đại diện") }
                }
                OutlinedTextField(
                    value = state.user?.email.orEmpty(), onValueChange = {},
                    enabled = false, label = { Text("Email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
        QuizPrimaryButton(text = "Đăng ký/Đăng nhập", onClick = onSignIn)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfilePreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            ProfileUiState(
                session = SessionState.LoggedIn(User(
                    id = 1, fullname = "Nguyễn Hoàng Long", email = "hoanglong@gmail.com", phone = "+84901234567",
                    description = "Cùng học và chơi quiz", createdAt = "2026-10-06", updatedAt = "2026-10-06"
                )),
                profileStats = ProfileStats(participatedSessionCount = 42, createdQuizCount = 12)
            ), {}, {}, {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Đang tải thống kê")
@Composable
private fun ProfileStatsLoadingPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            ProfileUiState(
                session = SessionState.LoggedIn(User(
                    id = 1, fullname = "Người chơi", email = "player@example.com",
                    createdAt = "2026-08-24", updatedAt = "2026-08-30"
                )),
                isLoadingStats = true
            ), {}, {}, {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Lỗi tải thống kê")
@Composable
private fun ProfileStatsErrorPreview() {
    MyQuizAppTheme {
        ProfileScreenContent(
            ProfileUiState(
                session = SessionState.LoggedIn(User(id = 1, fullname = "Người chơi", email = "player@example.com", createdAt = "2026-08-24", updatedAt = "2026-08-30")),
                profileStatsError = "SERVER_ERROR"
            ), {}, {}, {}, remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true, name = "Chỉnh sửa thông tin cá nhân")
@Composable
private fun ProfileEditPreview() {
    val user = User(
        id = 1, fullname = "Nguyễn Hoàng Long", email = "hoanglong@gmail.com", phone = "+84901234567",
        description = "Cùng học và chơi quiz", createdAt = "2026-10-06", updatedAt = "2026-10-06"
    )
    MyQuizAppTheme {
        ProfileScreenContent(
            ProfileUiState(session = SessionState.LoggedIn(user), draft = ProfileDraft.from(user)),
            {}, {}, {}, remember { SnackbarHostState() }
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
