package android.kma.myquizzapp.feature.home.presentation

/**
 * One-time effects for Home screen (MVI pattern).
 * 
 * Effects represent navigation events and side-effects that should happen once,
 * not persist in state. The Screen collects these effects and handles them.
 */
sealed interface HomeEffect {
    /**
     * Navigate to search screen.
     */
    data object NavigateToSearch : HomeEffect

    /**
     * Navigate to quiz detail screen.
     * 
     * @param quizId ID of the quiz to view
     */
    data class NavigateToQuizDetail(val quizId: Long) : HomeEffect
}
