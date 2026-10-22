package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.AnswerStats
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
    private var classicFeedbackJob: Job? = null
    private var finalResultRequested = false
    private var latestOffsetServerMs: Long? = null
    private val _uiState = MutableStateFlow(GameUiState(playerId = playerId))
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<GameEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init { connect() }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.SelectOption -> selectOption(intent.id)
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
        classicFeedbackJob?.cancel()
        classicFeedbackJob = null
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
        if (_uiState.value.phase == GamePhaseUi.Finished &&
            event !is GameEvent.StateSnapshot && event !is GameEvent.GameEndedEvent) return
        updateOffset(event.serverTimeOrNull())
        when (event) {
            GameEvent.Connected -> {
                _uiState.update { it.copy(connection = GameConnection.CONNECTED, isSynchronizing = it.question != null || it.phase is GamePhaseUi.Countdown) }
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
                ).copy(mode = event.mode)
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
                // Missing startsAt is an event fallback only, never a restarted local three-second timer.
                val target = event.countdown.startsAt ?: Instant.ofEpochMilli(
                    System.currentTimeMillis() + it.serverOffsetMs + event.countdown.seconds.coerceAtLeast(0) * 1_000L
                ).toString()
                it.copy(phase = GamePhaseUi.Countdown(target), question = null,
                    isSynchronizing = false, isInputLocked = true)
            }
            is GameEvent.QuestionStarted -> {
                val current = _uiState.value
                val incoming = event.started.question
                val currentQuestion = current.question
                if (current.isClassic && currentQuestion != null &&
                    (incoming.index < currentQuestion.index ||
                        (incoming.index == currentQuestion.index && incoming.id == currentQuestion.id &&
                            current.phase != GamePhaseUi.Question && current.phase != GamePhaseUi.Connecting))) return
                nextTimeoutJob?.cancel()
                _uiState.update {
                    it.copy(
                        phase = GamePhaseUi.Question,
                        isSynchronizing = false,
                        question = event.started.question,
                        selectedOptionId = null,
                        selectedOptionIds = emptySet(),
                        textAnswer = "",
                        endsAt = event.started.endsAt,
                        timeLimitSeconds = event.started.timeLimitSeconds,
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
                        acceptedAnswerKeys = null,
                        hasAnswerAttempt = false,
                        classicFeedbackRemainingMs = 0L,
                        isResultsLeaderboardFresh = false,
                        leaderboard = if (it.isClassic) emptyList() else it.leaderboard,
                        playerRank = if (it.isClassic) null else it.playerRank,
                        playerScore = if (it.isClassic) null else it.playerScore,
                        results = null,
                        personalResult = null,
                        classicPhaseServerTime = event.serverTime,
                        classicTimingFromSnapshot = false,
                        totalScore = if (it.isClassic) null else it.totalScore,
                        streak = if (it.isClassic) null else it.streak,
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
                    applyLeaderboard(event.leaderboard, event.serverTime)
                }
            }
            is GameEvent.QuestionLocked -> _uiState.update { state ->
                if (state.question?.index != event.index || state.phase is GamePhaseUi.Results) state
                else state.copy(phase = GamePhaseUi.Locked, isInputLocked = true)
            }
            is GameEvent.PersonalQuestionResultsReceived -> {
                val before = _uiState.value
                _uiState.update { it.reconcileClassicPersonalResult(event.result) }
                if (_uiState.value != before) updateOffset(event.result.serverTime)
            }
            is GameEvent.QuestionResultsReceived -> _uiState.update { state ->
                if (state.isClassic) return@update state.reconcileClassicCommonResult(event.results, event.serverTime)
                if (state.question?.index != event.results.index ||
                    (event.results.questionId != null && state.question.id != event.results.questionId) ||
                    state.phase == GamePhaseUi.Finished
                ) state else {
                    val feedback = resolveQuestionFeedback(
                        showCorrectAnswer = state.showCorrectAnswer,
                        questionType = state.question.questionType,
                        submitted = if (state.isClassic) state.acceptedAnswerKeys.orEmpty() else state.submittedAnswerKeys,
                        results = event.results
                    )
                    state.copy(
                        phase = GamePhaseUi.Results(),
                        results = feedback.results,
                        outcome = if (state.isClassic && state.acceptedAnswerKeys == null)
                            QuestionOutcome.HIDDEN else feedback.outcome,
                        endsAt = if (state.isClassic) event.results.nextQuestionAt else state.endsAt,
                        classicFeedbackRemainingMs = when {
                            !state.isClassic -> 0L
                            state.phase is GamePhaseUi.Results -> state.classicFeedbackRemainingMs
                            state.showCorrectAnswer == true && feedback.results.correctAnswers.isNotEmpty() ->
                                CLASSIC_FEEDBACK_DURATION_MS
                            else -> 0L
                        },
                        isResultsLeaderboardFresh = state.isResultsLeaderboardFresh && state.phase is GamePhaseUi.Results,
                        leaderboard = if (state.isClassic && state.phase !is GamePhaseUi.Results) emptyList() else state.leaderboard,
                        playerRank = if (state.isClassic && state.phase !is GamePhaseUi.Results) null else state.playerRank,
                        playerScore = if (state.isClassic && state.phase !is GamePhaseUi.Results) null else state.playerScore,
                        isInputLocked = true,
                        isSubmitting = false,
                        isConfirming = false
                    )
                }
            }
            is GameEvent.StateSnapshot -> {
                restore(event.snapshot, event.serverTime)
                if (event.snapshot.phase == GamePhase.FINISHED && !finalResultRequested) {
                    finalResultRequested = true
                    _effect.send(GameEffect.NavigateToFinalResult(gameId, playerId))
                }
            }
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
            is GameEvent.HostPlayerProgressUpdated,
            is GameEvent.HostLeaderboardUpdated,
            is GameEvent.Unhandled -> Unit
        }
        reconcileClassicFeedback()
    }

    /** A finite, cancellable presentation timer. Pause/reconnect retain completed ticks. */
    private fun reconcileClassicFeedback() {
        val state = _uiState.value
        if (!state.isClassic || state.phase !is GamePhaseUi.Results || state.isPaused ||
            state.connection != GameConnection.CONNECTED || state.isSynchronizing || state.classicFeedbackRemainingMs <= 0L ||
            !state.revealsClassicFeedback || state.showsClassicSummary
        ) {
            classicFeedbackJob?.cancel()
            classicFeedbackJob = null
            return
        }
        if (classicFeedbackJob?.isActive == true) return
        val questionId = state.question?.id
        val index = state.question?.index
        classicFeedbackJob = viewModelScope.launch {
            while (true) {
                val tick = minOf(50L, _uiState.value.classicFeedbackRemainingMs)
                if (tick <= 0L) break
                delay(tick)
                _uiState.update { current ->
                    if (current.question?.id != questionId || current.question?.index != index ||
                        current.phase !is GamePhaseUi.Results || current.isPaused ||
                        current.connection != GameConnection.CONNECTED || current.isSynchronizing
                    ) current else current.copy(
                        classicFeedbackRemainingMs = (current.classicFeedbackRemainingMs - tick).coerceAtLeast(0L)
                    )
                }
                if (_uiState.value.classicFeedbackRemainingMs <= 0L) break
            }
        }
    }

    private fun applyLeaderboard(rows: List<LeaderboardRow>, serverTime: String? = null) {
        val me = rows.firstOrNull { it.id == playerId }
        _uiState.update {
            if (it.isClassic && (!it.canShowClassicRank ||
                (serverTime.serverMillis() ?: Long.MAX_VALUE) < (it.classicPhaseServerTime.serverMillis() ?: Long.MIN_VALUE))) it
            else it.copy(leaderboard = rows, playerRank = me?.rank, playerScore = me?.playerScore,
                isResultsLeaderboardFresh = it.phase is GamePhaseUi.Results)
        }
    }

    private fun restore(snapshot: GameSnapshot, serverTime: String?) {
        val current = _uiState.value
        if (current.phase == GamePhaseUi.Finished) return
        if (current.isClassic && snapshot.phase != GamePhase.FINISHED &&
            snapshot.index < (current.question?.index ?: -1)) return
        if (current.isClassic && snapshot.phase != GamePhase.FINISHED &&
            current.question?.index == snapshot.index &&
            (serverTime.serverMillis() ?: Long.MAX_VALUE) < (current.classicPhaseServerTime.serverMillis() ?: Long.MIN_VALUE)) return
        // Snapshot models live in core:common; capture the public property for a stable smart cast.
        val snapshotQuestion = snapshot.question
        val answered = snapshot.player?.answerFor(snapshot.index)?.takeIf {
            snapshotQuestion == null || it.questionId == snapshotQuestion.id
        }
        _uiState.update { old ->
            val sameQuestion = old.question?.index == snapshot.index &&
                (snapshotQuestion == null || old.question?.id == snapshotQuestion.id)
            val hasCurrentResults = sameQuestion && (old.results?.index == snapshot.index ||
                (old.isClassic && old.phase is GamePhaseUi.Results))
            // ACK thành công là bằng chứng server đã ghi đáp án. Một snapshot thiếu
            // answered_questions (pause/resume hoặc cache vừa reconnect) không được
            // mở lại input. Chỉ ACK bất định (isConfirming=true) mới cho phép snapshot
            // "chưa trả lời" mở lại câu đang active.
            val hasLocallyAcceptedOrPendingAnswer =
                sameQuestion && (old.acceptedAnswerKeys != null || (old.phase == GamePhaseUi.Submitted && !old.isConfirming))
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
            val classic = (snapshot.mode ?: old.mode) == android.kma.myquizzapp.core.common.model.GameMode.CLASSIC &&
                pacing == android.kma.myquizzapp.core.common.model.Pacing.HOST
            val mayShowBoard = showBoard == ShowLeaderboard.BETWEEN_QUESTIONS &&
                (!classic || (showCorrect == true && phase is GamePhaseUi.Results))
            val rows = if (mayShowBoard) {
                if (snapshotRows.isNotEmpty()) snapshotRows
                else if (sameQuestion && (!old.isClassic || old.isResultsLeaderboardFresh)) old.leaderboard
                else emptyList()
            } else emptyList()
            val me = rows.firstOrNull { it.id == playerId }
            old.copy(
                connection = GameConnection.CONNECTED,
                isSynchronizing = false,
                sessionStatus = status,
                mode = snapshot.mode ?: old.mode,
                pacing = pacing,
                autoAdvance = autoAdvance,
                showCorrectAnswer = showCorrect,
                showLeaderboard = showBoard,
                phase = phase,
                question = snapshotQuestion ?: old.question.takeIf { sameQuestion },
                endsAt = when {
                    status == SessionStatus.PAUSED -> snapshot.endsAt
                    hasCurrentResults && snapshot.phase != GamePhase.SHOWING_RESULTS -> old.endsAt
                    else -> snapshot.endsAt
                },
                timeLimitSeconds = old.timeLimitSeconds.takeIf {
                    old.question?.id == snapshotQuestion?.id && old.question?.index == snapshot.index
                },
                matchEndsAt = snapshot.matchEndsAt,
                allowAnswerLate = allowAnswerLate,
                remainingSeconds = snapshot.remainingSeconds,
                lives = snapshot.player?.lives ?: old.lives,
                selectedOptionId = answered?.answerKeys?.singleOrNull() ?: old.selectedOptionId.takeIf { sameQuestion },
                selectedOptionIds = answered?.answerKeys?.toSet() ?: old.selectedOptionIds.takeIf { sameQuestion }.orEmpty(),
                textAnswer = answered?.answerKeys?.singleOrNull() ?: old.textAnswer.takeIf { sameQuestion }.orEmpty(),
                acceptedAnswerKeys = answered?.answerKeys ?: old.acceptedAnswerKeys.takeIf { sameQuestion },
                hasAnswerAttempt = answered != null || (sameQuestion && old.hasAnswerAttempt),
                results = old.results.takeIf { hasCurrentResults && showCorrect == true }
                    ?: old.results.takeIf { hasCurrentResults }?.copy(correctAnswers = emptyList(), stats = AnswerStats()),
                outcome = when {
                    !hasCurrentResults -> null
                    showCorrect != true -> QuestionOutcome.HIDDEN
                    else -> old.outcome
                },
                classicFeedbackRemainingMs = if (classic && old.isSynchronizing) 0L
                    else old.classicFeedbackRemainingMs.takeIf { hasCurrentResults } ?: 0L,
                personalResult = old.personalResult.takeIf { sameQuestion && showCorrect == true && phase is GamePhaseUi.Results },
                classicPhaseServerTime = if (classic) newerServerTime(old.classicPhaseServerTime.takeIf { sameQuestion }, serverTime) else null,
                classicTimingFromSnapshot = classic && phase is GamePhaseUi.Results,
                totalScore = if (classic) old.totalScore.takeIf { sameQuestion && showCorrect == true && phase is GamePhaseUi.Results } else old.totalScore,
                streak = if (classic) old.streak.takeIf { sameQuestion && showCorrect == true && phase is GamePhaseUi.Results } else old.streak,
                isResultsLeaderboardFresh = phase is GamePhaseUi.Results && mayShowBoard &&
                    (snapshotRows.isNotEmpty() || (sameQuestion && old.isResultsLeaderboardFresh)),
                scoreEarned = old.scoreEarned.takeIf { sameQuestion && (!classic ||
                    (showCorrect == true && phase is GamePhaseUi.Results)) },
                timedOut = sameQuestion && old.timedOut,
                wasLate = sameQuestion && old.wasLate,
                leaderboard = rows,
                playerRank = me?.rank,
                playerScore = me?.playerScore,
                isInputLocked = status == SessionStatus.PAUSED ||
                    phase != GamePhaseUi.Question || answered != null,
                isSubmitting = false,
                isConfirming = false
            ).let { restored ->
                if (!restored.isClassic) restored
                else {
                    val reconciled = if (snapshot.phase == GamePhase.SHOWING_RESULTS)
                        restored.reconcileClassicPersonalResult(snapshot.personalResult?.takeIf { it.index == snapshot.index }, restored = true)
                    else restored
                    if (phase == GamePhaseUi.Finished) reconciled.copy(personalResult = null, results = null,
                        outcome = null, totalScore = null, scoreEarned = null, streak = null, classicFeedbackRemainingMs = 0L)
                    else if (reconciled.visiblePersonalResult != null || reconciled.results == null) reconciled
                    else reconciled.copy(outcome = if (reconciled.acceptedAnswerKeys == null) QuestionOutcome.HIDDEN
                        else resolveQuestionFeedback(reconciled.showCorrectAnswer, reconciled.question?.questionType.orEmpty(),
                            reconciled.acceptedAnswerKeys, reconciled.results).outcome)
                }
            }
        }
    }

    private fun GameUiState.withServerConfig(
        sessionStatus: SessionStatus?,
        config: GameConfig
    ): GameUiState {
        val showCorrectAnswer = config.flow.showCorrectAnswer
        val showLeaderboard = config.flow.showLeaderboard
        val mayShowBoard = showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS &&
            (!isClassic || (showCorrectAnswer && phase is GamePhaseUi.Results))
        val resumedInput = sessionStatus == SessionStatus.ACTIVE && phase == GamePhaseUi.Question
        return copy(
            sessionStatus = sessionStatus,
            pacing = config.flow.pacing,
            autoAdvance = config.timing.autoAdvance,
            showCorrectAnswer = showCorrectAnswer,
            personalResult = personalResult.takeIf { showCorrectAnswer },
            totalScore = if (isClassic && !showCorrectAnswer) null else totalScore,
            scoreEarned = if (isClassic && !showCorrectAnswer) null else scoreEarned,
            streak = if (isClassic && !showCorrectAnswer) null else streak,
            showLeaderboard = showLeaderboard,
            allowAnswerLate = config.flow.allowAnswerLate,
            results = if (showCorrectAnswer) results else results?.copy(correctAnswers = emptyList(),
                stats = AnswerStats()),
            outcome = if (showCorrectAnswer) outcome else QuestionOutcome.HIDDEN.takeIf { results != null },
            isResultsLeaderboardFresh = mayShowBoard && isResultsLeaderboardFresh,
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

    private fun selectOption(id: String) {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.isAnswerInputEnabled || question.questionType != "multiple_choice" ||
            question.answerOptions.none { it.id == id }
        ) return
        _uiState.update { it.copy(selectedOptionId = id) }
        // Classic follows local Preview's tap-to-submit UX, not its local grading.
        // submit() locks synchronously before launch, so a second tap cannot send again.
        if (state.isClassic) submit()
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
        val submittedKeys = state.submittedAnswerKeys.map(String::trim)
        _uiState.update { it.copy(phase = GamePhaseUi.Submitted, isInputLocked = true, isSubmitting = true,
            hasAnswerAttempt = true) }
        viewModelScope.launch {
            val result = session.submit(answer)
            val current = _uiState.value
            // Late ACK/error must not modify a newer question or regress results/locked.
            if (current.question?.id != question.id || current.question?.index != question.index ||
                current.phase == GamePhaseUi.Finished
            ) return@launch
            if (current.phase != GamePhaseUi.Submitted) {
                // Results can beat the ACK coroutine. Keep the phase, but retain accepted evidence.
                if (current.isClassic && result is Result.Success && result.data.accepted) {
                    acceptClassicAnswer(submittedKeys)
                }
                return@launch
            }
            when (result) {
                is Result.Success -> {
                    if (current.isClassic && result.data.accepted) acceptClassicAnswer(submittedKeys)
                    applyAnswerAck(result.data)
                }
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

    private fun acceptClassicAnswer(keys: List<String>) {
        _uiState.update { state ->
            state.copy(acceptedAnswerKeys = keys.takeUnless { state.visiblePersonalResult?.answered == false },
                hasAnswerAttempt = state.visiblePersonalResult?.answered ?: true,
                phase = if (state.phase == GamePhaseUi.Question) GamePhaseUi.Submitted else state.phase,
                isInputLocked = true, isSubmitting = false, isConfirming = false,
                outcome = if (state.visiblePersonalResult != null) state.outcome else state.results?.let { results ->
                    resolveQuestionFeedback(state.showCorrectAnswer, state.question?.questionType.orEmpty(), keys, results).outcome
                } ?: state.outcome)
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
        session.saveResult(gameId, playerId, event.ended, _uiState.value.mode, socketToken)
        _uiState.update { it.copy(phase = GamePhaseUi.Finished, isInputLocked = true,
            personalResult = null, results = null, outcome = null, totalScore = null,
            scoreEarned = null, streak = null, classicFeedbackRemainingMs = 0L) }
        if (!finalResultRequested) {
            finalResultRequested = true
            _effect.send(GameEffect.NavigateToFinalResult(gameId, playerId))
        }
        classicFeedbackJob?.cancel()
        classicFeedbackJob = null
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
        if (serverMs < (latestOffsetServerMs ?: Long.MIN_VALUE)) return
        latestOffsetServerMs = serverMs
        _uiState.update { it.copy(serverOffsetMs = serverMs - System.currentTimeMillis()) }
    }

    private fun GameEvent.serverTimeOrNull(): String? = when (this) {
        is GameEvent.Countdown -> serverTime
        is GameEvent.QuestionStarted -> serverTime
        is GameEvent.QuestionAwaitingNext -> serverTime
        is GameEvent.QuestionTimedOut -> serverTime
        is GameEvent.QuestionLocked -> serverTime
        is GameEvent.QuestionResultsReceived -> serverTime
        // Updated only after identity/privacy reconciliation accepted this private payload.
        is GameEvent.PersonalQuestionResultsReceived -> null
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
        is GameEvent.HostPlayerProgressUpdated -> serverTime
        is GameEvent.HostLeaderboardUpdated -> serverTime
    }

    private suspend fun exit(message: String?) {
        _uiState.update { it.copy(classicFeedbackRemainingMs = 0L, personalResult = null,
            scoreEarned = null, totalScore = null, streak = null) }
        _effect.send(GameEffect.Exit(message))
        classicFeedbackJob?.cancel()
        classicFeedbackJob = null
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
