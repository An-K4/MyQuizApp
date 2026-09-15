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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomConfigFormTest {

    private val descriptor = GameModeDescriptor(
        mode = GameMode.CLASSIC,
        pacing = Pacing.HOST,
        scored = true,
        defaultConfig = GameConfig(),
        editable = listOf(
            GameConfigKey.SHOW_CORRECT_ANSWER,
            GameConfigKey.REVIEW_MODE
        ).associateWith { key ->
            GameConfigFieldSpec(
                key = key,
                constraint = GameConfigConstraint.BooleanConstraint,
                defaultValue = GameConfigValue.BooleanValue(true)
            )
        },
        locked = emptyMap()
    )

    @Test
    fun `disabling correct answer also disables review mode`() {
        val roomConfig = GameConfig(
            flow = GameConfig.Flow(
                showCorrectAnswer = true,
                reviewMode = true
            )
        )

        val form = RoomConfigForm.fromConfig(descriptor, roomConfig)
            .updateBoolean(GameConfigKey.SHOW_CORRECT_ANSWER, false)

        assertFalse(form.showCorrectAnswer.value)
        assertFalse(form.reviewMode.value)
        assertEquals(
            mapOf(
                GameConfigKey.SHOW_CORRECT_ANSWER to GameConfigValue.BooleanValue(false),
                GameConfigKey.REVIEW_MODE to GameConfigValue.BooleanValue(false)
            ),
            buildGameConfigPatch(
                descriptor = descriptor,
                values = form.values(),
                baseline = roomConfig.baselineFor(descriptor)
            )
        )
    }

    @Test
    fun `enabling review mode also enables correct answer`() {
        val form = RoomConfigForm.fromConfig(
            descriptor = descriptor,
            config = GameConfig(
                flow = GameConfig.Flow(
                    showCorrectAnswer = false,
                    reviewMode = false
                )
            )
        ).updateBoolean(GameConfigKey.REVIEW_MODE, true)

        assertTrue(form.reviewMode.value)
        assertTrue(form.showCorrectAnswer.value)
    }

    @Test
    fun `marathon keeps correct answer enabled and read only`() {
        val form = RoomConfigForm.fromConfig(
            descriptor = descriptor.copy(mode = GameMode.MARATHON),
            config = GameConfig(
                flow = GameConfig.Flow(
                    showCorrectAnswer = false,
                    reviewMode = false
                )
            )
        )

        assertTrue(form.showCorrectAnswer.value)
        assertFalse(form.showCorrectAnswer.editable)
    }
}
