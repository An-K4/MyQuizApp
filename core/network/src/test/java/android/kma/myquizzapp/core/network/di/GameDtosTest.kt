package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.CreateGameResponseDto
import android.kma.myquizzapp.core.network.dto.GameModesResponseDto
import android.kma.myquizzapp.core.network.dto.HostTokenResponseDto
import android.kma.myquizzapp.core.network.dto.JoinGameRequestDto
import android.kma.myquizzapp.core.network.dto.JoinGameResponseDto
import android.kma.myquizzapp.core.network.dto.RoomLookupResponseDto
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

    /**
     * N19: `GET /games/{code}` lồng `data.session.session` vì controller đặt tên biến
     * `session` cho cả cụm `{ session, players, config }` của `getLobby`. Test này giữ
     * lại payload THẬT để nếu backend sửa lại thành một cấp thì test đỏ ngay, thay vì
     * app crash trên máy người dùng.
     */
    @Test
    fun `room lookup decodes data session session wrapper and counts players`() {
        val envelope = json.decodeFromString<ApiEnvelope<RoomLookupResponseDto>>(
            """{"success":true,"data":{"session":{"session":{"id":91,"quiz_snapshot_id":12,"session_name":"Friday room","session_code":"ABC123","session_host":4,"total_players":0,"total_questions":10,"session_status":"lobby","game_mode":"classic","config":{},"current_question_index":-1,"current_phase":"lobby","phase_ends_at":null},"players":[{"id":5,"player_name":"An","player_score":0,"status":"connected"},{"id":6,"player_name":"Bình","player_score":0,"status":"connected"}],"config":{}}}}"""
        )

        val lookup = envelope.data!!.toDomain()
        assertEquals(91L, lookup.gameId)
        assertEquals("ABC123", lookup.sessionCode)
        assertEquals(GameMode.CLASSIC, lookup.mode)
        // total_players trên row vẫn là 0, sức chứa thật phải đếm từ `players`
        assertEquals(2, lookup.totalPlayers)
        assertTrue(lookup.isOpenForJoin)
    }

    /**
     * Join trả `socketToken` PHẲNG trong `data` (khác host token lồng một cấp), và
     * `player` là full row player_sessions — DTO chỉ lấy 4 field cần dùng, phần còn
     * lại phải bị bỏ qua chứ không được ném lỗi.
     */
    @Test
    fun `join response decodes flat socket token and ignores extra player columns`() {
        val envelope = json.decodeFromString<ApiEnvelope<JoinGameResponseDto>>(
            """{"success":true,"data":{"player":{"id":7,"game_session_id":91,"player_id":null,"player_guest_id":"6f1c1f8e-0b2a-4f1d-9a3e-0c7c9b2d5e11","player_avatar":null,"player_name":"Khách vui tính","player_score":0,"correct_answers_count":0,"answered_questions":[],"streak":0,"lives":null,"current_question_index":-1,"status":"connected"},"socketToken":"signed-player-token"}}"""
        )

        val result = envelope.data!!.toDomain()
        assertEquals(7L, result.player.id)
        assertEquals("Khách vui tính", result.player.playerName)
        assertEquals("signed-player-token", result.socketToken)
    }

    /** Người đã đăng nhập gửi body rỗng: server tự điền danh tính từ cookie. */
    @Test
    fun `logged in join request encodes to empty object`() {
        assertEquals("{}", json.encodeToString(JoinGameRequestDto()))

        val guestBody = json.encodeToString(
            JoinGameRequestDto(playerName = "An", playerGuestId = "uuid-1")
        )
        assertTrue(guestBody.contains("\"player_name\":\"An\""))
        assertTrue(guestBody.contains("\"player_guest_id\":\"uuid-1\""))
        assertFalse(guestBody.contains("playerName"))
    }
}
