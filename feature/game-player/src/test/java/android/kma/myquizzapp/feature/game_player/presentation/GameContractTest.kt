package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import org.junit.Assert.assertEquals
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

    @Test
    fun `multiple select must match full normalized set`() {
        assertEquals(
            QuestionOutcome.CORRECT,
            evaluateOutcome("multiple_select", listOf(" B ", "a"), listOf("A", "b"))
        )
        assertEquals(
            QuestionOutcome.INCORRECT,
            evaluateOutcome("multiple_select", listOf("a"), listOf("a", "b"))
        )
    }

    @Test
    fun `missing correct answer produces neutral result`() {
        assertEquals(QuestionOutcome.HIDDEN, evaluateOutcome("short_answer", listOf("Hanoi"), emptyList()))
    }

    @Test
    fun `hidden correct answer strips answer and distribution defensively`() {
        val feedback = resolveQuestionFeedback(
            showCorrectAnswer = false,
            questionType = "multiple_choice",
            submitted = listOf("a"),
            results = QuestionResults(
                index = 0,
                correctAnswers = listOf("a"),
                stats = AnswerStats(total = 1, distribution = mapOf("a" to 1))
            )
        )
        assertEquals(QuestionOutcome.HIDDEN, feedback.outcome)
        assertTrue(feedback.results.correctAnswers.isEmpty())
        assertTrue(feedback.results.stats.distribution.isEmpty())
    }

    @Test
    fun `paused game disables an otherwise valid answer`() {
        val paused = state("multiple_choice").copy(
            selectedOptionId = "0",
            sessionStatus = SessionStatus.PAUSED
        )
        assertFalse(paused.isAnswerInputEnabled)
        assertFalse(paused.canSubmit)
    }

    @Test
    fun `live leaderboard is visible only between questions`() {
        val row = LeaderboardRow(rank = 1, id = 1, playerName = "Kiro", playerScore = 100)
        val results = state("multiple_choice").copy(
            phase = GamePhaseUi.Results(),
            leaderboard = listOf(row),
            showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS
        )
        assertTrue(results.canShowLiveLeaderboard)
        assertFalse(results.copy(phase = GamePhaseUi.Question).canShowLiveLeaderboard)
        assertFalse(results.copy(showLeaderboard = ShowLeaderboard.END_ONLY).canShowLiveLeaderboard)
        assertFalse(results.copy(showLeaderboard = ShowLeaderboard.NEVER).canShowLiveLeaderboard)
    }
}
