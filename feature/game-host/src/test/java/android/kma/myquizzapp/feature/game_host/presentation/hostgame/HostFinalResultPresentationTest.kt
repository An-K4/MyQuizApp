package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostFinalResultPresentationTest {
    private fun state() = HostGameUiState(mode = GameMode.CLASSIC, phase = GamePhase.FINISHED,
        ended = GameEnded(leaderboard = (1..12).map { LeaderboardRow(it, it.toLong(), "Player $it", 1000 - it) }))

    @Test fun `host keeps full final leaderboard and ignores player visibility config`() {
        val state = state().copy(showLeaderboard = ShowLeaderboard.NEVER)
        assertEquals(12, hostFinalStandings(state).size)
        assertEquals(listOf(1, 2, 3), hostFinalPodium(state).map { it.rank })
    }

    @Test fun `missing final report never falls back to stale live tracker`() {
        val state = state().copy(ended = null,
            leaderboard = HostLeaderboard(rows = listOf(HostLeaderboardRow(1, 99, "Stale", 99999))))
        assertTrue(hostFinalStandings(state).isEmpty())
        assertTrue(hostFinalRemainingStandings(state).isEmpty())
        assertTrue(hostFinalPodium(state).isEmpty())
        assertTrue(hostFinalQuestionStats(state).isEmpty())
    }

    @Test fun `loading errors and practice never leak ranked facts`() {
        for (state in listOf(state().copy(isFinalResultLoading = true),
            state().copy(finalResultError = "error"), state().copy(mode = GameMode.PRACTICE))) {
            assertTrue(hostFinalStandings(state).isEmpty())
            assertTrue(hostFinalRemainingStandings(state).isEmpty())
            assertTrue(hostFinalPodium(state).isEmpty())
        }
    }

    @Test fun `one two and tied players keep real server ranks without fake places`() {
        val state = state()
        for (n in 1..2) {
            val few = state.copy(ended = state.ended!!.copy(leaderboard = state.ended.leaderboard.take(n)))
            assertEquals(n, hostFinalPodium(few).size)
        }
        val tied = state.copy(ended = GameEnded(leaderboard = listOf(
            LeaderboardRow(2, 3, "Second", 5000), LeaderboardRow(1, 1, "First", 10),
            LeaderboardRow(1, 2, "Tied", 10))))
        assertEquals(listOf(1, 1, 2), hostFinalPodium(tied).map { it.rank })
    }

    @Test fun `question stats sort real indexes without adding absent questions or replay rows`() {
        val stats = listOf(QuestionStat(12, 3, 30, 15), QuestionStat(10, 0, 20, 15))
        val state = state().copy(mode = GameMode.MARATHON, ended = GameEnded(perQuestion = stats))
        assertEquals(listOf(0, 3), hostFinalQuestionStats(state).map { it.questionIndex })
        assertEquals(30, hostFinalQuestionStats(state).last().answerCount)
    }

    @Test fun `question stat summary uses answer attempts not player count`() {
        val summary = hostFinalQuestionSummary(QuestionStat(10, 3, 20, 15))
        assertEquals("15/20 lượt trả lời đúng · 75%", summary.text)
        assertEquals(0.75f, summary.progress, 0.0001f)
    }

    @Test fun `zero and malformed answer counts do not divide by zero or produce invalid progress`() {
        assertEquals(HostFinalQuestionSummary("Chưa có lượt trả lời", 0f),
            hostFinalQuestionSummary(QuestionStat(10, 0, 0, 0)))
        for (stat in listOf(QuestionStat(10, 0, -1, 0), QuestionStat(10, 0, 2, 3), QuestionStat(10, 0, 2, -1))) {
            assertEquals(HostFinalQuestionSummary("Chưa có thống kê hợp lệ", 0f), hostFinalQuestionSummary(stat))
        }
        assertEquals(0f, hostFinalQuestionSummary(QuestionStat(10, 0, 20, 0)).progress, 0f)
    }

    @Test fun `large valid answer counts do not overflow percentage calculation`() {
        val summary = hostFinalQuestionSummary(QuestionStat(10, 0, Int.MAX_VALUE, Int.MAX_VALUE))
        assertTrue(summary.text.endsWith("100%"))
        assertEquals(1f, summary.progress, 0f)
    }

    @Test fun `zero to three players have no remaining board rows`() {
        val source = state()
        for (size in 0..3) {
            val state = source.copy(ended = GameEnded(leaderboard = source.ended!!.leaderboard.take(size)))
            assertTrue(hostFinalRemainingStandings(state).isEmpty())
        }
    }

    @Test fun `remaining board keeps fourth through last rank with no top ten limit`() {
        val source = state().copy(showLeaderboard = ShowLeaderboard.NEVER)
        assertEquals(source.ended!!.leaderboard.drop(3), hostFinalRemainingStandings(source))
        assertEquals((4..12).toList(), hostFinalRemainingStandings(source).map { it.rank })
        assertEquals(12, hostFinalStandings(source).size)
        assertEquals(3, hostFinalPodium(source).size)
    }

    @Test fun `remaining board excludes podium ids not shared player names`() {
        val source = state()
        val state = source.copy(ended = GameEnded(leaderboard = source.ended!!.leaderboard.take(4).map { it.copy(playerName = "Same name") }))
        assertEquals(listOf(4L), hostFinalRemainingStandings(state).map { it.id })
    }

    @Test fun `four tied podium ranks preserve the fourth player and original rank`() {
        val source = state()
        val state = source.copy(ended = GameEnded(leaderboard = source.ended!!.leaderboard.take(4).map { it.copy(rank = 1) }))
        assertEquals(3, hostFinalPodium(state).size)
        assertEquals(listOf(4L), hostFinalRemainingStandings(state).map { it.id })
        assertEquals(listOf(1), hostFinalRemainingStandings(state).map { it.rank })
    }

    @Test fun `missing podium ranks do not discard actual lower rank rows`() {
        val source = state()
        val rows = source.ended!!.leaderboard
        val state = source.copy(ended = GameEnded(leaderboard = listOf(rows[3], rows[1], rows[5])))
        assertEquals(listOf(2), hostFinalPodium(state).map { it.rank })
        assertEquals(listOf(4, 6), hostFinalRemainingStandings(state).map { it.rank })
    }
}
