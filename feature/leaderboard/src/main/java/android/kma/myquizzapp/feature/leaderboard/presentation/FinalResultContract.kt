package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.LeaderboardRow

data class FinalResultUiState(val gameId: Long, val playerId: Long, val result: GameEnded? = null) {
    val leaderboard: List<LeaderboardRow> get() = result?.leaderboard.orEmpty()
    val currentPlayer: LeaderboardRow? get() = leaderboard.firstOrNull { it.id == playerId }
    val isMissing: Boolean get() = result == null
    val isLeaderboardHidden: Boolean get() = result != null && leaderboard.isEmpty()
}
