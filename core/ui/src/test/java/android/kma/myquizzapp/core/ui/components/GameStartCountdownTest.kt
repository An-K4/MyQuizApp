package android.kma.myquizzapp.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class GameStartCountdownTest {
    @Test fun `missing target is unknown not zero`() {
        assertEquals(null, gameStartCountdownSeconds(null, 0, 1000))
    }
    @Test fun `ceil prevents skipping last second`() {
        assertEquals(3, gameStartCountdownSeconds(3000, 0, 0))
        assertEquals(3, gameStartCountdownSeconds(3000, 0, 1))
        assertEquals(1, gameStartCountdownSeconds(3000, 0, 2999))
    }
    @Test fun `clock uses server offset and clamps expired target`() {
        assertEquals(2, gameStartCountdownSeconds(5000, 2000, 1000))
        assertEquals(5, gameStartCountdownSeconds(5000, -1000, 1000))
        assertEquals(0, gameStartCountdownSeconds(5000, 0, 5000))
        assertEquals(0, gameStartCountdownSeconds(5000, 0, 9000))
    }
    @Test fun `resume uses replacement target not original duration`() {
        assertEquals(1, gameStartCountdownSeconds(3000, 0, 2000))
        assertEquals(2, gameStartCountdownSeconds(9000, 0, 7000))
    }
}
