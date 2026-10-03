package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.network.dto.ApiEnvelope
import android.kma.myquizzapp.core.network.dto.GameReviewResponseDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReviewDtoTest {
    private val json = NetworkModule.providePreserveCaseJson()

    @Test
    fun `review maps mixed question shapes and marathon indexes`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameReviewResponseDto>>(
            """{"success":true,"data":{"review":{"player_score":0,"correct_answers_count":1,"total_questions":2,"answered_count":2,"serverTime":"2026-10-03T08:00:00Z","items":[{"question_index":0,"question_id":10,"question_text":"Chọn A","answer_options":[{"id":0,"option_text":"A"},{"id":"b","text":"B"}],"explanation":"Vì A đúng","answered":true,"your_answer":0,"correct_answer":[0],"is_correct":true,"is_late":false,"score_earned":0,"time_taken":1.25},{"question_index":3,"question_id":11,"question_text":"Tự luận","answer_options":null,"explanation":null,"answered":false,"your_answer":null,"correct_answer":"Kotlin","is_correct":false,"is_late":false,"score_earned":0,"time_taken":null}]}}}"""
        )

        val review = requireNotNull(envelope.data).review.toDomain()
        assertEquals(0, review.playerScore)
        assertEquals("2026-10-03T08:00:00Z", review.serverTime)
        assertEquals(listOf("0"), review.items[0].yourAnswers)
        assertEquals("A", review.items[0].answerOptions[0].text)
        assertEquals("b", review.items[0].answerOptions[1].id)
        assertEquals(1.25, review.items[0].timeTakenSeconds!!, 0.0)
        assertTrue(review.items[0].isCorrect)
        assertEquals(3, review.items[1].questionIndex)
        assertEquals(listOf("Kotlin"), review.items[1].correctAnswers)
        assertFalse(review.items[1].answered)
        assertNull(review.items[1].explanation)
    }

    @Test
    fun `legacy plain string options use their position as id`() {
        val envelope = json.decodeFromString<ApiEnvelope<GameReviewResponseDto>>(
            """{"success":true,"data":{"review":{"items":[{"question_index":0,"answer_options":["Một","Hai"],"answered":true,"your_answer":[1],"correct_answer":1}]}}}"""
        )

        val item = requireNotNull(envelope.data).review.toDomain().items.single()
        assertEquals("0", item.answerOptions[0].id)
        assertEquals("Một", item.answerOptions[0].text)
        assertEquals(listOf("1"), item.yourAnswers)
        assertEquals(listOf("1"), item.correctAnswers)
    }
}
