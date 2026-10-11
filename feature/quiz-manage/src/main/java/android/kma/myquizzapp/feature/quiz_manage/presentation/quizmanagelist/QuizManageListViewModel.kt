package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.kma.myquizzapp.core.common.error.isMissingResource
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetMyQuizzesUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.DeleteQuizUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
    private val getMyQuizzesUseCase: GetMyQuizzesUseCase,
    private val deleteQuizUseCase: DeleteQuizUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizManageListUiState())
    val uiState: StateFlow<QuizManageListUiState> = _uiState.asStateFlow()

    private val _effect = Channel<QuizManageListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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

    fun onIntent(intent: QuizManageListIntent) {
        when (intent) {
            is QuizManageListIntent.VisibilityChanged ->
                _uiState.update { it.copy(visibility = intent.visibility) }
            is QuizManageListIntent.SortChanged ->
                _uiState.update { it.copy(sort = intent.sort) }
            is QuizManageListIntent.KeywordChanged ->
                _uiState.update { it.copy(keyword = intent.keyword) }
            // N16: Screen bắn intent này khi ON_RESUME (quay lại từ chi tiết/sửa/xóa).
            is QuizManageListIntent.Refresh -> refresh()
            is QuizManageListIntent.DeleteQuizClicked -> {
                if (!_uiState.value.isDeleting) {
                    _uiState.update { it.copy(deleteTarget = intent.target, deleteError = null) }
                }
            }
            QuizManageListIntent.DeleteQuizDismissed -> {
                if (!_uiState.value.isDeleting) {
                    _uiState.update { it.copy(deleteTarget = null, deleteError = null) }
                }
            }
            QuizManageListIntent.DeleteQuizConfirmed -> deleteQuiz()
        }
    }

    private fun refresh() = _refreshGeneration.update { it + 1 }

    private fun deleteQuiz() {
        val state = _uiState.value
        val target = state.deleteTarget ?: return
        if (state.isDeleting) return
        // Claim synchronously, before launch: a second tap cannot send another DELETE.
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteQuizUseCase(target.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(deleteTarget = null, isDeleting = false, deleteError = null) }
                    refresh() // Preserve filters/sort and replace the Paging generation.
                    _effect.send(QuizManageListEffect.ShowMessage("Đã xóa quiz."))
                }
                is Result.Error -> {
                    val message = result.error.toUserMessage()
                    if (result.error.isMissingResource("QUIZ_NOT_FOUND")) {
                        // It may have been removed elsewhere; do not leave a stale card/dialog.
                        _uiState.update { it.copy(deleteTarget = null, isDeleting = false, deleteError = null) }
                        refresh()
                        _effect.send(QuizManageListEffect.ShowMessage(message))
                    } else {
                        _uiState.update { it.copy(isDeleting = false, deleteError = message) }
                    }
                }
            }
        }
    }

}

