package android.kma.myquizzapp.feature.home.domain.discover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverSourceTest {
    @Test
    fun `all defaults to newest public browse`() {
        val request = resolveDiscoverRequest(null, null, null)
        assertEquals(DiscoverFilter.All, request.initialQuery.filter)
        assertEquals(DiscoverSort.NEWEST, request.initialQuery.sort)
    }

    @Test
    fun `category is selected from route independently from top bar`() {
        val request = resolveDiscoverRequest("category", "Khoa học nổi bật", "Science")
        assertEquals(
            DiscoverFilter.Category("Science", "Khoa học"),
            request.initialQuery.filter
        )
        assertEquals(DiscoverSort.TRENDING, request.initialQuery.sort)
        assertTrue(request.filters.contains(DiscoverFilter.All))
        assertTrue(request.filters.contains(DiscoverFilter.Category("Geography", "Địa lý")))
    }

    @Test
    fun `trending section opens trending sort`() {
        val request = resolveDiscoverRequest("trending", "Bất kỳ tiêu đề", null)
        assertEquals(DiscoverFilter.All, request.initialQuery.filter)
        assertEquals(DiscoverSort.TRENDING, request.initialQuery.sort)
    }

    @Test
    fun `custom routed category is appended and selected`() {
        val request = resolveDiscoverRequest("category", "Lịch sử", "History")
        val history = DiscoverFilter.Category("History", "Lịch sử")
        assertEquals(history, request.initialQuery.filter)
        assertTrue(request.filters.contains(history))
    }

    @Test
    fun `category without topic falls back safely`() {
        val request = resolveDiscoverRequest("category", "Category", null)
        assertEquals(DiscoverFilter.All, request.initialQuery.filter)
        assertEquals(DiscoverSort.NEWEST, request.initialQuery.sort)
    }
}
