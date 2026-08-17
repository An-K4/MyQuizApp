package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.error.toUserMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.home.domain.usecase.GetHomeContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen (MVI pattern).
 * 
 * Manages:
 * - Home content (sections of quiz cards) for browsing via scroll
 * - Tab switching between "Khám phá" and "Của tôi"
 * 
 * Search functionality is in a separate SearchViewModel/SearchScreen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeContentUseCase: GetHomeContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Load home content on init
        loadHomeContent()
    }

    /**
     * Handle user intents.
     */
    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadHome -> loadHomeContent()
            is HomeIntent.NavigateToSearch -> navigateToSearch()
            is HomeIntent.QuizCardClicked -> navigateToQuizDetail(intent.quizId)
            is HomeIntent.TabChanged -> switchTab(intent.tab)
            is HomeIntent.Retry -> retry()
        }
    }

    /**
     * Load home sections from backend.
     */
    private fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHome = true, homeError = null) }

            when (val result = getHomeContentUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            homeSections = result.data,
                            isLoadingHome = false,
                            homeError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHome = false,
                            homeError = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Switch between "Khám phá" and "Của tôi" tabs.
     */
    private fun switchTab(tab: HomeTab) {
        _uiState.update { it.copy(currentTab = tab) }
        
        // TODO: Load "Của tôi" data when switching to MY_QUIZZES tab (N13-14)
        if (tab == HomeTab.MY_QUIZZES) {
            // Will implement getMyQuizzes() in N13-14 (quiz-manage feature)
        }
    }

    /**
     * Navigate to SearchScreen.
     * TODO: Implement navigation in Phase 4
     */
    private fun navigateToSearch() {
        // Will implement in Phase 4 with Navigation
    }

    /**
     * Navigate to quiz detail screen.
     * TODO: Implement navigation in Phase 4
     */
    private fun navigateToQuizDetail(quizId: Long) {
        // Will implement in Phase 4 with Navigation
    }

    /**
     * Retry failed operation.
     */
    private fun retry() {
        when {
            _uiState.value.homeError != null -> loadHomeContent()
            _uiState.value.myQuizzesError != null -> {
                // TODO: Retry getMyQuizzes() in N13-14
            }
        }
    }
}