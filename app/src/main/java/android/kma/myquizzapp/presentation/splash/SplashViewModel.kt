package android.kma.myquizzapp.presentation.splash

import android.kma.myquizzapp.feature.auth.domain.usecase.RefreshSessionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash: khởi động việc đọc phiên rồi đi luôn vào Home.
 *
 * N19.6 đổi hai thứ ở đây:
 *
 * 1. `CheckAuthStateUseCase` → [RefreshSessionUseCase]. Cái cũ gọi
 *    `GET /users/me` rồi **ném kết quả đi** (Browse-First không rẽ nhánh ở
 *    Splash), còn ghi cờ guest vào DataStore — tức là một lượt mạng chỉ để
 *    bỏ đó, trong khi ngay sau đó AppNavGraph lại gọi một lượt nữa. Giờ cả
 *    hai đều đổ vào `SessionRepository` và được gộp thành một request.
 * 2. KHÔNG `await` nữa. Trước đây Splash đứng chờ xong một lượt mạng mới cho
 *    vào app, nghĩa là mạng chậm thì ngồi xem màn splash — dù kết quả không
 *    được dùng để quyết định đi đâu. Nay phát lệnh làm mới rồi vào Home
 *    ngay; chỗ nào cần biết chắc phiên (chốt gác, luồng vào phòng) đã tự biết
 *    chờ trạng thái `Unknown`.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val refreshSession: RefreshSessionUseCase,
) : ViewModel() {

    enum class UiState {
        Loading,  // Chưa phát lệnh đọc phiên
        Ready     // Đã phát lệnh → vào Home (Option B: Browse-First)
    }

    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        // Chạy nền: kết quả đi vào SessionRepository, không ai chờ ở đây.
        viewModelScope.launch { refreshSession() }

        // Option B (Browse-First): luôn vào Home dù là khách hay đã đăng nhập.
        // Trạng thái phiên chỉ dùng để các chỗ khác quyết định có yêu cầu đăng
        // nhập hay không — không rẽ nhánh ở Splash.
        _uiState.value = UiState.Ready
    }
}
