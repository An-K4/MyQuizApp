package android.kma.myquizzapp.feature.home.presentation.discover

import android.kma.myquizzapp.feature.home.domain.discover.DiscoverQuery
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverRequest
import android.kma.myquizzapp.feature.home.domain.discover.ObserveDiscoverQuizzesUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    observeDiscoverQuizzes: ObserveDiscoverQuizzesUseCase
) : ViewModel() {
    private val query = MutableStateFlow<DiscoverQuery?>(null)
    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState = _uiState
    private val _effect = Channel<DiscoverEffect>()
    val effect = _effect.receiveAsFlow()
    val quizzes = query.filterNotNull()
        .flatMapLatest(observeDiscoverQuizzes::invoke)
        .cachedIn(viewModelScope)

    fun onIntent(intent: DiscoverIntent) {
        when (intent) {
            is DiscoverIntent.Initialize -> initialize(intent.request)
            is DiscoverIntent.QueryChanged -> {
                _uiState.update {
                    it.copy(selectedFilter = intent.query.filter, sort = intent.query.sort)
                }
                query.value = intent.query
            }
            is DiscoverIntent.SortChanged -> {
                _uiState.update { it.copy(sort = intent.sort) }
                updateQuery()
            }
            is DiscoverIntent.QuizClicked -> viewModelScope.launch {
                _effect.send(DiscoverEffect.NavigateToQuizDetail(intent.quizId))
            }
        }
    }

    private fun initialize(request: DiscoverRequest) {
        if (_uiState.value.initialized) return
        _uiState.value = DiscoverUiState(
            initialized = true,
            filters = request.filters,
            selectedFilter = request.initialQuery.filter,
            sort = request.initialQuery.sort
        )
        query.value = request.initialQuery
    }

    private fun updateQuery() {
        val state = _uiState.value
        query.value = DiscoverQuery(filter = state.selectedFilter, sort = state.sort)
    }
}
