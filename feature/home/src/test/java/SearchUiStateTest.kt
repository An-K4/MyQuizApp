package android.kma.myquizzapp.feature.home.presentation.search

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
}
