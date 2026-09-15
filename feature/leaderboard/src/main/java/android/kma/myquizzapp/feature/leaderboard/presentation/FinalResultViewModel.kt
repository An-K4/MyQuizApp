package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.repository.GameResultRepository
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class FinalResultViewModel @Inject constructor(
    private val results: GameResultRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val _uiState = MutableStateFlow(FinalResultUiState(
        gameId = gameId,
        playerId = checkNotNull(savedStateHandle["playerId"]),
        result = results.get(gameId)?.result
    ))
    val uiState = _uiState.asStateFlow()
    fun consume() = results.clear(gameId)
}
