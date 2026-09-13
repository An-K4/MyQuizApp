package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.QuizCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionDiscoverTest {
    @Test
    fun `category topic comes from quiz data not title`() {
        val section = section(listOf(card("Science")), title = "Khoa học nổi bật")
        assertEquals("Science", section.discoverTopic)
        assertTrue(section.hasSeeMore)
    }

    @Test
    fun `inconsistent categories disable see more`() {
        val section = section(listOf(card("Science"), card("Geography", 2)))
        assertFalse(section.hasSeeMore)
    }

    private fun section(items: List<QuizCard>, title: String = "Category") = HomeSection(
        sectionKey = "category_any", title = title, sectionType = "category", items = items
    )

    private fun card(category: String, id: Long = 1) = QuizCard(
        id = id, quizName = "Quiz", quizCategory = category, quizLanguage = "vi",
        quizOwnerId = 1, questionCount = 1, playCount = 0, completionRate = 0.0,
        createdAt = "2026-09-13T00:00:00Z"
    )
}
