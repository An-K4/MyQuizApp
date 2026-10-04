package android.kma.myquizzapp.feature.lobby.presentation.playerlobby

import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.ConnectionStatus
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerLobbyViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var socket: FakePlayerLobbySocketRepository
    private lateinit var viewModel: PlayerLobbyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakePlayerLobbySocketRepository()
        viewModel = PlayerLobbyViewModel(
            socket,
            SavedStateHandle(
                mapOf(
                    "gameId" to 1L,
                    "playerId" to 2L,
                    "socketToken" to "player-token"
                )
            )
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reconnect exhaustion exposes retry and retry starts fresh connection`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed(null, "CLIENT_RECONNECT_EXHAUSTED"))
        runCurrent()
        assertEquals(ConnectionStatus.RECONNECT_FAILED, viewModel.uiState.value.connection)

        viewModel.onIntent(PlayerLobbyIntent.Retry)
        runCurrent()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.uiState.value.connection)
        assertEquals(listOf("player-token", "player-token"), socket.tokens)
    }

    @Test
    fun `connected rejoins lobby after every reconnect`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(2, socket.joinCalls)
        assertEquals(ConnectionStatus.CONNECTED, viewModel.uiState.value.connection)
    }
}

private class FakePlayerLobbySocketRepository : PlayerGameSocketRepository {
    private val events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val tokens = mutableListOf<String>()
    var joinCalls = 0

    override fun events(socketToken: String): Flow<GameEvent> {
        tokens += socketToken
        return events
    }

    suspend fun emit(event: GameEvent) {
        events.emit(event)
    }

    override suspend fun joinLobby() {
        joinCalls++
    }

    override suspend fun disconnect() = Unit
    override suspend fun leaveLobby() = Unit
    override suspend fun submitAnswer(answer: PlayerAnswer): Result<AnswerAck> = error("Not used")
    override suspend fun requestNextQuestion() = Unit
    override suspend fun sync() = Unit
}
