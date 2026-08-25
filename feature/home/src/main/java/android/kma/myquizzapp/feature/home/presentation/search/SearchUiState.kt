package android.kma.myquizzapp.feature.home.presentation.search

import android.kma.myquizzapp.core.common.model.QuizCard

/**
 * UI state for Search screen (MVI pattern).
 */
data class SearchUiState(
    // Search query
    val query: String = "",
    
    // Search results
    val results: List<QuizCard> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    
    // Pagination — N16.5: cursor thật thay vì đếm page (backend không đọc `page`;
    // bản cũ load more chỉ lặp lại trang 1). nextCursor null = không còn trang kế.
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
) {
    /**
     * Whether any loading is in progress.
     */
    val isLoading: Boolean
        get() = isSearching || isLoadingMore
    
    /**
     * Whether we have search results to display.
     */
    val hasResults: Boolean
        get() = results.isNotEmpty()
    
    /**
     * Whether search query is not blank.
     */
    val hasQuery: Boolean
        get() = query.isNotBlank()
}
