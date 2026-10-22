package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.*
import org.junit.Assert.*
import org.junit.Test

class ClassicPersonalResultsReducerTest {
    private val result = PersonalQuestionResult(0, 10, true, true, 950, 3420, 3,
        "2026-10-20T04:00:05Z", "2026-10-20T04:00:00Z")
    private val common = QuestionResults(0, 10, listOf("a"), nextQuestionAt = result.nextQuestionAt)
    private fun state() = GameUiState(mode = GameMode.CLASSIC, connection = GameConnection.CONNECTED,
        phase = GamePhaseUi.Question, showCorrectAnswer = true, showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        question = PublicQuestion(0, 2, 10, "multiple_choice", "Question"))

    @Test fun `grading and current streak are assigned idempotently without needing ACK or keys`() {
        val settled = state().reconcileClassicPersonalResult(result)
        assertEquals(settled, settled.reconcileClassicPersonalResult(result))
        assertEquals(950, settled.scoreEarned)
        assertEquals(3420, settled.totalScore)
        assertEquals(3, settled.streak)
        assertEquals(ClassicResultKind.CORRECT, classicResultKind(settled))
        assertTrue(settled.showsClassicSummary)
        assertFalse(settled.canSubmit)
        assertNull(settled.results)
        assertNull(settled.acceptedAnswerKeys)
    }

    @Test fun `common private order and duplicates preserve server grading without restarting feedback`() {
        val first = state().reconcileClassicCommonResult(common, result.serverTime)
        val ticked = first.copy(classicFeedbackRemainingMs = 700L)
        val settled = ticked.reconcileClassicPersonalResult(result)
        val repeated = settled.reconcileClassicCommonResult(common, result.serverTime).reconcileClassicPersonalResult(result)
        assertEquals(700L, repeated.classicFeedbackRemainingMs)
        assertEquals(QuestionOutcome.CORRECT, repeated.outcome)
        assertEquals(3, repeated.streak)
        val privateFirst = state().reconcileClassicPersonalResult(result).reconcileClassicCommonResult(common, result.serverTime)
        assertEquals(0L, privateFirst.classicFeedbackRemainingMs)
        assertEquals(QuestionOutcome.CORRECT, privateFirst.outcome)
        assertEquals(common.correctAnswers, privateFirst.results!!.correctAnswers)
    }

    @Test fun `incorrect unanswered and zero totals are distinct from missing data`() {
        val wrong = state().reconcileClassicPersonalResult(result.copy(isCorrect = false, scoreEarned = 0, streak = 0))
        assertEquals(ClassicResultKind.INCORRECT, classicResultKind(wrong))
        val unanswered = state().reconcileClassicPersonalResult(result.copy(answered = false, isCorrect = null,
            scoreEarned = 0, totalScore = 0, streak = 0))
        assertEquals(ClassicResultKind.UNANSWERED, classicResultKind(unanswered))
        assertEquals(0, unanswered.totalScore)
        assertNull(state().totalScore)
    }

    @Test fun `identity both parts and terminal or other modes reject private result`() {
        for (s in listOf(state().copy(question = state().question!!.copy(index = 1)),
            state().copy(question = state().question!!.copy(id = 11)),
            state().copy(phase = GamePhaseUi.Finished), state().copy(phase = GamePhaseUi.Countdown(null)),
            state().copy(mode = GameMode.SOLO, pacing = Pacing.SELF))) {
            assertEquals(s, s.reconcileClassicPersonalResult(result))
        }
    }

    @Test fun `privacy true false unknown crossed with every board setting`() {
        for (reveal in listOf(true, false, null)) for (board in ShowLeaderboard.entries) {
            val settled = state().copy(showCorrectAnswer = reveal, showLeaderboard = board)
                .reconcileClassicPersonalResult(result)
            assertEquals(reveal == true, settled.visiblePersonalResult != null)
            assertEquals(reveal == true && board == ShowLeaderboard.BETWEEN_QUESTIONS, settled.canShowClassicRank)
        }
    }

    @Test fun `snapshot timing wins over late common and private even across pause resume`() {
        val settled = state().reconcileClassicPersonalResult(result)
        val paused = settled.copy(sessionStatus = SessionStatus.PAUSED, endsAt = null, classicTimingFromSnapshot = true,
            classicPhaseServerTime = "2026-10-20T04:00:02Z")
        assertNull(paused.reconcileClassicPersonalResult(result).endsAt)
        assertNull(paused.reconcileClassicCommonResult(common, result.serverTime).endsAt)
        val resumed = paused.copy(sessionStatus = SessionStatus.ACTIVE, endsAt = "2026-10-20T04:00:12Z")
        assertEquals(resumed.endsAt, resumed.reconcileClassicPersonalResult(result).endsAt)
        assertEquals(resumed.endsAt, resumed.reconcileClassicCommonResult(common, result.serverTime).endsAt)
    }

    @Test fun `older private payload cannot replace a newer result or reopen active state`() {
        val newer = result.copy(serverTime = "2026-10-20T04:00:10Z")
        val settled = state().reconcileClassicPersonalResult(newer)
        assertEquals(settled, settled.reconcileClassicPersonalResult(result))
        val active = state().copy(classicPhaseServerTime = newer.serverTime)
        assertEquals(active, active.reconcileClassicPersonalResult(result))
    }

    @Test fun `restore does not replay feedback or manufacture an answer key or deadline`() {
        val restored = state().copy(phase = GamePhaseUi.Results(true), endsAt = null,
            classicFeedbackRemainingMs = 1500).reconcileClassicPersonalResult(result, restored = true)
        assertEquals(0L, restored.classicFeedbackRemainingMs)
        assertNull(restored.endsAt)
        assertNull(restored.results)
        assertTrue(restored.showsClassicSummary)
        assertFalse(restored.revealsClassicFeedback)
    }

    @Test fun `zero two five second and manual windows never extend server budget`() {
        for (seconds in listOf(0, 2, 5)) {
            val deadline = "2026-10-20T04:00:0${seconds}Z"
            val settled = state().reconcileClassicCommonResult(common.copy(nextQuestionAt = deadline), result.serverTime)
            assertEquals(minOf(2000L, seconds * 1000L), settled.classicFeedbackRemainingMs)
            assertEquals(deadline, settled.endsAt)
        }
        val manual = state().copy(autoAdvance = false).reconcileClassicCommonResult(common.copy(nextQuestionAt = null), result.serverTime)
        assertNull(manual.endsAt)
        assertFalse(manual.shouldShowNextAction)
    }
}
