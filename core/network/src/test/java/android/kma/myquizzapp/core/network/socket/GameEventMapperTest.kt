package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cho [GameEventMapper] — dùng ĐÚNG Json của production
 * (NetworkModule.providePreserveCaseJson) thay vì tự tạo Json trong test. Nếu
 * tự tạo, test sẽ xanh trong khi app thật vẫn vỡ vì khác cấu hình naming.
 *
 * Fixture JSON copy từ docs/components/socket.doc.ts của backend — giữ nguyên
 * kiểu trộn snake_case + camelCase, vì chính chỗ đó là nơi dễ sai nhất.
 */
class GameEventMapperTest {

    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `lobby updated doc payload maps mixed naming correctly`() {
        val payload = """
            {
              "session_status": "lobby",
              "config": { "version": 1, "lobby": { "maxPlayers": 50 } },
              "players": [
                {
                  "id": 7,
                  "player_name": "Kiro",
                  "player_score": 120,
                  "player_avatar": null,
                  "lives": null,
                  "status": "connected"
                }
              ],
              "serverTime": "2026-08-28T14:00:00.000Z"
            }
        """.trimIndent()

        val event = mapper.map(GameSocketEvents.LOBBY_UPDATED, payload)

        assertTrue(event is GameEvent.LobbyUpdated)
        val lobby = (event as GameEvent.LobbyUpdated).lobby
        assertEquals(SessionStatus.LOBBY, lobby.sessionStatus)
        assertEquals("2026-08-28T14:00:00.000Z", lobby.serverTime)
        // Field camelCase lồng bên trong config phải đọc được, không bị namingStrategy
        // biến thành max_players rồi rơi về giá trị mặc định 100.
        assertEquals(50, lobby.config.lobby.maxPlayers)
        assertEquals(1, lobby.players.size)
        assertEquals(7L, lobby.players[0].id)
        assertEquals("Kiro", lobby.players[0].playerName)
        assertEquals(120, lobby.players[0].playerScore)
        assertEquals("connected", lobby.players[0].status)
    }

    @Test
    fun `lobby updated tolerates missing config and empty players`() {
        val event = mapper.map(
            GameSocketEvents.LOBBY_UPDATED,
            """{"session_status":"lobby"}"""
        )

        assertTrue(event is GameEvent.LobbyUpdated)
        val lobby = (event as GameEvent.LobbyUpdated).lobby
        assertTrue(lobby.players.isEmpty())
        assertNull(lobby.serverTime)
        // Không có config trong payload → dùng GameConfig mặc định, không crash.
        assertEquals(100, lobby.config.lobby.maxPlayers)
    }

    @Test
    fun `error payload keeps backend code untouched`() {
        val event = mapper.map(
            GameSocketEvents.ERROR,
            """{"event":"lobby:join","code":"GAME_ROOM_NOT_FOUND"}"""
        )

        assertEquals(GameEvent.Failed("lobby:join", "GAME_ROOM_NOT_FOUND"), event)
    }

    @Test
    fun `malformed payload becomes client parse error instead of throwing`() {
        val event = mapper.map(GameSocketEvents.LOBBY_UPDATED, "{ không phải json }")

        assertEquals(
            GameEvent.Failed(
                GameSocketEvents.LOBBY_UPDATED,
                GameEventMapper.CODE_CLIENT_PARSE_ERROR
            ),
            event
        )
    }

    @Test
    fun `missing payload becomes client parse error`() {
        assertEquals(
            GameEvent.Failed(GameSocketEvents.ERROR, GameEventMapper.CODE_CLIENT_PARSE_ERROR),
            mapper.map(GameSocketEvents.ERROR, null)
        )
    }

    @Test
    fun `gameplay events are reported as unhandled not dropped`() {
        val event = mapper.map("question:started", """{"index":0}""")

        assertEquals(GameEvent.Unhandled("question:started"), event)
    }
}
