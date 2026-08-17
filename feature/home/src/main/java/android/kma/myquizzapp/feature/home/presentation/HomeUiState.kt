package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.QuizCard

/**
 * UI state for Home screen (MVI pattern).
 * 
 * Home screen focuses on browsing sections of quizzes via scroll.
 * Search functionality is in a separate SearchScreen.
 */
data class HomeUiState(
    // Tab state
    val currentTab: HomeTab = HomeTab.EXPLORE,
    
    // Home content (sections for "Khám phá" tab)
    val homeSections: List<HomeSection> = emptyList(),
    val isLoadingHome: Boolean = false,
    val homeError: String? = null,
    
    // "Của tôi" tab state (will implement later in N13-14)
    val myQuizzes: List<QuizCard> = emptyList(),
    val isLoadingMyQuizzes: Boolean = false,
    val myQuizzesError: String? = null
) {
    /**
     * Whether any loading is in progress.
     */
    val isLoading: Boolean
        get() = isLoadingHome || isLoadingMyQuizzes
    
    /**
     * Get current error based on active tab.
     */
    val currentError: String?
        get() = when (currentTab) {
            HomeTab.MY_QUIZZES -> myQuizzesError
            else -> homeError
        }
}
