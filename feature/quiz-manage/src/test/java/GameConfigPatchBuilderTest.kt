package android.kma.myquizzapp.feature.quiz_manage.domain.util

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigConstraint
import android.kma.myquizzapp.core.common.model.GameConfigFieldSpec
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.Pacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameConfigPatchBuilderTest {

    private val descriptor = GameModeDescriptor(
        mode = GameMode.CLASSIC,
        pacing = Pacing.HOST,
        scored = true,
        defaultConfig = GameConfig(),
        editable = linkedMapOf(
            GameConfigKey.MAX_PLAYERS to GameConfigFieldSpec(
                key = GameConfigKey.MAX_PLAYERS,
                constraint = GameConfigConstraint.NumberConstraint(min = 1, max = 500),
                defaultValue = GameConfigValue.NumberValue(100)
            ),
            GameConfigKey.SHUFFLE_QUESTIONS to GameConfigFieldSpec(
                key = GameConfigKey.SHUFFLE_QUESTIONS,
                constraint = GameConfigConstraint.BooleanConstraint,
                defaultValue = GameConfigValue.BooleanValue(false)
            ),
            GameConfigKey.SHOW_HINT to GameConfigFieldSpec(
                key = GameConfigKey.SHOW_HINT,
                constraint = GameConfigConstraint.BooleanConstraint,
                defaultValue = GameConfigValue.BooleanValue(false)
            )
        ),
        locked = emptyMap()
    )

    @Test
    fun `builder keeps typed changes and omits baseline values`() {
        val patch = buildGameConfigPatch(
            descriptor,
            mapOf(
                GameConfigKey.MAX_PLAYERS to GameConfigValue.NumberValue(50),
                GameConfigKey.SHUFFLE_QUESTIONS to GameConfigValue.BooleanValue(true),
                GameConfigKey.SHOW_HINT to GameConfigValue.BooleanValue(false)
            )
        )

        assertEquals(GameConfigValue.NumberValue(50), patch[GameConfigKey.MAX_PLAYERS])
        assertEquals(
            GameConfigValue.BooleanValue(true),
            patch[GameConfigKey.SHUFFLE_QUESTIONS]
        )
        assertFalse(patch.containsKey(GameConfigKey.SHOW_HINT))
    }

    @Test
    fun `unchanged values produce empty patch`() {
        val patch = buildGameConfigPatch(
            descriptor,
            descriptor.editable.mapValues { it.value.defaultValue }
        )

        assertTrue(patch.isEmpty())
    }
}
