package android.kma.myquizzapp.feature.home.presentation.search

import android.kma.myquizzapp.core.common.model.QuizCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUiStateTest {

    @Test
    fun `typed but unsubmitted query does not show no results`() {
        val state = SearchUiState(query = "android")

        assertFalse(state.shouldShowNoResults)
    }

    @Test
    fun `successful empty response for current query shows no results`() {
        val state = SearchUiState(
            query = "android",
            submittedQuery = "android",
            hasCompletedSearch = true
        )

        assertTrue(state.shouldShowNoResults)
    }

    @Test
    fun `response for stale query does not show no results`() {
        val state = SearchUiState(
            query = "android quiz",
            submittedQuery = "android",
            hasCompletedSearch = true
        )

        assertFalse(state.shouldShowNoResults)
    }

    @Test
    fun `loading or error state does not show no results`() {
        assertFalse(
            SearchUiState(
                query = "android",
                submittedQuery = "android",
                hasCompletedSearch = true,
                isSearching = true
            ).shouldShowNoResults
        )
        assertFalse(
            SearchUiState(
                query = "android",
                submittedQuery = "android",
                hasCompletedSearch = true,
                error = "Network error"
            ).shouldShowNoResults
        )
    }

    private fun appendableState() = SearchUiState(
        query = " android ", submittedQuery = "android", hasCompletedSearch = true,
        results = listOf(QuizCard(
            id = 1, quizName = "Android", quizLanguage = "vi", quizOwnerId = 1,
            questionCount = 10, playCount = 20, completionRate = 0.0, createdAt = "2026-10-10"
        )), nextCursor = "next-page", hasMore = true
    )

    @Test fun `successful current page with cursor can append`() {
        assertTrue(appendableState().canLoadMore)
        assertFalse(appendableState().canRetryLoadMore)
    }

    @Test fun `unsubmitted or stale query cannot append`() {
        assertFalse(appendableState().copy(hasCompletedSearch = false).canLoadMore)
        assertFalse(appendableState().copy(query = "new query").canLoadMore)
    }

    @Test fun `missing cursor exhausted page or empty results cannot append`() {
        assertFalse(appendableState().copy(nextCursor = null).canLoadMore)
        assertFalse(appendableState().copy(hasMore = false).canLoadMore)
        assertFalse(appendableState().copy(results = emptyList()).canLoadMore)
    }

    @Test fun `loading blocks both automatic append and explicit retry`() {
        assertFalse(appendableState().copy(isSearching = true).canLoadMore)
        assertFalse(appendableState().copy(isLoadingMore = true).canLoadMore)
        assertFalse(appendableState().copy(isLoadingMore = true, error = "Network").canRetryLoadMore)
    }

    @Test fun `append error pauses automatic loading but permits explicit retry`() {
        val state = appendableState().copy(error = "Network")
        assertFalse(state.canLoadMore)
        assertTrue(state.canRetryLoadMore)
        assertFalse(state.copy(nextCursor = null).canRetryLoadMore)
        assertFalse(state.copy(query = "different").canRetryLoadMore)
    }
}
