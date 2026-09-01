package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.auth.domain.usecase.GetCurrentUserUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.LogoutUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho màn Profile.
 *
 * Tải thông tin user hiện tại (tái sử dụng GetCurrentUserUseCase của
 * feature:auth — không tạo lại logic kiểm tra đăng nhập) và xử lý đăng
 * xuất. Mục "Xem quiz của tôi" chỉ là nav trigger (không qua ViewModel) sang
 * Route.MyQuizzes (QuizManageListScreen, feature:quiz-manage).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getCurrentUserUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(user = result.data, isLoading = false, error = null)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.toUserMessage())
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            when (logoutUseCase()) {
                is Result.Success -> emitEffect(ProfileEffect.NavigateBack)
                is Result.Error -> {
                    // Kể cả khi call logout API lỗi (ví dụ mất mạng), vẫn coi như đã
                    // đăng xuất ở client để không kẹt người dùng lại màn Profile.
                    emitEffect(ProfileEffect.NavigateBack)
                }
            }
        }
    }

    private suspend fun emitEffect(effect: ProfileEffect) {
        _effect.send(effect)
    }
}
