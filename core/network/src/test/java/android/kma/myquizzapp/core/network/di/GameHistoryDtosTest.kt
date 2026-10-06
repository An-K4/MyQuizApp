package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.GameHistoryResponseDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameHistoryDtosTest {
    private val json = NetworkModule.providePreserveCaseJson()

    @Test
    fun `history decodes snake case rows and camel case pagination`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameHistoryResponseDto>>(
            """{"success":true,"data":{"sessions":[{"id":91,"session_name":"Friday room","game_mode":"classic","session_status":"finished","total_players":8,"total_questions":10,"ended_at":"2026-10-03T14:30:00.000000Z","quiz_id":7,"quiz_name":"Android","quiz_image":null,"host_name":"Kiro","host_avatar":null,"player_score":830,"correct_answers_count":8,"rank":2}]},"meta":{"pagination":{"limit":20,"nextCursor":"opaque-next","hasMore":true}}}"""
        )

        val item = envelope.data!!.sessions.single().toDomain()
        assertEquals(91L, item.sessionId)
        assertEquals(GameMode.CLASSIC, item.gameMode)
        assertEquals(SessionStatus.FINISHED, item.sessionStatus)
        assertEquals(830, item.playerScore)
        assertEquals("opaque-next", envelope.meta!!.pagination!!.nextCursor)
        assertTrue(envelope.meta!!.pagination!!.hasMore)
        assertNull(envelope.meta!!.pagination!!.total)
    }
}
