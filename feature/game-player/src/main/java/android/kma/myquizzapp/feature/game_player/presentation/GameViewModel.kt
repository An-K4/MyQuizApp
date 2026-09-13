package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.PlayerAnswer
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
    private val playerId: Long = checkNotNull(savedStateHandle["playerId"])
    private val socketToken: String = checkNotNull(savedStateHandle["socketToken"])
    private var eventJob: Job? = null
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = Channel<GameEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init { connect() }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.SelectOption -> _uiState.update { if (it.isInputLocked) it else it.copy(selectedOptionId = intent.id) }
            is GameIntent.ToggleOption -> _uiState.update { state ->
                if (state.isInputLocked) state else state.copy(
                    selectedOptionIds = state.selectedOptionIds.toMutableSet().apply {
                        if (!add(intent.id)) remove(intent.id)
                    }
                )
            }
            is GameIntent.ChangeText -> _uiState.update { if (it.isInputLocked) it else it.copy(textAnswer = intent.value) }
            GameIntent.Submit -> submit()
            GameIntent.Retry -> connect()
            GameIntent.Sync -> viewModelScope.launch { session.sync() }
            GameIntent.Leave -> viewModelScope.launch { exit(null) }
            GameIntent.DeadlineReached -> _uiState.update { state ->
                if (state.phase == GamePhaseUi.Question) state.copy(isInputLocked = true) else state
            }
            GameIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun connect() {
        eventJob?.cancel()
        _uiState.update { it.copy(connection = if (it.question == null) GameConnection.CONNECTING else GameConnection.RECONNECTING) }
        eventJob = viewModelScope.launch { session.events(socketToken).collect(::onEvent) }
    }

    private suspend fun onEvent(event: GameEvent) {
        updateOffset(event.serverTimeOrNull())
        when (event) {
            GameEvent.Connected -> {
                _uiState.update { it.copy(connection = GameConnection.CONNECTED) }
                session.joinAndSync()
            }
            is GameEvent.Disconnected -> when (event.reason) {
                DisconnectReason.TRANSPORT -> _uiState.update { it.copy(connection = GameConnection.RECONNECTING, isInputLocked = true) }
                DisconnectReason.SERVER_DISCONNECT -> exit("Máy chủ đã đóng kết nối tới phòng này.")
                DisconnectReason.CLIENT -> Unit
            }
            is GameEvent.Countdown -> _uiState.update {
                it.copy(phase = GamePhaseUi.Countdown(event.countdown.startsAt), question = null, isInputLocked = true)
            }
            is GameEvent.QuestionStarted -> _uiState.update {
                it.copy(
                    phase = GamePhaseUi.Question,
                    question = event.started.question,
                    selectedOptionId = null,
                    selectedOptionIds = emptySet(),
                    textAnswer = "",
                    endsAt = event.started.endsAt,
                    remainingSeconds = event.started.remainingSeconds,
                    lives = event.started.lives,
                    isInputLocked = false,
                    isSubmitting = false,
                    isConfirming = false,
                    results = null
                )
            }
            is GameEvent.QuestionLocked -> _uiState.update { state ->
                if (state.question?.index != event.index) state else state.copy(phase = GamePhaseUi.Locked, isInputLocked = true)
            }
            is GameEvent.QuestionResultsReceived -> _uiState.update { state ->
                if (state.question?.index != event.results.index) state else state.copy(
                    phase = GamePhaseUi.Results(), results = event.results, isInputLocked = true, isSubmitting = false, isConfirming = false
                )
            }
            is GameEvent.StateSnapshot -> restore(event.snapshot)
            is GameEvent.GameEndedEvent -> _uiState.update { it.copy(phase = GamePhaseUi.Finished, isInputLocked = true) }
            is GameEvent.PlayerEliminated -> if (event.player.id == playerId) {
                _uiState.update { it.copy(phase = GamePhaseUi.Finished, isInputLocked = true, errorMessage = "Bạn đã bị loại khỏi trận.") }
            }
            is GameEvent.Failed -> onFailure(event.code)
            else -> Unit // Host-only/lobby/optional player events are intentionally ignored here.
        }
    }

    private fun restore(snapshot: GameSnapshot) {
        val answered = snapshot.player?.answerFor(snapshot.index)
        _uiState.update { old ->
            val phase = when (snapshot.phase) {
                GamePhase.COUNTDOWN -> GamePhaseUi.Countdown(snapshot.countdownStartsAt)
                GamePhase.QUESTION_ACTIVE -> if (answered != null) GamePhaseUi.Submitted else GamePhaseUi.Question
                GamePhase.QUESTION_LOCKED -> GamePhaseUi.Locked
                GamePhase.SHOWING_RESULTS -> GamePhaseUi.Results(restoredWithoutDetails = old.results?.index != snapshot.index)
                GamePhase.FINISHED -> GamePhaseUi.Finished
                GamePhase.UNKNOWN -> old.phase
            }
            old.copy(
                connection = GameConnection.CONNECTED,
                phase = phase,
                question = snapshot.question ?: old.question,
                endsAt = snapshot.endsAt,
                remainingSeconds = snapshot.remainingSeconds,
                lives = snapshot.player?.lives ?: old.lives,
                selectedOptionId = answered?.answerKeys?.singleOrNull() ?: old.selectedOptionId,
                selectedOptionIds = answered?.answerKeys?.toSet() ?: old.selectedOptionIds,
                textAnswer = answered?.answerKeys?.singleOrNull() ?: old.textAnswer,
                isInputLocked = snapshot.phase != GamePhase.QUESTION_ACTIVE || answered != null,
                isSubmitting = false,
                isConfirming = false
            )
        }
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
                is Result.Success -> _uiState.update {
                    it.copy(isSubmitting = false, isConfirming = false, lives = result.data.lives ?: it.lives)
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

    private suspend fun onFailure(code: String) {
        val message = AppError.Api(code).toUserMessage()
        if (code in FATAL_CODES) exit(message) else _uiState.update { it.copy(errorMessage = message) }
    }

    private fun updateOffset(serverTime: String?) {
        val serverMs = serverTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: return
        _uiState.update { it.copy(serverOffsetMs = serverMs - System.currentTimeMillis()) }
    }

    private fun GameEvent.serverTimeOrNull(): String? = when (this) {
        is GameEvent.Countdown -> serverTime
        is GameEvent.QuestionStarted -> serverTime
        is GameEvent.QuestionLocked -> serverTime
        is GameEvent.QuestionResultsReceived -> serverTime
        is GameEvent.StateSnapshot -> serverTime
        is GameEvent.GameStarted -> serverTime
        is GameEvent.GameEndedEvent -> serverTime
        is GameEvent.PlayerEliminated -> serverTime
        else -> null
    }

    private suspend fun exit(message: String?) {
        // Có thể được gọi ngay bên trong collector; phát effect trước khi hủy job.
        _effect.send(GameEffect.Exit(message))
        session.disconnect()
        eventJob?.cancel()
    }

    private companion object {
        val FATAL_CODES = setOf("GAME_TOKEN_INVALID", "GAME_TOKEN_WRONG_ROOM", "GAME_ROOM_NOT_FOUND", "GAME_PLAYER_NOT_FOUND")
    }
}
