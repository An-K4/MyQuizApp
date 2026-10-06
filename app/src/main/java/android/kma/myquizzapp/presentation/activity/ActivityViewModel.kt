package android.kma.myquizzapp.presentation.activity

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.activity.LoadGameHistoryUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val loadGameHistory: LoadGameHistoryUseCase,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private val jobs = mutableMapOf<GameHistoryRole, Job>()

    init {
        viewModelScope.launch {
            sessionRepository.state
                .map(::identityKey)
                .distinctUntilChanged()
                .collect { onIdentityChanged(sessionRepository.state.value) }
        }
    }

    fun onIntent(intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.SelectRole -> selectRole(intent.role)
            ActivityIntent.Refresh -> refresh()
            ActivityIntent.Retry -> retry()
            ActivityIntent.LoadMore -> loadMore()
        }
    }

    private fun onIdentityChanged(session: SessionState) {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _uiState.value = ActivityUiState(session = session)
        if (session !is SessionState.Unknown) load(GameHistoryRole.PLAYED, reset = true)
    }

    private fun selectRole(role: GameHistoryRole) {
        val state = _uiState.value
        if (role == GameHistoryRole.HOSTED && state.session !is SessionState.LoggedIn) return
        _uiState.update { it.copy(selectedRole = role) }
        if (!listState(role).hasLoaded && !listState(role).isInitialLoading) load(role, reset = true)
    }

    /** Server history có thể đổi khi màn được giữ trong back stack lúc người dùng chơi. */
    private fun refresh() {
        if (_uiState.value.session !is SessionState.Unknown) {
            load(_uiState.value.selectedRole, reset = true)
        }
    }

    private fun retry() {
        val role = _uiState.value.selectedRole
        load(role, reset = listState(role).items.isEmpty())
    }

    private fun loadMore() {
        val role = _uiState.value.selectedRole
        val page = listState(role)
        if (page.hasLoaded && page.hasMore && !page.isLoadingMore && !page.isInitialLoading) {
            load(role, reset = false)
        }
    }

    private fun load(role: GameHistoryRole, reset: Boolean) {
        val session = _uiState.value.session
        if (session is SessionState.Unknown) return
        val before = listState(role)
        if (reset && before.isInitialLoading) return
        if (!reset && (before.isLoadingMore || !before.hasMore)) return

        updateList(role) {
            if (reset) {
                it.copy(
                    isInitialLoading = true,
                    errorMessage = null,
                    appendErrorMessage = null
                )
            } else {
                it.copy(isLoadingMore = true, appendErrorMessage = null)
            }
        }

        jobs[role]?.cancel()
        jobs[role] = viewModelScope.launch {
            val cursor = if (reset) null else before.nextCursor
            when (val result = loadGameHistory(role, cursor, session)) {
                is Result.Success -> updateList(role) { current ->
                    val merged = if (reset) result.data else current.items + result.data
                    current.copy(
                        items = merged.distinctBy { it.sessionId },
                        nextCursor = result.page?.nextCursor,
                        hasMore = result.page?.hasMore == true,
                        hasLoaded = true,
                        isInitialLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        appendErrorMessage = null
                    )
                }
                is Result.Error -> updateList(role) {
                    val message = result.error.toUserMessage()
                    if (reset) {
                        it.copy(
                            hasLoaded = true,
                            isInitialLoading = false,
                            errorMessage = message
                        )
                    } else {
                        it.copy(isLoadingMore = false, appendErrorMessage = message)
                    }
                }
            }
        }
    }

    private fun listState(role: GameHistoryRole): HistoryListState =
        if (role == GameHistoryRole.PLAYED) _uiState.value.played else _uiState.value.hosted

    private fun updateList(role: GameHistoryRole, transform: (HistoryListState) -> HistoryListState) {
        _uiState.update { state ->
            if (role == GameHistoryRole.PLAYED) state.copy(played = transform(state.played))
            else state.copy(hosted = transform(state.hosted))
        }
    }

    private fun identityKey(session: SessionState): String = when (session) {
        SessionState.Unknown -> "unknown"
        SessionState.Guest -> "guest"
        is SessionState.LoggedIn -> "user:${session.user.id}"
    }
}
