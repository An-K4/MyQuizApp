package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel của màn điều khiển trận (HOST): host-paced từ N21, self-paced từ N28.5.
 *
 * Về bản chất đây là một máy trạng thái bám theo `current_phase` của server. Nguyên
 * tắc xương sống: **server là nguồn sự thật duy nhất**. Client không bao giờ tự
 * suy ra phase kế tiếp sau khi bấm nút, vì các lệnh điều khiển đều KHÔNG có ack —
 * mọi chuyển đổi đều chờ broadcast về.
 *
 * Bốn bẫy đã xác minh trong code backend và cách xử ở đây:
 *
 * 1. Host ở trong room chung nên nhận CẢ `question:started` (bản cắt đáp án) lẫn
 *    `host:question` (bản có đáp án). Chỉ xử lý bản thứ hai; bản đầu đã được
 *    mapper cố tình để rơi vào Unhandled.
 *
 * 2. Reconnect giữa một câu đang mở thì `game:state` chỉ trả bản công khai và
 *    backend KHÔNG phát lại `host:question` → mất đáp án đúng. Vì vậy đáp án được
 *    cache theo index ([answerKeysByIndex]) và UI có cờ [HostGameUiState.hasAnswerKey]
 *    để nói thật rằng không có đáp án cho câu này, thay vì hiện ô trống.
 *
 * 3. `answered` đếm theo TẮNG câu. Gói đến muộn của câu trước sẽ đè sai số liệu
 *    câu đang chạy, nên mọi gói có `index` khác index hiện tại đều bị bỏ.
 *
 * 4. Thời gian tạm dừng chỉ nằm trong RAM của server, không vào Redis. Client
 *    không tự cộng trừ hạn; khi resume, server gửi mốc mới và ta ghi đè.
 */
