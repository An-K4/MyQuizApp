package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.AnswerProgress
import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.AnsweredQuestionSnapshot
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.model.PlayerAwaitingNext
import android.kma.myquizzapp.core.common.model.PlayerFinished
import android.kma.myquizzapp.core.common.model.PlayerQuestionStarted
import android.kma.myquizzapp.core.common.model.PlayerQuestionTimeout
import android.kma.myquizzapp.core.common.model.PlayerStateSnapshot
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import android.kma.myquizzapp.core.common.repository.StoredGameResult
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.game_player.domain.PlayerGameSessionUseCase
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var socket: FakePlayerGameSocketRepository
    private lateinit var results: FakeGameResultRepository
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakePlayerGameSocketRepository()
        results = FakeGameResultRepository()
        viewModel = GameViewModel(
            PlayerGameSessionUseCase(socket, results),
            SavedStateHandle(mapOf("gameId" to GAME_ID, "playerId" to PLAYER_ID, "socketToken" to TOKEN))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start layout covers join sync gap and countdown without swallowing gameplay`() {
        val initial = GameUiState()
        assertTrue(initial.usesStartCountdownLayout)
        assertTrue(initial.copy(connection = GameConnection.CONNECTED,
            isSynchronizing = true).usesStartCountdownLayout)
        assertTrue(initial.copy(phase = GamePhaseUi.Countdown(null)).usesStartCountdownLayout)
        assertFalse(initial.copy(phase = GamePhaseUi.Question).usesStartCountdownLayout)
        assertFalse(initial.copy(phase = GamePhaseUi.Results()).usesStartCountdownLayout)
        assertFalse(initial.copy(phase = GamePhaseUi.Finished).usesStartCountdownLayout)
    }

    @Test
    fun `countdown waits for server and restores pause resume deadline`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.Countdown(android.kma.myquizzapp.core.common.model.GameCountdown(
            3, "2026-10-20T04:00:03Z")))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Countdown)
        assertFalse(viewModel.uiState.value.canSubmit)
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSynchronizing)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(phase = GamePhase.COUNTDOWN,
            sessionStatus = SessionStatus.PAUSED, countdownStartsAt = null)))
        runCurrent()
        assertTrue(viewModel.uiState.value.isPaused)
        assertNull((viewModel.uiState.value.phase as GamePhaseUi.Countdown).startsAt)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(phase = GamePhase.COUNTDOWN,
            sessionStatus = SessionStatus.ACTIVE, countdownStartsAt = "2026-10-20T04:00:20Z")))
        runCurrent()
        assertFalse(viewModel.uiState.value.isPaused)
        assertFalse(viewModel.uiState.value.isSynchronizing)
        assertEquals("2026-10-20T04:00:20Z", (viewModel.uiState.value.phase as GamePhaseUi.Countdown).startsAt)
    }

    @Test
    fun `countdown without absolute target uses event seconds not hardcoded duration`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Countdown(android.kma.myquizzapp.core.common.model.GameCountdown(7),
            serverTime = "2026-10-20T04:00:00Z"))
        runCurrent()
        val target = (viewModel.uiState.value.phase as GamePhaseUi.Countdown).startsAt
        val delta = java.time.Instant.parse(target).toEpochMilli() - java.time.Instant.parse("2026-10-20T04:00:00Z").toEpochMilli()
        assertTrue(delta in 7_000L..7_100L)
        assertFalse(viewModel.uiState.value.isAnswerInputEnabled)
    }

    @Test
    fun `connected always rejoins lobby then syncs including reconnect`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(GameConnection.CONNECTED, viewModel.uiState.value.connection)
        assertEquals(1, socket.joinCalls)
        assertEquals(1, socket.syncCalls)

        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        runCurrent()
        assertEquals(GameConnection.RECONNECTING, viewModel.uiState.value.connection)
        assertTrue(viewModel.uiState.value.isInputLocked)

        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(2, socket.joinCalls)
        assertEquals(2, socket.syncCalls)
    }

    @Test
    fun `reconnect exhaustion shows retry and retry starts a fresh connection`() = runTest(dispatcher) {
        startQuestion()
        assertEquals(1, socket.eventsCalls)

        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Failed("connect_error", "CLIENT_CONNECT_FAILED"))
        runCurrent()
        assertEquals(GameConnection.RECONNECTING, viewModel.uiState.value.connection)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isInputLocked)

        socket.emit(GameEvent.Failed("connect_error", "CLIENT_RECONNECT_EXHAUSTED"))
        runCurrent()
        assertEquals(GameConnection.RECONNECT_FAILED, viewModel.uiState.value.connection)
        assertTrue(viewModel.uiState.value.isInputLocked)

        viewModel.onIntent(GameIntent.Retry)
        runCurrent()
        assertEquals(GameConnection.RECONNECTING, viewModel.uiState.value.connection)
        assertEquals(2, socket.eventsCalls)

        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(GameConnection.CONNECTED, viewModel.uiState.value.connection)
        assertEquals(1, socket.joinCalls)
        assertEquals(1, socket.syncCalls)
    }

    @Test
    fun `classic sequence reaches results without leaking hidden answer`() = runTest(dispatcher) {
        startQuestion(showCorrectAnswer = false)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Submitted)

        socket.emit(GameEvent.AnswerProgressUpdated(AnswerProgress(0, 1, 2)))
        socket.emit(GameEvent.QuestionLocked(0, QuestionLockReason.ALL_ANSWERED))
        socket.emit(
            GameEvent.QuestionResultsReceived(
                QuestionResults(0, correctAnswers = listOf("a"), stats = AnswerStats(2, mapOf("a" to 1, "b" to 1)))
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Results)
        assertEquals(QuestionOutcome.HIDDEN, state.outcome)
        assertTrue(state.results!!.correctAnswers.isEmpty())
        assertTrue(state.results.stats.distribution.isEmpty())
        assertEquals(1, state.answeredCount)
        assertEquals(2, state.activePlayers)
    }

    @Test
    fun `submit locks before ack and sends typed answer once`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<AnswerAck>>()
        socket.submitGate = gate
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        val waiting = viewModel.uiState.value
        assertTrue(waiting.phase is GamePhaseUi.Submitted)
        assertTrue(waiting.isInputLocked)
        assertTrue(waiting.isSubmitting)
        assertEquals(listOf(PlayerAnswer.SingleChoice("a")), socket.answers)

        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(1, socket.answers.size)

        gate.complete(Result.Success(AnswerAck(accepted = true)))
        runCurrent()
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.isInputLocked)
    }

    @Test
    fun `self paced answer ack shows immediate server authoritative feedback`() = runTest(dispatcher) {
        socket.submitResult = Result.Success(
            AnswerAck(
                accepted = true,
                isCorrect = true,
                scoreEarned = 800,
                totalScore = 1200,
                streak = 2,
                correctAnswers = listOf("a")
            )
        )
        startQuestion(pacing = Pacing.SELF)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Results)
        assertEquals(QuestionOutcome.CORRECT, state.outcome)
        assertEquals(listOf("a"), state.results?.correctAnswers)
        assertEquals(800, state.scoreEarned)
        assertEquals(1200, state.totalScore)
        assertEquals(2, state.streak)
        assertTrue(state.isInputLocked)
        assertFalse(state.shouldShowNextAction)
    }

    @Test
    fun `self paced hidden answer ack cannot leak grading fields`() = runTest(dispatcher) {
        socket.submitResult = Result.Success(
            AnswerAck(
                accepted = true,
                isCorrect = true,
                scoreEarned = 800,
                totalScore = 1200,
                streak = 2,
                correctAnswers = listOf("a")
            )
        )
        startQuestion(pacing = Pacing.SELF, showCorrectAnswer = false)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Results)
        assertEquals(QuestionOutcome.HIDDEN, state.outcome)
        assertTrue(state.results?.correctAnswers.orEmpty().isEmpty())
        assertNull(state.scoreEarned)
        assertNull(state.totalScore)
        assertNull(state.streak)
    }

    @Test
    fun `survival ack elimination enters eliminated phase`() = runTest(dispatcher) {
        socket.submitResult = Result.Success(
            AnswerAck(
                accepted = true,
                lives = 0,
                eliminated = true,
                isCorrect = false,
                scoreEarned = 0,
                totalScore = 500,
                streak = 0,
                correctAnswers = listOf("a")
            )
        )
        startQuestion(pacing = Pacing.SELF)
        viewModel.onIntent(GameIntent.SelectOption("b"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(GamePhaseUi.Eliminated, state.phase)
        assertEquals(0, state.lives)
        assertTrue(state.isInputLocked)
        assertFalse(state.shouldShowNextAction)
    }

    @Test
    fun `question timeout shows result and updates lives`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF)
        socket.emit(
            GameEvent.QuestionTimedOut(
                PlayerQuestionTimeout(
                    questionIndex = 0,
                    questionId = 10,
                    correctAnswers = listOf("a"),
                    lives = 2
                )
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Results)
        assertEquals(QuestionOutcome.INCORRECT, state.outcome)
        assertEquals(listOf("a"), state.results?.correctAnswers)
        assertEquals(0, state.scoreEarned)
        assertTrue(state.timedOut)
        assertFalse(state.wasLate)
        assertEquals(2, state.lives)
    }

    @Test
    fun `question timeout cannot leak hidden answer`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, showCorrectAnswer = false)
        socket.emit(
            GameEvent.QuestionTimedOut(
                PlayerQuestionTimeout(
                    questionIndex = 0,
                    correctAnswers = listOf("a"),
                    lives = 1
                )
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(QuestionOutcome.HIDDEN, state.outcome)
        assertTrue(state.results?.correctAnswers.orEmpty().isEmpty())
        assertNull(state.scoreEarned)
        assertEquals(1, state.lives)
    }

    @Test
    fun `question timeout elimination enters eliminated phase`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF)
        socket.emit(
            GameEvent.QuestionTimedOut(
                PlayerQuestionTimeout(questionIndex = 0, lives = 0, eliminated = true)
            )
        )
        runCurrent()

        assertEquals(GamePhaseUi.Eliminated, viewModel.uiState.value.phase)
        assertEquals(0, viewModel.uiState.value.lives)
    }

    @Test
    fun `player finished waits for game ended and keeps personal leaderboard`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, showLeaderboard = ShowLeaderboard.END_ONLY)
        val rows = listOf(LeaderboardRow(1, PLAYER_ID, "Kiro", 2400))
        socket.emit(
            GameEvent.PlayerFinishedEvent(
                PlayerFinished(
                    id = PLAYER_ID,
                    playerScore = 2400,
                    correctAnswersCount = 3,
                    status = "finished",
                    leaderboard = rows
                )
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(GamePhaseUi.PlayerFinished, state.phase)
        assertEquals(2400, state.totalScore)
        assertEquals(rows, state.leaderboard)
        assertTrue(state.canShowLiveLeaderboard)
        assertEquals(0, socket.disconnectCalls)
    }

    @Test
    fun `marathon deadline locks input while waiting for server finish`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF)
        socket.emit(
            GameEvent.QuestionStarted(
                PlayerQuestionStarted(
                    question = question(),
                    matchEndsAt = "2026-09-27T12:05:00Z"
                )
            )
        )
        runCurrent()

        viewModel.onIntent(GameIntent.MatchDeadlineReached)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.matchTimeExpired)
        assertTrue(state.isInputLocked)
        assertTrue(state.phase is GamePhaseUi.Question)
    }

    @Test
    fun `self paced soft deadline keeps input open when late answers are allowed`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, allowAnswerLate = true)
        viewModel.onIntent(GameIntent.DeadlineReached)
        runCurrent()

        assertFalse(viewModel.uiState.value.isInputLocked)
    }

    @Test
    fun `manual next emits once and next question confirms transition`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, autoAdvance = false)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertTrue(viewModel.uiState.value.shouldShowNextAction)

        viewModel.onIntent(GameIntent.NextQuestion)
        viewModel.onIntent(GameIntent.NextQuestion)
        runCurrent()
        assertEquals(1, socket.nextCalls)
        assertTrue(viewModel.uiState.value.isRequestingNext)

        socket.emit(GameEvent.QuestionStarted(PlayerQuestionStarted(question())))
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Question)
        assertFalse(viewModel.uiState.value.isRequestingNext)
        assertFalse(viewModel.uiState.value.shouldShowNextAction)
    }

    @Test
    fun `awaiting next restores result and next action`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, autoAdvance = false)
        socket.emit(
            GameEvent.QuestionAwaitingNext(
                PlayerAwaitingNext(
                    questionIndex = 0,
                    isCorrect = true,
                    scoreEarned = 800,
                    correctAnswers = listOf("a"),
                    playerScore = 1200,
                    lives = 2
                )
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Results)
        assertEquals(QuestionOutcome.CORRECT, state.outcome)
        assertEquals(800, state.scoreEarned)
        assertEquals(1200, state.totalScore)
        assertEquals(2, state.lives)
        assertTrue(state.shouldShowNextAction)
    }

    @Test
    fun `awaiting next cannot leak hidden grading fields`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, autoAdvance = false, showCorrectAnswer = false)
        socket.emit(
            GameEvent.QuestionAwaitingNext(
                PlayerAwaitingNext(
                    questionIndex = 0,
                    isCorrect = true,
                    scoreEarned = 800,
                    correctAnswers = listOf("a"),
                    playerScore = 1200
                )
            )
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(QuestionOutcome.HIDDEN, state.outcome)
        assertTrue(state.results?.correctAnswers.orEmpty().isEmpty())
        assertNull(state.scoreEarned)
        assertNull(state.totalScore)
        assertTrue(state.shouldShowNextAction)
    }

    @Test
    fun `manual next timeout reopens action without sending twice`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, autoAdvance = false)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        viewModel.onIntent(GameIntent.NextQuestion)
        runCurrent()

        advanceTimeBy(5_001)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isRequestingNext)
        assertTrue(state.shouldShowNextAction)
        assertEquals("Máy chủ chưa gửi câu tiếp theo, hãy thử lại.", state.errorMessage)
        assertEquals(1, socket.nextCalls)
    }

    @Test
    fun `ack uncertainty keeps input locked and requests sync`() = runTest(dispatcher) {
        socket.submitResult = Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT"))
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Submitted)
        assertTrue(state.isInputLocked)
        assertTrue(state.isConfirming)
        assertEquals(1, socket.syncCalls)
        assertEquals(1, socket.answers.size)
    }

    @Test
    fun `sync snapshot keeps submitted when server recorded answer`() = runTest(dispatcher) {
        socket.submitResult = Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT"))
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        socket.emit(snapshot(answered = true))
        runCurrent()
        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Submitted)
        assertTrue(state.isInputLocked)
        assertFalse(state.isConfirming)
        assertEquals("a", state.selectedOptionId)
    }

    @Test
    fun `sync snapshot reopens only when server did not record answer and question is active`() = runTest(dispatcher) {
        socket.submitResult = Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT"))
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()

        socket.emit(snapshot(answered = false))
        runCurrent()
        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Question)
        assertFalse(state.isInputLocked)
        assertFalse(state.isConfirming)
    }

    @Test
    fun `late active snapshot cannot pull current results backwards`() = runTest(dispatcher) {
        startQuestion()
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, correctAnswers = listOf("a"))))
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)

        socket.emit(snapshot(answered = false))
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)
        assertTrue(viewModel.uiState.value.isInputLocked)
        assertEquals(0, viewModel.uiState.value.results?.index)
    }

    @Test
    fun `pause locks answer and resume opens only unanswered active question`() = runTest(dispatcher) {
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        socket.emit(configEvent(SessionStatus.PAUSED))
        runCurrent()
        assertTrue(viewModel.uiState.value.isPaused)
        assertFalse(viewModel.uiState.value.canSubmit)

        socket.emit(configEvent(SessionStatus.ACTIVE))
        runCurrent()
        assertFalse(viewModel.uiState.value.isPaused)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `confirmed answer stays submitted across pause and resume even when snapshot omits answer`() = runTest(dispatcher) {
        startQuestion()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Submitted)

        socket.emit(snapshot(answered = false, status = SessionStatus.PAUSED))
        socket.emit(snapshot(answered = false, status = SessionStatus.ACTIVE))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.phase is GamePhaseUi.Submitted)
        assertTrue(state.isInputLocked)
        viewModel.onIntent(GameIntent.SelectOption("b"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals("a", viewModel.uiState.value.selectedOptionId)
        assertEquals(1, socket.answers.size)
    }

    @Test
    fun `leaderboard visibility follows server config`() = runTest(dispatcher) {
        startQuestion(showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS)
        val rows = listOf(LeaderboardRow(1, PLAYER_ID, "Kiro", 100))
        socket.emit(GameEvent.PlayerLeaderboardUpdated(rows))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, correctAnswers = listOf("a"))))
        runCurrent()
        assertTrue(viewModel.uiState.value.canShowLiveLeaderboard)
        assertEquals(1, viewModel.uiState.value.playerRank)

        socket.emit(configEvent(SessionStatus.ACTIVE, showLeaderboard = ShowLeaderboard.END_ONLY))
        runCurrent()
        assertFalse(viewModel.uiState.value.canShowLiveLeaderboard)
        assertTrue(viewModel.uiState.value.leaderboard.isEmpty())
        assertNull(viewModel.uiState.value.playerRank)

        socket.emit(configEvent(SessionStatus.ACTIVE, showLeaderboard = ShowLeaderboard.NEVER))
        socket.emit(GameEvent.PlayerLeaderboardUpdated(rows))
        runCurrent()
        assertTrue(viewModel.uiState.value.leaderboard.isEmpty())
    }

    @Test
    fun `game ended stores result emits navigation and disconnects`() = runTest(dispatcher) {
        runCurrent()
        val ended = GameEnded(leaderboard = listOf(LeaderboardRow(1, PLAYER_ID, "Kiro", 900)))
        viewModel.effect.test {
            socket.emit(GameEvent.GameEndedEvent(ended))
            runCurrent()
            assertEquals(GameEffect.NavigateToFinalResult(GAME_ID, PLAYER_ID), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(StoredGameResult(GAME_ID, PLAYER_ID, ended, null, TOKEN), results.get(GAME_ID))
        assertEquals(1, socket.disconnectCalls)
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Finished)
    }

    @Test
    fun `classic single tap submits once and cannot reveal even a verbose ack`() = runTest(dispatcher) {
        socket.submitResult = Result.Success(AnswerAck(accepted = true, isCorrect = true,
            scoreEarned = 950, totalScore = 950, streak = 1, correctAnswers = listOf("a")))
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        assertTrue(viewModel.uiState.value.isInputLocked)
        viewModel.onIntent(GameIntent.SelectOption("b"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(listOf(PlayerAnswer.SingleChoice("a")), socket.answers)
        assertEquals("a", viewModel.uiState.value.selectedOptionId)
        assertEquals(GamePhaseUi.Submitted, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.outcome)
        assertNull(viewModel.uiState.value.results)
        assertNull(viewModel.uiState.value.scoreEarned)
        assertNull(viewModel.uiState.value.streak)
    }

    @Test
    fun `classic rejects invalid choice and paused disconnected or expired taps`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("not-an-option"))
        assertNull(viewModel.uiState.value.selectedOptionId)
        socket.emit(configEvent(SessionStatus.PAUSED))
        runCurrent()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        socket.emit(configEvent(SessionStatus.ACTIVE))
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        runCurrent()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        assertTrue(socket.answers.isEmpty())
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.DeadlineReached)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        assertTrue(socket.answers.isEmpty())
    }

    @Test
    fun `classic multi select waits for submit and preserves server option order`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, questionType = "multiple_select")
        viewModel.onIntent(GameIntent.ToggleOption("b"))
        viewModel.onIntent(GameIntent.ToggleOption("a"))
        runCurrent()
        assertTrue(socket.answers.isEmpty())
        viewModel.onIntent(GameIntent.Submit)
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(listOf(PlayerAnswer.MultipleSelect(listOf("a", "b"))), socket.answers)
    }

    @Test
    fun `classic text answers require submit and never send blank values`() = runTest(dispatcher) {
        assertClassicTextSubmission("short_answer")
    }

    @Test
    fun `classic long text answers require submit and never send blank values`() = runTest(dispatcher) {
        assertClassicTextSubmission("long_answer")
    }

    /** Each type needs a fresh session: a submitted question identity may not reopen. */
    private suspend fun TestScope.assertClassicTextSubmission(type: String) {
        startQuestion(mode = GameMode.CLASSIC, questionType = type)
        val before = socket.answers.size
        viewModel.onIntent(GameIntent.ChangeText("   "))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(before, socket.answers.size)
        viewModel.onIntent(GameIntent.ChangeText("  câu trả lời  "))
        runCurrent()
        assertEquals(before, socket.answers.size)
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(PlayerAnswer.Text("câu trả lời"), socket.answers.last())
        assertEquals(before + 1, socket.answers.size)
    }

    @Test
    fun `classic duplicate question started cannot reopen submitted text question`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, questionType = "short_answer")
        viewModel.onIntent(GameIntent.ChangeText("câu trả lời"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(GamePhaseUi.Submitted, viewModel.uiState.value.phase)
        socket.emit(GameEvent.QuestionStarted(PlayerQuestionStarted(question().copy(questionType = "short_answer"))))
        runCurrent()
        assertEquals(GamePhaseUi.Submitted, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.isInputLocked)
        assertEquals(listOf("câu trả lời"), viewModel.uiState.value.acceptedAnswerKeys)
        viewModel.onIntent(GameIntent.ChangeText("gửi lại"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(listOf(PlayerAnswer.Text("câu trả lời")), socket.answers)
    }

    @Test
    fun `classic tap with ack timeout syncs without retrying answer`() = runTest(dispatcher) {
        socket.submitResult = Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT"))
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        assertTrue(viewModel.uiState.value.isConfirming)
        assertEquals(1, socket.syncCalls)
        viewModel.onIntent(GameIntent.SelectOption("b"))
        runCurrent()
        assertEquals(1, socket.answers.size)
        assertEquals("a", viewModel.uiState.value.selectedOptionId)
    }

    @Test
    fun `classic confirmed tap stays locked after pause resume`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        socket.emit(snapshot(answered = false, status = SessionStatus.PAUSED))
        socket.emit(snapshot(answered = false, status = SessionStatus.ACTIVE))
        runCurrent()
        viewModel.onIntent(GameIntent.SelectOption("b"))
        runCurrent()
        assertEquals(GamePhaseUi.Submitted, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.isInputLocked)
        assertEquals(1, socket.answers.size)
    }

    @Test
    fun `late classic submit error cannot contaminate a new question`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<AnswerAck>>()
        socket.submitGate = gate
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        socket.emit(GameEvent.QuestionStarted(PlayerQuestionStarted(question().copy(index = 1, id = 11))))
        runCurrent()
        gate.complete(Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT")))
        runCurrent()
        assertEquals(1, viewModel.uiState.value.question?.index)
        assertEquals(GamePhaseUi.Question, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.isInputLocked)
        assertFalse(viewModel.uiState.value.isConfirming)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(0, socket.syncCalls)
    }

    @Test
    fun `classic results before ack cannot be pulled back into confirming`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<AnswerAck>>()
        socket.submitGate = gate
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, correctAnswers = listOf("a"))))
        runCurrent()
        gate.complete(Result.Error(AppError.Api("CLIENT_ACK_TIMEOUT")))
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)
        assertFalse(viewModel.uiState.value.isConfirming)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `classic duration survives same question snapshot but not an unseen next question`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        assertEquals(30, viewModel.uiState.value.timeLimitSeconds)
        socket.emit(snapshot(answered = false))
        runCurrent()
        assertEquals(30, viewModel.uiState.value.timeLimitSeconds)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(mode = GameMode.CLASSIC,
            phase = GamePhase.QUESTION_ACTIVE, index = 1, question = question().copy(index = 1, id = 11),
            endsAt = "2026-09-15T14:00:30Z", remainingSeconds = 8)))
        runCurrent()
        assertNull(viewModel.uiState.value.timeLimitSeconds)
    }

    @Test
    fun `non classic single choice still waits for explicit submit`() = runTest(dispatcher) {
        startQuestion(pacing = Pacing.SELF, mode = GameMode.SOLO)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        assertTrue(socket.answers.isEmpty())
        assertEquals(GamePhaseUi.Question, viewModel.uiState.value.phase)
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(1, socket.answers.size)
    }

    @Test
    fun `classic server results reveal in place for two seconds then show compact summary`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        assertEquals(listOf("a"), viewModel.uiState.value.acceptedAnswerKeys)
        assertNull(viewModel.uiState.value.outcome) // ACK is not a results event.
        emitClassicResults()
        assertTrue(viewModel.uiState.value.usesClassicQuestionLayout)
        assertFalse(viewModel.uiState.value.showsClassicSummary)
        assertEquals(QuestionOutcome.CORRECT, viewModel.uiState.value.outcome)
        assertEquals(2_000L, viewModel.uiState.value.classicFeedbackRemainingMs)
        advanceTimeBy(1_999)
        runCurrent()
        assertFalse(viewModel.uiState.value.showsClassicSummary)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertFalse(viewModel.uiState.value.usesClassicQuestionLayout)
        // Even after the configured five-second window, only the server can advance.
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `classic hidden answers go directly to rank summary and both hidden skip it`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, showCorrectAnswer = false)
        emitClassicResults()
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertFalse(viewModel.uiState.value.revealsClassicFeedback)
        assertEquals(0L, viewModel.uiState.value.classicFeedbackRemainingMs)
        socket.emit(configEvent(SessionStatus.ACTIVE, showCorrectAnswer = false, showLeaderboard = ShowLeaderboard.NEVER))
        runCurrent()
        assertTrue(viewModel.uiState.value.skipsClassicSummary)
        assertFalse(viewModel.uiState.value.showsClassicSummary)
        assertTrue(viewModel.uiState.value.usesClassicQuestionLayout)
        assertTrue(viewModel.uiState.value.isInputLocked)
        assertTrue(viewModel.uiState.value.results!!.correctAnswers.isEmpty())
    }

    @Test
    fun `classic feedback pause retains remaining time and resume uses new server deadline`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        emitClassicResults()
        advanceTimeBy(400)
        runCurrent()
        assertEquals(1_600L, viewModel.uiState.value.classicFeedbackRemainingMs)
        socket.emit(classicResultsSnapshot(SessionStatus.PAUSED, endsAt = null))
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1_600L, viewModel.uiState.value.classicFeedbackRemainingMs)
        assertFalse(viewModel.uiState.value.showsClassicSummary)
        val resumedDeadline = "2026-10-20T04:00:10Z"
        socket.emit(classicResultsSnapshot(SessionStatus.ACTIVE, endsAt = resumedDeadline))
        runCurrent()
        assertEquals(resumedDeadline, viewModel.uiState.value.endsAt)
        advanceTimeBy(1_599)
        runCurrent()
        assertFalse(viewModel.uiState.value.showsClassicSummary)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(viewModel.uiState.value.showsClassicSummary)
    }

    @Test
    fun `classic reconnect does not resume feedback timer until snapshot arrives`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        emitClassicResults()
        advanceTimeBy(400)
        runCurrent()
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSynchronizing)
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(1_600L, viewModel.uiState.value.classicFeedbackRemainingMs)
        socket.emit(classicResultsSnapshot())
        runCurrent()
        advanceTimeBy(1_600)
        runCurrent()
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertFalse(viewModel.uiState.value.isSynchronizing)
    }

    @Test
    fun `classic next question cancels pending feedback and clears previous result state`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        emitClassicResults()
        advanceTimeBy(400)
        runCurrent()
        socket.emit(GameEvent.QuestionStarted(PlayerQuestionStarted(question().copy(index = 1, id = 11))))
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(GamePhaseUi.Question, state.phase)
        assertEquals(11L, state.question?.id)
        assertFalse(state.showsClassicSummary)
        assertEquals(0L, state.classicFeedbackRemainingMs)
        assertNull(state.results)
        assertNull(state.acceptedAnswerKeys)
        assertNull(state.selectedOptionId)
        assertFalse(state.isResultsLeaderboardFresh)
    }

    @Test
    fun `classic results cannot grade pending submission but late accepted ack can confirm without rewinding phase`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<AnswerAck>>()
        socket.submitGate = gate
        startQuestion(mode = GameMode.CLASSIC)
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        emitClassicResults()
        assertEquals(QuestionOutcome.HIDDEN, viewModel.uiState.value.outcome)
        assertEquals(ClassicResultKind.UNKNOWN, classicResultKind(viewModel.uiState.value))
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        gate.complete(Result.Success(AnswerAck(accepted = true)))
        runCurrent()
        assertEquals(QuestionOutcome.CORRECT, viewModel.uiState.value.outcome)
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertFalse(viewModel.uiState.value.isConfirming)
    }

    @Test
    fun `classic editable draft is not a confirmed answer`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, questionType = "multiple_select")
        viewModel.onIntent(GameIntent.ToggleOption("a"))
        emitClassicResults()
        assertTrue(socket.answers.isEmpty())
        assertNull(viewModel.uiState.value.acceptedAnswerKeys)
        assertEquals(QuestionOutcome.HIDDEN, viewModel.uiState.value.outcome)
        assertEquals(ClassicResultKind.UNANSWERED, classicResultKind(viewModel.uiState.value))
    }

    @Test
    fun `classic summary waits for current result leaderboard instead of reusing previous rank`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        val oldRows = listOf(LeaderboardRow(1, PLAYER_ID, "Kiro", 100))
        socket.emit(GameEvent.PlayerLeaderboardUpdated(oldRows)) // Before results is not current evidence.
        runCurrent()
        emitClassicResults()
        assertFalse(viewModel.uiState.value.isResultsLeaderboardFresh)
        assertNull(viewModel.uiState.value.playerRank)
        val currentRows = listOf(LeaderboardRow(2, PLAYER_ID, "Kiro", 200))
        socket.emit(GameEvent.PlayerLeaderboardUpdated(currentRows))
        runCurrent()
        assertTrue(viewModel.uiState.value.isResultsLeaderboardFresh)
        assertEquals(2, viewModel.uiState.value.playerRank)
        socket.emit(GameEvent.QuestionStarted(PlayerQuestionStarted(question().copy(index = 1, id = 11))))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(1, questionId = 11, correctAnswers = listOf("a"))))
        runCurrent()
        assertFalse(viewModel.uiState.value.isResultsLeaderboardFresh)
        assertTrue(viewModel.uiState.value.leaderboard.isEmpty())
        assertNull(viewModel.uiState.value.playerScore)
    }

    @Test
    fun `classic fresh reconnect to results does not invent missing grading details`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(classicResultsSnapshot().let { it.copy(snapshot = it.snapshot.copy(mode = GameMode.CLASSIC,
            leaderboard = listOf(LeaderboardRow(2, PLAYER_ID, "Kiro", 200)))) })
        runCurrent()
        val state = viewModel.uiState.value
        assertTrue(state.showsClassicSummary)
        assertTrue((state.phase as GamePhaseUi.Results).restoredWithoutDetails)
        assertEquals(ClassicResultKind.UNKNOWN, classicResultKind(state))
        assertNull(state.results)
        assertTrue(state.isResultsLeaderboardFresh)
        assertEquals(200, state.playerScore)
    }

    @Test
    fun `classic snapshot for unseen next question clears draft accepted keys and old results`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, questionType = "long_answer")
        viewModel.onIntent(GameIntent.ChangeText("previous answer"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        emitClassicResults()
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE, index = 1, question = question().copy(index = 1, id = 11))))
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(GamePhaseUi.Question, state.phase)
        assertEquals("", state.textAnswer)
        assertTrue(state.selectedOptionIds.isEmpty())
        assertNull(state.selectedOptionId)
        assertNull(state.acceptedAnswerKeys)
        assertFalse(state.hasAnswerAttempt)
        assertNull(state.results)
        assertNull(state.outcome)
    }

    @Test
    fun `classic wrong question id result cannot start feedback or expose keys`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, questionId = 999, correctAnswers = listOf("a"))))
        runCurrent()
        assertEquals(GamePhaseUi.Question, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.results)
        assertEquals(0L, viewModel.uiState.value.classicFeedbackRemainingMs)
    }

    @Test
    fun `classic game ended preempts inline feedback and navigates immediately`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        emitClassicResults()
        advanceTimeBy(200)
        runCurrent()
        viewModel.effect.test {
            socket.emit(GameEvent.GameEndedEvent(GameEnded(leaderboard = emptyList())))
            runCurrent()
            assertEquals(GameEffect.NavigateToFinalResult(GAME_ID, PLAYER_ID), awaitItem())
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(GamePhaseUi.Finished, viewModel.uiState.value.phase)
            assertFalse(viewModel.uiState.value.showsClassicSummary)
            expectNoEvents()
        }
    }

    @Test
    fun `finished reconnect snapshot navigates once for REST recovery`() = runTest(dispatcher) {
        runCurrent()
        viewModel.effect.test {
            val finished = GameEvent.StateSnapshot(GameSnapshot(phase = GamePhase.FINISHED))
            socket.emit(finished)
            runCurrent()
            assertEquals(GameEffect.NavigateToFinalResult(GAME_ID, PLAYER_ID), awaitItem())
            socket.emit(finished)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `N24_5 private result overrides lost ACK inference and survives late accepted ACK`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.submitGate = CompletableDeferred()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        emitClassicResults()
        socket.emit(personalEvent().copy(result = personalEvent().result.copy(isCorrect = false, scoreEarned = 0, streak = 0)))
        runCurrent()
        assertEquals(ClassicResultKind.INCORRECT, classicResultKind(viewModel.uiState.value))
        val remaining = viewModel.uiState.value.classicFeedbackRemainingMs
        socket.submitGate!!.complete(Result.Success(AnswerAck(accepted = true, isCorrect = true, scoreEarned = 9999, streak = 99)))
        runCurrent()
        assertEquals(ClassicResultKind.INCORRECT, classicResultKind(viewModel.uiState.value))
        assertEquals(0, viewModel.uiState.value.scoreEarned)
        assertEquals(0, viewModel.uiState.value.streak)
        assertEquals(remaining, viewModel.uiState.value.classicFeedbackRemainingMs)
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)
    }

    @Test
    fun `N24_5 private before common survives locked and duplicate events`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(personalEvent())
        socket.emit(GameEvent.QuestionLocked(0, QuestionLockReason.ALL_ANSWERED))
        runCurrent()
        assertTrue(viewModel.uiState.value.phase is GamePhaseUi.Results)
        emitClassicResults()
        socket.emit(personalEvent())
        runCurrent()
        assertEquals(3, viewModel.uiState.value.streak)
        assertEquals(3420, viewModel.uiState.value.totalScore)
        assertEquals(0L, viewModel.uiState.value.classicFeedbackRemainingMs)
        assertEquals(0, socket.nextCalls)
        assertEquals(0, socket.syncCalls)
    }

    @Test
    fun `N24_5 reconnect private snapshot restores summary without keys and shared null keeps it`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        runCurrent()
        val syncCount = socket.syncCalls
        val restored = classicResultsSnapshot().let { it.copy(snapshot = it.snapshot.copy(personalResult = personalEvent().result)) }
        socket.emit(restored)
        runCurrent()
        assertEquals(ClassicResultKind.CORRECT, classicResultKind(viewModel.uiState.value))
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertNull(viewModel.uiState.value.results)
        assertNull(viewModel.uiState.value.acceptedAnswerKeys)
        socket.emit(classicResultsSnapshot(SessionStatus.PAUSED, endsAt = null))
        runCurrent()
        assertEquals(3, viewModel.uiState.value.streak)
        assertNull(viewModel.uiState.value.endsAt)
        assertEquals(syncCount, socket.syncCalls)
        socket.emit(personalEvent())
        runCurrent()
        assertNull(viewModel.uiState.value.endsAt)
        assertTrue(viewModel.uiState.value.isPaused)
    }

    @Test
    fun `N24_5 snapshot resume timing cannot be overwritten by old private or common event`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        emitClassicResults()
        socket.emit(personalEvent())
        socket.emit(classicResultsSnapshot(SessionStatus.PAUSED, null).copy(serverTime = "2026-10-20T04:00:02Z"))
        socket.emit(classicResultsSnapshot(endsAt = "2026-10-20T04:00:12Z").copy(serverTime = "2026-10-20T04:00:03Z"))
        runCurrent()
        socket.emit(personalEvent())
        emitClassicResults()
        assertEquals("2026-10-20T04:00:12Z", viewModel.uiState.value.endsAt)
        assertFalse(viewModel.uiState.value.isPaused)
        assertEquals(3, viewModel.uiState.value.streak)
    }

    @Test
    fun `N24_5 reveal disabled clears personal cache and cannot resurrect it on reenable`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(personalEvent())
        runCurrent()
        socket.emit(classicResultsSnapshot().let { it.copy(snapshot = it.snapshot.copy(config = GameConfig(
            flow = GameConfig.Flow(showCorrectAnswer = false)))) })
        socket.emit(personalEvent())
        socket.emit(GameEvent.PlayerLeaderboardUpdated(listOf(LeaderboardRow(id = PLAYER_ID, playerName = "Player", playerScore = 999, rank = 1))))
        runCurrent()
        val hidden = viewModel.uiState.value
        assertNull(hidden.personalResult)
        assertNull(hidden.scoreEarned)
        assertNull(hidden.totalScore)
        assertNull(hidden.streak)
        assertNull(hidden.playerRank)
        assertFalse(hidden.canShowClassicRank)
        socket.emit(classicResultsSnapshot())
        runCurrent()
        assertNull(viewModel.uiState.value.visiblePersonalResult)
    }

    @Test
    fun `N24_5 wrong identity ignored and unseen new question clears all personal grading`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(personalEvent().copy(result = personalEvent().result.copy(questionId = 999)))
        socket.emit(personalEvent().copy(result = personalEvent().result.copy(index = 1)))
        runCurrent()
        assertNull(viewModel.uiState.value.personalResult)
        socket.emit(personalEvent())
        runCurrent()
        socket.emit(configEvent(SessionStatus.ACTIVE).let { it.copy(snapshot = it.snapshot.copy(index = 1,
            question = question().copy(index = 1, id = 11))) })
        runCurrent()
        assertNull(viewModel.uiState.value.personalResult)
        assertNull(viewModel.uiState.value.streak)
        assertNull(viewModel.uiState.value.totalScore)
        socket.emit(personalEvent())
        socket.emit(classicResultsSnapshot())
        runCurrent()
        assertEquals(11L, viewModel.uiState.value.question!!.id)
        assertNull(viewModel.uiState.value.personalResult)
    }

    @Test
    fun `N24_5 personal summary is independent of hidden board but never affects self paced ACK`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC, showLeaderboard = ShowLeaderboard.NEVER)
        socket.emit(personalEvent())
        runCurrent()
        assertEquals(3420, viewModel.uiState.value.totalScore)
        assertTrue(viewModel.uiState.value.showsClassicSummary)
        assertFalse(viewModel.uiState.value.canShowClassicRank)
        assertEquals(0, socket.nextCalls)
        startQuestion(mode = GameMode.SOLO, pacing = Pacing.SELF)
        socket.emit(personalEvent())
        runCurrent()
        assertNull(viewModel.uiState.value.personalResult)
        socket.submitResult = Result.Success(AnswerAck(true, isCorrect = true, scoreEarned = 100, totalScore = 100, streak = 1))
        viewModel.onIntent(GameIntent.SelectOption("a"))
        viewModel.onIntent(GameIntent.Submit)
        runCurrent()
        assertEquals(100, viewModel.uiState.value.scoreEarned)
        assertEquals(1, viewModel.uiState.value.streak)
    }

    @Test
    fun `N24_5 finished snapshot preempts private summary and stale events cannot navigate twice`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(personalEvent())
        runCurrent()
        viewModel.effect.test {
            socket.emit(GameEvent.StateSnapshot(GameSnapshot(phase = GamePhase.FINISHED)))
            runCurrent()
            assertEquals(GameEffect.NavigateToFinalResult(GAME_ID, PLAYER_ID), awaitItem())
            socket.emit(personalEvent())
            socket.emit(classicResultsSnapshot())
            socket.emit(GameEvent.StateSnapshot(GameSnapshot(phase = GamePhase.FINISHED)))
            runCurrent()
            assertEquals(GamePhaseUi.Finished, viewModel.uiState.value.phase)
            assertNull(viewModel.uiState.value.personalResult)
            assertNull(viewModel.uiState.value.totalScore)
            expectNoEvents()
        }
    }

    @Test
    fun `N24_5 server unanswered defeats late ACK and never creates accepted selection`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.submitGate = CompletableDeferred()
        viewModel.onIntent(GameIntent.SelectOption("a"))
        runCurrent()
        socket.emit(personalEvent().copy(result = personalEvent().result.copy(answered = false,
            isCorrect = null, scoreEarned = 0, totalScore = 0, streak = 0)))
        runCurrent()
        socket.submitGate!!.complete(Result.Success(AnswerAck(accepted = true)))
        runCurrent()
        assertEquals(ClassicResultKind.UNANSWERED, classicResultKind(viewModel.uiState.value))
        assertFalse(viewModel.uiState.value.hasAnswerAttempt)
        assertNull(viewModel.uiState.value.acceptedAnswerKeys)
        assertEquals(0, viewModel.uiState.value.totalScore)
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `N24_5 stale leaderboard cannot replace current personal total or provide current rank`() = runTest(dispatcher) {
        startQuestion(mode = GameMode.CLASSIC)
        socket.emit(personalEvent())
        socket.emit(GameEvent.PlayerLeaderboardUpdated(
            listOf(LeaderboardRow(1, PLAYER_ID, "Player", 999)), "2026-10-20T03:59:59Z"))
        runCurrent()
        assertNull(viewModel.uiState.value.playerRank)
        assertEquals(3420, viewModel.uiState.value.totalScore)
        socket.emit(GameEvent.PlayerLeaderboardUpdated(
            listOf(LeaderboardRow(2, PLAYER_ID, "Player", 3500)), "2026-10-20T04:00:01Z"))
        runCurrent()
        assertEquals(2, viewModel.uiState.value.playerRank)
        assertEquals(3420, viewModel.uiState.value.visiblePersonalResult!!.totalScore)
    }

    @Test
    fun `N24_5 snapshot private result must also match the enclosing snapshot index`() = runTest(dispatcher) {
        runCurrent()
        val malformed = classicResultsSnapshot().let { it.copy(snapshot = it.snapshot.copy(mode = GameMode.CLASSIC,
            index = 1, personalResult = personalEvent().result)) }
        socket.emit(malformed)
        runCurrent()
        assertNull(viewModel.uiState.value.personalResult)
        assertNull(viewModel.uiState.value.streak)
    }

    private fun personalEvent() = GameEvent.PersonalQuestionResultsReceived(
        android.kma.myquizzapp.core.common.model.PersonalQuestionResult(0, 10, true, true, 950, 3420, 3,
            "2026-10-20T04:00:05Z", "2026-10-20T04:00:00Z"))

    private suspend fun TestScope.emitClassicResults() {
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, questionId = 10,
            correctAnswers = listOf("a"), nextQuestionAt = "2026-10-20T04:00:05Z")))
        runCurrent()
    }

    private fun classicResultsSnapshot(
        status: SessionStatus = SessionStatus.ACTIVE,
        endsAt: String? = "2026-10-20T04:00:05Z"
    ): GameEvent.StateSnapshot {
        val base = configEvent(status)
        return base.copy(snapshot = base.snapshot.copy(phase = GamePhase.SHOWING_RESULTS,
            endsAt = endsAt, remainingSeconds = 5))
    }

    private suspend fun TestScope.startQuestion(
        showCorrectAnswer: Boolean = true,
        showLeaderboard: ShowLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        pacing: Pacing = Pacing.HOST,
        allowAnswerLate: Boolean = false,
        autoAdvance: Boolean = true,
        mode: GameMode? = null,
        questionType: String = "multiple_choice"
    ) {
        runCurrent()
        val config = configEvent(SessionStatus.ACTIVE, showCorrectAnswer, showLeaderboard, pacing, allowAnswerLate, autoAdvance)
        socket.emit(config.copy(snapshot = config.snapshot.copy(mode = mode)))
        socket.emit(
            GameEvent.QuestionStarted(
                PlayerQuestionStarted(
                    question().copy(questionType = questionType),
                    timeLimitSeconds = 30,
                    endsAt = "2026-09-15T14:00:30Z",
                    allowAnswerLate = allowAnswerLate
                )
            )
        )
        runCurrent()
    }

    private fun configEvent(
        status: SessionStatus,
        showCorrectAnswer: Boolean = true,
        showLeaderboard: ShowLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        pacing: Pacing = Pacing.HOST,
        allowAnswerLate: Boolean = false,
        autoAdvance: Boolean = true
    ) = GameEvent.StateSnapshot(
        GameSnapshot(
            sessionStatus = status,
            phase = GamePhase.QUESTION_ACTIVE,
            config = GameConfig(
                timing = GameConfig.Timing(autoAdvance = autoAdvance),
                flow = GameConfig.Flow(
                    pacing = pacing,
                    showCorrectAnswer = showCorrectAnswer,
                    showLeaderboard = showLeaderboard,
                    allowAnswerLate = allowAnswerLate
                )
            ),
            index = 0,
            question = question()
        )
    )

    private fun snapshot(
        answered: Boolean,
        status: SessionStatus = SessionStatus.ACTIVE
    ) = GameEvent.StateSnapshot(
        GameSnapshot(
            sessionStatus = status,
            phase = GamePhase.QUESTION_ACTIVE,
            config = GameConfig(),
            index = 0,
            totalQuestions = 2,
            question = question(),
            player = PlayerStateSnapshot(
                id = PLAYER_ID,
                playerName = "Kiro",
                status = "connected",
                answeredQuestions = if (answered) {
                    listOf(AnsweredQuestionSnapshot(questionId = 10, questionIndex = 0, answerKeys = listOf("a")))
                } else emptyList()
            )
        )
    )

    private fun question() = PublicQuestion(
        index = 0,
        total = 2,
        id = 10,
        questionType = "multiple_choice",
        questionText = "2 + 2?",
        answerOptions = listOf(PublicAnswerOption("a", "4"), PublicAnswerOption("b", "5"))
    )

    private companion object {
        const val GAME_ID = 101L
        const val PLAYER_ID = 202L
        const val TOKEN = "socket-token"
    }
}

private class FakePlayerGameSocketRepository : PlayerGameSocketRepository {
    private val eventFlow = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    var eventsCalls = 0
    var joinCalls = 0
    var syncCalls = 0
    var disconnectCalls = 0
    var nextCalls = 0
    val answers = mutableListOf<PlayerAnswer>()
    var submitResult: Result<AnswerAck> = Result.Success(AnswerAck(accepted = true))
    var submitGate: CompletableDeferred<Result<AnswerAck>>? = null

    override fun events(socketToken: String): Flow<GameEvent> {
        eventsCalls++
        return eventFlow
    }
    suspend fun emit(event: GameEvent) { eventFlow.emit(event) }
    override suspend fun joinLobby() { joinCalls++ }
    override suspend fun disconnect() { disconnectCalls++ }
    override suspend fun leaveLobby() = Unit
    override suspend fun requestNextQuestion() { nextCalls++ }
    override suspend fun sync() { syncCalls++ }
    override suspend fun submitAnswer(answer: PlayerAnswer): Result<AnswerAck> {
        answers += answer
        return submitGate?.await() ?: submitResult
    }
}

private class FakeGameResultRepository : GameResultRepository {
    private val values = mutableMapOf<Long, StoredGameResult>()
    override fun save(value: StoredGameResult) { values[value.gameId] = value }
    override fun get(gameId: Long): StoredGameResult? = values[gameId]
    override fun clear(gameId: Long) { values.remove(gameId) }
}
