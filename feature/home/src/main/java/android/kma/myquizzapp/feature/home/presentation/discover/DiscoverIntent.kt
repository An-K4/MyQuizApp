package android.kma.myquizzapp.feature.home.presentation.discover

import android.kma.myquizzapp.feature.home.domain.discover.DiscoverQuery
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverRequest
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverSort

sealed interface DiscoverIntent {
    data class Initialize(val request: DiscoverRequest) : DiscoverIntent
    data class QueryChanged(val query: DiscoverQuery) : DiscoverIntent
    data class SortChanged(val sort: DiscoverSort) : DiscoverIntent
    data class QuizClicked(val quizId: Long) : DiscoverIntent
}
