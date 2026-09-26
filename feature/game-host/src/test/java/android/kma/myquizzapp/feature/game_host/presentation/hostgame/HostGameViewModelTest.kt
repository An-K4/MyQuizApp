package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.HostAnswerReceived
import android.kma.myquizzapp.core.common.model.HostQuestion
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.SessionStatus
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
    private lateinit var viewModel: HostGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakeHostGameSocketRepository()
        viewModel = HostGameViewModel(socket, SavedStateHandle(mapOf("socketToken" to "host-token")))
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
    var joinCalls = 0
    var nextCalls = 0
    override fun events(socketToken: String): Flow<GameEvent> = events
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
