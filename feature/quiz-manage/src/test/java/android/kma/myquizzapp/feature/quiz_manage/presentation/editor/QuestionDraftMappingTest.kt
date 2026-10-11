package android.kma.myquizzapp.feature.quiz_manage.presentation.editor

import android.kma.myquizzapp.core.common.model.CorrectAnswer
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.toNewQuestion
import org.junit.Assert.*
import org.junit.Test

class QuestionDraftMappingTest {
    @Test fun `mapper trims options without filtering rows or shifting indexes`() {
        val mapped = QuestionDraft(options = listOf(" A ", " ", " B "), correctIndexes = setOf(2)).toNewQuestion(null)
        assertEquals(listOf("A", "", "B"), mapped.answerOptions)
        assertEquals(CorrectAnswer.Indexes(listOf(2)), mapped.correctAnswer)
    }
    @Test fun `text answer ignores options and trims optional content`() {
        val mapped = QuestionDraft(questionType = QuestionType.LONG_ANSWER, questionText = " Câu hỏi ",
            correctText = " Đáp án ", questionHint = " ", explanation = " Giải thích ",
            timeLimit = "45", usesCustomTime = true).toNewQuestion("image")
        assertNull(mapped.answerOptions)
        assertEquals(CorrectAnswer.Text("Đáp án"), mapped.correctAnswer)
        assertEquals(45, mapped.timeLimit)
        assertNull(mapped.questionHint)
        assertEquals("Giải thích", mapped.explanation)
        assertEquals("image", mapped.questionImage)
    }
}
