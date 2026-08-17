package android.kma.myquizzapp.feature.home.presentation.search

import android.kma.myquizzapp.core.common.error.toUserMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.home.domain.usecase.SearchQuizzesUseCase
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search screen (MVI pattern).
 * 
 * Manages:
 * - Search query input
 * - Search results with pagination
 * - Error handling and retry
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchQuizzesUseCase: SearchQuizzesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * Handle user intents.
     */
    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> updateQuery(intent.query)
            is SearchIntent.SubmitSearch -> submitSearch()
            is SearchIntent.LoadMore -> loadMore()
            is SearchIntent.QuizCardClicked -> navigateToQuizDetail(intent.quizId)
            is SearchIntent.ClearSearch -> clearSearch()
            is SearchIntent.Retry -> retry()
        }
    }

    /**
     * Update search query as user types.
     */
    private fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Submit search query (user pressed search button or keyboard action).
     */
    private fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            // Blank query → clear results
            clearSearch()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    error = null,
                    currentPage = 1,
                    results = emptyList(),
                    hasMore = true
                )
            }

            when (val result = searchQuizzesUseCase(query, page = 1)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            results = result.data,
                            isSearching = false,
                            error = null,
                            // Has more if we got full page (20 items)
                            hasMore = result.data.size >= 20
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Load next page of search results (infinite scroll).
     */
    private fun loadMore() {
        val currentState = _uiState.value
        
        // Guard: Don't load if already loading or no more results
        if (!currentState.hasMore || currentState.isLoadingMore) {
            return
        }

        val query = currentState.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPage = currentState.currentPage + 1
            when (val result = searchQuizzesUseCase(query, page = nextPage)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            // Append new results to existing list
                            results = it.results + result.data,
                            currentPage = nextPage,
                            isLoadingMore = false,
                            // Has more if we got full page (20 items)
                            hasMore = result.data.size >= 20
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Clear search query and results.
     */
    private fun clearSearch() {
        _uiState.update {
            SearchUiState() // Reset to initial state
        }
    }

    /**
     * Navigate to quiz detail screen.
     * TODO: Implement navigation in Phase 4
     */
    private fun navigateToQuizDetail(quizId: Long) {
        // Will implement in Phase 4 with Navigation
    }

    /**
     * Retry failed search.
     */
    private fun retry() {
        if (_uiState.value.error != null) {
            submitSearch()
        }
    }
}
