package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.game_player.domain.PlayerGameSessionUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameViewModel @Inject constructor(
    private val session: PlayerGameSessionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = checkNotNull(savedStateHandle["gameId"])
    private val playerId: Long = checkNotNull(savedStateHandle["playerId"])
    private val socketToken: String = checkNotNull(savedStateHandle["socketToken"])
    private var eventJob: Job? = null
    private var nextTimeoutJob: Job? = null
    private val _uiState = MutableStateFlow(GameUiState(playerId = playerId))
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<GameEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init { connect() }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.SelectOption -> _uiState.update { if (!it.isAnswerInputEnabled) it else it.copy(selectedOptionId = intent.id) }
            is GameIntent.ToggleOption -> _uiState.update { state ->
                if (!state.isAnswerInputEnabled) state else state.copy(
                    selectedOptionIds = state.selectedOptionIds.toMutableSet().apply {
                        if (!add(intent.id)) remove(intent.id)
                    }
                )
            }
            is GameIntent.ChangeText -> _uiState.update { if (!it.isAnswerInputEnabled) it else it.copy(textAnswer = intent.value) }
            GameIntent.Submit -> submit()
            GameIntent.NextQuestion -> requestNextQuestion()
            GameIntent.MatchDeadlineReached -> _uiState.update { state ->
                if (!state.hasMatchBudget || state.isPersonallyDone) state
                else state.copy(matchTimeExpired = true, isInputLocked = true)
            }
            GameIntent.Retry -> connect()
            GameIntent.Sync -> viewModelScope.launch { session.sync() }
            GameIntent.Leave -> viewModelScope.launch { exit(null) }
            GameIntent.DeadlineReached -> _uiState.update { state ->
                if (state.phase == GamePhaseUi.Question && !state.allowAnswerLate) {
                    state.copy(isInputLocked = true)
                } else state
            }
            GameIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun connect() {
        eventJob?.cancel()
        _uiState.update {
            it.copy(
                connection = if (it.question == null) GameConnection.CONNECTING else GameConnection.RECONNECTING,
                errorMessage = null,
                isInputLocked = true
            )
        }
        eventJob = viewModelScope.launch { session.events(socketToken).collect(::onEvent) }
    }

    private suspend fun onEvent(event: GameEvent) {
        updateOffset(event.serverTimeOrNull())
        when (event) {
            GameEvent.Connected -> {
                _uiState.update { it.copy(connection = GameConnection.CONNECTED) }
                session.joinAndSync()
            }
            is GameEvent.LobbyUpdated -> _uiState.update { state ->
                state.withServerConfig(
                    sessionStatus = event.lobby.sessionStatus,
                    config = event.lobby.config
                )
            }
            is GameEvent.GameStarted -> _uiState.update { state ->
                state.withServerConfig(
                    sessionStatus = SessionStatus.ACTIVE,
                    config = event.config
                )
            }
            is GameEvent.Disconnected -> when (event.reason) {
                DisconnectReason.TRANSPORT -> {
                    nextTimeoutJob?.cancel()
                    _uiState.update {
                        it.copy(
                            connection = GameConnection.RECONNECTING,
                            isInputLocked = true,
                            canRequestNext = false,
                            isRequestingNext = false
                        )
                    }
                }
                DisconnectReason.SERVER_DISCONNECT -> exit("Máy chủ đã đóng kết nối tới phòng này.")
                DisconnectReason.CLIENT -> Unit
            }
            is GameEvent.Countdown -> _uiState.update {
                it.copy(phase = GamePhaseUi.Countdown(event.countdown.startsAt), question = null, isInputLocked = true)
            }
            is GameEvent.QuestionStarted -> {
                nextTimeoutJob?.cancel()
                _uiState.update {
                    it.copy(
                        phase = GamePhaseUi.Question,
                        question = event.started.question,
                        selectedOptionId = null,
                        selectedOptionIds = emptySet(),
                        textAnswer = "",
                        endsAt = event.started.endsAt,
                        matchEndsAt = event.started.matchEndsAt,
                        allowAnswerLate = event.started.allowAnswerLate,
                        remainingSeconds = event.started.remainingSeconds,
                        lives = event.started.lives,
                        matchTimeExpired = false,
                        isInputLocked = false,
                        isSubmitting = false,
                        isConfirming = false,
                        canRequestNext = false,
                        isRequestingNext = false,
                        results = null,
                        outcome = null,
                        scoreEarned = null,
                        wasLate = false,
                        timedOut = false,
                        answeredCount = null,
                        activePlayers = null
                    )
                }
            }
            is GameEvent.QuestionAwaitingNext -> applyAwaitingNext(event)
            is GameEvent.QuestionTimedOut -> applyQuestionTimeout(event)
            is GameEvent.AnswerProgressUpdated -> _uiState.update { state ->
                if (state.question?.index != event.progress.index) state else state.copy(
                    answeredCount = event.progress.answered,
                    activePlayers = event.progress.activePlayers
                )
            }
            is GameEvent.PlayerLeaderboardUpdated -> {
                if (_uiState.value.showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS) {
                    applyLeaderboard(event.leaderboard)
                }
            }
            is GameEvent.QuestionLocked -> _uiState.update { state ->
                if (state.question?.index != event.index || state.results?.index == event.index) state
                else state.copy(phase = GamePhaseUi.Locked, isInputLocked = true)
            }
            is GameEvent.QuestionResultsReceived -> _uiState.update { state ->
                if (state.question?.index != event.results.index) state else {
                    val feedback = resolveQuestionFeedback(
                        showCorrectAnswer = state.showCorrectAnswer,
                        questionType = state.question.questionType,
                        submitted = state.submittedAnswerKeys,
                        results = event.results
                    )
                    state.copy(
                        phase = GamePhaseUi.Results(),
                        results = feedback.results,
                        outcome = feedback.outcome,
                        isInputLocked = true,
                        isSubmitting = false,
                        isConfirming = false
                    )
                }
            }
            is GameEvent.StateSnapshot -> restore(event.snapshot)
            is GameEvent.GameEndedEvent -> finish(event)
            is GameEvent.PlayerEliminated -> if (event.player.id == playerId) {
                _uiState.update {
                    it.copy(
                        phase = GamePhaseUi.Eliminated,
                        lives = 0,
                        isInputLocked = true,
                        canRequestNext = false,
                        isRequestingNext = false
                    )
                }
            }
            is GameEvent.PlayerFinishedEvent -> applyPlayerFinished(event)
            is GameEvent.Failed -> {
                if (event.event == EVENT_QUESTION_NEXT) {
                    nextTimeoutJob?.cancel()
                    _uiState.update { it.copy(isRequestingNext = false, canRequestNext = true) }
                }
                onFailure(event.code)
            }
            is GameEvent.HostQuestionReceived,
            is GameEvent.HostAnswerReceivedEvent,
            is GameEvent.HostLeaderboardUpdated,
            is GameEvent.Unhandled -> Unit
        }
    }

    private fun applyLeaderboard(rows: List<LeaderboardRow>) {
        val me = rows.firstOrNull { it.id == playerId }
        _uiState.update { it.copy(leaderboard = rows, playerRank = me?.rank, playerScore = me?.playerScore) }
    }

    private fun restore(snapshot: GameSnapshot) {
        val answered = snapshot.player?.answerFor(snapshot.index)
        _uiState.update { old ->
            val hasCurrentResults = old.results?.index == snapshot.index
            // ACK thành công là bằng chứng server đã ghi đáp án. Một snapshot thiếu
            // answered_questions (pause/resume hoặc cache vừa reconnect) không được
            // mở lại input. Chỉ ACK bất định (isConfirming=true) mới cho phép snapshot
            // "chưa trả lời" mở lại câu đang active.
            val hasLocallyAcceptedOrPendingAnswer =
                old.question?.index == snapshot.index &&
                    old.phase == GamePhaseUi.Submitted &&
                    !old.isConfirming
            val phase = when (snapshot.player?.status) {
                STATUS_ELIMINATED -> GamePhaseUi.Eliminated
                STATUS_FINISHED -> GamePhaseUi.PlayerFinished
                else -> when (snapshot.phase) {
                    GamePhase.COUNTDOWN -> GamePhaseUi.Countdown(snapshot.countdownStartsAt)
                    GamePhase.QUESTION_ACTIVE -> when {
                        hasCurrentResults -> old.phase
                        answered != null || hasLocallyAcceptedOrPendingAnswer -> GamePhaseUi.Submitted
                        else -> GamePhaseUi.Question
                    }
                    GamePhase.QUESTION_LOCKED -> if (hasCurrentResults) old.phase else GamePhaseUi.Locked
                    GamePhase.SHOWING_RESULTS -> GamePhaseUi.Results(restoredWithoutDetails = !hasCurrentResults)
                    GamePhase.FINISHED -> GamePhaseUi.Finished
                    GamePhase.UNKNOWN -> old.phase
                }
            }
            val status = snapshot.sessionStatus ?: old.sessionStatus
            val config = snapshot.config
            val pacing = config?.flow?.pacing ?: old.pacing
            val autoAdvance = config?.timing?.autoAdvance ?: old.autoAdvance
            val showCorrect = config?.flow?.showCorrectAnswer ?: old.showCorrectAnswer
            val showBoard = config?.flow?.showLeaderboard ?: old.showLeaderboard
            val allowAnswerLate = config?.flow?.allowAnswerLate ?: snapshot.allowAnswerLate
            val snapshotRows = snapshot.leaderboard
            val rows = if (showBoard == ShowLeaderboard.BETWEEN_QUESTIONS) {
                if (snapshotRows.isNotEmpty()) snapshotRows else old.leaderboard
            } else emptyList()
            val me = rows.firstOrNull { it.id == playerId }
            old.copy(
                connection = GameConnection.CONNECTED,
                sessionStatus = status,
                pacing = pacing,
                autoAdvance = autoAdvance,
                showCorrectAnswer = showCorrect,
                showLeaderboard = showBoard,
                phase = phase,
                question = snapshot.question ?: old.question,
                endsAt = snapshot.endsAt,
                matchEndsAt = snapshot.matchEndsAt,
                allowAnswerLate = allowAnswerLate,
                remainingSeconds = snapshot.remainingSeconds,
                lives = snapshot.player?.lives ?: old.lives,
                selectedOptionId = answered?.answerKeys?.singleOrNull() ?: old.selectedOptionId,
                selectedOptionIds = answered?.answerKeys?.toSet() ?: old.selectedOptionIds,
                textAnswer = answered?.answerKeys?.singleOrNull() ?: old.textAnswer,
                leaderboard = rows,
                playerRank = me?.rank,
                playerScore = me?.playerScore,
                isInputLocked = status == SessionStatus.PAUSED ||
                    phase != GamePhaseUi.Question || answered != null,
                isSubmitting = false,
                isConfirming = false
            )
        }
    }

    private fun GameUiState.withServerConfig(
        sessionStatus: SessionStatus?,
        config: GameConfig
    ): GameUiState {
        val showCorrectAnswer = config.flow.showCorrectAnswer
        val showLeaderboard = config.flow.showLeaderboard
        val mayShowBoard = showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS
        val resumedInput = sessionStatus == SessionStatus.ACTIVE && phase == GamePhaseUi.Question
        return copy(
            sessionStatus = sessionStatus,
            pacing = config.flow.pacing,
            autoAdvance = config.timing.autoAdvance,
            showCorrectAnswer = showCorrectAnswer,
            showLeaderboard = showLeaderboard,
            allowAnswerLate = config.flow.allowAnswerLate,
            leaderboard = if (mayShowBoard) leaderboard else emptyList(),
            playerRank = if (mayShowBoard) playerRank else null,
            playerScore = if (mayShowBoard) playerScore else null,
            isInputLocked = when {
                sessionStatus == SessionStatus.PAUSED -> true
                resumedInput -> false
                else -> isInputLocked
            }
        )
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val question = state.question ?: return
        val answer = when (question.questionType) {
            "multiple_choice" -> PlayerAnswer.SingleChoice(checkNotNull(state.selectedOptionId))
            "multiple_select" -> PlayerAnswer.MultipleSelect(question.answerOptions.map { it.id }.filter(state.selectedOptionIds::contains))
            "short_answer", "long_answer" -> PlayerAnswer.Text(state.textAnswer.trim())
            else -> return
        }
        _uiState.update { it.copy(phase = GamePhaseUi.Submitted, isInputLocked = true, isSubmitting = true) }
        viewModelScope.launch {
            when (val result = session.submit(answer)) {
                is Result.Success -> applyAnswerAck(result.data)
                is Result.Error -> {
                    val code = (result.error as? AppError.Api)?.code.orEmpty()
                    if (code in FATAL_CODES) exit(result.error.toUserMessage())
                    else {
                        _uiState.update { it.copy(isSubmitting = false, isConfirming = true, errorMessage = "Đang xác nhận câu trả lời với máy chủ…") }
                        session.sync()
                    }
                }
            }
        }
    }

    private fun applyAnswerAck(ack: AnswerAck) {
        updateOffset(ack.serverTime)
        _uiState.update { state ->
            if (!state.isSelfPaced) {
                return@update state.copy(
                    isSubmitting = false,
                    isConfirming = false,
                    lives = ack.lives ?: state.lives
                )
            }
            val questionIndex = state.question?.index ?: return@update state.copy(
                isSubmitting = false,
                isConfirming = false,
                lives = ack.lives ?: state.lives
            )
            val feedback = resolveSelfPacedFeedback(
                showCorrectAnswer = state.showCorrectAnswer,
                questionIndex = questionIndex,
                ack = ack
            )
            val hasNextQuestion = state.question?.let { it.index + 1 < it.total } == true
            val canReveal = state.showCorrectAnswer == true && ack.isCorrect != null
            state.copy(
                phase = if (ack.eliminated) GamePhaseUi.Eliminated else GamePhaseUi.Results(),
                isInputLocked = true,
                isSubmitting = false,
                isConfirming = false,
                canRequestNext = !ack.eliminated && !state.autoAdvance && hasNextQuestion,
                isRequestingNext = false,
                results = feedback.results,
                outcome = feedback.outcome,
                totalScore = ack.totalScore.takeIf { canReveal } ?: state.totalScore,
                scoreEarned = ack.scoreEarned.takeIf { canReveal },
                streak = ack.streak.takeIf { canReveal } ?: state.streak,
                wasLate = ack.isLate,
                timedOut = false,
                lives = ack.lives ?: state.lives
            )
        }
    }

    private fun applyQuestionTimeout(event: GameEvent.QuestionTimedOut) {
        nextTimeoutJob?.cancel()
        val timeout = event.timeout
        _uiState.update { state ->
            if (state.question?.index != timeout.questionIndex) return@update state
            val canReveal = state.showCorrectAnswer == true
            state.copy(
                phase = if (timeout.eliminated) GamePhaseUi.Eliminated else GamePhaseUi.Results(),
                isInputLocked = true,
                isSubmitting = false,
                isConfirming = false,
                canRequestNext = false,
                isRequestingNext = false,
                results = QuestionResults(
                    index = timeout.questionIndex,
                    questionId = timeout.questionId,
                    correctAnswers = timeout.correctAnswers.takeIf { canReveal }.orEmpty()
                ),
                outcome = if (canReveal) QuestionOutcome.INCORRECT else QuestionOutcome.HIDDEN,
                scoreEarned = 0.takeIf { canReveal },
                streak = 0.takeIf { canReveal } ?: state.streak,
                wasLate = false,
                timedOut = true,
                lives = timeout.lives ?: state.lives
            )
        }
    }

    private fun applyPlayerFinished(event: GameEvent.PlayerFinishedEvent) {
        val finished = event.finished
        if (finished.id != playerId) return
        nextTimeoutJob?.cancel()
        _uiState.update { state ->
            val rows = if (state.showLeaderboard == ShowLeaderboard.NEVER) emptyList() else finished.leaderboard
            val me = rows.firstOrNull { it.id == playerId }
            state.copy(
                phase = if (finished.status == STATUS_ELIMINATED) {
                    GamePhaseUi.Eliminated
                } else {
                    GamePhaseUi.PlayerFinished
                },
                isInputLocked = true,
                canRequestNext = false,
                isRequestingNext = false,
                totalScore = finished.playerScore,
                matchTimeExpired = state.matchTimeExpired || state.hasMatchBudget,
                leaderboard = rows,
                playerRank = me?.rank,
                playerScore = me?.playerScore ?: finished.playerScore
            )
        }
    }

    private fun applyAwaitingNext(event: GameEvent.QuestionAwaitingNext) {
        nextTimeoutJob?.cancel()
        val awaiting = event.awaiting
        val ack = AnswerAck(
            accepted = true,
            lives = awaiting.lives,
            serverTime = event.serverTime,
            isCorrect = awaiting.isCorrect,
            scoreEarned = awaiting.scoreEarned,
            totalScore = awaiting.playerScore,
            correctAnswers = awaiting.correctAnswers
        )
        _uiState.update { state ->
            val feedback = resolveSelfPacedFeedback(
                showCorrectAnswer = state.showCorrectAnswer,
                questionIndex = awaiting.questionIndex,
                ack = ack
            )
            val canReveal = state.showCorrectAnswer == true
            state.copy(
                phase = GamePhaseUi.Results(),
                isInputLocked = true,
                isSubmitting = false,
                isConfirming = false,
                canRequestNext = state.isSelfPaced && !state.autoAdvance,
                isRequestingNext = false,
                results = feedback.results,
                outcome = feedback.outcome,
                totalScore = awaiting.playerScore.takeIf { canReveal } ?: state.totalScore,
                scoreEarned = awaiting.scoreEarned.takeIf { canReveal },
                wasLate = false,
                timedOut = false,
                lives = awaiting.lives ?: state.lives
            )
        }
    }

    private fun requestNextQuestion() {
        val state = _uiState.value
        if (!state.shouldShowNextAction || state.isRequestingNext || state.connection != GameConnection.CONNECTED) return
        _uiState.update { it.copy(isRequestingNext = true, errorMessage = null) }
        nextTimeoutJob?.cancel()
        viewModelScope.launch {
            session.requestNext()
            nextTimeoutJob = viewModelScope.launch {
                delay(NEXT_TIMEOUT_MS)
                _uiState.update { current ->
                    if (!current.isRequestingNext) current else current.copy(
                        isRequestingNext = false,
                        canRequestNext = true,
                        errorMessage = "Máy chủ chưa gửi câu tiếp theo, hãy thử lại."
                    )
                }
            }
        }
    }

    private suspend fun finish(event: GameEvent.GameEndedEvent) {
        session.saveResult(gameId, playerId, event.ended)
        _uiState.update { it.copy(phase = GamePhaseUi.Finished, isInputLocked = true) }
        _effect.send(GameEffect.NavigateToFinalResult(gameId, playerId))
        nextTimeoutJob?.cancel()
        session.disconnect()
        eventJob?.cancel()
    }

    private suspend fun onFailure(code: String) {
        when {
            code in FATAL_CODES -> exit(AppError.Api(code).toUserMessage())
            code == CODE_RECONNECT_EXHAUSTED -> _uiState.update {
                it.copy(
                    connection = GameConnection.RECONNECT_FAILED,
                    isInputLocked = true,
                    errorMessage = null
                )
            }
            // Các lỗi transport trung gian đã được banner RECONNECTING thể hiện.
            // Không bật snackbar sau mỗi lần Socket.IO tự retry.
            code == CODE_CONNECT_FAILED -> Unit
            else -> _uiState.update { it.copy(errorMessage = AppError.Api(code).toUserMessage()) }
        }
    }

    private fun updateOffset(serverTime: String?) {
        val serverMs = serverTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return
        _uiState.update { it.copy(serverOffsetMs = serverMs - System.currentTimeMillis()) }
    }

    private fun GameEvent.serverTimeOrNull(): String? = when (this) {
        is GameEvent.Countdown -> serverTime
        is GameEvent.QuestionStarted -> serverTime
        is GameEvent.QuestionAwaitingNext -> serverTime
        is GameEvent.QuestionTimedOut -> serverTime
        is GameEvent.QuestionLocked -> serverTime
        is GameEvent.QuestionResultsReceived -> serverTime
        is GameEvent.AnswerProgressUpdated -> serverTime
        is GameEvent.PlayerLeaderboardUpdated -> serverTime
        is GameEvent.StateSnapshot -> serverTime
        is GameEvent.GameStarted -> serverTime
        is GameEvent.GameEndedEvent -> serverTime
        is GameEvent.PlayerEliminated -> serverTime
        is GameEvent.PlayerFinishedEvent -> serverTime
        GameEvent.Connected,
        is GameEvent.Disconnected,
        is GameEvent.LobbyUpdated,
        is GameEvent.Failed,
        is GameEvent.Unhandled -> null
        is GameEvent.HostQuestionReceived -> serverTime
        is GameEvent.HostAnswerReceivedEvent -> serverTime
        is GameEvent.HostLeaderboardUpdated -> serverTime
    }

    private suspend fun exit(message: String?) {
        _effect.send(GameEffect.Exit(message))
        nextTimeoutJob?.cancel()
        session.disconnect()
        eventJob?.cancel()
    }

    private companion object {
        const val EVENT_QUESTION_NEXT = "question:next"
        const val NEXT_TIMEOUT_MS = 5_000L
        const val CODE_CONNECT_FAILED = "CLIENT_CONNECT_FAILED"
        const val CODE_RECONNECT_EXHAUSTED = "CLIENT_RECONNECT_EXHAUSTED"
        const val STATUS_ELIMINATED = "eliminated"
        const val STATUS_FINISHED = "finished"

        val FATAL_CODES = setOf("GAME_TOKEN_INVALID", "GAME_TOKEN_WRONG_ROOM", "GAME_ROOM_NOT_FOUND", "GAME_PLAYER_NOT_FOUND")
    }
}
