package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class N24GameEventMapperTest {
    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `answer received maps latest progress snapshot`() {
        val event = mapper.map(GameSocketEvents.ANSWER_RECEIVED,
            """{"index":2,"answered":3,"activePlayers":5,"serverTime":"2026-09-14T00:00:00Z"}""")
        assertTrue(event is GameEvent.AnswerProgressUpdated)
        val progress = (event as GameEvent.AnswerProgressUpdated).progress
        assertEquals(2, progress.index)
        assertEquals(3, progress.answered)
        assertEquals(5, progress.activePlayers)
    }

    @Test
    fun `lean and final leaderboard row shapes remain compatible`() {
        val lean = mapper.map(GameSocketEvents.LEADERBOARD_UPDATED,
            """{"leaderboard":[{"rank":1,"id":7,"player_name":"Kiro","player_score":900}]}""")
        assertTrue(lean is GameEvent.PlayerLeaderboardUpdated)
        assertNull((lean as GameEvent.PlayerLeaderboardUpdated).leaderboard.single().correctAnswersCount)

        val ended = mapper.map(GameSocketEvents.GAME_ENDED,
            """{"leaderboard":[{"rank":1,"id":7,"player_name":"Kiro","player_score":900,"correct_answers_count":4,"streak":3,"status":"connected"}],"perQuestion":[],"review_enabled":true}""")
        assertTrue(ended is GameEvent.GameEndedEvent)
        val row = (ended as GameEvent.GameEndedEvent).ended.leaderboard.single()
        assertEquals(4, row.correctAnswersCount)
        assertEquals(3, row.streak)
    }
}
