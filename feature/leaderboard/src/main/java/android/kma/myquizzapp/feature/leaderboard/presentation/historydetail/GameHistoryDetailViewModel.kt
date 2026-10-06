package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.error.hasApiCode
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistoryAnswersUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistorySummaryUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameHistoryDetailViewModel @Inject constructor(
    private val loadHistorySummary: LoadGameHistorySummaryUseCase,
    private val loadHistoryAnswers: LoadGameHistoryAnswersUseCase,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val _uiState = MutableStateFlow(GameHistoryDetailUiState(gameId = gameId))
    val uiState = _uiState.asStateFlow()
    private var summaryRequestInFlight = false
    private var answersRequestInFlight = false

    init {
        viewModelScope.launch {
            sessionRepository.state
                .map(::identityKey)
                .distinctUntilChanged()
                .collect { key ->
                    if (key != "unknown") {
                        _uiState.value = GameHistoryDetailUiState(gameId = gameId)
                        loadSummary()
                    }
                }
        }
    }

    fun onIntent(intent: GameHistoryDetailIntent) {
        when (intent) {
            GameHistoryDetailIntent.RetrySummary -> loadSummary()
            GameHistoryDetailIntent.RetryAnswers -> loadAnswers()
        }
    }

    private fun loadSummary() {
        if (summaryRequestInFlight) return
        val session = sessionRepository.state.value
        if (session is SessionState.Unknown) return
        summaryRequestInFlight = true
        viewModelScope.launch {
            _uiState.update { it.copy(isSummaryLoading = true, summaryError = null) }
            when (val result = loadHistorySummary(gameId, session)) {
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
            summaryRequestInFlight = false
        }
    }

    private fun loadAnswers() {
        if (answersRequestInFlight || _uiState.value.playerId == null) return
        val session = sessionRepository.state.value
        if (session is SessionState.Unknown) return
        answersRequestInFlight = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAnswersLoading = true, answersError = null, reviewDisabled = false)
            }
            when (val result = loadHistoryAnswers(gameId, session)) {
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
            answersRequestInFlight = false
        }
    }

    private fun identityKey(session: SessionState): String = when (session) {
        SessionState.Unknown -> "unknown"
        SessionState.Guest -> "guest"
        is SessionState.LoggedIn -> "user:${session.user.id}"
    }
}
