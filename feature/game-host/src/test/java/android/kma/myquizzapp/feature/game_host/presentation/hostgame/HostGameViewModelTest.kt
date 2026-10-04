package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.EliminatedPlayer
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameResults
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.HostAnswerReceived
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.HostPlayerProgress
import android.kma.myquizzapp.core.common.model.HostQuestion
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PlayerFinished
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HostGameViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var socket: FakeHostGameSocketRepository
    private lateinit var sessions: FakeGameSessionRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: HostGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakeHostGameSocketRepository()
        sessions = FakeGameSessionRepository()
        savedStateHandle = SavedStateHandle(
            mapOf("gameId" to 1L, "socketToken" to "host-token")
        )
        viewModel = HostGameViewModel(socket, sessions, savedStateHandle)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `connected rejoins host room after every reconnect`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(2, socket.joinCalls)
        assertEquals(HostGameConnection.CONNECTED, viewModel.uiState.value.connection)
    }

    @Test
    fun `reconnect exhaustion exposes retry and retry starts a fresh connection`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed("connect", "CLIENT_RECONNECT_EXHAUSTED"))
        runCurrent()
        assertEquals(HostGameConnection.RECONNECT_FAILED, viewModel.uiState.value.connection)

        viewModel.onIntent(HostGameIntent.Retry)
        runCurrent()
        assertEquals(HostGameConnection.CONNECTING, viewModel.uiState.value.connection)
        assertEquals(listOf("host-token", "host-token"), socket.tokens)
    }

    @Test
    fun `invalid host token is refreshed before reconnecting`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed("connect", "GAME_TOKEN_INVALID"))
        runCurrent()

        assertEquals(1, sessions.hostTokenCalls)
        assertEquals("new-host-token", savedStateHandle.get<String>("socketToken"))
        assertEquals(listOf("host-token", "new-host-token"), socket.tokens)
    }

    @Test
    fun `server disconnect exits instead of waiting forever`() = runTest(dispatcher) {
        runCurrent()
        viewModel.effects.test {
            socket.emit(GameEvent.Disconnected(DisconnectReason.SERVER_DISCONNECT))
            runCurrent()
            assertEquals(HostGameEffect.ExitGame("Máy chủ đã đóng kết nối tới phòng này"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `late answer progress from previous question is ignored`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(hostQuestion(index = 1, answers = listOf("b")))
        socket.emit(GameEvent.HostAnswerReceivedEvent(HostAnswerReceived(0, 2, 2, 7, "Late", true)))
        runCurrent()
        assertEquals(0, viewModel.uiState.value.answeredCount)
        assertEquals(0, viewModel.uiState.value.activePlayers)
    }

    @Test
    fun `new question hides previously revealed answer`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(hostQuestion(0, listOf("a")))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ToggleAnswerKey)
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        socket.emit(hostQuestion(1, listOf("b")))
        runCurrent()
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(listOf("b"), viewModel.uiState.value.correctAnswers)
    }

    @Test
    fun `fresh reconnect snapshot does not invent host answer key`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            index = 0,
            totalQuestions = 2,
            question = question(0)
        )))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasAnswerKey)
        assertTrue(viewModel.uiState.value.correctAnswers.isEmpty())
    }

    @Test
    fun `self paced snapshot ignores fake shared question and deadline`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            mode = GameMode.SURVIVAL,
            config = GameConfig(
                timing = GameConfig.Timing(autoAdvance = false),
                flow = GameConfig.Flow(pacing = Pacing.SELF, lives = 3)
            ),
            index = 4,
            totalQuestions = 10,
            question = question(1),
            endsAt = "2026-09-10T14:02:00.000Z"
        )))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isSelfPaced)
        assertEquals(GameMode.SURVIVAL, state.mode)
        assertEquals(0, state.index)
        assertEquals(null, state.question)
        assertEquals(null, state.deadlineEpochMs)
        assertFalse(state.isManualAdvanceVisible)
        assertFalse(state.canRevealAnswer)
    }

    @Test
    fun `self paced progress merges into baseline and preserves survival fields`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.HostLeaderboardUpdated(HostLeaderboard(
            rows = listOf(HostLeaderboardRow(
                rank = 1,
                id = 31,
                playerName = "Kiro",
                playerScore = 1000,
                answeredCount = 2,
                correctCount = 2,
                wrongCount = 0,
                unansweredCount = 8,
                totalQuestions = 10,
                currentQuestionIndex = 2,
                streak = 2,
                lives = 3
            )),
            totalQuestions = 10,
            answeredTotal = 2
        )))
        socket.emit(GameEvent.HostPlayerProgressUpdated(HostPlayerProgress(
            id = 31,
            playerName = "Kiro",
            currentQuestionIndex = 3,
            playerScore = 1700,
            correctAnswersCount = 2,
            status = "connected",
            totalQuestions = 10
        )))
        runCurrent()

        val state = viewModel.uiState.value
        val row = state.leaderboard.rows.single()
        assertEquals(1700, row.playerScore)
        assertEquals(3, row.answeredCount)
        assertEquals(2, row.correctCount)
        assertEquals(1, row.wrongCount)
        assertEquals(7, row.unansweredCount)
        assertEquals(2, row.streak)
        assertEquals(3, row.lives)
        assertEquals(3, state.leaderboard.answeredTotal)
    }

    @Test
    fun `self paced terminal events update host rows`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.HostLeaderboardUpdated(HostLeaderboard(
            rows = listOf(
                HostLeaderboardRow(1, 31, "Kiro", 1000, lives = 1),
                HostLeaderboardRow(2, 32, "An", 900, lives = 2)
            )
        )))
        socket.emit(GameEvent.PlayerEliminated(EliminatedPlayer(31, "Kiro")))
        socket.emit(GameEvent.PlayerFinishedEvent(PlayerFinished(
            id = 32,
            playerScore = 2200,
            correctAnswersCount = 4,
            status = "finished",
            playerName = "An"
        )))
        runCurrent()

        val rows = viewModel.uiState.value.leaderboard.rows.associateBy { it.id }
        assertEquals("eliminated", rows.getValue(31).status)
        assertEquals(0, rows.getValue(31).lives)
        assertEquals("finished", rows.getValue(32).status)
        assertEquals(2200, rows.getValue(32).playerScore)
        assertEquals(4, rows.getValue(32).correctCount)
    }

    @Test
    fun `fire and forget command is guarded against double tap then reopens`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            config = GameConfig(timing = GameConfig.Timing(autoAdvance = false)),
            index = 0,
            totalQuestions = 2,
            question = question(0)
        )))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceQuestion)
        viewModel.onIntent(HostGameIntent.AdvanceQuestion)
        runCurrent()
        assertEquals(1, socket.nextCalls)
        assertTrue(viewModel.uiState.value.isSendingCommand)
        advanceTimeBy(800)
        runCurrent()
        assertFalse(viewModel.uiState.value.isSendingCommand)
    }

    private fun hostQuestion(index: Int, answers: List<String>) = GameEvent.HostQuestionReceived(
        HostQuestion(question(index), answers, totalQuestions = 2)
    )

    private fun question(index: Int) = PublicQuestion(
        index = index,
        total = 2,
        id = 10L + index,
        questionType = "multiple_choice",
        questionText = "Question $index",
        answerOptions = listOf(PublicAnswerOption("a", "A"), PublicAnswerOption("b", "B"))
    )
}

