package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizDraft
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.CreateQuizWithAssetsUseCase
import android.kma.myquizzapp.core.common.validator.QuizEditorValidator
import android.kma.myquizzapp.core.common.validator.QuizEditorErrors
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.toValidationInput
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
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
 * ViewModel cho màn Tạo quiz (N13-14, ảnh quiz/câu hỏi bổ sung ở N15).
 *
 * Upload ảnh (N15): chỉ upload thật lúc bấm "Tạo quiz" (Submit), KHÔNG upload
 * ngay khi người dùng chọn ảnh — tránh tạo object rác trên storage nếu người
 * dùng bỏ ngang. Nếu bất kỳ ảnh nào upload lỗi, dừng toàn bộ và KHÔNG gọi
 * createQuiz (xem submit()).
 */
@HiltViewModel
class CreateQuizViewModel @Inject constructor(
    private val createQuizWithAssetsUseCase: CreateQuizWithAssetsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CreateQuizEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: CreateQuizIntent) {
        if (_uiState.value.isSubmitting) return
        when (intent) {
            is CreateQuizIntent.QuizNameChanged -> _uiState.update { it.copy(quizName = intent.value, isDirty = true) }
            is CreateQuizIntent.QuizDescriptionChanged -> _uiState.update { it.copy(quizDescription = intent.value, isDirty = true) }
            is CreateQuizIntent.QuizLanguageChanged -> _uiState.update { it.copy(quizLanguage = intent.value, isDirty = true) }
            is CreateQuizIntent.QuizCategoryChanged -> _uiState.update { it.copy(quizCategory = intent.value, isDirty = true) }
            is CreateQuizIntent.IsPublicChanged -> _uiState.update { it.copy(isPublic = intent.value, isDirty = true) }

            is CreateQuizIntent.PickCoverImage -> _uiState.update { it.copy(coverImageUri = intent.uri, isDirty = true) }
            is CreateQuizIntent.RemoveCoverImage -> _uiState.update { it.copy(coverImageUri = null, isDirty = true) }

            is CreateQuizIntent.AddQuestion -> _uiState.update { it.copy(questions = it.questions + QuestionDraft(), isDirty = true) }
            is CreateQuizIntent.RemoveQuestion -> _uiState.update { state ->
                // Luôn giữ lại ít nhất 1 câu hỏi để tránh submit quiz rỗng.
                if (state.questions.size <= 1) state
                else state.copy(questions = state.questions.filterNot { it.localId == intent.localId }, isDirty = true)
            }
            is CreateQuizIntent.PickQuestionImage -> updateQuestion(intent.localId) { it.copy(imageUri = intent.uri) }
            is CreateQuizIntent.RemoveQuestionImage -> updateQuestion(intent.localId) { it.copy(imageUri = null) }
            is CreateQuizIntent.QuestionTypeChanged -> updateQuestion(intent.localId) {
                if (it.questionType == intent.type) it else
                    it.copy(questionType = intent.type, correctIndexes = emptySet(), correctText = "")
            }
            is CreateQuizIntent.QuestionTextChanged -> updateQuestion(intent.localId) { it.copy(questionText = intent.value) }
            is CreateQuizIntent.TimePresetSelected -> updateQuestion(intent.localId) {
                if (intent.seconds !in listOf(10, 20, 30, 60)) it else
                    it.copy(timeLimit = intent.seconds.toString(), usesCustomTime = false)
            }
            is CreateQuizIntent.CustomTimeSelected -> updateQuestion(intent.localId) { it.copy(usesCustomTime = true) }
            is CreateQuizIntent.TimeLimitChanged -> updateQuestion(intent.localId) { it.copy(timeLimit = intent.value) }
            is CreateQuizIntent.HintChanged -> updateQuestion(intent.localId) { it.copy(questionHint = intent.value) }
            is CreateQuizIntent.ExplanationChanged -> updateQuestion(intent.localId) { it.copy(explanation = intent.value) }

            is CreateQuizIntent.OptionChanged -> updateQuestion(intent.localId) { q ->
                q.copy(options = q.options.mapIndexed { i, v -> if (i == intent.index) intent.value else v })
            }
            is CreateQuizIntent.AddOption -> updateQuestion(intent.localId) { q ->
                if (q.options.size >= 4) q else q.copy(options = q.options + "")
            }
            is CreateQuizIntent.RemoveOption -> updateQuestion(intent.localId) { q ->
                if (q.options.size <= 2 || intent.index !in q.options.indices) q
                else q.copy(
                    options = q.options.filterIndexed { i, _ -> i != intent.index },
                    correctIndexes = q.correctIndexes
                        .filterNot { it == intent.index }
                        .map { if (it > intent.index) it - 1 else it }
                        .toSet()
                )
            }
            is CreateQuizIntent.ToggleCorrectIndex -> updateQuestion(intent.localId) { q ->
                if (!q.isChoiceType || intent.index !in q.options.indices) return@updateQuestion q
                val newIndexes = when {
                    q.questionType == QuestionType.MULTIPLE_CHOICE -> setOf(intent.index)
                    q.correctIndexes.contains(intent.index) -> q.correctIndexes - intent.index
                    else -> q.correctIndexes + intent.index
                }
                q.copy(correctIndexes = newIndexes)
            }
            is CreateQuizIntent.CorrectTextChanged -> updateQuestion(intent.localId) { it.copy(correctText = intent.value) }

            is CreateQuizIntent.Submit -> submit()
            is CreateQuizIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun updateQuestion(localId: String, transform: (QuestionDraft) -> QuestionDraft) {
        _uiState.update { state ->
            state.copy(questions = state.questions.map { if (it.localId == localId) transform(it) else it }, isDirty = true)
        }
    }

    private fun validate(state: CreateQuizUiState): QuizEditorErrors = QuizEditorValidator.validate(
        name = state.quizName, description = state.quizDescription,
        language = state.quizLanguage, category = state.quizCategory,
        questions = state.questions.map { it.toValidationInput() },
    )

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val errors = validate(state)
        if (errors.hasErrors) {
            _uiState.update { it.copy(validationErrors = errors, validationAttempt = it.validationAttempt + 1, errorMessage = null) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, validationErrors = QuizEditorErrors(), errorMessage = null) }

        viewModelScope.launch {
            // Build QuizDraft từ UI state — validation đã xong ở trên
            val draft = QuizDraft(
                quizName = state.quizName.trim(),
                quizDescription = state.quizDescription.trim().ifBlank { null },
                quizLanguage = state.quizLanguage.trim(),
                quizCategory = state.quizCategory.trim().ifBlank { null },
                isPublic = state.isPublic,
                coverImageUri = state.coverImageUri,
                questions = state.questions
            )

            // Delegate orchestration logic (upload + create) to UseCase
            when (val result = createQuizWithAssetsUseCase(draft)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isDirty = false) }
                    _effect.send(CreateQuizEffect.QuizCreated(result.data.id))
                }
                is Result.Error -> failSubmit(result.error.toUserMessage())
            }
        }
    }

    private fun failSubmit(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }
}

