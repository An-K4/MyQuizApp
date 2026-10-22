package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.ui.components.FinalResultPodiumEntry
import java.text.NumberFormat

/** A host sees the final report even when the player-facing standings are hidden. */
internal fun hostFinalStandings(state: HostGameUiState): List<LeaderboardRow> =
    state.ended?.leaderboard.orEmpty().takeIf {
        state.isFinished && !state.isFinalResultLoading && state.finalResultError == null &&
            state.mode != GameMode.PRACTICE
    }.orEmpty()

internal fun hostFinalPodium(state: HostGameUiState): List<FinalResultPodiumEntry> =
    hostFinalStandings(state).filter { it.rank in 1..3 }.sortedBy { it.rank }.take(3).map {
        FinalResultPodiumEntry(it.rank, it.playerName, "${hostFinalScore(it.playerScore)} điểm")
    }

internal fun hostFinalQuestionStats(state: HostGameUiState): List<QuestionStat> =
    state.ended?.perQuestion.orEmpty().takeIf {
        state.isFinished && !state.isFinalResultLoading && state.finalResultError == null
    }.orEmpty().sortedBy { it.questionIndex }

internal fun hostFinalScore(score: Int): String = NumberFormat.getIntegerInstance().format(score)

internal data class HostFinalQuestionSummary(val text: String, val progress: Float)

internal fun hostFinalQuestionSummary(stat: QuestionStat): HostFinalQuestionSummary {
    if (stat.answerCount < 0 || stat.correctCount !in 0..stat.answerCount) {
        return HostFinalQuestionSummary("Chưa có thống kê hợp lệ", 0f)
    }
    if (stat.answerCount == 0) return HostFinalQuestionSummary("Chưa có lượt trả lời", 0f)
    val percent = stat.correctCount.toLong() * 100 / stat.answerCount
    return HostFinalQuestionSummary(
        "${stat.correctCount}/${stat.answerCount} lượt trả lời đúng · $percent%",
        stat.correctCount.toFloat() / stat.answerCount
    )
}