private class FakeHostGameSocketRepository : HostGameSocketRepository {
    private val events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val tokens = mutableListOf<String>()
    var joinCalls = 0
    var nextCalls = 0
    override fun events(socketToken: String): Flow<GameEvent> {
        tokens += socketToken
        return events
    }
    suspend fun emit(event: GameEvent) { events.emit(event) }
    override suspend fun joinLobby() { joinCalls++ }
    override suspend fun disconnect() = Unit
    override suspend fun startGame() = Unit
    override suspend fun nextQuestion() { nextCalls++ }
    override suspend fun pauseGame() = Unit
    override suspend fun resumeGame() = Unit
    override suspend fun endGame() = Unit
    override suspend fun updateConfig(patch: Map<GameConfigKey, GameConfigValue>): Result<ConfigUpdateAck> =
        error("Not used by HostGameViewModel tests")
}

private class FakeGameSessionRepository : GameSessionRepository {
    var hostTokenCalls = 0

    override suspend fun getHostToken(gameId: Long): Result<String> {
        hostTokenCalls++
        return Result.Success("new-host-token")
    }

    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> = error("Not used")
    override suspend fun createGameSession(
        params: CreateGameSessionParams
    ): Result<CreateGameSessionResult> = error("Not used")
    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> = error("Not used")
    override suspend fun joinRoom(
        sessionCode: String,
        playerName: String?,
        guestId: String?
    ): Result<JoinRoomResult> = error("Not used")
    override suspend fun getGameResults(gameId: Long): Result<GameResults> = error("Not used")
    override suspend fun getGameReview(
        gameId: Long,
        socketToken: String
    ): Result<GameReview> = error("Not used")
}