@HiltViewModel
class HostGameViewModel @Inject constructor(
    private val socketRepository: HostGameSocketRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Long = checkNotNull(savedStateHandle[KEY_GAME_ID])
    private var socketToken: String = savedStateHandle.get<String>(KEY_SOCKET_TOKEN).orEmpty()

    private val _uiState = MutableStateFlow(HostGameUiState())
    val uiState: StateFlow<HostGameUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HostGameEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effects: SharedFlow<HostGameEffect> = _effects.asSharedFlow()

    /**
     * Kho đáp án đúng theo index câu hỏi — xem bẫy (2).
     *
     * Giữ ngoài UiState vì đây là dữ liệu lịch sử của cả trận, không phải thứ cần
     * vẽ ra màn hình.
     */
    private val answerKeysByIndex = mutableMapOf<Int, List<String>>()
    private var connectedRosterIds: Set<Long>? = null

    private var finalResultJob: Job? = null
    private var finalResultGeneration = 0
    private var socketJob: Job? = null
    private var commandGuardJob: Job? = null
    private var startStateRefreshJob: Job? = null
    /** Retain the attempted target after retries finish; do not restart on every roster event. */
    private var startStateRefreshTarget: Long? = null
    private var tokenRefreshAttempted = false

    init {
        if (socketToken.isBlank()) {
            // Không có token thì không thể làm gì: thoát ngay thay vì ngồi "Đang kết nối..."
            // vô thời hạn.
            Timber.e("HostGame thiếu socketToken")
            viewModelScope.launch { _effects.emit(HostGameEffect.ExitGame(MESSAGE_SESSION_LOST)) }
        } else {
            connect()
        }
    }

    fun onIntent(intent: HostGameIntent) {
        when (intent) {
            HostGameIntent.Retry -> connect()
            HostGameIntent.RetryFinalResults -> loadFinalResults()
            HostGameIntent.ToggleAnswerKey -> toggleAnswerKey()
            HostGameIntent.HideAnswerKey -> _uiState.update { it.copy(isAnswerRevealed = false) }
            is HostGameIntent.AdvanceFromResults -> {
                val state = _uiState.value
                if (state.canAdvanceFromResults && state.index == intent.index) advanceQuestion()
            }
            HostGameIntent.AdvanceQuestion -> advanceQuestion()
            is HostGameIntent.ConfirmCloseQuestion -> {
                val state = _uiState.value
                if (state.canCloseClassicQuestion && state.index == intent.index &&
                    state.question?.id == intent.questionId
                ) advanceQuestion()
            }
            HostGameIntent.PauseOrResume -> pauseOrResume()
            HostGameIntent.RequestEndGame -> _uiState.update { it.copy(isEndDialogOpen = true) }
            HostGameIntent.DismissEndDialog -> _uiState.update { it.copy(isEndDialogOpen = false) }
            HostGameIntent.ConfirmEndGame -> endGame()
            HostGameIntent.LeaveGame -> leaveGame()
            HostGameIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
            HostGameIntent.NoticeShown -> _uiState.update { it.copy(notice = null) }
        }
    }

    private fun connect() {
        startStateRefreshJob?.cancel()
        startStateRefreshTarget = null
        socketJob?.cancel()
        connectedRosterIds = null
        _uiState.update {
            it.copy(
                connection = if (it.hasSnapshot) {
                    HostGameConnection.RECONNECTING
                } else {
                    HostGameConnection.CONNECTING
                },
                errorMessage = null,
                hasAnswerProgress = false,
                hasResultsLeaderboard = false,
                hasMonitoringData = false,
                isStateSynchronized = false
            )
        }
        socketJob = viewModelScope.launch {
            socketRepository.events(socketToken).collect(::onEvent)
        }
    }

    private suspend fun onEvent(event: GameEvent) {
        when (event) {
            // `lobby:join` phải gửi lại sau MỌI lần Connected, kể cả lần reconnect tự
            // động: server không tự xếp socket mới vào room cũ. Đây cũng là đường duy
            // nhất lấy lại `game:state` để dựng lại màn hình.
            GameEvent.Connected -> {
                tokenRefreshAttempted = false
                _uiState.update { it.copy(connection = HostGameConnection.CONNECTED,
                    isStateSynchronized = false, hasMonitoringData = false) }
                socketRepository.joinLobby()
            }

            is GameEvent.Disconnected -> onDisconnected(event.reason)

            is GameEvent.LobbyUpdated -> {
                updateConnectedRoster(event.lobby.players.filter { it.status == "connected" }.map { it.id }.toSet())
                _uiState.update {
                    it.copy(
                        sessionStatus = event.lobby.sessionStatus,
                        pacing = event.lobby.config.flow.pacing,
                        autoAdvance = event.lobby.config.timing.autoAdvance,
                        showCorrectAnswer = event.lobby.config.flow.showCorrectAnswer,
                        showLeaderboard = event.lobby.config.flow.showLeaderboard,
                        serverOffsetMs = offsetOf(event.lobby.serverTime, it.serverOffsetMs)
                    )
                }
            }

            is GameEvent.GameStarted -> _uiState.update {
                it.copy(
                    mode = event.mode,
                    isStateSynchronized = true,
                    pacing = event.config.flow.pacing,
                    autoAdvance = event.config.timing.autoAdvance,
                    showCorrectAnswer = event.config.flow.showCorrectAnswer,
                    showLeaderboard = event.config.flow.showLeaderboard,
                    totalQuestions = event.totalQuestions,
                    sessionStatus = SessionStatus.ACTIVE,
                    serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs)
                )
            }

            is GameEvent.Countdown -> onCountdown(event)
            is GameEvent.HostQuestionReceived -> onHostQuestion(event)
            is GameEvent.QuestionLocked -> onQuestionLocked(event)
            is GameEvent.QuestionResultsReceived -> onQuestionResults(event)
            is GameEvent.HostAnswerReceivedEvent -> onAnswerReceived(event)
            is GameEvent.HostPlayerProgressUpdated -> onPlayerProgress(event)
            is GameEvent.HostLeaderboardUpdated -> onLeaderboard(event)
            is GameEvent.StateSnapshot -> onSnapshot(event)
            is GameEvent.GameEndedEvent -> onGameEnded(event)
            is GameEvent.PlayerEliminated -> onPlayerEliminated(event)
            is GameEvent.PlayerFinishedEvent -> onPlayerFinished(event)
            is GameEvent.Failed -> onFailure(event)

            // `question:started`, `answer:received`, `leaderboard:updated`... là bản dành
            // cho người chơi. Bỏ qua CÓ Ý THỨC: xem bẫy (1).
            else -> Unit
        }
        refreshSelfPacedStartIfNeeded()
    }

    /**
     * Self-paced beginFirstQuestion does not broadcast a new phase to the Host.
     * Ask for authoritative game:state through the existing, idempotent Host lobby:join.
     * Never advance the phase locally; bounded retries cover a snapshot/countdown-timer race.
     */
    private fun selfPacedStartRefreshTarget(state: HostGameUiState): Long? {
        if (!state.isSelfPaced || state.isFinished || state.sessionStatus != SessionStatus.ACTIVE ||
            state.connection != HostGameConnection.CONNECTED || !state.isStateSynchronized) return null
        return when (state.phase) {
            GamePhase.COUNTDOWN -> state.countdownTargetEpochMs
            GamePhase.UNKNOWN -> 0L // e.g. Practice: no countdown event at all
            else -> null
        }
    }

    private fun refreshSelfPacedStartIfNeeded() {
        val target = selfPacedStartRefreshTarget(_uiState.value)
        if (target == null) {
            startStateRefreshJob?.cancel()
            startStateRefreshTarget = null
            return
        }
        if (startStateRefreshTarget == target) return
        startStateRefreshJob?.cancel()
        startStateRefreshTarget = target
        startStateRefreshJob = viewModelScope.launch {
            val wait = if (target == 0L) 0L else
                (target - System.currentTimeMillis() - _uiState.value.serverOffsetMs).coerceAtLeast(0L)
            delay(wait + 100L)
            repeat(4) {
                if (selfPacedStartRefreshTarget(_uiState.value) != target) return@launch
                socketRepository.joinLobby()
                delay(500L)
            }
            if (selfPacedStartRefreshTarget(_uiState.value) == target) {
                _uiState.update { it.copy(errorMessage =
                    "Máy chủ chưa xác nhận bắt đầu. Hãy bấm Kết nối lại để đồng bộ.") }
            }
        }
    }

    private suspend fun onDisconnected(reason: DisconnectReason) {
        if (reason != DisconnectReason.CLIENT) {
            connectedRosterIds = null
            _uiState.update { it.copy(hasAnswerProgress = false, hasResultsLeaderboard = false,
                isStateSynchronized = false, hasMonitoringData = false) }
        }
        when (reason) {
            // Chủ động rời màn thì không phải sự cố.
            DisconnectReason.CLIENT -> Unit
            // Rớt transport: Socket.IO sẽ tự reconnect; giữ snapshot cũ trên màn.
            DisconnectReason.TRANSPORT -> _uiState.update {
                it.copy(connection = HostGameConnection.RECONNECTING)
            }
            // `io server disconnect` không được Socket.IO tự retry. Ở lại màn với
            // nhãn "đang kết nối lại" sẽ treo vô hạn, nên phải thoát rõ ràng.
            DisconnectReason.SERVER_DISCONNECT -> _effects.emit(
                HostGameEffect.ExitGame(MESSAGE_SERVER_DISCONNECTED)
            )
        }
    }

    private fun onCountdown(event: GameEvent.Countdown) {
        val offset = offsetOf(event.serverTime, _uiState.value.serverOffsetMs)
        // Ưu tiên mốc tuyệt đối `startsAt`; chỉ khi thiếu mới tự cộng `seconds` vào bây
        // giờ — vào giữa lúc đang đếm thì cách tự cộng sẽ dài hơn thực tế.
        val target = event.countdown.startsAt?.let(::epochMillisOrNull)
            ?: (System.currentTimeMillis() + offset + event.countdown.seconds * 1_000L)
        _uiState.update {
            it.copy(
                phase = GamePhase.COUNTDOWN,
                hasResultsLeaderboard = false,
                isStateSynchronized = true,
                hasAnswerProgress = false,
                isQuestionClockKnown = false,
                sessionStatus = it.sessionStatus ?: SessionStatus.ACTIVE,
                countdownTargetEpochMs = target,
                deadlineEpochMs = null,
                results = null,
                lockReason = null,
                serverOffsetMs = offset,
                isSendingCommand = false
            )
        }
    }

    private fun onHostQuestion(event: GameEvent.HostQuestionReceived) {
        val hostQuestion = event.hostQuestion
        val index = hostQuestion.question.index
        val deadline = hostQuestion.endsAt?.let(::epochMillisOrNull)
        if (hostQuestion.correctAnswers.isNotEmpty()) {
            answerKeysByIndex[index] = hostQuestion.correctAnswers
        }
        _uiState.update {
            it.copy(
                phase = GamePhase.QUESTION_ACTIVE,
                sessionStatus = it.sessionStatus ?: SessionStatus.ACTIVE,
                index = index,
                totalQuestions = maxOf(hostQuestion.totalQuestions, hostQuestion.question.total),
                question = hostQuestion.question,
                correctAnswers = answerKeysByIndex[index].orEmpty(),
                hasAnswerKey = answerKeysByIndex.containsKey(index),
                // Câu mới thì ẩn lại đáp án: không được để trạng thái "đã mở" của câu
                // trước làm lộ đáp án câu đang hỏi.
                isAnswerRevealed = false,
                deadlineEpochMs = deadline,
                isQuestionClockKnown = hostQuestion.endsAt == null || deadline != null,
                isStateSynchronized = true,
                countdownTargetEpochMs = null,
                answeredCount = 0,
                activePlayers = connectedRosterIds?.size ?: 0,
                hasAnswerProgress = connectedRosterIds != null,
                hasResultsLeaderboard = false,
                results = null,
                lockReason = null,
                serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs),
                isSendingCommand = false
            )
        }
    }

    private fun onQuestionLocked(event: GameEvent.QuestionLocked) {
        if (event.index != _uiState.value.index) return
        _uiState.update {
            it.copy(
                phase = GamePhase.QUESTION_LOCKED,
                lockReason = event.reason,
                deadlineEpochMs = null,
                serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs),
                isSendingCommand = false
            )
        }
    }

    private fun onQuestionResults(event: GameEvent.QuestionResultsReceived) {
        val results = event.results
        val current = _uiState.value
        // Results from an older question must not put Classic back into the results phase.
        if (current.isClassic && (current.isFinished || results.index != current.index ||
                (results.questionId != null && current.question != null && results.questionId != current.question?.id))
        ) return
        if (results.correctAnswers.isNotEmpty()) {
            answerKeysByIndex[results.index] = results.correctAnswers
        }
        _uiState.update { state ->
            val sameQuestion = results.index == state.index
            val firstResults = !state.isShowingQuestionResults || state.results == null
            val keys = if (sameQuestion) answerKeysByIndex[results.index].orEmpty() else state.correctAnswers
            state.copy(
                phase = GamePhase.SHOWING_RESULTS,
                results = results,
                hasResultsLeaderboard = if (firstResults) false else state.hasResultsLeaderboard,
                deadlineEpochMs = if (state.isClassic) results.nextQuestionAt?.let(::epochMillisOrNull) else null,
                correctAnswers = keys,
                hasAnswerKey = if (sameQuestion) answerKeysByIndex.containsKey(results.index) else state.hasAnswerKey,
                isAnswerRevealed = if (state.isClassic && firstResults)
                    state.showCorrectAnswer == true && sameQuestion && keys.isNotEmpty()
                else state.isAnswerRevealed,
                serverOffsetMs = offsetOf(event.serverTime, state.serverOffsetMs),
                isSendingCommand = false
            )
        }
    }

    private fun onAnswerReceived(event: GameEvent.HostAnswerReceivedEvent) {
        val answer = event.answer
        // Bỏ gói của câu khác — xem bẫy (3).
        val current = _uiState.value
        if (answer.index != current.index ||
            (current.isClassic && current.phase != GamePhase.QUESTION_ACTIVE)
        ) return
        _uiState.update {
            it.copy(
                answeredCount = answer.answered,
                activePlayers = answer.activePlayers,
                hasAnswerProgress = answer.activePlayers > 0 && answer.answered in 0..answer.activePlayers,
                serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs)
            )
        }
    }

    private fun onPlayerProgress(event: GameEvent.HostPlayerProgressUpdated) {
        val progress = event.progress
        _uiState.update { state ->
            val total = progress.totalQuestions.takeIf { it > 0 } ?: state.totalQuestions
            val existing = state.leaderboard.rows.firstOrNull { it.id == progress.id }
            val answered = progress.currentQuestionIndex.coerceAtLeast(0)
            val updated = (existing ?: HostLeaderboardRow(
                rank = 0,
                id = progress.id,
                playerName = progress.playerName,
                playerScore = progress.playerScore
            )).copy(
                playerName = progress.playerName.ifBlank { existing?.playerName.orEmpty() },
                playerScore = progress.playerScore,
                answeredCount = answered,
                correctCount = progress.correctAnswersCount,
                wrongCount = (answered - progress.correctAnswersCount).coerceAtLeast(0),
                unansweredCount = (total - answered).coerceAtLeast(0),
                totalQuestions = total,
                currentQuestionIndex = progress.currentQuestionIndex,
                status = progress.status
            )
            val rows = state.leaderboard.rows
                .filterNot { it.id == progress.id }
                .plus(updated)
                .sortedByDescending { it.playerScore }
            state.copy(
                leaderboard = state.leaderboard.copy(
                    rows = rows,
                    totalQuestions = total,
                    answeredTotal = rows.sumOf { it.answeredCount }
                ),
                totalQuestions = total,
                serverOffsetMs = offsetOf(event.serverTime, state.serverOffsetMs)
            )
        }
    }

    private fun onLeaderboard(event: GameEvent.HostLeaderboardUpdated) {
        updateConnectedRoster(event.leaderboard.rows.filter { it.status == "connected" }.map { it.id }.toSet())
        _uiState.update {
            it.copy(
                leaderboard = event.leaderboard,
                hasMonitoringData = true,
                hasResultsLeaderboard = it.isShowingQuestionResults && it.isStateSynchronized &&
                    it.connection == HostGameConnection.CONNECTED,
                totalQuestions = if (event.leaderboard.totalQuestions > 0) {
                    event.leaderboard.totalQuestions
                } else {
                    it.totalQuestions
                },
                serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs)
            )
        }
    }

    /** A roster change invalidates the live pair: cumulative totals cannot repair this question's count. */
    private fun updateConnectedRoster(ids: Set<Long>) {
        val changed = connectedRosterIds != ids
        connectedRosterIds = ids
        if (changed) _uiState.update { state ->
            if (state.isClassic) state.copy(hasAnswerProgress = false) else state
        }
    }

    /** Restore only what game:state actually provides; no live counts or answer key are invented. */
    private fun onSnapshot(event: GameEvent.StateSnapshot) {
        val snapshot = event.snapshot
        _uiState.update { state ->
            val pacing = snapshot.config?.flow?.pacing ?: state.pacing
            val selfPaced = pacing == Pacing.SELF
            val deadline = if (selfPaced) null else snapshot.endsAt?.let(::epochMillisOrNull)
            val question = if (selfPaced) null else snapshot.question ?: state.question?.takeIf { it.index == snapshot.index }
            val sameQuestion = state.question?.id == question?.id && state.index == snapshot.index
            val phase = if (snapshot.phase == GamePhase.UNKNOWN) state.phase else snapshot.phase
            val classic = (snapshot.mode ?: state.mode) == GameMode.CLASSIC && !selfPaced
            val showCorrect = snapshot.config?.flow?.showCorrectAnswer ?: state.showCorrectAnswer
            val keepLocalReveal = sameQuestion && state.phase == phase && state.isStateSynchronized &&
                state.showCorrectAnswer == showCorrect
            state.copy(
                isStateSynchronized = true,
                isQuestionClockKnown = !selfPaced && snapshot.question != null &&
                    (snapshot.endsAt == null || deadline != null),
                hasAnswerProgress = !selfPaced && sameQuestion && state.isStateSynchronized && state.hasAnswerProgress,
                sessionStatus = snapshot.sessionStatus ?: state.sessionStatus,
                phase = phase,
                showCorrectAnswer = showCorrect,
                showLeaderboard = snapshot.config?.flow?.showLeaderboard ?: state.showLeaderboard,
                results = state.results?.takeIf { !selfPaced && sameQuestion && it.index == snapshot.index &&
                    (it.questionId == null || it.questionId == question?.id) },
                hasResultsLeaderboard = sameQuestion && state.isStateSynchronized && state.hasResultsLeaderboard &&
                    phase == GamePhase.SHOWING_RESULTS,
                mode = snapshot.mode ?: state.mode,
                pacing = pacing,
                autoAdvance = snapshot.config?.timing?.autoAdvance ?: state.autoAdvance,
                index = if (selfPaced) 0 else snapshot.index,
                totalQuestions = if (snapshot.totalQuestions > 0) {
                    snapshot.totalQuestions
                } else {
                    state.totalQuestions
                },
                question = question,
                correctAnswers = if (selfPaced) emptyList() else answerKeysByIndex[snapshot.index].orEmpty(),
                hasAnswerKey = !selfPaced && answerKeysByIndex.containsKey(snapshot.index),
                isAnswerRevealed = classic && if (keepLocalReveal) state.isAnswerRevealed else
                    phase == GamePhase.SHOWING_RESULTS && showCorrect == true && answerKeysByIndex[snapshot.index]?.isNotEmpty() == true,
                deadlineEpochMs = deadline,
                countdownTargetEpochMs = snapshot.countdownStartsAt?.let(::epochMillisOrNull),
                serverOffsetMs = offsetOf(event.serverTime, state.serverOffsetMs),
                isSendingCommand = false
            )
        }
        if (_uiState.value.isFinished && _uiState.value.ended == null && _uiState.value.finalResultError == null) {
            loadFinalResults()
        }
    }

    /** Socket wins; REST only fills a missing final report after a finished snapshot. */
    private fun loadFinalResults() {
        val state = _uiState.value
        if (!state.isFinished || state.ended != null || finalResultJob?.isActive == true) return
        val generation = ++finalResultGeneration
        _uiState.update { it.copy(isFinalResultLoading = true, finalResultError = null) }
        finalResultJob = viewModelScope.launch {
            val response = gameSessionRepository.getGameResults(gameId)
            if (generation != finalResultGeneration || _uiState.value.ended != null) return@launch
            when (response) {
                is Result.Success -> {
                    val result = response.data
                    if (result.sessionStatus != null && result.sessionStatus != SessionStatus.FINISHED &&
                        result.sessionStatus != SessionStatus.CANCELLED) {
                        _uiState.update { it.copy(isFinalResultLoading = false,
                            finalResultError = "Máy chủ chưa có kết quả cuối trận. Vui lòng thử lại.") }
                    } else {
                        _uiState.update { it.copy(
                            ended = GameEnded(result.leaderboard, result.perQuestion, result.reviewEnabled),
                            mode = result.mode ?: it.mode,
                            sessionStatus = result.sessionStatus ?: it.sessionStatus,
                            isFinalResultLoading = false, finalResultError = null,
                        ) }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isFinalResultLoading = false,
                    finalResultError = response.error.toUserMessage()) }
            }
        }
    }

    private fun onGameEnded(event: GameEvent.GameEndedEvent) {
        ++finalResultGeneration
        finalResultJob?.cancel()
        _uiState.update {
            it.copy(
                phase = GamePhase.FINISHED,
                sessionStatus = SessionStatus.FINISHED,
                ended = event.ended,
                isFinalResultLoading = false,
                finalResultError = null,
                deadlineEpochMs = null,
                countdownTargetEpochMs = null,
                isEndDialogOpen = false,
                isSendingCommand = false,
                serverOffsetMs = offsetOf(event.serverTime, it.serverOffsetMs)
            )
        }
    }

    private fun onPlayerEliminated(event: GameEvent.PlayerEliminated) {
        _uiState.update { state ->
            val rows = state.leaderboard.rows.map { row ->
                if (row.id == event.player.id) row.copy(status = STATUS_ELIMINATED, lives = 0) else row
            }
            state.copy(
                leaderboard = state.leaderboard.copy(rows = rows),
                notice = "${event.player.playerName} đã bị loại",
                serverOffsetMs = offsetOf(event.serverTime, state.serverOffsetMs)
            )
        }
    }

    private fun onPlayerFinished(event: GameEvent.PlayerFinishedEvent) {
        val finished = event.finished
        _uiState.update { state ->
            val existing = state.leaderboard.rows.firstOrNull { it.id == finished.id }
            val updated = (existing ?: HostLeaderboardRow(
                rank = 0,
                id = finished.id,
                playerName = finished.playerName.orEmpty(),
                playerScore = finished.playerScore
            )).copy(
                playerName = finished.playerName ?: existing?.playerName.orEmpty(),
                playerScore = finished.playerScore,
                correctCount = finished.correctAnswersCount,
                status = finished.status
            )
            val rows = state.leaderboard.rows
                .filterNot { it.id == finished.id }
                .plus(updated)
                .sortedByDescending { it.playerScore }
            state.copy(
                leaderboard = state.leaderboard.copy(rows = rows),
                serverOffsetMs = offsetOf(event.serverTime, state.serverOffsetMs)
            )
        }
    }

    /**
     * Lỗi từ server. Token hết hạn được làm mới đúng một lần cho mỗi vòng kết nối;
     * các code fatal còn lại thì thoát luôn. Khi Socket.IO cạn lượt tự thử, giữ
     * snapshot và chuyển sang trạng thái chờ người dùng bấm kết nối lại.
     */
    private suspend fun onFailure(event: GameEvent.Failed) {
        commandGuardJob?.cancel()
        if (event.code == CODE_RECONNECT_EXHAUSTED) {
            connectedRosterIds = null
            _uiState.update {
                it.copy(
                    connection = HostGameConnection.RECONNECT_FAILED,
                    hasMonitoringData = false,
                    hasAnswerProgress = false,
                    hasResultsLeaderboard = false,
                    isStateSynchronized = false,
                    isSendingCommand = false
                )
            }
            return
        }
        if (event.code == CODE_TOKEN_INVALID && !tokenRefreshAttempted) {
            tokenRefreshAttempted = true
            when (val result = gameSessionRepository.getHostToken(gameId)) {
                is Result.Success -> {
                    socketToken = result.data
                    savedStateHandle[KEY_SOCKET_TOKEN] = result.data
                    connect()
                }
                is Result.Error -> _effects.emit(HostGameEffect.ExitGame(MESSAGE_SESSION_LOST))
            }
            return
        }
        if (event.code in FATAL_CODES) {
            _effects.emit(HostGameEffect.ExitGame(messageForCode(event.code)))
            return
        }
        Timber.w("HostGame lệnh %s bị từ chối: %s", event.event, event.code)
        _uiState.update {
            it.copy(isSendingCommand = false, errorMessage = messageForCode(event.code))
        }
    }

    private fun toggleAnswerKey() {
        val state = _uiState.value
        if (!state.canRevealAnswer) {
            _uiState.update { it.copy(notice = MESSAGE_NO_ANSWER_KEY) }
            return
        }
        _uiState.update { it.copy(isAnswerRevealed = !it.isAnswerRevealed) }
    }

    private fun advanceQuestion() {
        if (!_uiState.value.canAdvance) return
        sendCommand { socketRepository.nextQuestion() }
    }

    private fun pauseOrResume() {
        val state = _uiState.value
        if (!state.canPauseOrResume) return
        val wasPaused = state.isPaused
        sendCommand {
            if (wasPaused) socketRepository.resumeGame() else socketRepository.pauseGame()
        }
    }

    private fun endGame() {
        if (!_uiState.value.canEndGame) {
            _uiState.update { it.copy(isEndDialogOpen = false) }
            return
        }
        _uiState.update { it.copy(isEndDialogOpen = false) }
        sendCommand { socketRepository.endGame() }
    }

    /**
     * Gửi một lệnh điều khiển và chặn bấm đúp trong lúc chờ.
     *
     * Các lệnh này KHÔNG có ack, nên không có tín hiệu "đã xong" để mở lại nút.
     * Thực tế cờ được mở bởi event tiếp theo (đã xử ở từng handler) hoặc bởi hạn
     * chờ này — nếu không thì một gói rơi sẽ khoá nút vĩnh viễn giữa trận.
     */
    private fun sendCommand(block: suspend () -> Unit) {
        _uiState.update { it.copy(isSendingCommand = true, errorMessage = null) }
        commandGuardJob?.cancel()
        commandGuardJob = viewModelScope.launch {
            runCatching { block() }
                .onFailure { Timber.e(it, "Gửi lệnh host thất bại") }
            delay(COMMAND_GUARD_MS)
            _uiState.update { it.copy(isSendingCommand = false) }
        }
    }

    private fun leaveGame() {
        startStateRefreshJob?.cancel()
        ++finalResultGeneration
        finalResultJob?.cancel()
        viewModelScope.launch {
            socketRepository.disconnect()
            _effects.emit(HostGameEffect.ExitGame())
        }
    }

    /**
     * Độ lệch đồng hồ máy so với server, tính lại mỗi khi có `serverTime`.
     *
     * Mọi mốc thời gian backend gửi đều tuyệt đối theo đồng hồ server, nên máy lệch
     * giờ sẽ đếm sai nếu không bù. Giữ lại giá trị cũ khi gói thiếu `serverTime`.
     */
    private fun offsetOf(serverTime: String?, current: Long): Long {
        val serverMs = serverTime?.let(::epochMillisOrNull) ?: return current
        return serverMs - System.currentTimeMillis()
    }

    private fun epochMillisOrNull(iso: String): Long? =
        runCatching { Instant.parse(iso).toEpochMilli() }
            .onFailure { Timber.w(it, "Mốc thời gian không đọc được: %s", iso) }
            .getOrNull()

    override fun onCleared() {
        super.onCleared()
        socketJob?.cancel()
        finalResultJob?.cancel()
        commandGuardJob?.cancel()
        startStateRefreshJob?.cancel()
    }

    private companion object {
        const val KEY_GAME_ID = "gameId"
        const val KEY_SOCKET_TOKEN = "socketToken"
        const val CODE_TOKEN_INVALID = "GAME_TOKEN_INVALID"
        const val CODE_RECONNECT_EXHAUSTED = "CLIENT_RECONNECT_EXHAUSTED"
        const val STATUS_ELIMINATED = "eliminated"

        /**
         * Hạn mở lại nút sau khi gửi lệnh không ack. Đủ dài để chặn bấm đúp, đủ
         * ngắn để host không cảm thấy màn hình treo.
         */
        const val COMMAND_GUARD_MS = 800L

        val FATAL_CODES = setOf(
            "GAME_TOKEN_INVALID",
            "GAME_TOKEN_WRONG_ROOM",
            "GAME_ROOM_NOT_FOUND",
            "GAME_PLAYER_NOT_FOUND"
        )

        const val MESSAGE_SESSION_LOST = "Phiên điều khiển không còn hợp lệ"
        const val MESSAGE_SERVER_DISCONNECTED = "Máy chủ đã đóng kết nối tới phòng này"
        const val MESSAGE_NO_ANSWER_KEY =
            "Chưa có đáp án cho câu này (do vừa kết nối lại giữa câu)"

        /**
         * Thông điệp cho các code riêng của giai đoạn chơi.
         *
         * Không dùng bảng chung của core:common vì các code này chỉ xuất hiện ở màn
         * này, và câu chữ cần nói đúng việc host vừa bấm.
         */
        fun messageForCode(code: String): String = when (code) {
            "GAME_ADVANCE_NOT_ALLOWED" -> "Phòng đang tự chuyển câu nên không chuyển tay được"
            "GAME_PACING_MISMATCH" -> "Chế độ chơi này không do host điều nhịp"
            "GAME_NOT_STARTED" -> "Trận chưa bắt đầu"
            "GAME_NOT_ACTIVE" -> "Trận không còn đang diễn ra"
            "GAME_NOT_PAUSED" -> "Trận không ở trạng thái tạm dừng"
            "GAME_NOT_HOST" -> "Bạn không còn là chủ phòng"
            "GAME_TOKEN_INVALID", "GAME_TOKEN_WRONG_ROOM" -> MESSAGE_SESSION_LOST
            "GAME_ROOM_NOT_FOUND" -> "Phòng không còn tồn tại"
            "CLIENT_NOT_CONNECTED" -> "Mất kết nối tới phòng, đang thử kết nối lại"
            else -> "Đã có lỗi xảy ra, vui lòng thử lại"
        }
    }
}
