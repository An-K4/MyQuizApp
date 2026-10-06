package android.kma.myquizzapp.presentation.activity

import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.SessionState

data class HistoryListState(
    val items: List<GameHistoryItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val hasLoaded: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null
)

data class ActivityUiState(
    val session: SessionState = SessionState.Unknown,
    val selectedRole: GameHistoryRole = GameHistoryRole.PLAYED,
    val played: HistoryListState = HistoryListState(),
    val hosted: HistoryListState = HistoryListState()
) {
    val showRoleTabs: Boolean get() = session is SessionState.LoggedIn
    val currentList: HistoryListState
        get() = if (selectedRole == GameHistoryRole.PLAYED) played else hosted
}

sealed interface ActivityIntent {
    data class SelectRole(val role: GameHistoryRole) : ActivityIntent
    data object Refresh : ActivityIntent
    data object Retry : ActivityIntent
    data object LoadMore : ActivityIntent
}
