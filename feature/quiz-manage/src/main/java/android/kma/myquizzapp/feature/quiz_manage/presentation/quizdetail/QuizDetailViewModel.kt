package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizWithOwnership
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.DeleteQuizUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizDetailUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizWithOwnershipUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel màn Chi tiết quiz. N16 bổ sung:
 * - isOwner: check qua GetQuizWithOwnershipUseCase để hiện nút Chỉnh sửa/Xóa.
 *   Guest xem quiz public → isOwner = false.
 * - Xóa quiz: hard delete qua DeleteQuizUseCase; lỗi thì giữ dialog mở kèm lý do
 *   (pattern QuizDetailPage web); thành công thì bắn QuizDeleted effect.
 */
@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val getQuizWithOwnershipUseCase: GetQuizWithOwnershipUseCase,
    private val deleteQuizUseCase: DeleteQuizUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizDetailUiState())
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<QuizDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(QuizDetailIntent.LoadQuizDetail)
    }

    fun onIntent(intent: QuizDetailIntent) {
        when (intent) {
            is QuizDetailIntent.LoadQuizDetail -> loadQuizDetail()
            is QuizDetailIntent.Retry -> loadQuizDetail()
            is QuizDetailIntent.DeleteQuizClicked -> _uiState.update {
                it.copy(isConfirmingDelete = true, deleteError = null)
            }
            is QuizDetailIntent.DeleteQuizDismissed -> {
                // Đang xóa dở thì không cho đóng dialog — nút đã disable ở UI,
                // đây là lớp chặn thứ hai cho onDismissRequest (bấm ra ngoài dialog).
                if (!_uiState.value.isDeleting) {
                    _uiState.update { it.copy(isConfirmingDelete = false, deleteError = null) }
                }
            }
            is QuizDetailIntent.DeleteQuizConfirmed -> deleteQuiz()
        }
    }

    private fun loadQuizDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = getQuizWithOwnershipUseCase(quizId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            quiz = result.data.quiz,
                            isLoading = false,
                            error = null,
                            isOwner = result.data.isOwner
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun deleteQuiz() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            when (val result = deleteQuizUseCase(quizId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false, isConfirmingDelete = false) }
                    _effect.send(QuizDetailEffect.QuizDeleted)
                }
                is Result.Error -> {
                    // Quiz vẫn còn — giữ dialog mở, hiện lý do lỗi trong dialog.
                    _uiState.update {
                        it.copy(isDeleting = false, deleteError = result.error.toUserMessage())
                    }
                }
            }
        }
    }
}
