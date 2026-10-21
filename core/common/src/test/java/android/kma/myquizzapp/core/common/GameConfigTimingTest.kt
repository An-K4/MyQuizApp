package android.kma.myquizzapp.core.common

import android.kma.myquizzapp.core.common.model.GameConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameConfigTimingTest {
    @Test
    fun `results window defaults to five seconds`() {
        assertEquals(5, GameConfig().timing.showResultsSeconds)
        assertEquals(5, GameConfig.Timing().showResultsSeconds)
    }

    @Test
    fun `missing timing or missing results duration uses new default`() {
        for (payload in listOf("{}", """{"timing":{}}""")) {
            val config = Json.decodeFromString<GameConfig>(payload)
            assertEquals(5, config.timing.showResultsSeconds)
        }
    }

    @Test
    fun `explicit server duration wins including older two second rooms`() {
        for (seconds in listOf(0, 2, 5, 9)) {
            val config = Json.decodeFromString<GameConfig>(
                """{"timing":{"showResultsSeconds":$seconds}}"""
            )
            assertEquals(seconds, config.timing.showResultsSeconds)
        }
    }
}
