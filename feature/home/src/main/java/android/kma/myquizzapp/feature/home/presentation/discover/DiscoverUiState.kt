package android.kma.myquizzapp.feature.home.presentation.discover

import android.kma.myquizzapp.feature.home.domain.discover.DiscoverFilter
import android.kma.myquizzapp.feature.home.domain.discover.DiscoverSort

data class DiscoverUiState(
    val initialized: Boolean = false,
    val filters: List<DiscoverFilter> = listOf(DiscoverFilter.All),
    val selectedFilter: DiscoverFilter = DiscoverFilter.All,
    val sort: DiscoverSort = DiscoverSort.NEWEST
)
