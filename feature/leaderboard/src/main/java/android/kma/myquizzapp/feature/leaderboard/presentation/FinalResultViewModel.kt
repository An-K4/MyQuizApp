package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameReviewUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FinalResultViewModel @Inject constructor(
    private val results: GameResultRepository,
    private val loadGameReview: LoadGameReviewUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val stored = results.get(gameId)
    private val _uiState = MutableStateFlow(
        FinalResultUiState(
            gameId = gameId,
            playerId = checkNotNull(savedStateHandle["playerId"]),
            mode = stored?.mode,
            result = stored?.result
        )
    )
    val uiState = _uiState.asStateFlow()

    fun handleIntent(intent: FinalResultIntent) {
        when (intent) {
            FinalResultIntent.ToggleReview -> toggleReview()
            FinalResultIntent.RetryReview -> loadReview()
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
