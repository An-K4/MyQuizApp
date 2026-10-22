package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.ui.components.FinalResultMetric
import android.kma.myquizzapp.core.ui.components.FinalResultPodiumEntry
import java.text.NumberFormat

/** Do not let the new summary/podium bypass the existing result visibility states. */
internal fun finalResultStandings(state: FinalResultUiState): List<LeaderboardRow> =
    state.leaderboard.takeUnless {
        state.isPractice || state.isResultLoading || state.resultError != null ||
            state.isMissing || state.isLeaderboardHidden
    }.orEmpty()

internal fun finalResultPlayer(state: FinalResultUiState): LeaderboardRow? =
    finalResultStandings(state).firstOrNull { it.id == state.playerId }

internal fun finalResultPodium(state: FinalResultUiState): List<FinalResultPodiumEntry> =
    finalResultStandings(state).filter { it.rank in 1..3 }.sortedBy { it.rank }.take(3).map {
        FinalResultPodiumEntry(it.rank, it.playerName, "${formatFinalResultScore(it.playerScore)} điểm")
    }

internal fun finalResultMetrics(state: FinalResultUiState): List<FinalResultMetric> {
    val player = finalResultPlayer(state) ?: return emptyList()
    return buildList {
        add(FinalResultMetric("Hạng", "#${player.rank}"))
        add(FinalResultMetric("Điểm", formatFinalResultScore(player.playerScore)))
        player.correctAnswersCount?.let { add(FinalResultMetric("Câu đúng", "$it")) }
        // No invented total-question denominator, accuracy, or relabelled max streak.
    }
}

internal fun formatFinalResultScore(value: Int): String = NumberFormat.getIntegerInstance().format(value)
