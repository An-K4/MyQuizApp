package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.feature.quiz_manage.domain.paging.MyQuizzesPagingSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case trả về Flow<PagingData<QuizSummary>> cho danh sách "Quiz của tôi".
 *
 * ViewModel gọi .cachedIn(viewModelScope) lên kết quả trước khi expose ra UI
 * (không cachedIn ở đây vì use case không có CoroutineScope sống theo lifecycle).
 */
class GetMyQuizzesUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    operator fun invoke(params: MyQuizzesParams): Flow<PagingData<QuizSummary>> =
        Pager(
            config = PagingConfig(
                pageSize = params.limit,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { MyQuizzesPagingSource(quizRepository, params) }
        ).flow
}
