package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat

sealed interface FinalResultEffect {
    data class ResourceMissing(val message: String) : FinalResultEffect
}

data class FinalResultUiState(
    val gameId: Long,
    val playerId: Long,
    val mode: GameMode? = null,
    val result: GameEnded? = null,
    val isResultLoading: Boolean = false,
    val resultError: String? = null,
    val leaderboardHiddenByConfig: Boolean = false,
    val review: GameReview? = null,
    val isReviewVisible: Boolean = false,
    val isReviewLoading: Boolean = false,
    val reviewError: String? = null
) {
    val leaderboard: List<LeaderboardRow> get() = result?.leaderboard.orEmpty()
    val currentPlayer: LeaderboardRow? get() = leaderboard.firstOrNull { it.id == playerId }
    val isMissing: Boolean get() = result == null && !isResultLoading && resultError == null
    val isLeaderboardHidden: Boolean
        get() = result != null && (leaderboardHiddenByConfig || leaderboard.isEmpty())
    val reviewEnabled: Boolean get() = result?.reviewEnabled == true
    val isPractice: Boolean get() = mode == GameMode.PRACTICE
    val questionStats: List<QuestionStat>
        get() = result?.perQuestion.orEmpty().sortedBy { it.questionIndex }
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
