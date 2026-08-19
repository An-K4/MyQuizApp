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
        Loading,      // Đang check auth state
        ShowAuth,     // First launch → navigate to AuthGraph (Login/Register)
        ShowGuest,    // Guest mode → navigate to PlayerGraph/HomeGraph
        ShowHost      // Authenticated → navigate to HostGraph
    }

    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = when (checkAuthState()) {
                AuthState.FIRST_LAUNCH -> UiState.ShowAuth
                AuthState.GUEST_MODE -> UiState.ShowGuest
                AuthState.AUTHENTICATED -> UiState.ShowHost
            }
        }
    }
}