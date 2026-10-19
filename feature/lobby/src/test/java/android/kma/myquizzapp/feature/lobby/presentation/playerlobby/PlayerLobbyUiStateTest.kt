package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.feature.lobby.presentation.components.lobbyPlayerStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLobbyUiStateTest {
    @Test
    fun `identity uses player id even when names are equal`() {
        val other = player(1, "connected").copy(playerName = "Kiro")
        val me = player(2, "connected").copy(playerName = "Kiro")
        val state = PlayerLobbyUiState(myPlayerId = 2, players = listOf(other, me))

        assertEquals(me, state.myPlayer)
    }

    @Test
    fun `missing identity never borrows another player's name`() {
        val state = PlayerLobbyUiState(myPlayerId = 5, players = listOf(player(1, "connected")))

        assertNull(state.myPlayer)
    }

    @Test
    fun `total retains offline and unknown players but online is exact`() {
        val state = PlayerLobbyUiState(
            players = listOf(
                player(1, "connected"), player(2, "disconnected"), player(3, "connected"),
                player(4, "eliminated"), player(5, "finished"), player(6, "future-status"),
            ),
        )

        assertEquals(6, state.playerCount)
        assertEquals(2, state.connectedPlayerCount)
    }

    @Test
    fun `new snapshot recomputes identity and connection count`() {
        val state = PlayerLobbyUiState(myPlayerId = 1, players = listOf(player(1, "connected")))
        val updated = state.copy(players = listOf(player(1, "disconnected")))

        assertEquals(1, updated.playerCount)
        assertEquals(0, updated.connectedPlayerCount)
        assertEquals("disconnected", updated.myPlayer?.status)
    }

    @Test
    fun `connection alone does not invent a lobby snapshot`() {
        val initial = PlayerLobbyUiState()
        assertFalse(initial.hasLobbySnapshot)
        assertNull(initial.myPlayer)
        assertEquals(0, initial.playerCount)
        assertTrue(initial.copy(sessionStatus = SessionStatus.LOBBY).hasLobbySnapshot)
    }

    @Test
    fun `nullable lives are preserved including zero`() {
        val noLives = player(1, "connected")
        val zeroLives = player(2, "eliminated").copy(lives = 0)
        val state = PlayerLobbyUiState(players = listOf(noLives, zeroLives))

        assertNull(state.players[0].lives)
        assertEquals(0, state.players[1].lives)
        assertTrue(state.showLives)
        assertFalse(state.copy(players = listOf(noLives)).showLives)
    }

    @Test
    fun `status labels never interpret unknown as ready`() {
        assertEquals("Đã kết nối", lobbyPlayerStatusLabel("connected"))
        assertEquals("Mất kết nối", lobbyPlayerStatusLabel("disconnected"))
        assertEquals("Đã bị loại", lobbyPlayerStatusLabel("eliminated"))
        assertEquals("Đã hoàn thành", lobbyPlayerStatusLabel("finished"))
        assertEquals("Không rõ", lobbyPlayerStatusLabel("future-status"))
        assertEquals("Không rõ", lobbyPlayerStatusLabel("CONNECTED"))
    }

    private fun player(id: Long, status: String) = LobbyPlayer(
        id = id, playerName = "Player $id", playerScore = 0, status = status,
    )
}
