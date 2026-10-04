package android.kma.myquizzapp.feature.lobby.presentation.hostlobby

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameResults
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.lobby.domain.usecase.GetGameModesUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.LookupRoomUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.RefreshHostTokenUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.UpdateRoomConfigUseCase
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
class HostLobbyViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var socket: FakeHostLobbySocketRepository
    private lateinit var sessions: FakeLobbyGameSessionRepository
    private lateinit var viewModel: HostLobbyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakeHostLobbySocketRepository()
        sessions = FakeLobbyGameSessionRepository()
        viewModel = HostLobbyViewModel(
            socketRepository = socket,
            refreshHostToken = RefreshHostTokenUseCase(sessions),
            lookupRoom = LookupRoomUseCase(sessions),
            getGameModes = GetGameModesUseCase(sessions),
            updateRoomConfig = UpdateRoomConfigUseCase(socket),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "gameId" to 1L,
                    "sessionCode" to "123456",
                    "socketToken" to "host-token"
                )
            )
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `cold start exhaustion exposes retry instead of spinning forever`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed(null, "CLIENT_RECONNECT_EXHAUSTED"))
        runCurrent()
        assertEquals(ConnectionStatus.RECONNECT_FAILED, viewModel.uiState.value.connection)

        viewModel.onIntent(HostLobbyIntent.Retry)
        runCurrent()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.uiState.value.connection)
        assertEquals(listOf("host-token", "host-token"), socket.tokens)
    }

    @Test
    fun `successful connection resets host token refresh allowance`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed(null, "GAME_TOKEN_INVALID"))
        runCurrent()
        socket.emit(GameEvent.Connected)
        runCurrent()
        socket.emit(GameEvent.Failed(null, "GAME_TOKEN_INVALID"))
        runCurrent()

        assertEquals(2, sessions.hostTokenCalls)
        assertEquals(listOf("host-token", "host-token-1", "host-token-2"), socket.tokens)
    }
}

private class FakeHostLobbySocketRepository : HostGameSocketRepository {
    private val events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val tokens = mutableListOf<String>()

    override fun events(socketToken: String): Flow<GameEvent> {
        tokens += socketToken
        return events
    }

    suspend fun emit(event: GameEvent) {
        events.emit(event)
    }

    override suspend fun joinLobby() = Unit
    override suspend fun disconnect() = Unit
    override suspend fun startGame() = Unit
    override suspend fun nextQuestion() = Unit
    override suspend fun pauseGame() = Unit
    override suspend fun resumeGame() = Unit
    override suspend fun endGame() = Unit
    override suspend fun updateConfig(
        patch: Map<GameConfigKey, GameConfigValue>
    ): Result<ConfigUpdateAck> = error("Not used")
}

private class FakeLobbyGameSessionRepository : GameSessionRepository {
    var hostTokenCalls = 0

    override suspend fun getHostToken(gameId: Long): Result<String> {
        hostTokenCalls++
        return Result.Success("host-token-$hostTokenCalls")
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
