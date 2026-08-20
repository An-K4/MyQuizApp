package android.kma.myquizzapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.core.datastore.usecase.CheckAuthStateUseCase
import android.kma.myquizzapp.core.common.model.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuthState: CheckAuthStateUseCase,
) : ViewModel() {

    enum class UiState {
        Loading,  // Đang check auth state
        Ready     // Check xong → navigate thẳng vào Home (Option B: Browse-First)
    }

    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val authState = checkAuthState()

            // TODO(N12+): refresh-token / verify-session chưa được implement.
            // Khi có, nếu authState == AuthState.AUTHENTICATED thì gọi ở đây
            // (chạy nền, không chặn điều hướng) để giữ phiên đăng nhập.
            if (authState == AuthState.AUTHENTICATED) {
                // no-op for now - refresh token use case not implemented yet
            }

            // Option B (Browse-First): luôn vào Home dù là guest hay đã
            // authenticated. authState chỉ dùng để các feature khác quyết
            // định có yêu cầu đăng nhập hay không - không rẽ nhánh ở Splash.
            _uiState.value = UiState.Ready
        }
    }
}
