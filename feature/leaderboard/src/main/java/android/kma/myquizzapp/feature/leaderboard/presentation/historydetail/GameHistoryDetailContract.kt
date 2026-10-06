package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat

sealed interface GameHistoryDetailIntent {
    data object RetrySummary : GameHistoryDetailIntent
    data object RetryAnswers : GameHistoryDetailIntent
}

data class GameHistoryDetailUiState(
    val gameId: Long,
    val summary: GameHistorySummary? = null,
    val isSummaryLoading: Boolean = true,
    val summaryError: String? = null,
    val review: GameReview? = null,
    val isAnswersLoading: Boolean = false,
    val answersError: String? = null,
    val reviewDisabled: Boolean = false
) {
    val isHost: Boolean get() = summary?.viewer?.isHost == true
    val playerId: Long? get() = summary?.viewer?.playerId
    val currentPlayer: LeaderboardRow?
        get() = summary?.viewerResult
    val questionStats: List<QuestionStat>
        get() = summary?.perQuestion.orEmpty().sortedBy { it.questionIndex }
    val reviewItems: List<GameReviewItem>
        get() = review?.items.orEmpty().sortedBy { it.questionIndex }
}
