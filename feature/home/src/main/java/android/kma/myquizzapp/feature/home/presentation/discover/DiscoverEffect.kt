package android.kma.myquizzapp.feature.home.presentation.discover

sealed interface DiscoverEffect {
    data class NavigateToQuizDetail(val quizId: Long) : DiscoverEffect
}
