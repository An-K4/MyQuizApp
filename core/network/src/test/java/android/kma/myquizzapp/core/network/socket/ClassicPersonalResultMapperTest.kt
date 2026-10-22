package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.network.di.NetworkModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ClassicPersonalResultMapperTest {
    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())
    private val fixture = """{"index":0,"question_id":123,"answered":true,"isCorrect":true,
        "scoreEarned":950,"totalScore":3420,"streak":3,"nextQuestionAt":"2026-10-20T04:00:05Z",
        "serverTime":"2026-10-20T04:00:00Z"}"""

    private fun event(raw: String) = mapper.map(GameSocketEvents.QUESTION_PERSONAL_RESULTS, raw)
    private fun snapshot(field: String) = mapper.map(GameSocketEvents.GAME_STATE,
        """{"current_phase":"showing_results","session_status":"paused","index":0,"endsAt":null$field}""")
        as GameEvent.StateSnapshot

    @Test fun `private event is registered exactly once`() {
        assertEquals(1, GameSocketEvents.SERVER_EVENTS.count { it == GameSocketEvents.QUESTION_PERSONAL_RESULTS })
    }

    @Test fun `mixed casing and numeric identity map identically through event and snapshot`() {
        val result = (event(fixture) as GameEvent.PersonalQuestionResultsReceived).result
        assertEquals(result, snapshot(",\"personal_result\":$fixture").snapshot.personalResult)
        assertEquals(123L, result.questionId)
        assertEquals(950, result.scoreEarned)
        assertEquals(3420, result.totalScore)
        assertEquals(3, result.streak)
        assertEquals("2026-10-20T04:00:05Z", result.nextQuestionAt)
    }

    @Test fun `unanswered null correctness and explicit zeros are genuine data`() {
        val raw = fixture.replace("\"answered\":true", "\"answered\":false")
            .replace("\"isCorrect\":true", "\"isCorrect\":null")
            .replace("\"scoreEarned\":950", "\"scoreEarned\":0")
            .replace("\"streak\":3", "\"streak\":0")
            .replace("\"nextQuestionAt\":\"2026-10-20T04:00:05Z\"", "\"nextQuestionAt\":null")
        val result = (event(raw) as GameEvent.PersonalQuestionResultsReceived).result
        assertFalse(result.answered)
        assertNull(result.isCorrect)
        assertEquals(0, result.scoreEarned)
        assertEquals(0, result.streak)
        assertNull(result.nextQuestionAt)
    }

    @Test fun `old missing or explicit null private snapshot remains compatible`() {
        for (field in listOf("", ",\"personal_result\":null")) {
            assertNull(snapshot(field).snapshot.personalResult)
            assertEquals(android.kma.myquizzapp.core.common.model.SessionStatus.PAUSED, snapshot(field).snapshot.sessionStatus)
        }
    }

    @Test fun `every private field is required and cannot default to zero or failure`() {
        val obj = Json.parseToJsonElement(fixture).jsonObject
        for (key in obj.keys) {
            val raw = kotlinx.serialization.json.JsonObject(obj - key).toString()
            assertTrue("Missing $key", event(raw) is GameEvent.Failed)
            assertNull(snapshot(",\"personal_result\":$raw").snapshot.personalResult)
        }
    }

    @Test fun `malformed identity grading and timing do not destroy the enclosing pause snapshot`() {
        val obj = Json.parseToJsonElement(fixture).jsonObject
        val badValues = listOf(
            "index" to JsonPrimitive(-1), "question_id" to JsonPrimitive(0),
            "answered" to JsonNull, "isCorrect" to JsonNull,
            "scoreEarned" to JsonPrimitive(-1), "totalScore" to JsonPrimitive("bad"),
            "streak" to JsonPrimitive(-1), "serverTime" to JsonPrimitive("not-a-date"),
            "nextQuestionAt" to JsonPrimitive("not-a-date")
        )
        for ((key, value) in badValues) {
            val raw = kotlinx.serialization.json.JsonObject(obj + (key to value)).toString()
            assertTrue("Malformed $key", event(raw) is GameEvent.Failed)
            val state = snapshot(",\"personal_result\":$raw").snapshot
            assertNull(state.personalResult)
            assertEquals(android.kma.myquizzapp.core.common.model.GamePhase.SHOWING_RESULTS, state.phase)
            assertNull(state.endsAt)
        }
    }

    @Test fun `wrong answer zero score is valid but unanswered with correctness is not`() {
        val wrong = fixture.replace("\"isCorrect\":true", "\"isCorrect\":false")
            .replace("\"scoreEarned\":950", "\"scoreEarned\":0").replace("\"streak\":3", "\"streak\":0")
        assertTrue(event(wrong) is GameEvent.PersonalQuestionResultsReceived)
        assertTrue(event(wrong.replace("\"answered\":true", "\"answered\":false")) is GameEvent.Failed)
    }

    @Test fun `omitted lobby score remains compatible with active backend payload`() {
        val event = mapper.map(GameSocketEvents.LOBBY_UPDATED,
            """{"session_status":"active","players":[{"id":202,"player_name":"Player","status":"connected"}],"config":{}}""")
        assertTrue(event is GameEvent.LobbyUpdated)
    }
}
