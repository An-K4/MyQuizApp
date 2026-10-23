package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class OnlineReviewPresentationTest {
    private fun item() = GameReviewItem(
        questionIndex = 0, questionId = 7, questionText = "Question",
        answerOptions = listOf(PublicAnswerOption("a", "First"), PublicAnswerOption("b", "Second"), PublicAnswerOption("c", "Third")),
        answered = true, yourAnswers = listOf("b"), correctAnswers = listOf("b"),
        isCorrect = true, isLate = false, scoreEarned = 950,
    )

    @Test fun `correct card identifies the chosen answer and correct option`() {
        val ui = item().toOnlineReviewCardUi(GameMode.CLASSIC)
        assertEquals(OnlineReviewTone.Correct, ui.tone)
        assertEquals(listOf(false, true, false), ui.options.map { it.selected })
        assertEquals(OnlineReviewTone.Correct, ui.options[1].tone)
        assertEquals("Second\nBạn đã chọn", ui.options[1].displayText)
        assertTrue(ui.responses.isEmpty())
    }

    @Test fun `wrong selected option is red while the correct option is green`() {
        val ui = item().copy(yourAnswers = listOf("a"), isCorrect = false).toOnlineReviewCardUi(GameMode.SOLO)
        assertEquals(OnlineReviewTone.Incorrect, ui.tone)
        assertEquals(listOf(OnlineReviewTone.Incorrect, OnlineReviewTone.Correct, OnlineReviewTone.Neutral), ui.options.map { it.tone })
        assertTrue(ui.options[0].displayText.contains("Bạn đã chọn"))
        assertFalse(ui.options[1].displayText.contains("Bạn đã chọn"))
    }

    @Test fun `partial selection never marks a correctly selected option wrong`() {
        val ui = item().copy(yourAnswers = listOf("a"), correctAnswers = listOf("a", "b"), isCorrect = false)
            .toOnlineReviewCardUi(GameMode.SURVIVAL)
        assertEquals(OnlineReviewTone.Incorrect, ui.tone)
        assertEquals(OnlineReviewTone.Correct, ui.options[0].tone)
        assertEquals(OnlineReviewTone.Correct, ui.options[1].tone)
        assertTrue(ui.options[0].selected)
        assertFalse(ui.options[1].selected)
    }

    @Test fun `multiple submitted options remain explicit including a wrong extra choice`() {
        val ui = item().copy(yourAnswers = listOf("a", "c"), correctAnswers = listOf("a", "b"), isCorrect = false)
            .toOnlineReviewCardUi(GameMode.SOLO)
        assertEquals(listOf(true, false, true), ui.options.map { it.selected })
        assertEquals(listOf(OnlineReviewTone.Correct, OnlineReviewTone.Correct, OnlineReviewTone.Incorrect), ui.options.map { it.tone })
    }

    @Test fun `no correct answer data means no invented red option`() {
        val ui = item().copy(correctAnswers = emptyList(), isCorrect = false).toOnlineReviewCardUi(GameMode.CLASSIC)
        assertEquals(OnlineReviewTone.Incorrect, ui.tone) // server verdict remains authoritative
        assertTrue(ui.options.all { it.tone == OnlineReviewTone.Neutral })
        assertTrue(ui.options[1].selected)
        assertTrue(ui.notices.any { it.contains("Chưa có dữ liệu đáp án đúng") })
    }

    @Test fun `missing options do not discard submitted or correct ids`() {
        val ui = item().copy(yourAnswers = listOf("removed-mine"), correctAnswers = listOf("removed-correct"), isCorrect = false)
            .toOnlineReviewCardUi(GameMode.SOLO)
        assertTrue(ui.notices.any { it.contains("removed-mine") })
        assertTrue(ui.notices.any { it.contains("removed-correct") })
        assertTrue(ui.options.none { it.selected })
        assertEquals(3, ui.options.size)
    }

    @Test fun `option order is unchanged and matching uses ids not display letters`() {
        val source = item().copy(answerOptions = item().answerOptions.reversed())
        val before = source.answerOptions.toList()
        val ui = source.toOnlineReviewCardUi(GameMode.CLASSIC)
        assertEquals(listOf("Third", "Second", "First"), ui.options.map { it.text })
        assertEquals(listOf("A", "B", "C"), ui.options.map { it.label })
        assertTrue(ui.options[1].selected)
        assertEquals(before, source.answerOptions)
    }

    @Test fun `id normalization is independent of the device locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val ui = item().copy(answerOptions = listOf(PublicAnswerOption("I", "Option")),
                yourAnswers = listOf(" i "), correctAnswers = listOf("I"))
                .toOnlineReviewCardUi(GameMode.CLASSIC)
            assertTrue(ui.options.single().selected)
            assertEquals(OnlineReviewTone.Correct, ui.options.single().tone)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun `unanswered overrides conflicting flags and never displays stale selection`() {
        val ui = item().copy(answered = false, isCorrect = true).toOnlineReviewCardUi(GameMode.SOLO)
        assertEquals(OnlineReviewTone.Skipped, ui.tone)
        assertTrue(ui.options.none { it.selected })
        assertEquals(OnlineReviewTone.Correct, ui.options[1].tone) // may still reveal correct answer
    }

    @Test fun `text is preserved exactly and is never locally regraded`() {
        val ui = item().copy(answerOptions = emptyList(), yourAnswers = listOf("  Học & chơi\nDòng hai  "),
            correctAnswers = listOf("A different text"), isCorrect = true).toOnlineReviewCardUi(GameMode.SOLO)
        assertTrue(ui.options.isEmpty())
        assertEquals("  Học & chơi\nDòng hai  ", ui.responses[0].text)
        assertEquals(OnlineReviewTone.Correct, ui.responses[0].tone)
        assertEquals("A different text", ui.responses[1].text)
    }

    @Test fun `missing personal text is not relabelled as a skipped question`() {
        val ui = item().copy(answerOptions = emptyList(), yourAnswers = emptyList(), isCorrect = false)
            .toOnlineReviewCardUi(GameMode.SOLO)
        assertEquals(OnlineReviewTone.Incorrect, ui.tone)
        assertEquals("Không có dữ liệu câu trả lời", ui.responses[0].text)
        assertEquals(OnlineReviewTone.Neutral, ui.responses[0].tone)
    }

    @Test fun `skipped text and unavailable expected answer stay neutral and explicit`() {
        val ui = item().copy(answerOptions = emptyList(), answered = false, yourAnswers = emptyList(),
            correctAnswers = emptyList(), isCorrect = false).toOnlineReviewCardUi(GameMode.PRACTICE)
        assertEquals("Chưa trả lời", ui.responses.single().text)
        assertEquals(OnlineReviewTone.Neutral, ui.responses.single().tone)
        assertEquals(1, ui.notices.size)
    }

    @Test fun `practice hides score for every server value without hiding timing or late evidence`() {
        for (score in listOf(0, 950, -10)) {
            val ui = item().copy(scoreEarned = score, timeTakenSeconds = 2.35, isLate = true)
                .toOnlineReviewCardUi(GameMode.PRACTICE)
            assertNull(ui.scoreText)
            assertEquals("2,35 giây", ui.timeText)
            assertTrue(ui.isLate)
        }
    }

    @Test fun `competitive modes keep actual zero and negative scores`() {
        assertEquals("0 điểm", item().copy(scoreEarned = 0).toOnlineReviewCardUi(GameMode.SOLO).scoreText)
        assertEquals("-10 điểm", item().copy(scoreEarned = -10).toOnlineReviewCardUi(GameMode.SURVIVAL).scoreText)
        assertEquals("1.250 điểm", item().copy(scoreEarned = 1250).toOnlineReviewCardUi(GameMode.MARATHON).scoreText)
    }

    @Test fun `invalid timing is omitted but zero remains a real value`() {
        for (value in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertNull(item().copy(timeTakenSeconds = value).toOnlineReviewCardUi(GameMode.SOLO).timeText)
        }
        assertEquals("0,00 giây", item().copy(timeTakenSeconds = 0.0).toOnlineReviewCardUi(GameMode.SOLO).timeText)
    }

    @Test fun `missing content has honest fallback and blank explanation is omitted`() {
        val ui = item().copy(questionText = " ", explanation = " ",
            answerOptions = listOf(PublicAnswerOption("a", ""), PublicAnswerOption("", null)))
            .toOnlineReviewCardUi(GameMode.CLASSIC)
        assertEquals("Nội dung câu hỏi không còn khả dụng", ui.questionText)
        assertEquals("Đáp án a", ui.options[0].text)
        assertEquals("Nội dung đáp án không còn khả dụng", ui.options[1].text)
        assertNull(ui.explanation)
    }

    @Test fun `marathon keeps attempt index and never shows a bank size denominator`() {
        val ui = item().copy(questionIndex = 20).toOnlineReviewCardUi(GameMode.MARATHON)
        assertEquals("Câu 21", ui.heading)
        val review = GameReview(950, 15, 10, 21, emptyList())
        assertEquals("15 câu đúng • 21 lượt trả lời", onlineReviewSummary(review, GameMode.MARATHON))
        assertEquals("2 câu đúng • 3 lượt trả lời", onlineReviewSummary(review.copy(correctAnswersCount = 2, answeredCount = 3), GameMode.MARATHON))
    }

    @Test fun `finite summary uses server counts while absent invalid totals do not produce fake ratios`() {
        val review = GameReview(950, 2, 3, 3, emptyList())
        assertEquals("2/3 câu đúng • 3 lượt trả lời", onlineReviewSummary(review, GameMode.CLASSIC))
        assertEquals("2 câu đúng • 3 lượt trả lời", onlineReviewSummary(review.copy(totalQuestions = 0), GameMode.SOLO))
        assertEquals("4 câu đúng • 3 lượt trả lời", onlineReviewSummary(review.copy(correctAnswersCount = 4), GameMode.SOLO))
        assertEquals("Chưa có dữ liệu tổng kết câu trả lời.", onlineReviewSummary(review.copy(correctAnswersCount = -1, answeredCount = -1), GameMode.SOLO))
    }

    @Test fun `review availability and previous ordering remain controlled by final state`() {
        val wrong = item().copy(questionIndex = 3, isCorrect = false)
        val skipped = item().copy(questionIndex = 5, answered = false, isCorrect = false)
        val review = GameReview(950, 1, 6, 2, listOf(item(), wrong, skipped))
        val state = FinalResultUiState(gameId = 7, playerId = 9, result = GameEnded(reviewEnabled = false), review = review)
        assertFalse(state.reviewEnabled)
        assertEquals(listOf(5, 3, 0), state.reviewItems.map { it.questionIndex })
        assertEquals(listOf(0, 3, 5), review.items.map { it.questionIndex })
    }
    @Test fun `known single choice omits the redundant chosen caption but keeps selection evidence`() {
        val ui = item().toOnlineReviewCardUi(GameMode.CLASSIC, "multiple_choice")
        assertTrue(ui.options[1].selected)
        assertEquals("Second", ui.options[1].displayText)
        val wrong = item().copy(yourAnswers = listOf("a"), isCorrect = false)
            .toOnlineReviewCardUi(GameMode.SOLO, "multiple_choice")
        assertEquals("First", wrong.options[0].displayText)
        assertEquals(OnlineReviewTone.Incorrect, wrong.options[0].tone)
    }
    @Test fun `known multiple select with one chosen option still keeps caption`() {
        val ui = item().toOnlineReviewCardUi(GameMode.SOLO, "multiple_select")
        assertEquals("Second\nBạn đã chọn", ui.options[1].displayText)
    }
    @Test fun `unknown type is never inferred from one selected and one correct key`() {
        val ui = item().toOnlineReviewCardUi(GameMode.CLASSIC)
        assertEquals("Second\nBạn đã chọn", ui.options[1].displayText)
    }
    @Test fun `single choice missing correct answer evidence retains chosen caption`() {
        val ui = item().copy(correctAnswers = emptyList()).toOnlineReviewCardUi(GameMode.SOLO, "multiple_choice")
        assertEquals("Second\nBạn đã chọn", ui.options[1].displayText)
        assertEquals(OnlineReviewTone.Neutral, ui.options[1].tone)
    }


}
