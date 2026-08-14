package com.example.myquizzapp.auth.presentation.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.myquizzapp.auth.domain.usecase.GetCurrentUserUseCase
import com.example.myquizzapp.auth.domain.usecase.LogoutUseCase
import com.example.myquizzapp.core.common.result.Result
import com.example.myquizzapp.core.common.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostHomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    data class UiState(val fullname: String = "", val isLoggingOut: Boolean = false)
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private val _loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOut = _loggedOut.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Nếu cookie auth đúng, call này trả về user thật — bằng chứng M2.
            getCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(fullname = user.fullname) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            logoutUseCase()
            _loggedOut.send(Unit)
        }
    }
}

@Composable
fun HostHomePlaceholder(onLoggedOut: () -> Unit, viewModel: HostHomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loggedOut.collect { onLoggedOut() } }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Xin chào, ${uiState.fullname.ifBlank { "..." }}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = viewModel::logout, enabled = !uiState.isLoggingOut) { Text("Đăng xuất") }
    }
}