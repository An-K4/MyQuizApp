package android.kma.myquizzapp.feature.home.presentation

/**
 * User intents for Home screen (MVI pattern).
 * 
 * Home screen focuses on browsing sections of quizzes via scroll.
 * Search functionality is in a separate SearchScreen.
 */
sealed interface HomeIntent {
    /** Load home content (sections) */
    data object LoadHome : HomeIntent
    
    /** User clicked search icon → navigate to SearchScreen */
    data object NavigateToSearch : HomeIntent
    
    /** User clicked a quiz card */
    data class QuizCardClicked(val quizId: Long) : HomeIntent
    
    /** Switch between "Khám phá" and "Của tôi" tabs */
    data class TabChanged(val tab: HomeTab) : HomeIntent
    
    /** Retry after error */
    data object Retry : HomeIntent
}

/**
 * Tabs on Home screen.
 */
enum class HomeTab {
    /** "Khám phá" - browse public quizzes feed via sections */
    EXPLORE,
    
    /** "Của tôi" - user's created quizzes (requires auth) */
    MY_QUIZZES
}
