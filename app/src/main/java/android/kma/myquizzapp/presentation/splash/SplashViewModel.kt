package android.kma.myquizzapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.auth.domain.usecase.GetCurrentUserUseCase
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase, // từ feature:auth — :app được phép phụ thuộc
) : ViewModel() {

    enum class UiState { Loading, LoggedIn, LoggedOut }

    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = when (getCurrentUser()) {
                is Result.Success -> UiState.LoggedIn
                // 401 là LUỒNG BÌNH THƯỜNG (chưa login / cookie hết hạn cả cặp).
                // Lỗi khác (500, mất mạng) tạm coi như LoggedOut ở M2 — backlog: màn offline + nút retry.
                is Result.Error -> UiState.LoggedOut
            }
        }
    }
}