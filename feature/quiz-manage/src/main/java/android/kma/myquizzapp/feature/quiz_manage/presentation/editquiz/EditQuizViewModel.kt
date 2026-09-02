package android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizDraft
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizDetailUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.UpdateQuizWithAssetsUseCase
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject

/**
 * ViewModel cho màn Sửa quiz (N16) — pattern EditQuizPage.vue của frontend web:
 * load quiz detail → pre-fill editor (kể cả correct_answer, chỉ owner mới vào được
 * màn này) → PATCH metadata + THAY THẾ toàn bộ câu hỏi (backend replaceQuizQuestions,
 * xem quiz.service.ts — không có patch từng câu).
 *
 * Upload ảnh: chỉ upload ảnh MỚI CHỌN lúc bấm "Lưu thay đổi" (giống quyết định N15).
 * Ảnh cũ (existingCoverUrl/existingImageUrl) được gửi lại nguyên URL khi giữ nguyên.
 */
@HiltViewModel
class EditQuizViewModel @Inject constructor(
    private val getQuizDetailUseCase: GetQuizDetailUseCase,
    private val updateQuizWithAssetsUseCase: UpdateQuizWithAssetsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(EditQuizUiState())
    val uiState: StateFlow<EditQuizUiState> = _uiState.asStateFlow()

    private val _effect = Channel<EditQuizEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadQuiz()
    }

    fun onIntent(intent: EditQuizIntent) {
        when (intent) {
            is EditQuizIntent.LoadQuiz, is EditQuizIntent.Retry -> loadQuiz()

            // Mọi intent thay đổi nội dung đều đánh dấu isDirty = true — dùng cho
            // dialog "Bỏ thay đổi chưa lưu?" khi thoát (frontend: emit('dirty', true)).
            is EditQuizIntent.QuizNameChanged -> _uiState.update { it.copy(quizName = intent.value, isDirty = true) }
            is EditQuizIntent.QuizDescriptionChanged -> _uiState.update { it.copy(quizDescription = intent.value, isDirty = true) }
            is EditQuizIntent.QuizLanguageChanged -> _uiState.update { it.copy(quizLanguage = intent.value, isDirty = true) }
            is EditQuizIntent.QuizCategoryChanged -> _uiState.update { it.copy(quizCategory = intent.value, isDirty = true) }
            is EditQuizIntent.IsPublicChanged -> _uiState.update { it.copy(isPublic = intent.value, isDirty = true) }

            is EditQuizIntent.PickCoverImage -> _uiState.update {
                it.copy(coverImageUri = intent.uri, coverRemoved = false, isDirty = true)
            }
            is EditQuizIntent.RemoveCoverImage -> _uiState.update {
                it.copy(coverImageUri = null, coverRemoved = true, isDirty = true)
            }

            is EditQuizIntent.AddQuestion -> _uiState.update {
                it.copy(questions = it.questions + QuestionDraft(), isDirty = true)
            }
            is EditQuizIntent.RemoveQuestion -> _uiState.update { state ->
                // Luôn giữ lại ít nhất 1 câu — backend từ chối questions: [] (QUIZ_NO_QUESTIONS).
                if (state.questions.size <= 1) state
                else state.copy(
                    questions = state.questions.filterNot { it.localId == intent.localId },
                    isDirty = true
                )
            }
            is EditQuizIntent.PickQuestionImage -> updateQuestion(intent.localId) { it.copy(imageUri = intent.uri) }
            // Xóa ảnh câu hỏi xóa được thật: questions bị replace toàn bộ nên câu mới
            // không có question_image (khác cover quiz — metadata không clear được).
            is EditQuizIntent.RemoveQuestionImage -> updateQuestion(intent.localId) {
                it.copy(imageUri = null, existingImageUrl = null)
            }
            is EditQuizIntent.QuestionTypeChanged -> updateQuestion(intent.localId) {
                // Đổi loại câu reset trạng thái đáp án để dữ liệu thừa không lọt vào payload.
                it.copy(questionType = intent.type, correctIndexes = emptySet(), correctText = "")
            }
            is EditQuizIntent.QuestionTextChanged -> updateQuestion(intent.localId) { it.copy(questionText = intent.value) }
            is EditQuizIntent.TimeLimitChanged -> updateQuestion(intent.localId) { it.copy(timeLimit = intent.value) }
            is EditQuizIntent.HintChanged -> updateQuestion(intent.localId) { it.copy(questionHint = intent.value) }
            is EditQuizIntent.ExplanationChanged -> updateQuestion(intent.localId) { it.copy(explanation = intent.value) }

            is EditQuizIntent.OptionChanged -> updateQuestion(intent.localId) { q ->
                q.copy(options = q.options.mapIndexed { i, v -> if (i == intent.index) intent.value else v })
            }
            is EditQuizIntent.AddOption -> updateQuestion(intent.localId) { q ->
                if (q.options.size >= 4) q else q.copy(options = q.options + "")
            }
            is EditQuizIntent.RemoveOption -> updateQuestion(intent.localId) { q ->
                if (q.options.size <= 2) q
                else q.copy(
                    options = q.options.filterIndexed { i, _ -> i != intent.index },
                    // Xóa 1 lựa chọn phải dời index đáp án đúng theo (giống CreateQuizViewModel).
                    correctIndexes = q.correctIndexes
                        .filterNot { it == intent.index }
                        .map { if (it > intent.index) it - 1 else it }
                        .toSet()
                )
            }
            is EditQuizIntent.ToggleCorrectIndex -> updateQuestion(intent.localId) { q ->
                val newIndexes = when {
                    q.questionType == QuestionType.MULTIPLE_CHOICE -> setOf(intent.index)
                    q.correctIndexes.contains(intent.index) -> q.correctIndexes - intent.index
                    else -> q.correctIndexes + intent.index
                }
                q.copy(correctIndexes = newIndexes)
            }
            is EditQuizIntent.CorrectTextChanged -> updateQuestion(intent.localId) { it.copy(correctText = intent.value) }

            is EditQuizIntent.Submit -> submit()
            is EditQuizIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun updateQuestion(localId: String, transform: (QuestionDraft) -> QuestionDraft) {
        _uiState.update { state ->
            state.copy(
                questions = state.questions.map { if (it.localId == localId) transform(it) else it },
                isDirty = true
            )
        }
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }

            when (val result = getQuizDetailUseCase(quizId)) {
                is Result.Success -> _uiState.value = result.data.toEditUiState()
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, loadError = result.error.toUserMessage())
                }
            }
        }
    }

    private fun Quiz.toEditUiState(): EditQuizUiState = EditQuizUiState(
        isLoading = false,
        quizName = quizName,
        quizDescription = quizDescription.orEmpty(),
        quizLanguage = quizLanguage,
        quizCategory = quizCategory.orEmpty(),
        isPublic = isPublic,
        existingCoverUrl = quizImage,
        questions = questions.map { it.toDraft() },
        isDirty = false
    )

    /** Quiz detail (GET) → draft cho editor. correct_answer trên wire là union: number[] | string. */
    private fun Question.toDraft(): QuestionDraft {
        var correctIndexes = emptySet<Int>()
        var correctText = ""
        when (val ca = correctAnswer) {
            is JsonArray -> correctIndexes =
                ca.mapNotNull { (it as? JsonPrimitive)?.intOrNull }.toSet()
            is JsonPrimitive -> correctText = ca.contentOrNull.orEmpty()
            // null/không parse được → editor bắt chọn lại đáp án khi validate.
            else -> Unit
        }
        return QuestionDraft(
            questionType = questionType,
            questionText = questionText,
            timeLimit = timeLimit.toString(),
            questionHint = questionHint.orEmpty(),
            explanation = explanation.orEmpty(),
            options = answerOptions?.map { it.optionText } ?: listOf("", ""),
            correctIndexes = correctIndexes,
            correctText = correctText,
            existingImageUrl = questionImage
        )
    }

    private fun validate(state: EditQuizUiState): List<String> {
        // Cùng bộ rule với CreateQuizViewModel — khớp createQuestionSchema (quiz.schema.ts).
        val errors = mutableListOf<String>()
        if (state.quizName.trim().length < 3) {
            errors += "Tên quiz phải có ít nhất 3 ký tự."
        }
        if (state.questions.isEmpty()) {
            errors += "Quiz phải có ít nhất 1 câu hỏi."
        }
        state.questions.forEachIndexed { index, q ->
            val position = index + 1
            if (q.questionText.trim().isEmpty()) {
                errors += "Câu $position: thiếu nội dung câu hỏi."
            }
            // Để trống = mặc định 30s lúc submit; nhập thì phải 5-600s
            // (TIME_LIMIT_MIN/MAX, quiz.schema.ts).
            val timeText = q.timeLimit.trim()
            if (timeText.isNotEmpty()) {
                val seconds = timeText.toIntOrNull()
                if (seconds == null || seconds < 5 || seconds > 600) {
                    errors += "Câu $position: thời gian phải từ 5 đến 600 giây."
                }
            }
            if (q.isChoiceType) {
                val nonBlankOptions = q.options.count { it.isNotBlank() }
                if (nonBlankOptions < 2) {
                    errors += "Câu $position: cần ít nhất 2 lựa chọn."
                }
                if (q.correctIndexes.isEmpty()) {
                    errors += "Câu $position: chọn ít nhất 1 đáp án đúng."
                }
            } else {
                if (q.correctText.trim().isEmpty()) {
                    errors += "Câu $position: thiếu đáp án đúng."
                }
            }
        }
        return errors
    }

    private fun submit() {
        val state = _uiState.value
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, fieldErrors = emptyList(), errorMessage = null) }

        viewModelScope.launch {
            // Build QuizDraft từ UI state — validation đã xong ở trên
            val draft = QuizDraft(
                quizName = state.quizName,
                quizDescription = state.quizDescription.trim().ifBlank { null },
                quizLanguage = state.quizLanguage,
                quizCategory = state.quizCategory.trim().ifBlank { null },
                isPublic = state.isPublic,
                coverImageUri = state.coverImageUri,
                existingCoverUrl = state.existingCoverUrl,
                questions = state.questions
            )

            // Delegate orchestration logic (upload + PATCH) to UseCase
            when (val result = updateQuizWithAssetsUseCase(quizId, draft)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isDirty = false) }
                    _effect.send(EditQuizEffect.QuizUpdated(result.data.id))
                }
                is Result.Error -> failSubmit(result.error.toUserMessage())
            }
        }
    }

    private fun failSubmit(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }
}
