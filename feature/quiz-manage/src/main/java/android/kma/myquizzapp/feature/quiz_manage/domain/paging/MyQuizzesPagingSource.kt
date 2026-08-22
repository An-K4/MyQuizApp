package android.kma.myquizzapp.feature.quiz_manage.domain.paging

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * PagingSource cho danh sách "Quiz của tôi" (GET /quizzes/me).
 *
 * Key = cursor String? do backend cấp (keyset cursor, xem listing.cursor.ts) —
 * null ở trang đầu. Backend không hỗ trợ "nhảy tới trang N" nên refreshKey
 * luôn null — khi Paging 3 refresh (VD đổi filter/sort), nó sẽ load lại từ
 * trang đầu, đúng với bản chất cursor pagination (không thể “quay về giữa”).
 */
class MyQuizzesPagingSource(
    private val repository: QuizRepository,
    private val baseParams: MyQuizzesParams
) : PagingSource<String, QuizSummary>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, QuizSummary> {
        val cursor = params.key
        val result = repository.getMyQuizzes(
            baseParams.copy(cursor = cursor, limit = params.loadSize.coerceIn(1, 24))
        )
        return when (result) {
            is Result.Success -> {
                val page = result.page
                LoadResult.Page(
                    data = result.data,
                    prevKey = null, // chỉ scroll xuôi, không hỗ trợ load ngược
                    nextKey = if (page?.hasMore == true) page.nextCursor else null
                )
            }
            is Result.Error -> LoadResult.Error(RuntimeException(result.error.toUserMessage()))
        }
    }

    override fun getRefreshKey(state: PagingState<String, QuizSummary>): String? = null
}
