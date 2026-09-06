package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.feature.auth.domain.usecase.LogoutUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.ObserveSessionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho màn Profile.
 *
 * N19.6 — KHÔNG còn `init { loadCurrentUser() }`. Đó chính là nguyên nhân bug:
 * màn Hồ sơ là tab của bottom nav nên ViewModel của nó được giữ lại khi đổi
 * tab; `init` chỉ chạy đúng một lần trong cả phiên, nên sau khi đăng xuất và
 * vào lại, state cũ được dọn ra nguyên vẹn cùng thông tin người đã rời đi.
 * Giờ màn này chỉ chiếu lại [ObserveSessionUseCase], không sở hữu dữ liệu user.
 *
 * Vì sao không tự gọi `refresh()` ở đây: AppNavGraph đã làm mới phiên lúc dụng
 * thanh nav và mỗi lần app trở lại foreground. Thêm một lần gọi ở đây chỉ là
 * request trùng — `GET /users/me` không có cache.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = observeSession()
        .map { ProfileUiState(session = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    private val _effect = Channel<ProfileEffect>()
    val effect = _effect.receiveAsFlow()

    fun logout() {
        viewModelScope.launch {
            // Kể cả khi API logout lỗi (mất mạng...) vẫn coi như đã đăng xuất ở
            // client — LogoutUseCase đã dọn cả cookie và session state ở cả hai
            // nhánh, nên không cần phân biệt Success/Error ở đây nữa.
            logoutUseCase()
            _effect.send(ProfileEffect.NavigateBack)
        }
    }
}
