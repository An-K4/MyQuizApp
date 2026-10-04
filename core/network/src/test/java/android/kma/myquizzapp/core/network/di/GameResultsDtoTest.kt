package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.GameResultsResponseDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameResultsDtoTest {
    private val json = NetworkModule.providePreserveCaseJson()

    @Test
    fun `results preserve mixed case config and map aggregate rows`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameResultsResponseDto>>(
            """{"success":true,"data":{"results":{"session":{"game_mode":"solo","session_status":"finished","config":{"flow":{"showLeaderboard":"never","reviewMode":true}}},"leaderboard":[{"rank":1,"id":9,"player_name":"Kiro","player_score":1200,"correct_answers_count":2,"streak":2,"status":"finished"}],"perQuestion":[{"question_id":10,"question_index":0,"answer_count":3,"correct_count":2}]}}}"""
        )

        val results = requireNotNull(envelope.data).results.toDomain()
        assertEquals(GameMode.SOLO, results.mode)
        assertEquals(SessionStatus.FINISHED, results.sessionStatus)
        assertEquals(ShowLeaderboard.NEVER, results.showLeaderboard)
        assertTrue(results.reviewEnabled)
        assertEquals("Kiro", results.leaderboard.single().playerName)
        assertEquals(1200, results.leaderboard.single().playerScore)
        assertEquals(3, results.perQuestion.single().answerCount)
        assertEquals(2, results.perQuestion.single().correctCount)
    }
}
