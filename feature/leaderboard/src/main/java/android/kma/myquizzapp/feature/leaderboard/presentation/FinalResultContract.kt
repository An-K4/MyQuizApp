package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.LeaderboardRow

data class FinalResultUiState(
    val gameId: Long,
    val playerId: Long,
    val mode: GameMode? = null,
    val result: GameEnded? = null,
    val review: GameReview? = null,
    val isReviewVisible: Boolean = false,
    val isReviewLoading: Boolean = false,
    val reviewError: String? = null
) {
    val leaderboard: List<LeaderboardRow> get() = result?.leaderboard.orEmpty()
    val currentPlayer: LeaderboardRow? get() = leaderboard.firstOrNull { it.id == playerId }
    val isMissing: Boolean get() = result == null
    val isLeaderboardHidden: Boolean get() = result != null && leaderboard.isEmpty()
    val reviewEnabled: Boolean get() = result?.reviewEnabled == true
    val isPractice: Boolean get() = mode == GameMode.PRACTICE
    val reviewItems: List<GameReviewItem>
        get() = review?.items.orEmpty().sortedWith(
            compareBy<GameReviewItem> {
                when {
                    !it.answered -> 0
                    !it.isCorrect -> 1
                    else -> 2
                }
            }.thenBy { it.questionIndex }
        )
}
