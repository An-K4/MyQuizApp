package android.kma.myquizzapp.feature.home.domain.discover

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.paging.PagingSource
import androidx.paging.PagingState

class DiscoverPagingSource(
    private val repository: QuizRepository,
    private val query: DiscoverQuery
) : PagingSource<String, QuizCard>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, QuizCard> {
        val cursor = params.key
        val limit = params.loadSize.coerceIn(1, 24)
        val topic = (query.filter as? DiscoverFilter.Category)?.topic
        val result = if (query.sort == DiscoverSort.TRENDING) {
            repository.getQuizFeed(topic, cursor, limit)
        } else {
            repository.searchPublicQuizzes(topic, query.sort.apiValue, cursor, limit)
        }
        return when (result) {
            is Result.Success -> {
                val page = result.page
                LoadResult.Page(
                    data = result.data,
                    prevKey = null,
                    nextKey = if (page?.hasMore == true) page.nextCursor else null
                )
            }
            is Result.Error -> LoadResult.Error(RuntimeException(result.error.toUserMessage()))
        }
    }

    override fun getRefreshKey(state: PagingState<String, QuizCard>): String? = null
}
