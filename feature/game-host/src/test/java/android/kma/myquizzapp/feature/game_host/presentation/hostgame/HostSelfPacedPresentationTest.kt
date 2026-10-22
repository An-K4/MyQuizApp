package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.SessionStatus
import org.junit.Assert.*
import org.junit.Test

class HostSelfPacedPresentationTest {
    private fun state(mode: GameMode = GameMode.SOLO) = HostGameUiState(
        mode = mode, pacing = Pacing.SELF, phase = GamePhase.QUESTION_ACTIVE,
        sessionStatus = SessionStatus.ACTIVE, connection = HostGameConnection.CONNECTED,
        isStateSynchronized = true, hasMonitoringData = true, totalQuestions = 10,
    )
    private fun row(id: Long = 1, status: String = "connected") = HostLeaderboardRow(
        rank = 0, id = id, playerName = "Nguyễn Minh Anh", playerScore = 1250,
        answeredCount = 3, correctCount = 2, wrongCount = 1, totalQuestions = 10, status = status,
    )
    @Test fun `four modes share monitor layout without swallowing Classic countdown or final`() {
        listOf(GameMode.SOLO, GameMode.SURVIVAL, GameMode.MARATHON, GameMode.PRACTICE).forEach {
            assertTrue(state(it).usesSelfPacedMonitorLayout)
        }
        assertFalse(state(GameMode.CLASSIC).copy(pacing = Pacing.HOST).usesSelfPacedMonitorLayout)
        assertFalse(state().copy(phase = GamePhase.COUNTDOWN).usesSelfPacedMonitorLayout)
        assertFalse(state().copy(phase = GamePhase.UNKNOWN).usesSelfPacedMonitorLayout)
        assertFalse(state().copy(sessionStatus = SessionStatus.FINISHED).usesSelfPacedMonitorLayout)
    }
    @Test fun `empty synchronized table means known zero not loading`() {
        assertTrue(state().hasFreshMonitoringData)
        assertEquals(listOf(0, 0, 0), hostMonitorMetrics(state()).map { it.value })
        assertNull(hostMonitorNotice(state()))
    }
    @Test fun `missing table and reconnect display unknown counters instead of stale zeros`() {
        val states = listOf(state().copy(hasMonitoringData = false),
            state().copy(isStateSynchronized = false), state().copy(connection = HostGameConnection.RECONNECTING))
        states.forEach { s ->
            assertFalse(s.hasFreshMonitoringData)
            assertTrue(hostMonitorMetrics(s).all { it.value == null })
            assertNotNull(hostMonitorNotice(s))
        }
    }
    @Test fun `status counters use explicit server status only`() {
        val s = state(GameMode.SURVIVAL).copy(leaderboard = HostLeaderboard(rows = listOf(
            row(1), row(2, "finished"), row(3, "disconnected"), row(4, "eliminated"), row(5, "unknown"))))
        assertEquals(listOf(1, 1, 1, 1), hostMonitorMetrics(s).map { it.value })
        assertEquals(HostMonitorStatus.UNKNOWN, hostMonitorStatus("new_server_status"))
    }
    @Test fun `Solo and Practice do not add meaningless eliminated counters`() {
        assertEquals(3, hostMonitorMetrics(state()).size)
        assertEquals(3, hostMonitorMetrics(state(GameMode.PRACTICE)).size)
        assertEquals(4, hostMonitorMetrics(state(GameMode.SURVIVAL)).size)
    }
    @Test fun `Marathon shows elimination summary only with evidence of lives or elimination`() {
        assertFalse(state(GameMode.MARATHON).monitorShowsEliminated)
        assertTrue(state(GameMode.MARATHON).copy(leaderboard = HostLeaderboard(rows = listOf(row().copy(lives = 3)))).monitorShowsEliminated)
        assertTrue(state(GameMode.MARATHON).copy(leaderboard = HostLeaderboard(rows = listOf(row(status = "eliminated")))).monitorShowsEliminated)
    }
    @Test fun `Practice hides competitive score even if stored score is populated`() {
        assertFalse(state(GameMode.PRACTICE).monitorShowsScores)
        assertTrue(state(GameMode.SOLO).monitorShowsScores)
        assertTrue(state(GameMode.SURVIVAL).monitorShowsScores)
    }
    @Test fun `finite progress uses answered count not next question index`() {
        val p = hostMonitorProgress(row().copy(currentQuestionIndex = 4), state())
        assertEquals("Đã làm 3/10 câu", p.label)
        assertEquals(0.3f, p.fraction!!, 0.0001f)
    }
    @Test fun `Marathon loops never produce finite percentage or question fraction`() {
        val p = hostMonitorProgress(row().copy(answeredCount = 18), state(GameMode.MARATHON))
        assertEquals("Đã làm 18 câu", p.label)
        assertNull(p.fraction)
    }
    @Test fun `finished and eliminated do not invent full completion`() {
        listOf("finished", "eliminated").forEach {
            assertEquals(0.3f, hostMonitorProgress(row(status = it), state()).fraction!!, 0.0001f)
        }
    }
    @Test fun `unknown invalid or exceeded totals never produce misleading progress bars`() {
        assertNull(hostMonitorProgress(row().copy(totalQuestions = 0), state().copy(totalQuestions = 0)).fraction)
        assertNull(hostMonitorProgress(row().copy(answeredCount = 11), state()).fraction)
        assertEquals("Chưa có tiến độ", hostMonitorProgress(row().copy(answeredCount = -1), state()).label)
    }
    @Test fun `Practice cards retain stable id order when incremental score ordering changes`() {
        val rows = listOf(row(3).copy(playerScore = 0), row(1).copy(playerScore = 0), row(2).copy(playerScore = 0))
        val s = state(GameMode.PRACTICE).copy(leaderboard = HostLeaderboard(rows = rows))
        assertEquals(listOf(1L, 2L, 3L), hostMonitorRows(s).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), hostMonitorRows(s.copy(leaderboard = s.leaderboard.copy(rows = rows.reversed()))).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), s.leaderboard.rows.map { it.id })
    }
    @Test fun `initials handle blank names and unicode without fake avatars`() {
        assertEquals("NA", hostMonitorInitials(" Nguyễn Minh Anh "))
        assertEquals("A", hostMonitorInitials("Anh"))
        assertEquals("?", hostMonitorInitials("  "))
        assertEquals("😀", hostMonitorInitials("😀"))
        assertEquals("1.250 điểm", hostMonitorScoreLabel(1250))
    }
    @Test fun `self paced commands remain locked until state snapshot is synchronized`() {
        val pending = state().copy(isStateSynchronized = false)
        assertFalse(pending.canPauseOrResume)
        assertFalse(pending.canEndGame)
        assertTrue(state().canPauseOrResume)
        assertTrue(state().canEndGame)
        assertFalse(state().canAdvance)
    }
    @Test fun `pause notice makes no promise about individual player clocks`() {
        assertEquals("Trận đang tạm dừng.", hostMonitorNotice(state().copy(sessionStatus = SessionStatus.PAUSED)))
    }
}
