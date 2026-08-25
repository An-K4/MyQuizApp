package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetMyQuizzesUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuizManageListViewModel @Inject constructor(
    private val getMyQuizzesUseCase: GetMyQuizzesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizManageListUiState())
    val uiState: StateFlow<QuizManageListUiState> = _uiState.asStateFlow()

    /**
     * Flow<PagingData<QuizSummary>> dừng theo filter/sort trong uiState. Đổi
     * visibility/sort/keyword → params mới → flatMapLatest tạo Pager mới (Paging 3
     * tự hủy Pager cũ ), rồi cachedIn(viewModelScope) để sống qua config change.
     */
    /**
     * N16: generation tăng mỗi lần nhận intent Refresh → combine phát cặp
     * (params, generation) mới → flatMapLatest tạo Pager mới → load lại ngay từ
     * mạng. Thay cho LazyPagingItems.refresh() vì refresh() trên flow đã cachedIn
     * có thể no-op (không invalidate PagingSource) — quiz vừa xóa vẫn hiện.
     */
    private val _refreshGeneration = MutableStateFlow(0)

    val quizzes: Flow<PagingData<QuizSummary>> = _uiState
        .map { state ->
            MyQuizzesParams(
                visibility = state.visibility,
                keyword = state.keyword.trim().ifBlank { null },
                sort = state.sort
            )
        }
        .distinctUntilChanged()
        .combine(_refreshGeneration) { params, generation -> params to generation }
        .distinctUntilChanged()
        .flatMapLatest { (params, _) -> getMyQuizzesUseCase(params) }
        .cachedIn(viewModelScope)

    fun handleIntent(intent: QuizManageListIntent) {
        when (intent) {
            is QuizManageListIntent.VisibilityChanged ->
                _uiState.update { it.copy(visibility = intent.visibility) }
            is QuizManageListIntent.SortChanged ->
                _uiState.update { it.copy(sort = intent.sort) }
            is QuizManageListIntent.KeywordChanged ->
                _uiState.update { it.copy(keyword = intent.keyword) }
            // N16: Screen bắn intent này khi ON_RESUME (quay lại từ chi tiết/sửa/xóa).
            is QuizManageListIntent.Refresh -> _refreshGeneration.update { it + 1 }
        }
    }
}
