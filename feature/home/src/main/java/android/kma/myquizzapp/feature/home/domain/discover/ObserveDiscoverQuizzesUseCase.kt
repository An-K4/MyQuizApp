package android.kma.myquizzapp.feature.home.domain.discover

import android.kma.myquizzapp.core.common.repository.QuizRepository
import androidx.paging.Pager
import androidx.paging.PagingConfig
import javax.inject.Inject

class ObserveDiscoverQuizzesUseCase @Inject constructor(
    private val repository: QuizRepository
) {
    operator fun invoke(query: DiscoverQuery) = Pager(
        config = PagingConfig(pageSize = 12, initialLoadSize = 12, prefetchDistance = 3, enablePlaceholders = false),
        pagingSourceFactory = { DiscoverPagingSource(repository, query) }
    ).flow
}
