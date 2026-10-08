package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.error.hasApiCode
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.SessionIdentity
import android.kma.myquizzapp.core.common.model.SessionSnapshot
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistoryAnswersUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistorySummaryUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.ObserveHistorySessionUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class GameHistoryDetailViewModel @Inject constructor(
    private val loadHistorySummary: LoadGameHistorySummaryUseCase,
    private val loadHistoryAnswers: LoadGameHistoryAnswersUseCase,
    private val observeSession: ObserveHistorySessionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val _uiState = MutableStateFlow(GameHistoryDetailUiState(gameId = gameId))
    val uiState = _uiState.asStateFlow()
    private var identity: SessionIdentity? = null
    private var epoch = 0L
    private var summaryJob: Job? = null
    private var answersJob: Job? = null

    init {
        viewModelScope.launch { observeSession().collect { reconcileSession(it) } }
    }

    fun onIntent(intent: GameHistoryDetailIntent) {
        reconcileSession(observeSession.current())
        when (intent) {
            GameHistoryDetailIntent.RetrySummary -> loadSummary()
            GameHistoryDetailIntent.RetryAnswers -> loadAnswers()
        }
    }

    private fun reconcileSession(snapshot: SessionSnapshot) {
        if (identity == snapshot.identity) return
        epoch++
        identity = snapshot.identity
        summaryJob?.cancel()
        answersJob?.cancel()
        summaryJob = null
        answersJob = null
        _uiState.value = GameHistoryDetailUiState(gameId = gameId, isSummaryLoading = false)
        if (snapshot.identity.resolved) loadSummary()
    }

    private fun snapshotForRequest(): SessionSnapshot? {
        val snapshot = observeSession.current()
        if (snapshot.identity != identity) {
            reconcileSession(snapshot)
            return null
        }
        return snapshot.takeIf { it.identity.resolved }
    }

    private fun accepts(snapshot: SessionSnapshot, requestEpoch: Long): Boolean {
        if (requestEpoch != epoch) return false
        if (!observeSession.isCurrent(snapshot.identity)) {
            reconcileSession(observeSession.current())
            return false
        }
        return true
    }

    private fun loadSummary() {
        val snapshot = snapshotForRequest() ?: return
        if (summaryJob?.isActive == true) return
        val requestEpoch = epoch
        summaryJob = viewModelScope.launch {
            _uiState.update { it.copy(isSummaryLoading = true, summaryError = null) }
            val result = loadHistorySummary(gameId, snapshot.state)
            if (!isActive || !accepts(snapshot, requestEpoch)) return@launch
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(summary = result.data, isSummaryLoading = false, summaryError = null)
                    }
                    if (result.data.viewer.playerId != null) loadAnswers()
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSummaryLoading = false, summaryError = result.error.toUserMessage())
                }
            }
        }
    }

    private fun loadAnswers() {
        val snapshot = snapshotForRequest() ?: return
        if (answersJob?.isActive == true || _uiState.value.playerId == null) return
        val requestEpoch = epoch
        answersJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isAnswersLoading = true, answersError = null, reviewDisabled = false)
            }
            val result = loadHistoryAnswers(gameId, snapshot.state)
            if (!isActive || !accepts(snapshot, requestEpoch)) return@launch
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(review = result.data, isAnswersLoading = false, answersError = null)
                }
                is Result.Error -> {
                    val disabled = result.error.hasApiCode("GAME_REVIEW_DISABLED")
                    _uiState.update {
                        it.copy(
                            isAnswersLoading = false,
                            reviewDisabled = disabled,
                            answersError = if (disabled) null else result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }
}
