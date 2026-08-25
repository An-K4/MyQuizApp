package android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.CreateQuizUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.UploadImageUseCase
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionDraft
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.toNewQuestion
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
    private val createQuizUseCase: CreateQuizUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CreateQuizEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: CreateQuizIntent) {
        when (intent) {
            is CreateQuizIntent.QuizNameChanged -> _uiState.update { it.copy(quizName = intent.value) }
            is CreateQuizIntent.QuizDescriptionChanged -> _uiState.update { it.copy(quizDescription = intent.value) }
            is CreateQuizIntent.QuizLanguageChanged -> _uiState.update { it.copy(quizLanguage = intent.value) }
            is CreateQuizIntent.QuizCategoryChanged -> _uiState.update { it.copy(quizCategory = intent.value) }
            is CreateQuizIntent.IsPublicChanged -> _uiState.update { it.copy(isPublic = intent.value) }

            is CreateQuizIntent.PickCoverImage -> _uiState.update { it.copy(coverImageUri = intent.uri) }
            is CreateQuizIntent.RemoveCoverImage -> _uiState.update { it.copy(coverImageUri = null) }

            is CreateQuizIntent.AddQuestion -> _uiState.update { it.copy(questions = it.questions + QuestionDraft()) }
            is CreateQuizIntent.RemoveQuestion -> _uiState.update { state ->
                // Luôn giữ lại ít nhất 1 câu hỏi để tránh submit quiz rỗng.
                if (state.questions.size <= 1) state
                else state.copy(questions = state.questions.filterNot { it.localId == intent.localId })
            }
            is CreateQuizIntent.PickQuestionImage -> updateQuestion(intent.localId) { it.copy(imageUri = intent.uri) }
            is CreateQuizIntent.RemoveQuestionImage -> updateQuestion(intent.localId) { it.copy(imageUri = null) }
            is CreateQuizIntent.QuestionTypeChanged -> updateQuestion(intent.localId) {
                it.copy(questionType = intent.type, correctIndexes = emptySet(), correctText = "")
            }
            is CreateQuizIntent.QuestionTextChanged -> updateQuestion(intent.localId) { it.copy(questionText = intent.value) }
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
                if (q.options.size <= 2) q
                else q.copy(
                    options = q.options.filterIndexed { i, _ -> i != intent.index },
                    correctIndexes = q.correctIndexes
                        .filterNot { it == intent.index }
                        .map { if (it > intent.index) it - 1 else it }
                        .toSet()
                )
            }
            is CreateQuizIntent.ToggleCorrectIndex -> updateQuestion(intent.localId) { q ->
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
            state.copy(questions = state.questions.map { if (it.localId == localId) transform(it) else it })
        }
    }

    private fun validate(state: CreateQuizUiState): List<String> {
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
            // 1) Ảnh cover (nếu có) — upload thật ngay bây giờ, không phải lúc chọn ảnh.
            val coverImageUrl: String? = state.coverImageUri?.let { uri ->
                when (val result = uploadImageUseCase(uri, folder = "quizzes")) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        failSubmit(result.error.toUserMessage())
                        return@launch
                    }
                }
            }

            // 2) Ảnh từng câu hỏi (nếu có). Bất kỳ ảnh nào lỗi -> dừng toàn bộ,
            // KHÔNG gọi createQuiz (tránh tạo quiz thiếu ảnh).
            val questionImageUrls = mutableMapOf<String, String>()
            for (question in state.questions) {
                val uri = question.imageUri ?: continue
                when (val result = uploadImageUseCase(uri, folder = "questions")) {
                    is Result.Success -> questionImageUrls[question.localId] = result.data
                    is Result.Error -> {
                        failSubmit(result.error.toUserMessage())
                        return@launch
                    }
                }
            }

            // 3) Toàn bộ ảnh (nếu có) đã có publicUrl thật — tạo quiz.
            val newQuiz = NewQuiz(
                quizName = state.quizName.trim(),
                quizDescription = state.quizDescription.trim().ifBlank { null },
                quizLanguage = state.quizLanguage,
                quizImage = coverImageUrl,
                quizCategory = state.quizCategory.trim().ifBlank { null },
                isPublic = state.isPublic,
                questions = state.questions.map { it.toNewQuestion(questionImageUrls[it.localId]) }
            )

            when (val result = createQuizUseCase(newQuiz)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
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
