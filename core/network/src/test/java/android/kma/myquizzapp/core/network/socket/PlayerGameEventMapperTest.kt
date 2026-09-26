package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGameEventMapperTest {
    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `question started maps option object plain string and id zero`() {
        val event = mapper.map(
            GameSocketEvents.QUESTION_STARTED,
            """{
              "question": {
                "index": 0,
                "total": 2,
                "id": 11,
                "question_type": "multiple_choice",
                "question_text": "Chọn đáp án",
                "answer_options": [{"id":0,"option_text":"A"}, "B"]
              },
              "time_limit": 20,
              "endsAt": "2026-09-13T12:00:20.000Z",
              "matchEndsAt": "2026-09-13T12:05:00.000Z",
              "allow_answer_late": true,
              "remainingSeconds": 12,
              "lives": 3,
              "serverTime": "2026-09-13T12:00:00.000Z"
            }""".trimIndent()
        )
        assertTrue(event is GameEvent.QuestionStarted)
        val started = (event as GameEvent.QuestionStarted).started
        assertEquals("0", started.question.answerOptions[0].id)
        assertEquals("A", started.question.answerOptions[0].text)
        assertEquals("1", started.question.answerOptions[1].id)
        assertEquals("B", started.question.answerOptions[1].text)
        assertEquals(20, started.timeLimitSeconds)
        assertEquals("2026-09-13T12:00:20.000Z", started.endsAt)
        assertEquals("2026-09-13T12:05:00.000Z", started.matchEndsAt)
        assertTrue(started.allowAnswerLate)
        assertEquals(12, started.remainingSeconds)
        assertEquals(3, started.lives)
    }

    @Test
    fun `question awaiting next maps previous result and player state`() {
        val event = mapper.map(
            GameSocketEvents.QUESTION_AWAITING_NEXT,
            """{
              "previous_result": {
                "question_index": 0,
                "is_correct": true,
                "score_earned": 800,
                "correct_answer": ["0", "2"]
              },
              "player_score": 1200,
              "lives": 2,
              "serverTime": "2026-09-26T12:00:00.000Z"
            }""".trimIndent()
        )

        assertTrue(event is GameEvent.QuestionAwaitingNext)
        val awaiting = (event as GameEvent.QuestionAwaitingNext).awaiting
        assertEquals(0, awaiting.questionIndex)
        assertTrue(awaiting.isCorrect)
        assertEquals(800, awaiting.scoreEarned)
        assertEquals(listOf("0", "2"), awaiting.correctAnswers)
        assertEquals(1200, awaiting.playerScore)
        assertEquals(2, awaiting.lives)
        assertEquals("2026-09-26T12:00:00.000Z", event.serverTime)
    }

    @Test
    fun `game state restores answered question snapshot`() {
        val event = mapper.map(
            GameSocketEvents.GAME_STATE,
            """{
              "session_status":"active",
              "current_phase":"question_active",
              "index":1,
              "total_questions":3,
              "question":{"index":1,"total":3,"id":22,"question_type":"multiple_select","question_text":"Q","answer_options":[]},
              "player":{
                "id":7,"player_name":"Kiro","status":"connected","current_question_index":1,
                "answered_questions":[{"question_id":22,"question_index":1,"answer":[0,2],"is_late":false,"answered_at":"2026-09-13T12:01:00.000Z"}]
              }
            }""".trimIndent()
        )
        assertTrue(event is GameEvent.StateSnapshot)
        val snapshot = (event as GameEvent.StateSnapshot).snapshot
        assertEquals(GamePhase.QUESTION_ACTIVE, snapshot.phase)
        assertEquals(listOf("0", "2"), snapshot.player?.answerFor(1)?.answerKeys)
        assertFalse(snapshot.player?.answerFor(1)?.isLate ?: true)
    }
}
