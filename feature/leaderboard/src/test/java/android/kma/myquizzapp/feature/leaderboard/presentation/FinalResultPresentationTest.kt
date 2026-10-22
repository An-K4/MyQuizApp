package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import org.junit.Assert.*
import org.junit.Test

class FinalResultPresentationTest {
    private fun state() = FinalResultUiState(
        gameId = 91, playerId = 4, mode = GameMode.CLASSIC,
        result = GameEnded(leaderboard = listOf(
            LeaderboardRow(1, 1, "Minh Anh", 12450),
            LeaderboardRow(2, 2, "Quang Huy", 11200),
            LeaderboardRow(3, 3, "Tuấn Kiệt", 10850),
            LeaderboardRow(4, 4, "Quốc Tuấn", 9650, correctAnswersCount = 13, streak = 0),
        )),
    )

    private fun assertNoStandings(state: FinalResultUiState) {
        assertTrue(finalResultStandings(state).isEmpty())
        assertTrue(finalResultRemainingStandings(state).isEmpty())
        assertTrue(finalResultPodium(state).isEmpty())
        assertNull(finalResultPlayer(state))
        assertTrue(finalResultMetrics(state).isEmpty())
    }

    @Test fun `podium preserves server ranks and does not include the fourth player`() {
        val source = state()
        val podium = finalResultPodium(source)
        assertEquals(listOf(1, 2, 3), podium.map { it.rank })
        assertEquals(listOf("Minh Anh", "Quang Huy", "Tuấn Kiệt"), podium.map { it.name })
        assertEquals(source.leaderboard, finalResultStandings(source))
    }

    @Test fun `hidden configuration suppresses all summaries even if rows are present`() {
        assertNoStandings(state().copy(leaderboardHiddenByConfig = true))
    }

    @Test fun `loading error missing empty and practice states never render podium facts`() {
        assertNoStandings(state().copy(isResultLoading = true))
        assertNoStandings(state().copy(resultError = "Network error"))
        assertNoStandings(state().copy(result = null))
        assertNoStandings(state().copy(result = GameEnded()))
        assertNoStandings(state().copy(mode = GameMode.PRACTICE))
    }

    @Test fun `personal identity uses player id not name or leaderboard position`() {
        val source = state()
        val state = source.copy(result = source.result!!.copy(leaderboard = source.leaderboard.map {
            it.copy(playerName = "Same name")
        }))
        assertEquals(4L, finalResultPlayer(state)?.id)
        assertNull(finalResultPlayer(state.copy(playerId = 99)))
        assertTrue(finalResultMetrics(state.copy(playerId = 99)).isEmpty())
    }

    @Test fun `personal metrics preserve zero and omit a missing correct count`() {
        val source = state()
        val zero = source.copy(result = GameEnded(leaderboard = listOf(
            LeaderboardRow(4, 4, "Quốc Tuấn", 0, correctAnswersCount = 0, streak = 99),
        )))
        assertEquals(listOf("Hạng", "Điểm", "Câu đúng"), finalResultMetrics(zero).map { it.label })
        assertEquals(listOf("#4", "0", "0"), finalResultMetrics(zero).map { it.value })
        val missing = source.copy(result = GameEnded(leaderboard = listOf(
            LeaderboardRow(4, 4, "Quốc Tuấn", 0),
        )))
        assertEquals(listOf("Hạng", "Điểm"), finalResultMetrics(missing).map { it.label })
    }

    @Test fun `one or two players never create missing podium places`() {
        val source = state()
        for (size in 1..2) {
            val state = source.copy(result = GameEnded(leaderboard = source.leaderboard.take(size)))
            assertEquals(size, finalResultPodium(state).size)
        }
        val absent = source.copy(result = GameEnded(leaderboard = listOf(source.leaderboard[1])))
        assertEquals(listOf(2), finalResultPodium(absent).map { it.rank })
    }

    @Test fun `ties remain ties and higher scores never override the server rank`() {
        val state = state().copy(result = GameEnded(leaderboard = listOf(
            LeaderboardRow(2, 20, "Second", 99999),
            LeaderboardRow(1, 10, "First", 10),
            LeaderboardRow(1, 11, "Tied first", 10),
        )))
        assertEquals(listOf(1, 1, 2), finalResultPodium(state).map { it.rank })
        assertEquals(listOf("First", "Tied first", "Second"), finalResultPodium(state).map { it.name })
    }

    @Test fun `zero to three players have no remaining board rows`() {
        val source = state()
        for (size in 0..3) {
            val state = source.copy(result = GameEnded(leaderboard = source.leaderboard.take(size)))
            assertTrue(finalResultRemainingStandings(state).isEmpty())
        }
    }

    @Test fun `expanded board starts at fourth rank without modifying personal metrics`() {
        val source = state()
        val before = finalResultMetrics(source)
        assertEquals(listOf(source.leaderboard[3]), finalResultRemainingStandings(source))
        assertEquals(listOf(4), finalResultRemainingStandings(source).map { it.rank })
        assertEquals(before, finalResultMetrics(source))
        assertEquals(4L, finalResultPlayer(source)?.id)
        val winner = source.copy(playerId = 1)
        assertEquals(1L, finalResultPlayer(winner)?.id)
        assertTrue(finalResultMetrics(winner).isNotEmpty())
    }

    @Test fun `remaining board excludes podium identities not duplicate display names`() {
        val source = state()
        val state = source.copy(result = GameEnded(leaderboard = source.leaderboard.map { it.copy(playerName = "Same name") }))
        assertEquals(listOf(4L), finalResultRemainingStandings(state).map { it.id })
    }

    @Test fun `four tied podium ranks keep the fourth identity outside podium with its real rank`() {
        val source = state()
        val state = source.copy(result = GameEnded(leaderboard = source.leaderboard.map { it.copy(rank = 1) }))
        assertEquals(3, finalResultPodium(state).size)
        assertEquals(listOf(4L), finalResultRemainingStandings(state).map { it.id })
        assertEquals(listOf(1), finalResultRemainingStandings(state).map { it.rank })
    }

    @Test fun `missing podium ranks do not discard real lower rank rows`() {
        val source = state()
        val state = source.copy(result = GameEnded(leaderboard = listOf(source.leaderboard[3], source.leaderboard[1])))
        assertEquals(listOf(2), finalResultPodium(state).map { it.rank })
        assertEquals(listOf(4), finalResultRemainingStandings(state).map { it.rank })
    }
}
