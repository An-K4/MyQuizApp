package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.GameHistoryResponseDto
import android.kma.myquizzapp.core.network.dto.GameHistorySummaryResponseDto
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

    @Test
    fun `player summary hides forbidden leaderboard but keeps own result`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameHistorySummaryResponseDto>>(
            """{"success":true,"data":{"summary":{"session":{"id":91,"session_name":"Friday room","game_mode":"classic","session_status":"finished","total_players":2,"total_questions":10,"finished_at":"2026-10-03T14:30:00Z","config":{"flow":{"showLeaderboard":"never"}}},"quiz":{"id":7,"quiz_name":"Android","quiz_description":"Snapshot"},"leaderboard":[{"id":5,"player_name":"Kiro","player_score":830,"correct_answers_count":8,"rank":1,"status":"finished"},{"id":6,"player_name":"Other","player_score":500,"correct_answers_count":5,"rank":2,"status":"finished"}],"perQuestion":null,"viewer":{"isHost":false,"playerId":5}}}}"""
        )

        val summary = envelope.data!!.summary.toDomain()
        assertTrue(summary.leaderboardHiddenByConfig)
        assertTrue(summary.leaderboard.isEmpty())
        assertEquals(5L, summary.viewerResult?.id)
        assertEquals("Android", summary.quiz?.name)
    }

    @Test
    fun `host summary keeps leaderboard and per question stats`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameHistorySummaryResponseDto>>(
            """{"success":true,"data":{"summary":{"session":{"id":91,"session_name":"Friday room","game_mode":"classic","session_status":"cancelled","total_players":2,"total_questions":10,"created_at":"2026-10-03T14:30:00Z","config":{"flow":{"showLeaderboard":"never"}}},"quiz":null,"leaderboard":[{"id":5,"player_name":"Kiro","player_score":830,"correct_answers_count":8,"rank":1,"status":"finished"}],"perQuestion":[{"question_id":3,"question_index":0,"answer_count":2,"correct_count":1}],"viewer":{"isHost":true,"playerId":null}}}}"""
        )

        val summary = envelope.data!!.summary.toDomain()
        assertTrue(!summary.leaderboardHiddenByConfig)
        assertEquals(1, summary.leaderboard.size)
        assertEquals(1, summary.perQuestion.size)
        assertEquals("2026-10-03T14:30:00Z", summary.finishedAt)
    }
}
