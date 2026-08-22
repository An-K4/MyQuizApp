package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizDetailUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizDetailViewModel @Inject constructor(
    private val getQuizDetailUseCase: GetQuizDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizDetailUiState())
    val uiState: StateFlow<QuizDetailUiState> = _uiState.asStateFlow()

    init {
        handleIntent(QuizDetailIntent.LoadQuizDetail)
    }

    fun handleIntent(intent: QuizDetailIntent) {
        when (intent) {
            is QuizDetailIntent.LoadQuizDetail -> loadQuizDetail()
            is QuizDetailIntent.Retry -> loadQuizDetail()
        }
    }

    private fun loadQuizDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = getQuizDetailUseCase(quizId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        quiz = result.data,
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.toUserMessage()
                    )
                }
            }
        }
    }
}
