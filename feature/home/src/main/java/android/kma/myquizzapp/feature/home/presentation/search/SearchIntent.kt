package android.kma.myquizzapp.feature.home.presentation.search

/**
 * User intents for Search screen (MVI pattern).
 */
sealed interface SearchIntent {
    /** User typed in search box */
    data class QueryChanged(val query: String) : SearchIntent
    
    /** User submitted search (pressed search button or keyboard action) */
    data object SubmitSearch : SearchIntent
    
    /** Load more search results (pagination) */
    data object LoadMore : SearchIntent
    
    /** User clicked a quiz card */
    data class QuizCardClicked(val quizId: Long) : SearchIntent
    
    /** Clear search query and results */
    data object ClearSearch : SearchIntent
    
    /** Retry after error */
    data object Retry : SearchIntent
}
