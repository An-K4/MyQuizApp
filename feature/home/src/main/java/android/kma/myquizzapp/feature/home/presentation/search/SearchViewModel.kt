package android.kma.myquizzapp.feature.home.presentation.search

import android.kma.myquizzapp.core.common.error.toUserMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.feature.home.domain.usecase.SearchQuizzesUseCase
import android.kma.myquizzapp.core.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    // Hủy request cũ khi query thay đổi để response của từ khóa trước không ghi
    // đè state của từ khóa hiện tại.
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    /**
     * Handle user intents.
     */
    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> updateQuery(intent.query)
            is SearchIntent.SubmitSearch -> submitSearch()
            is SearchIntent.LoadMore -> loadMore()
            is SearchIntent.ClearSearch -> clearSearch()
            is SearchIntent.Retry -> retry()
        }
    }

    /**
     * Update search query as user types.
     *
     * N16.5 hotfix 2: KHÔNG real-time search — chỉ chạy khi user bấm nút kính lúp
     * hoặc phím Search (SubmitSearch). Xóa trắng ô thì clear kết quả ngay.
     */
    private fun updateQuery(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        cancelPendingRequests()
        // Manual search: input mới chưa có response tương ứng, nên bỏ kết quả cũ
        // và không đánh dấu empty-result cho đến lần SubmitSearch tiếp theo.
        _uiState.value = SearchUiState(query = query)
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

        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    error = null,
                    hasCompletedSearch = false,
                    submittedQuery = query,
                    // N16.5: query mới → reset cursor (cursor gắn fingerprint của filter;
                    // dùng cursor cũ với keyword khác → QUIZ_CURSOR_INVALID 400).
                    nextCursor = null,
                    results = emptyList(),
                    hasMore = true
                )
            }

            when (val result = searchQuizzesUseCase(query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            results = result.data,
                            isSearching = false,
                            error = null,
                            hasCompletedSearch = true,
                            submittedQuery = query,
                            // N16.5: lấy từ meta.pagination thật thay vì đoán size >= 20
                            nextCursor = result.page?.nextCursor,
                            hasMore = result.page?.hasMore ?: false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = result.error.toUserMessage(),
                            hasCompletedSearch = false
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

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            // N16.5: cursor của trang trước (nextCursor null ⇒ hasMore=false ⇒ không vào đây)
            when (val result = searchQuizzesUseCase(query, cursor = currentState.nextCursor)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            // Append new results to existing list
                            results = it.results + result.data,
                            nextCursor = result.page?.nextCursor,
                            isLoadingMore = false,
                            hasMore = result.page?.hasMore ?: false
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
        cancelPendingRequests()
        _uiState.value = SearchUiState()
    }

    private fun cancelPendingRequests() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = null
        loadMoreJob = null
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
