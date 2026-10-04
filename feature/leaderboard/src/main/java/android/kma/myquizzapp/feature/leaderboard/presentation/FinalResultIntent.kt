package android.kma.myquizzapp.feature.leaderboard.presentation

sealed interface FinalResultIntent {
    data object RetryResult : FinalResultIntent
    data object ToggleReview : FinalResultIntent
    data object RetryReview : FinalResultIntent
}
