package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.CreateGameResponseDto
import android.kma.myquizzapp.core.network.dto.GameModesResponseDto
import android.kma.myquizzapp.core.network.dto.HostTokenResponseDto
import android.kma.myquizzapp.core.network.dto.toRequestDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDtosTest {

    private val json = NetworkModule.providePreserveCaseJson()

    @Test
    fun `typed patch becomes outer snake case and nested camelCase`() {
        val request = CreateGameSessionParams(
            quizId = 7,
            sessionName = "Friday room",
            mode = GameMode.MARATHON,
            configPatch = mapOf(
                GameConfigKey.TOTAL_MATCH_SECONDS to GameConfigValue.NumberValue(300)
            )
        ).toRequestDto()

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"quiz_id\":7"))
        assertTrue(encoded.contains("\"session_name\":\"Friday room\""))
        assertTrue(encoded.contains("\"totalMatchSeconds\":300"))
        assertFalse(encoded.contains("quizId"))
        assertFalse(encoded.contains("total_match_seconds"))
    }

    @Test
    fun `game modes response maps wire paths to typed config keys`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameModesResponseDto>>(
            """{"success":true,"data":{"gameModes":[{"mode":"classic","pacing":"host","scored":true,"defaultConfig":{},"editable":{"lobby.maxPlayers":{"kind":"number","min":1,"max":500,"default":100},"flow.allowAnswerLate":{"kind":"boolean","default":false}},"locked":{"flow.allowAnswerLate":false}}]}}"""
        )

        val mode = envelope.data!!.gameModes.single().toDomain()
        assertEquals(GameMode.CLASSIC, mode.mode)
        assertEquals(
            GameConfigValue.NumberValue(100),
            mode.editable.getValue(GameConfigKey.MAX_PLAYERS).defaultValue
        )
        assertFalse(mode.editable.containsKey(GameConfigKey.ALLOW_ANSWER_LATE))
        assertEquals(
            GameConfigValue.BooleanValue(false),
            mode.locked[GameConfigKey.ALLOW_ANSWER_LATE]
        )
    }

    @Test
    fun `create response decodes data data session wrapper`() {
        val envelope = json.decodeFromString<ApiEnvelope<CreateGameResponseDto>>(
            """{"success":true,"data":{"data":{"session":{"id":91,"quiz_snapshot_id":12,"session_name":"Friday room","session_code":"ABC123","session_host":4,"total_players":0,"total_questions":10,"session_status":"lobby","game_mode":"classic","config":{},"current_question_index":-1,"current_phase":"lobby","phase_ends_at":null}},"ignored":[]}}"""
        )

        val result = envelope.data!!.toDomain()
        assertEquals(91L, result.session.id)
        assertEquals("ABC123", result.session.sessionCode)
        assertTrue(result.ignored.isEmpty())
    }

    @Test
    fun `host token response decodes nested socket token`() {
        val envelope = json.decodeFromString<ApiEnvelope<HostTokenResponseDto>>(
            """{"success":true,"data":{"hostToken":{"socketToken":"signed-token"}}}"""
        )

        assertEquals("signed-token", envelope.data!!.hostToken.socketToken)
    }
}
