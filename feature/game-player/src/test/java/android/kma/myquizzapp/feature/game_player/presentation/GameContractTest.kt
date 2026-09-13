package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameContractTest {
    private fun state(type: String) = GameUiState(
        phase = GamePhaseUi.Question,
        question = PublicQuestion(
            index = 0,
            total = 1,
            id = 1,
            questionType = type,
            questionText = "Q",
            answerOptions = listOf(PublicAnswerOption(id = "0", text = "Zero"))
        ),
        isInputLocked = false
    )

    @Test
    fun `option id zero is a valid single choice answer`() {
        assertTrue(state("multiple_choice").copy(selectedOptionId = "0").canSubmit)
    }

    @Test
    fun `empty multiple choice cannot submit`() {
        assertFalse(state("multiple_select").canSubmit)
    }

    @Test
    fun `blank text cannot submit`() {
        assertFalse(state("short_answer").copy(textAnswer = "   ").canSubmit)
    }
}
