package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.IgnoredGameConfigReason
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val ack = (mapper.toConfigUpdateAck(SocketAckResult.Payload(raw)) as Result.Success).data
        assertTrue(ack.changed)
        assertTrue(ack.ignored.isEmpty())
        assertEquals(50, ack.config.lobby.maxPlayers)
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
        assertEquals(false, ack.changed)
        assertEquals(3, ack.ignored.size)
        assertEquals("flow.lives", ack.ignored[0].rawPath)
        assertEquals(IgnoredGameConfigReason.LOCKED, ack.ignored[0].reason)
        assertEquals(IgnoredGameConfigReason.UNKNOWN, ack.ignored[1].reason)
        assertEquals(IgnoredGameConfigReason.INVALID, ack.ignored[2].reason)
    }

    @Test
    fun `ack loi giu nguyen code cua backend`() {
        assertEquals(
            Result.Error(AppError.Api("GAME_NOT_HOST")),
            mapper.toConfigUpdateAck(SocketAckResult.Payload("""{"error":{"code":"GAME_NOT_HOST"}}"""))
        )
    }

    @Test
    fun `khong co ack thanh loi timeout chu khong treo`() {
        assertEquals(Result.Error(AppError.Api(SocketAckMapper.CODE_ACK_TIMEOUT)), mapper.toConfigUpdateAck(SocketAckResult.Timeout))
    }

    @Test
    fun `mat ket noi thanh code rieng cua client`() {
        assertEquals(Result.Error(AppError.Api(SocketAckMapper.CODE_NOT_CONNECTED)), mapper.toConfigUpdateAck(SocketAckResult.NotConnected))
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

    @Test
    fun `host paced answer ack parses without grading fields`() {
        val result = mapper.toAnswerAck(
            SocketAckResult.Payload("""{"accepted":true,"isLate":false,"lives":3,"eliminated":false,"serverTime":"2026-09-13T12:00:00.000Z"}""")
        )
        assertTrue(result is Result.Success)
        val ack = (result as Result.Success).data
        assertTrue(ack.accepted)
        assertFalse(ack.isLate)
        assertNull(ack.isCorrect)
        assertNull(ack.scoreEarned)
    }

    @Test
    fun `self paced expanded answer ack remains compatible`() {
        val ack = (mapper.toAnswerAck(
            SocketAckResult.Payload("""{"accepted":true,"isCorrect":true,"scoreEarned":800,"totalScore":1200,"streak":2,"correct_answer":["0","2"]}""")
        ) as Result.Success).data
        assertEquals(listOf("0", "2"), ack.correctAnswers)
        assertEquals(true, ack.isCorrect)
        assertEquals(800, ack.scoreEarned)
    }

    @Test
    fun `answer ack nested error keeps backend code`() {
        assertEquals(
            Result.Error(AppError.Api("GAME_ANSWER_DUPLICATE")),
            mapper.toAnswerAck(SocketAckResult.Payload("""{"error":{"code":"GAME_ANSWER_DUPLICATE"}}"""))
        )
    }

    @Test
    fun `answer ack timeout is explicit uncertainty error`() {
        assertEquals(
            Result.Error(AppError.Api(SocketAckMapper.CODE_ACK_TIMEOUT)),
            mapper.toAnswerAck(SocketAckResult.Timeout)
        )
    }
}
