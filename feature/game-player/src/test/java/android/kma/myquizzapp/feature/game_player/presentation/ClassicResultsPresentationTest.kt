package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.PersonalQuestionResult
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ClassicResultsPresentationTest {
    private val now = Instant.parse("2026-10-20T04:00:00Z").toEpochMilli()
    private fun state() = GameUiState(mode = GameMode.CLASSIC, connection = GameConnection.CONNECTED,
        sessionStatus = SessionStatus.ACTIVE, phase = GamePhaseUi.Results(), showCorrectAnswer = true,
        showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        question = PublicQuestion(0, 2, 10, "multiple_choice", "question"),
        results = QuestionResults(0, 10, correctAnswers = listOf("a")),
        acceptedAnswerKeys = listOf("a"), hasAnswerAttempt = true, outcome = QuestionOutcome.CORRECT,
        endsAt = "2026-10-20T04:00:02Z")

    private fun personalResult() = PersonalQuestionResult(
        index = 0, questionId = 10, answered = true, isCorrect = true,
        scoreEarned = 950, totalScore = 3420, streak = 3,
        nextQuestionAt = null, serverTime = "2026-10-20T04:00:00Z",
    )

    @Test fun `score label uses authoritative signed points and preserves zero`() {
        val state = state().copy(personalResult = personalResult())
        assertEquals("+950 điểm", classicQuestionScoreLabel(state))
        assertEquals("0 điểm", classicQuestionScoreLabel(state.copy(personalResult = personalResult().copy(scoreEarned = 0))))
        assertEquals("-50 điểm", classicQuestionScoreLabel(state.copy(personalResult = personalResult().copy(scoreEarned = -50))))
        assertEquals("+950 điểm", classicQuestionScoreLabel(state.copy(playerScore = 99999)))
    }

    @Test fun `streak badge celebrates only a positive current server streak`() {
        val state = state().copy(personalResult = personalResult())
        assertEquals("Chuỗi 3 câu đúng liên tiếp!", classicStreakBadgeLabel(state))
        assertNull(classicStreakBadgeLabel(state.copy(personalResult = personalResult().copy(streak = 0))))
        assertNull(classicStreakBadgeLabel(state.copy(personalResult = personalResult().copy(streak = -1))))
    }

    @Test fun `missing result or hidden unknown reveal never creates score or streak labels`() {
        for (state in listOf(state(),
            state().copy(personalResult = personalResult(), showCorrectAnswer = false),
            state().copy(personalResult = personalResult(), showCorrectAnswer = null))) {
            assertNull(classicQuestionScoreLabel(state))
            assertNull(classicStreakBadgeLabel(state))
        }
    }

    @Test fun `another question result cannot render points or streak`() {
        for (result in listOf(personalResult().copy(index = 1), personalResult().copy(questionId = 99))) {
            val state = state().copy(personalResult = result)
            assertNull(classicQuestionScoreLabel(state))
            assertNull(classicStreakBadgeLabel(state))
        }
    }

    @Test fun `labels are scoped to Classic results and disappear on a new phase`() {
        val state = state().copy(personalResult = personalResult())
        for (phase in listOf(GamePhaseUi.Question, GamePhaseUi.Submitted, GamePhaseUi.Finished)) {
            assertNull(classicQuestionScoreLabel(state.copy(phase = phase)))
            assertNull(classicStreakBadgeLabel(state.copy(phase = phase)))
        }
        assertNull(classicQuestionScoreLabel(state.copy(mode = null)))
        assertNull(classicStreakBadgeLabel(state.copy(mode = null)))
    }

    @Test fun `restored private result can render labels without common answer details`() {
        val state = state().copy(personalResult = personalResult(), results = null,
            acceptedAnswerKeys = null, phase = GamePhaseUi.Results(restoredWithoutDetails = true))
        assertEquals("+950 điểm", classicQuestionScoreLabel(state))
        assertEquals("Chuỗi 3 câu đúng liên tiếp!", classicStreakBadgeLabel(state))
    }

    @Test fun `leaderboard hidden does not hide an authorized personal result`() {
        for (board in listOf(ShowLeaderboard.END_ONLY, ShowLeaderboard.NEVER)) {
            val state = state().copy(personalResult = personalResult(), showLeaderboard = board)
            assertEquals("+950 điểm", classicQuestionScoreLabel(state))
            assertEquals("Chuỗi 3 câu đúng liên tiếp!", classicStreakBadgeLabel(state))
        }
    }

    @Test fun `auto wait uses phase deadline offset ceil and never navigates at zero`() {
        assertEquals("Câu tiếp theo sẽ bắt đầu sau: 2s", classicResultsWaitText(state(), now + 1))
        assertEquals("Câu tiếp theo sẽ bắt đầu sau: 1s", classicResultsWaitText(state().copy(serverOffsetMs = 1_000), now))
        assertEquals("Đang chờ máy chủ chuyển tiếp…", classicResultsWaitText(state(), now + 2_000))
        assertEquals("Đang chờ máy chủ chuyển tiếp…", classicResultsWaitText(state(), now + 100_000))
    }

    @Test fun `manual paused reconnect and last question use appropriate wait copy`() {
        assertEquals("Chờ Host chuyển câu.", classicResultsWaitText(state().copy(autoAdvance = false), now))
        assertEquals("Trận đang tạm dừng — chờ Host tiếp tục.", classicResultsWaitText(state().copy(sessionStatus = SessionStatus.PAUSED), now + 100_000))
        assertEquals("Đang kết nối lại và đồng bộ phiên chơi…", classicResultsWaitText(state().copy(isSynchronizing = true), now))
        assertEquals("Tổng kết trận sẽ bắt đầu sau: 2s", classicResultsWaitText(state().copy(question = state().question!!.copy(index = 1)), now))
    }

    @Test fun `invalid or missing phase deadline does not fall back to stale results deadline`() {
        for (deadline in listOf(null, "invalid")) {
            val state = state().copy(endsAt = deadline, results = state().results!!.copy(nextQuestionAt = "2026-10-20T04:00:59Z"))
            assertEquals("Đang chờ máy chủ chuyển tiếp…", classicResultsWaitText(state, now))
        }
    }

    @Test fun `outcomes distinguish incorrect unanswered unconfirmed restored and hidden`() {
        assertEquals(ClassicResultKind.CORRECT, classicResultKind(state()))
        assertEquals(ClassicResultKind.INCORRECT, classicResultKind(state().copy(outcome = QuestionOutcome.INCORRECT)))
        assertEquals(ClassicResultKind.UNANSWERED, classicResultKind(state().copy(acceptedAnswerKeys = null, hasAnswerAttempt = false)))
        assertEquals(ClassicResultKind.UNKNOWN, classicResultKind(state().copy(acceptedAnswerKeys = null)))
        assertEquals(ClassicResultKind.UNKNOWN, classicResultKind(state().copy(phase = GamePhaseUi.Results(true))))
        assertEquals(ClassicResultKind.HIDDEN, classicResultKind(state().copy(showCorrectAnswer = false)))
    }

    @Test fun `unknown configuration or missing permitted data is not configured hidden`() {
        assertFalse(state().copy(showCorrectAnswer = null, showLeaderboard = null).skipsClassicSummary)
        assertTrue(state().copy(showCorrectAnswer = null, showLeaderboard = null).showsClassicSummary)
        assertTrue(state().copy(results = null).showsClassicSummary)
        assertFalse(state().copy(results = null).revealsClassicFeedback)
    }

    @Test fun `only both hidden skips summary and all controls remain locked during results`() {
        for (board in listOf(ShowLeaderboard.END_ONLY, ShowLeaderboard.NEVER)) {
            val state = state().copy(showCorrectAnswer = false, showLeaderboard = board)
            assertTrue(state.skipsClassicSummary)
            assertFalse(state.showsClassicSummary)
            assertTrue(state.usesClassicQuestionLayout)
            assertFalse(state.canSubmit)
        }
        assertTrue(state().copy(showCorrectAnswer = false).showsClassicSummary)
        assertTrue(state().copy(showLeaderboard = ShowLeaderboard.NEVER).showsClassicSummary)
    }

    @Test fun `feedback and summary are local views of the same server results phase`() {
        val feedback = state().copy(classicFeedbackRemainingMs = CLASSIC_FEEDBACK_DURATION_MS)
        assertTrue(feedback.usesClassicQuestionLayout)
        assertFalse(feedback.showsClassicSummary)
        assertTrue(feedback.revealsClassicFeedback)
        val summary = feedback.copy(classicFeedbackRemainingMs = 0)
        assertTrue(summary.showsClassicSummary)
        assertFalse(summary.usesClassicQuestionLayout)
        assertEquals(feedback.phase, summary.phase)
        assertFalse(summary.shouldShowNextAction)
    }
}
