package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.auth.domain.usecase.ObserveSessionUseCase
import android.kma.myquizzapp.feature.home.domain.usecase.GetHomeContentUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen (MVI pattern).
 *
 * Manages home content (sections of quiz cards) for browsing via scroll, cộng
 * với trạng thái đăng nhập để quyết định có hiện lối đăng nhập ở top bar hay
 * không.
 *
 * N19.6 — đã bỏ `checkAuthState()` và `HomeIntent.CheckAuthState`. Trước đây
 * Home phải tự gọi lại `GET /users/me` mỗi lần ON_RESUME vì không có cách nào
 * biết người dùng vừa đăng nhập/đăng xuất ở màn khác — tức là mỗi lần quay
 * về Home là một request, chỉ để đồng bộ lại một bản sao. Giờ
 * [ObserveSessionUseCase] đẩy thay đổi tới nơi, không ai phải đi hỏi lại.
 *
 * Search functionality is in a separate SearchViewModel/SearchScreen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeContentUseCase: GetHomeContentUseCase,
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    /** Phần state do chính Home sở hữu (nội dung + loading + lỗi). */
    private val _contentState = MutableStateFlow(HomeUiState())

    /**
     * State cuối = state của Home + phiên đăng nhập đọc từ nguồn chung.
     *
     * Home KHÔNG sở hữu trường [HomeUiState.session] — nó chỉ đi qua đây. Đó là
     * lý do dùng `combine` chứ không phải copy giá trị vào `_contentState`:
     * copy là tạo lại một bản sao có thể cũ.
     */
    val uiState: StateFlow<HomeUiState> =
        combine(_contentState, observeSession()) { content, session ->
            content.copy(session = session)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    private val _effect = Channel<HomeEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadHomeContent()
    }

    /**
     * Handle user intents.
     */
    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadHome -> loadHomeContent()
            is HomeIntent.NavigateToSearch -> viewModelScope.launch {
                emitEffect(HomeEffect.NavigateToSearch)
            }
            is HomeIntent.QuizCardClicked -> viewModelScope.launch {
                emitEffect(HomeEffect.NavigateToQuizDetail(intent.quizId))
            }
            is HomeIntent.Retry -> retry()
        }
    }

    /**
     * Load home sections from backend.
     */
    private fun loadHomeContent() {
        viewModelScope.launch {
            _contentState.update { it.copy(isLoadingHome = true, homeError = null) }

            when (val result = getHomeContentUseCase()) {
                is Result.Success -> {
                    _contentState.update {
                        it.copy(
                            homeSections = result.data,
                            isLoadingHome = false,
                            homeError = null
                        )
                    }
                }
                is Result.Error -> {
                    _contentState.update {
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
     * Emit a one-time effect.
     */
    private suspend fun emitEffect(effect: HomeEffect) {
        _effect.send(effect)
    }

    /**
     * Retry failed operation.
     */
    private fun retry() {
        if (_contentState.value.homeError != null) {
            loadHomeContent()
        }
    }
}
