package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.AnswerProgress
import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.AnsweredQuestionSnapshot
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameEnded
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

    private suspend fun TestScope.startQuestion(
        showCorrectAnswer: Boolean = true,
        showLeaderboard: ShowLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        pacing: Pacing = Pacing.HOST,
        allowAnswerLate: Boolean = false,
        autoAdvance: Boolean = true
    ) {
        runCurrent()
        socket.emit(configEvent(SessionStatus.ACTIVE, showCorrectAnswer, showLeaderboard, pacing, allowAnswerLate, autoAdvance))
        socket.emit(
            GameEvent.QuestionStarted(
                PlayerQuestionStarted(
                    question(),
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
