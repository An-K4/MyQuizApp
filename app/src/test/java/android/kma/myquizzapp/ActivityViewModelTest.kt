package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameHistoryItem
import android.kma.myquizzapp.core.common.model.GameHistoryRole
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameResults
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.PageInfo
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import android.kma.myquizzapp.domain.activity.LoadGameHistoryUseCase
import android.kma.myquizzapp.presentation.activity.ActivityIntent
import android.kma.myquizzapp.presentation.activity.ActivityViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class ActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `user keeps independent played and hosted cursors`() = runTest(dispatcher) {
        val repository = FakeGameSessionRepository()
        val session = FakeSessionRepository(SessionState.LoggedIn(user()))
        val guestStore = mockk<GuestIdentityStore>()
        val viewModel = ActivityViewModel(
            LoadGameHistoryUseCase(repository, guestStore),
            session
        )

        runCurrent()
        assertEquals(listOf(1L), viewModel.uiState.value.played.items.map { it.sessionId })
        assertTrue(viewModel.uiState.value.played.hasMore)

        viewModel.onIntent(ActivityIntent.SelectRole(GameHistoryRole.HOSTED))
        runCurrent()
        assertEquals(listOf(9L), viewModel.uiState.value.hosted.items.map { it.sessionId })

        viewModel.onIntent(ActivityIntent.SelectRole(GameHistoryRole.PLAYED))
        viewModel.onIntent(ActivityIntent.LoadMore)
        runCurrent()
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.played.items.map { it.sessionId })
        assertFalse(viewModel.uiState.value.played.hasMore)
        assertEquals(
            listOf(
                GameHistoryRole.PLAYED to null,
                GameHistoryRole.HOSTED to null,
                GameHistoryRole.PLAYED to "played-2"
            ),
            repository.calls.map { it.role to it.cursor }
        )
    }

    @Test
    fun `refresh reloads current role when returning to screen`() = runTest(dispatcher) {
        val repository = FakeGameSessionRepository()
        val session = FakeSessionRepository(SessionState.LoggedIn(user()))
        val viewModel = ActivityViewModel(
            LoadGameHistoryUseCase(repository, mockk()),
            session
        )
        runCurrent()
        assertEquals(1, repository.calls.size)

        viewModel.onIntent(ActivityIntent.Refresh)
        runCurrent()

        assertEquals(2, repository.calls.size)
        assertEquals(GameHistoryRole.PLAYED, repository.calls.last().role)
        assertEquals(null, repository.calls.last().cursor)
    }

    @Test
    fun `guest without stored identity gets local empty state without request`() = runTest(dispatcher) {
        val repository = FakeGameSessionRepository()
        val session = FakeSessionRepository(SessionState.Guest)
        val guestStore = mockk<GuestIdentityStore>()
        coEvery { guestStore.getGuestIdOrNull() } returns null
        val viewModel = ActivityViewModel(
            LoadGameHistoryUseCase(repository, guestStore),
            session
        )

        runCurrent()

        assertTrue(viewModel.uiState.value.played.hasLoaded)
        assertTrue(viewModel.uiState.value.played.items.isEmpty())
        assertTrue(repository.calls.isEmpty())
    }

    private fun user() = User(
        id = 7,
        fullname = "Kiro",
        email = "kiro@example.com",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
}

private data class HistoryCall(
    val role: GameHistoryRole,
    val cursor: String?,
    val guestId: String?
)

private class FakeGameSessionRepository : GameSessionRepository {
    val calls = mutableListOf<HistoryCall>()

    override suspend fun getGameHistory(
        role: GameHistoryRole,
        cursor: String?,
        limit: Int,
        guestId: String?
    ): Result<List<GameHistoryItem>> {
        calls += HistoryCall(role, cursor, guestId)
        return when (role to cursor) {
            GameHistoryRole.PLAYED to null -> Result.Success(
                listOf(historyItem(1)), PageInfo("played-2", hasMore = true)
            )
            GameHistoryRole.PLAYED to "played-2" -> Result.Success(
                listOf(historyItem(2)), PageInfo(null, hasMore = false)
            )
            GameHistoryRole.HOSTED to null -> Result.Success(
                listOf(historyItem(9)), PageInfo(null, hasMore = false)
            )
            else -> error("Unexpected history request: $role $cursor")
        }
    }

    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> = error("unused")
    override suspend fun createGameSession(params: CreateGameSessionParams): Result<CreateGameSessionResult> = error("unused")
    override suspend fun getHostToken(gameId: Long): Result<String> = error("unused")
    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> = error("unused")
    override suspend fun joinRoom(sessionCode: String, playerName: String?, guestId: String?): Result<JoinRoomResult> = error("unused")
    override suspend fun getGameHistorySummary(gameId: Long, guestId: String?): Result<GameHistorySummary> = error("unused")
    override suspend fun getGameHistoryAnswers(gameId: Long, guestId: String?): Result<GameReview> = error("unused")
    override suspend fun getGameResults(gameId: Long): Result<GameResults> = error("unused")
    override suspend fun getGameReview(gameId: Long, socketToken: String): Result<GameReview> = error("unused")
}

private class FakeSessionRepository(initial: SessionState) : SessionRepository {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = mutable
    override suspend fun refresh() = Unit
    override fun onAuthenticated(user: User) { mutable.value = SessionState.LoggedIn(user) }
    override fun onSignedOut() { mutable.value = SessionState.Guest }
}

private fun historyItem(id: Long) = GameHistoryItem(
    sessionId = id,
    sessionName = "Room $id",
    gameMode = GameMode.CLASSIC,
    sessionStatus = SessionStatus.FINISHED,
    totalPlayers = 4,
    totalQuestions = 10,
    endedAt = "2026-10-03T14:30:00Z",
    quizId = id,
    quizName = "Quiz $id",
    quizImage = null,
    hostName = "Host",
    hostAvatar = null,
    playerScore = 100,
    correctAnswersCount = 8,
    rank = 1
)
