package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
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
class QuizPreviewViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow(QuizPreviewUiState())
    val uiState: StateFlow<QuizPreviewUiState> = _uiState.asStateFlow()

    private val _effect = Channel<QuizPreviewEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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
            is QuizPreviewIntent.ChangeText -> _uiState.update { it.copy(textAnswer = intent.value) }
            QuizPreviewIntent.Submit -> submit()
            QuizPreviewIntent.Skip -> grade(answered = false)
            QuizPreviewIntent.Next -> advance()
            QuizPreviewIntent.Restart -> startRun()
            QuizPreviewIntent.Exit -> viewModelScope.launch { _effect.send(QuizPreviewEffect.Exit) }
            QuizPreviewIntent.EditQuiz -> editQuiz()
        }
    }

    private fun loadQuiz() {
        phaseJob?.cancel()
        elapsedJob?.cancel()
        viewModelScope.launch {
            _uiState.update { QuizPreviewUiState(isLoading = true) }
            when (val result = quizRepository.getQuizDetail(quizId)) {
                is Result.Success -> {
                    val quiz = result.data
                    val owner = sessionRepository.state.value.userOrNull?.id == quiz.quizOwner
                    _uiState.update {
                        QuizPreviewUiState(
                            isLoading = false,
                            quiz = quiz,
                            isOwner = owner
                        )
                    }
                    if (quiz.questions.isNotEmpty()) startRun()
                }
                is Result.Error -> _uiState.update {
                    QuizPreviewUiState(
                        isLoading = false,
                        errorMessage = result.error.toUserMessage()
                    )
                }
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
        val limit = question.timeLimit.takeIf { it > 0 } ?: FALLBACK_SECONDS
        questionOpenedAtMs = monotonicMillis()
        _uiState.update {
            it.copy(
                phase = PreviewPhase.QUESTION,
                currentIndex = index,
                remainingSeconds = limit,
                isLate = false,
                selectedOptionIds = emptySet(),
                textAnswer = "",
                currentResult = null
            )
        }
        phaseJob = viewModelScope.launch {
            val deadline = questionOpenedAtMs + limit * 1_000L
            while (isActive) {
                val left = ((deadline - monotonicMillis() + 999L) / 1_000L)
                    .coerceAtLeast(0L).toInt()
                _uiState.update { it.copy(remainingSeconds = left) }
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
        _uiState.update { it.copy(selectedOptionIds = setOf(optionId)) }
        grade(answered = true)
    }

    private fun toggleOption(optionId: Long) {
        val state = _uiState.value
        if (state.phase != PreviewPhase.QUESTION ||
            state.currentQuestion?.questionType != QuestionType.MULTIPLE_SELECT
        ) return
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
        val evaluation = evaluatePreviewAnswer(
            question = question,
            selectedOptionIds = if (answered) state.selectedOptionIds else emptySet(),
            textAnswer = if (answered) state.textAnswer else "",
            answered = answered
        )
        val earned = computePreviewScore(
            isCorrect = evaluation.isCorrect == true,
            timeTakenSeconds = timeTaken,
            timeLimitSeconds = question.timeLimit.takeIf { it > 0 } ?: FALLBACK_SECONDS,
            isLate = state.isLate
        )
        val result = PreviewQuestionResult(
            question = question,
            submittedAnswer = evaluation.submittedLabel,
            correctAnswer = evaluation.correctLabel,
            isCorrect = evaluation.isCorrect,
            isAnswered = answered,
            isLate = state.isLate,
            scoreEarned = earned,
            timeTakenSeconds = timeTaken
        )
        _uiState.update {
            it.copy(
                phase = PreviewPhase.FEEDBACK,
                currentResult = result,
                results = it.results + result,
                totalScore = it.totalScore + earned
            )
        }
        val revealSeconds = if (evaluation.isCorrect == true) 2L else 4L
        phaseJob = viewModelScope.launch {
            delay(revealSeconds * 1_000L)
            advance()
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
        if (!state.isOwner) return
        viewModelScope.launch { _effect.send(QuizPreviewEffect.EditQuiz(quizId)) }
    }

    private fun android.kma.myquizzapp.core.common.model.Quiz?.orEmptyQuestionCount(): Int =
        this?.questions?.size ?: 0

    override fun onCleared() {
        phaseJob?.cancel()
        elapsedJob?.cancel()
        super.onCleared()
    }

    private fun monotonicMillis(): Long = System.nanoTime() / 1_000_000L

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val FALLBACK_SECONDS = 30
    }
}
