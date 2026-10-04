package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.error.isMissingResource
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameResultsUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameReviewUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FinalResultViewModel @Inject constructor(
    private val results: GameResultRepository,
    private val loadGameResults: LoadGameResultsUseCase,
    private val loadGameReview: LoadGameReviewUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val stored = results.get(gameId)
    private var resultRequestInFlight = false
    private val _uiState = MutableStateFlow(
        FinalResultUiState(
            gameId = gameId,
            playerId = checkNotNull(savedStateHandle["playerId"]),
            mode = stored?.mode,
            result = stored?.result,
            isResultLoading = stored == null
        )
    )
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<FinalResultEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        if (stored == null) loadResults()
    }

    fun handleIntent(intent: FinalResultIntent) {
        when (intent) {
            FinalResultIntent.RetryResult -> loadResults()
            FinalResultIntent.ToggleReview -> toggleReview()
            FinalResultIntent.RetryReview -> loadReview()
        }
    }

    private fun loadResults() {
        if (resultRequestInFlight) return
        resultRequestInFlight = true
        viewModelScope.launch {
            _uiState.update { it.copy(isResultLoading = true, resultError = null) }
            when (val result = loadGameResults(gameId)) {
                is Result.Success -> {
                    val restored = result.data
                    val hidden = restored.showLeaderboard == ShowLeaderboard.NEVER
                    _uiState.update {
                        it.copy(
                            mode = restored.mode,
                            result = GameEnded(
                                leaderboard = restored.leaderboard.takeUnless { hidden }.orEmpty(),
                                perQuestion = restored.perQuestion,
                                reviewEnabled = restored.reviewEnabled
                            ),
                            isResultLoading = false,
                            resultError = null,
                            leaderboardHiddenByConfig = hidden
                        )
                    }
                }
                is Result.Error -> {
                    val message = result.error.toUserMessage()
                    if (result.error.isMissingResource("GAME_ROOM_NOT_FOUND")) {
                        _uiState.update { it.copy(isResultLoading = false) }
                        _effect.send(FinalResultEffect.ResourceMissing(message))
                    } else {
                        _uiState.update { it.copy(isResultLoading = false, resultError = message) }
                    }
                }
            }
            resultRequestInFlight = false
        }
    }

    private fun toggleReview() {
        val nextVisible = !_uiState.value.isReviewVisible
        _uiState.update { it.copy(isReviewVisible = nextVisible) }
        if (nextVisible && _uiState.value.review == null) loadReview()
    }

    private fun loadReview() {
        val state = _uiState.value
        if (!state.reviewEnabled || state.isReviewLoading) return
        val token = stored?.socketToken
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isReviewVisible = true,
                    reviewError = "Phiên xem lại không còn sau khi ứng dụng được khởi động lại."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewLoading = true, reviewError = null) }
            when (val result = loadGameReview(gameId, token)) {
                is Result.Success -> _uiState.update {
                    it.copy(review = result.data, isReviewLoading = false, reviewError = null)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isReviewLoading = false, reviewError = result.error.toUserMessage())
                }
            }
        }
    }

    fun consume() = results.clear(gameId)
}
