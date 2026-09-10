package android.kma.myquizzapp.core.ui.gameconfig

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigConstraint
import android.kma.myquizzapp.core.common.model.GameConfigFieldSpec
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.Pacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameConfigPatchBuilderTest {

    private val descriptor = GameModeDescriptor(
        mode = GameMode.CLASSIC,
        pacing = Pacing.HOST,
        scored = true,
        defaultConfig = GameConfig(),
        editable = mapOf(
            GameConfigKey.PER_QUESTION_SECONDS to GameConfigFieldSpec(
                key = GameConfigKey.PER_QUESTION_SECONDS,
                constraint = GameConfigConstraint.NumberConstraint(
                    min = 0,
                    max = 600,
                    nullable = true,
                    note = null
                ),
                defaultValue = GameConfigValue.NumberValue(null)
            ),
            GameConfigKey.MAX_PLAYERS to GameConfigFieldSpec(
                key = GameConfigKey.MAX_PLAYERS,
                constraint = GameConfigConstraint.NumberConstraint(
                    min = 1,
                    max = 500,
                    nullable = false,
                    note = null
                ),
                defaultValue = GameConfigValue.NumberValue(100)
            ),
            GameConfigKey.SHOW_HINT to GameConfigFieldSpec(
                key = GameConfigKey.SHOW_HINT,
                constraint = GameConfigConstraint.BooleanConstraint,
                defaultValue = GameConfigValue.BooleanValue(false)
            )
        ),
        locked = mapOf(
            GameConfigKey.LIVES to GameConfigValue.NumberValue(null)
        )
    )

    @Test
    fun `builder keeps typed changes and omits baseline values`() {
        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = mapOf(
                GameConfigKey.PER_QUESTION_SECONDS to GameConfigValue.NumberValue(30),
                GameConfigKey.MAX_PLAYERS to GameConfigValue.NumberValue(100),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(true)
            )
        )

        assertEquals(
            mapOf(
                GameConfigKey.PER_QUESTION_SECONDS to GameConfigValue.NumberValue(30),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(true)
            ),
            patch
        )
    }

    @Test
    fun `unchanged values produce empty patch`() {
        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = mapOf(
                GameConfigKey.PER_QUESTION_SECONDS to GameConfigValue.NumberValue(null),
                GameConfigKey.MAX_PLAYERS to GameConfigValue.NumberValue(100),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(false)
            )
        )

        assertTrue(patch.isEmpty())
    }

    @Test
    fun `locked keys never enter the patch`() {
        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = mapOf(GameConfigKey.LIVES to GameConfigValue.NumberValue(3))
        )

        assertTrue(patch.isEmpty())
    }

    /*
     * Ca quan trọng của HostLobby: phòng đang bật showHint (khác default), host tắt
     * nó đi (về đúng default). Nếu vẫn diff so với default của mode thì patch rỗng
     * và host bấm lưu không gì xảy ra.
     */
    @Test
    fun `room baseline detects revert back to mode default`() {
        val roomConfig = GameConfig(
            flow = GameConfig.Flow(showHint = true),
            timing = GameConfig.Timing(perQuestionSeconds = 30)
        )

        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = mapOf(
                GameConfigKey.PER_QUESTION_SECONDS to GameConfigValue.NumberValue(30),
                GameConfigKey.MAX_PLAYERS to GameConfigValue.NumberValue(100),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(false)
            ),
            baseline = roomConfig.baselineFor(descriptor)
        )

        assertEquals(
            mapOf(GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(false)),
            patch
        )
    }

    @Test
    fun `room baseline omits fields the host did not touch`() {
        val roomConfig = GameConfig(
            lobby = GameConfig.LobbyConfig(maxPlayers = 40),
            timing = GameConfig.Timing(perQuestionSeconds = 15)
        )

        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = mapOf(
                GameConfigKey.PER_QUESTION_SECONDS to GameConfigValue.NumberValue(15),
                GameConfigKey.MAX_PLAYERS to GameConfigValue.NumberValue(40),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(false)
            ),
            baseline = roomConfig.baselineFor(descriptor)
        )

        assertTrue(patch.isEmpty())
    }

    @Test
    fun `form built from room config round-trips into an empty patch`() {
        val roomConfig = GameConfig(
            flow = GameConfig.Flow(showHint = true),
            lobby = GameConfig.LobbyConfig(maxPlayers = 25),
            timing = GameConfig.Timing(perQuestionSeconds = 45)
        )
        val form = RoomConfigForm.fromConfig(descriptor, roomConfig)

        val patch = buildGameConfigPatch(
            descriptor = descriptor,
            values = form.values(),
            baseline = roomConfig.baselineFor(descriptor)
        )

        assertTrue(patch.isEmpty())
    }
}
