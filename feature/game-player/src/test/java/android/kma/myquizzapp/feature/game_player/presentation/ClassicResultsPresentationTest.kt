package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
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
