package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostLobbyUiStateTest {
    @Test
    fun `empty snapshot has zero total and connected players`() {
        val state = HostLobbyUiState(sessionStatus = SessionStatus.LOBBY)
        assertEquals(0, state.playerCount)
        assertEquals(0, state.connectedPlayerCount)
        assertTrue(state.hasLobbySnapshot)
    }

    @Test
    fun `total keeps disconnected and unknown players but connected count is exact`() {
        val state = HostLobbyUiState(players = listOf(
            player(1, "connected"), player(2, "disconnected"), player(3, "connected"),
            player(4, "finished"), player(5, "eliminated"), player(6, "future-status"),
        ))
        assertEquals(6, state.playerCount)
        assertEquals(2, state.connectedPlayerCount)
    }

    @Test
    fun `new snapshot updates connected count without removing offline roster entries`() {
        val before = HostLobbyUiState(players = listOf(player(1, "connected"), player(2, "connected")))
        val after = before.copy(players = listOf(player(1, "connected"), player(2, "disconnected")))
        assertEquals(2, before.connectedPlayerCount)
        assertEquals(2, after.playerCount)
        assertEquals(1, after.connectedPlayerCount)
    }

    @Test
    fun `empty connected lobby can still start while cold start cannot`() {
        val cold = HostLobbyUiState(connection = ConnectionStatus.CONNECTED)
        assertFalse(cold.hasLobbySnapshot)
        assertFalse(cold.canStartGame)
        assertTrue(cold.copy(sessionStatus = SessionStatus.LOBBY).canStartGame)
    }

    @Test
    fun `start stays disabled during reconnect saving starting or non lobby status`() {
        val ready = HostLobbyUiState(sessionStatus = SessionStatus.LOBBY, connection = ConnectionStatus.CONNECTED)
        assertTrue(ready.canStartGame)
        ConnectionStatus.entries.filter { it != ConnectionStatus.CONNECTED }.forEach {
            assertFalse(ready.copy(connection = it).canStartGame)
        }
        assertFalse(ready.copy(isSavingConfig = true).canStartGame)
        assertFalse(ready.copy(isStarting = true).canStartGame)
        SessionStatus.entries.filter { it != SessionStatus.LOBBY }.forEach {
            assertFalse(ready.copy(sessionStatus = it).canStartGame)
        }
    }

    private fun player(id: Long, status: String) = LobbyPlayer(
        id = id, playerName = "Người chơi $id", playerScore = 0, status = status,
    )
}
