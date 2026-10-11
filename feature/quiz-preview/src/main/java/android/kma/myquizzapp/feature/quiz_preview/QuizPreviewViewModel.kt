package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.feature.quiz_preview.domain.usecase.LoadQuizPreviewUseCase

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class QuizPreviewViewModel internal constructor(
    private val loadQuizPreview: LoadQuizPreviewUseCase,
    savedStateHandle: SavedStateHandle,
    private val monotonicMillis: () -> Long,
) : ViewModel() {
    @Inject
    constructor(loadQuizPreview: LoadQuizPreviewUseCase, savedStateHandle: SavedStateHandle) :
        this(loadQuizPreview, savedStateHandle, { System.nanoTime() / 1_000_000L })

    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizPreviewUiState())
    val uiState: StateFlow<QuizPreviewUiState> = _uiState.asStateFlow()

    private val _effect = Channel<QuizPreviewEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadRequested = false
    private var phaseJob: Job? = null
    private var elapsedJob: Job? = null
    private var questionOpenedAtMs: Long = 0L
    private var runStartedAtMs: Long = 0L

    init {
        loadQuiz()
    }

    fun onIntent(intent: QuizPreviewIntent) {
        when (intent) {
            QuizPreviewIntent.Retry -> loadQuiz()
            is QuizPreviewIntent.SelectOption -> selectSingle(intent.optionId)
            is QuizPreviewIntent.ToggleOption -> toggleOption(intent.optionId)
            is QuizPreviewIntent.ChangeText -> {
                val state = _uiState.value
                if (state.phase == PreviewPhase.QUESTION &&
                    state.currentQuestion?.questionType in setOf(QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER)
                ) _uiState.update { it.copy(textAnswer = intent.value) }
            }
            QuizPreviewIntent.Submit -> submit()
            QuizPreviewIntent.Skip -> grade(answered = false)
            QuizPreviewIntent.Next -> advance()
            QuizPreviewIntent.Restart -> startRun()
            QuizPreviewIntent.Exit -> viewModelScope.launch { _effect.send(QuizPreviewEffect.Exit) }
            QuizPreviewIntent.EditQuiz -> editQuiz()
        }
    }

    private fun loadQuiz() {
        if (loadRequested) return
        loadRequested = true // Claim before launch, including rapid Retry taps.
        phaseJob?.cancel()
        elapsedJob?.cancel()
        _uiState.value = QuizPreviewUiState(isLoading = true)
        viewModelScope.launch {
            try {
                when (val result = loadQuizPreview(quizId)) {
                    is Result.Success -> {
                        _uiState.value = QuizPreviewUiState(
                            isLoading = false,
                            quiz = result.data.quiz,
                            isOwner = result.data.isOwner,
                        )
                        if (result.data.quiz.questions.isNotEmpty()) startRun()
                    }
                    is Result.Error -> _uiState.value = QuizPreviewUiState(
                        isLoading = false, errorMessage = result.error.toUserMessage(),
                    )
                }
            } finally {
                loadRequested = false
            }
        }
    }

    private fun startRun() {
        val quiz = _uiState.value.quiz ?: return
        if (quiz.questions.isEmpty()) return
        phaseJob?.cancel()
        elapsedJob?.cancel()
        runStartedAtMs = monotonicMillis()
        _uiState.update {
            it.copy(
                phase = PreviewPhase.COUNTDOWN,
                currentIndex = 0,
                countdownSeconds = COUNTDOWN_SECONDS,
                remainingSeconds = 0,
                remainingTimeMillis = null,
                isLate = false,
                selectedOptionIds = emptySet(),
                textAnswer = "",
                currentResult = null,
                results = emptyList(),
                totalScore = 0,
                elapsedSeconds = 0
            )
        }
        elapsedJob = viewModelScope.launch {
            while (isActive && _uiState.value.phase != PreviewPhase.FINISHED) {
                delay(1_000)
                val elapsed = ((monotonicMillis() - runStartedAtMs) / 1_000L).toInt()
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
        phaseJob = viewModelScope.launch {
            for (second in COUNTDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(countdownSeconds = second) }
                delay(1_000)
            }
            openQuestion(0)
        }
    }

    private fun openQuestion(index: Int) {
        val question = _uiState.value.quiz?.questions?.getOrNull(index) ?: return finish()
        phaseJob?.cancel()
        val limit = question.timeLimit.coerceAtLeast(0)
        questionOpenedAtMs = monotonicMillis()
        _uiState.update {
            it.copy(
                phase = PreviewPhase.QUESTION,
                currentIndex = index,
                remainingSeconds = limit,
                remainingTimeMillis = limit * 1_000L,
                isLate = false,
                selectedOptionIds = emptySet(),
                textAnswer = "",
                currentResult = null
            )
        }
        if (limit == 0) return // Truly untimed: no fallback timer or late/speed penalty.
        phaseJob = viewModelScope.launch {
            val deadline = questionOpenedAtMs + limit * 1_000L
            while (isActive) {
                val leftMillis = (deadline - monotonicMillis()).coerceAtLeast(0L)
                val left = ((leftMillis + 999L) / 1_000L).toInt()
                _uiState.update { it.copy(remainingSeconds = left, remainingTimeMillis = leftMillis) }
                if (left == 0) {
                    _uiState.update { it.copy(isLate = true) }
                    break
                }
                delay(200)
            }
        }
    }

    private fun selectSingle(optionId: Long) {
        val state = _uiState.value
        if (state.phase != PreviewPhase.QUESTION ||
            state.currentQuestion?.questionType != QuestionType.MULTIPLE_CHOICE
        ) return
        if (state.currentQuestion?.answerOptions.orEmpty().none { it.id == optionId }) return
        _uiState.update { it.copy(selectedOptionIds = setOf(optionId)) }
        grade(answered = true)
    }

    private fun toggleOption(optionId: Long) {
        val state = _uiState.value
        if (state.phase != PreviewPhase.QUESTION ||
            state.currentQuestion?.questionType != QuestionType.MULTIPLE_SELECT
        ) return
        if (state.currentQuestion?.answerOptions.orEmpty().none { it.id == optionId }) return
        _uiState.update {
            val next = it.selectedOptionIds.toMutableSet().apply {
                if (!add(optionId)) remove(optionId)
            }
            it.copy(selectedOptionIds = next)
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        grade(answered = true)
    }

    private fun grade(answered: Boolean) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.phase != PreviewPhase.QUESTION) return
        phaseJob?.cancel()
        val timeTaken = (monotonicMillis() - questionOpenedAtMs).coerceAtLeast(0L) / 1_000.0
        val late = question.timeLimit > 0 && timeTaken >= question.timeLimit
        val evaluation = evaluatePreviewAnswer(
            question = question,
            selectedOptionIds = if (answered) state.selectedOptionIds else emptySet(),
            textAnswer = if (answered) state.textAnswer else "",
            answered = answered
        )
        val earned = computePreviewScore(
            isCorrect = evaluation.isCorrect == true,
            timeTakenSeconds = timeTaken,
            timeLimitSeconds = question.timeLimit.coerceAtLeast(0),
            isLate = late
        )
        val result = PreviewQuestionResult(
            question = question,
            submittedAnswer = evaluation.submittedLabel,
            correctAnswer = evaluation.correctLabel,
            isCorrect = evaluation.isCorrect,
            isAnswered = answered,
            isLate = late,
            scoreEarned = earned,
            timeTakenSeconds = timeTaken
        )
        _uiState.update {
            it.copy(
                phase = PreviewPhase.FEEDBACK,
                isLate = late,
                remainingSeconds = if (late) 0 else it.remainingSeconds,
                remainingTimeMillis = if (late) 0L else it.remainingTimeMillis,
                currentResult = result,
                results = it.results + result,
                totalScore = it.totalScore + earned
            )
        }

    }

    private fun advance() {
        val state = _uiState.value
        if (state.phase != PreviewPhase.FEEDBACK) return
        phaseJob?.cancel()
        val next = state.currentIndex + 1
        if (next >= state.quiz.orEmptyQuestionCount()) finish() else openQuestion(next)
    }

    private fun finish() {
        phaseJob?.cancel()
        elapsedJob?.cancel()
        val elapsed = if (runStartedAtMs == 0L) 0
        else ((monotonicMillis() - runStartedAtMs) / 1_000L).toInt()
        _uiState.update { it.copy(phase = PreviewPhase.FINISHED, elapsedSeconds = elapsed) }
    }

    private fun editQuiz() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        if (!state.isOwner || !loadQuizPreview.isOwner(quiz)) return
        viewModelScope.launch { _effect.send(QuizPreviewEffect.EditQuiz(quizId)) }
    }

    private fun android.kma.myquizzapp.core.common.model.Quiz?.orEmptyQuestionCount(): Int =
        this?.questions?.size ?: 0

    override fun onCleared() {
        phaseJob?.cancel()
        elapsedJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
    }
}

