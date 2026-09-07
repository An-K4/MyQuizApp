package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test riêng cho nhánh `game:started` của [GameEventMapper].
 *
 * `game:start` KHÔNG có ack (fire-and-forget ở `game.socket.ts::onStart`), nên
 * đây là event duy nhất xác nhận trận đã bắt đầu thật. Nếu mapper làm sai chỗ
 * này thì nút "Bắt đầu" sẽ quay mãi rồi tràn timeout — đáng test tách bạch.
 *
 * Fixture theo emit thật: `{ mode, config, total_questions, serverTime }` — chú ý
 * `total_questions` là snake_case trong khi `serverTime` là camelCase.
 */
class GameStartedMapperTest {

    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `game started doc payload maps mixed naming correctly`() {
        val payload = """
            {
              "mode": "classic",
              "config": { "version": 1, "timing": { "countdownSeconds": 3 } },
              "total_questions": 12,
              "serverTime": "2026-09-07T02:30:00.000Z"
            }
        """.trimIndent()

        val event = mapper.map(GameSocketEvents.GAME_STARTED, payload)

        assertTrue(event is GameEvent.GameStarted)
        val started = event as GameEvent.GameStarted
        assertEquals(GameMode.CLASSIC, started.mode)
        assertEquals(12, started.totalQuestions)
        assertEquals("2026-09-07T02:30:00.000Z", started.serverTime)
        assertEquals(3, started.config.timing.countdownSeconds)
    }

    @Test
    fun `game started tolerates missing optional fields`() {
        val event = mapper.map(GameSocketEvents.GAME_STARTED, """{"mode":"survival"}""")

        val started = event as GameEvent.GameStarted
        assertEquals(GameMode.SURVIVAL, started.mode)
        assertEquals(0, started.totalQuestions)
        assertNull(started.serverTime)
    }

    @Test
    fun `game started rac thanh client parse error chu khong throw`() {
        assertEquals(
            GameEvent.Failed(
                GameSocketEvents.GAME_STARTED,
                GameEventMapper.CODE_CLIENT_PARSE_ERROR
            ),
            mapper.map(GameSocketEvents.GAME_STARTED, "{ mode: thiếu ngoặc }")
        )
    }
}
