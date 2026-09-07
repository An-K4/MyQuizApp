package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.IgnoredGameConfigReason
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cho [SocketAckMapper] — phủ đủ 4 nhánh của ack `lobby:config-update`:
 * thành công, thành công nhưng có field bị bỏ, lỗi có code, và không có ack.
 *
 * Dùng ĐÚNG Json của production (NetworkModule.providePreserveCaseJson) vì chính
 * cấu hình naming là chỗ từng gây lỗi ở N16/N17 — tự tạo Json trong test sẽ cho
 * test xanh trong khi app thật vỡ.
 *
 * Fixture copy theo `game.socket.ts::onConfigUpdate`:
 * `ack({ ok: true, changed, config: session.config, ignored })`.
 */
class SocketAckMapperTest {

    private val mapper = SocketAckMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `ack thanh cong tra ve full config da normalize`() {
        val raw = """
            {
              "ok": true,
              "changed": true,
              "config": {
                "version": 1,
                "timing": { "perQuestionSeconds": null, "autoAdvance": true },
                "lobby": { "maxPlayers": 50 },
                "flow": { "showLeaderboard": "end_only", "reviewMode": true }
              },
              "ignored": []
            }
        """.trimIndent()

        val result = mapper.toConfigUpdateAck(SocketAckResult.Payload(raw))

        assertTrue(result is Result.Success)
        val ack = (result as Result.Success).data
        assertTrue(ack.changed)
        assertTrue(ack.ignored.isEmpty())
        // Field camelCase lồng trong config phải đọc đúng, không bị namingStrategy
        // đổi thành max_players rồng rồi rơi về default 100.
        assertEquals(50, ack.config.lobby.maxPlayers)
        // null = dùng time limit của từng câu, khác hẳn 0 = không giới hạn.
        assertNull(ack.config.timing.perQuestionSeconds)
    }

    @Test
    fun `ack giu nguyen ly do cua tung field bi bo`() {
        val raw = """
            {
              "ok": true,
              "changed": false,
              "config": { "version": 1 },
              "ignored": [
                { "path": "flow.lives", "value": 3, "reason": "locked" },
                { "path": "flow.khongTonTai", "value": true, "reason": "unknown" },
                { "path": "lobby.maxPlayers", "value": 9999, "reason": "invalid" }
              ]
            }
        """.trimIndent()

        val ack = (mapper.toConfigUpdateAck(SocketAckResult.Payload(raw)) as Result.Success).data

        // changed=false → backend KHÔNG broadcast lobby:updated, UI phải dùng ack này.
        assertEquals(false, ack.changed)
        assertEquals(3, ack.ignored.size)
        assertEquals("flow.lives", ack.ignored[0].rawPath)
        assertEquals(IgnoredGameConfigReason.LOCKED, ack.ignored[0].reason)
        assertEquals(IgnoredGameConfigReason.UNKNOWN, ack.ignored[1].reason)
        assertEquals(IgnoredGameConfigReason.INVALID, ack.ignored[2].reason)
    }

    @Test
    fun `ack loi giu nguyen code cua backend`() {
        val result = mapper.toConfigUpdateAck(
            SocketAckResult.Payload("""{"error":{"code":"GAME_NOT_HOST"}}""")
        )

        assertEquals(Result.Error(AppError.Api("GAME_NOT_HOST")), result)
    }

    @Test
    fun `khong co ack thanh loi timeout chu khong treo`() {
        assertEquals(
            Result.Error(AppError.Api(SocketAckMapper.CODE_ACK_TIMEOUT)),
            mapper.toConfigUpdateAck(SocketAckResult.Timeout)
        )
    }

    @Test
    fun `mat ket noi thanh code rieng cua client`() {
        assertEquals(
            Result.Error(AppError.Api(SocketAckMapper.CODE_NOT_CONNECTED)),
            mapper.toConfigUpdateAck(SocketAckResult.NotConnected)
        )
    }

    @Test
    fun `payload rac thanh client parse error chu khong throw`() {
        assertEquals(
            Result.Error(AppError.Api(GameEventMapper.CODE_CLIENT_PARSE_ERROR)),
            mapper.toConfigUpdateAck(SocketAckResult.Payload("{ không phải json }"))
        )
    }

    @Test
    fun `ack ok false ma khong co error cung thanh loi`() {
        assertEquals(
            Result.Error(AppError.Api(GameEventMapper.CODE_CLIENT_PARSE_ERROR)),
            mapper.toConfigUpdateAck(SocketAckResult.Payload("""{"ok":false}"""))
        )
    }
}
