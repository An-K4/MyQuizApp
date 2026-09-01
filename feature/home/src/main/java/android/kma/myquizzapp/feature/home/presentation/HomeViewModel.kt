package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.auth.domain.usecase.GetCurrentUserUseCase
import android.kma.myquizzapp.feature.home.domain.usecase.GetHomeContentUseCase
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
 * ViewModel for Home screen (MVI pattern).
 *
 * Manages home content (sections of quiz cards) for browsing via scroll, và
 * trạng thái đăng nhập hiện tại (currentUser) để quyết định hiện nút
 * đăng nhập hay avatar cạnh nút tìm kiếm.
 *
 * "Của tôi" (N13-14) không còn liên quan tới Home — mục đó giờ nằm
 * trong màn Profile (app-level), điều hướng thẳng sang Route.MyQuizzes.
 *
 * Lưu ý: AuthRepository chỉ có các suspend fun một lần (getCurrentUser,
 * isAuthenticated), không có Flow<User?> phản ứng theo thời gian thực. Vì
 * vậy checkAuthState() cần được gọi lại mệi khi Home resume (xem
 * LifecycleResumeEffect trong HomeScreen) để cập nhật sau khi người dùng
 * đăng nhập/đăng xuất ở màn khác rỚi quay lại.
 *
 * Search functionality is in a separate SearchViewModel/SearchScreen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeContentUseCase: GetHomeContentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        // Load home content on init
        loadHomeContent()
        checkAuthState()
    }

    /**
     * Handle user intents.
     */
    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadHome -> loadHomeContent()
            is HomeIntent.NavigateToSearch -> viewModelScope.launch {
                emitEffect(HomeEffect.NavigateToSearch)
            }
            is HomeIntent.QuizCardClicked -> viewModelScope.launch {
                emitEffect(HomeEffect.NavigateToQuizDetail(intent.quizId))
            }
            is HomeIntent.Retry -> retry()
            is HomeIntent.CheckAuthState -> checkAuthState()
        }
    }

    /**
     * Load home sections from backend.
     */
    private fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHome = true, homeError = null) }

            when (val result = getHomeContentUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            homeSections = result.data,
                            isLoadingHome = false,
                            homeError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHome = false,
                            homeError = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra trạng thái đăng nhập hiện tại. Lỗi (chưa đăng nhập / cookie
     * không hợp lệ) được xử lý y hệt guest — currentUser = null, không hiện
     * lỗi cho người dùng vì đây là trạng thái bình thường của guest.
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is Result.Success -> _uiState.update { it.copy(currentUser = result.data) }
                is Result.Error -> _uiState.update { it.copy(currentUser = null) }
            }
        }
    }

    /**
     * Emit a one-time effect.
     */
    private suspend fun emitEffect(effect: HomeEffect) {
        _effect.send(effect)
    }

    /**
     * Retry failed operation.
     */
    private fun retry() {
        if (_uiState.value.homeError != null) {
            loadHomeContent()
        }
    }
}
