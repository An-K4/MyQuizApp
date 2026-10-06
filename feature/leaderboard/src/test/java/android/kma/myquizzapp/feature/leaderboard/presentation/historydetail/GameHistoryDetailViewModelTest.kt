package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameHistoryViewer
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistoryAnswersUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameHistorySummaryUseCase
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameHistoryDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `host loads summary without requesting personal answers`() = runTest(dispatcher) {
        val repository = mockk<GameSessionRepository>()
        coEvery { repository.getGameHistorySummary(91, null) } returns Result.Success(summary(isHost = true))
        val viewModel = viewModel(repository)

        runCurrent()

        assertTrue(viewModel.uiState.value.isHost)
        assertFalse(viewModel.uiState.value.isSummaryLoading)
        coVerify(exactly = 0) { repository.getGameHistoryAnswers(any(), any()) }
    }

    @Test
    fun `player loads summary then own answers`() = runTest(dispatcher) {
        val repository = mockk<GameSessionRepository>()
        val review = GameReview(900, 9, 10, 10, emptyList())
        coEvery { repository.getGameHistorySummary(91, null) } returns Result.Success(summary(isHost = false))
        coEvery { repository.getGameHistoryAnswers(91, null) } returns Result.Success(review)
        val viewModel = viewModel(repository)

        runCurrent()

        assertEquals(5L, viewModel.uiState.value.playerId)
        assertNotNull(viewModel.uiState.value.review)
        assertEquals(900, viewModel.uiState.value.review?.playerScore)
        coVerify(exactly = 1) { repository.getGameHistoryAnswers(91, null) }
    }

    @Test
    fun `review disabled is a normal section state`() = runTest(dispatcher) {
        val repository = mockk<GameSessionRepository>()
        coEvery { repository.getGameHistorySummary(91, null) } returns Result.Success(summary(isHost = false))
        coEvery { repository.getGameHistoryAnswers(91, null) } returns
            Result.Error(AppError.Api("GAME_REVIEW_DISABLED"))
        val viewModel = viewModel(repository)

        runCurrent()

        assertTrue(viewModel.uiState.value.reviewDisabled)
        assertEquals(null, viewModel.uiState.value.answersError)
        assertFalse(viewModel.uiState.value.isAnswersLoading)
    }

    private fun viewModel(repository: GameSessionRepository): GameHistoryDetailViewModel {
        val guestStore = mockk<GuestIdentityStore>(relaxed = true)
        return GameHistoryDetailViewModel(
            loadHistorySummary = LoadGameHistorySummaryUseCase(repository, guestStore),
            loadHistoryAnswers = LoadGameHistoryAnswersUseCase(repository, guestStore),
            sessionRepository = FakeSessionRepository(SessionState.LoggedIn(user())),
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to 91L))
        )
    }

    private fun summary(isHost: Boolean) = GameHistorySummary(
        sessionId = 91,
        sessionName = "Friday room",
        gameMode = GameMode.CLASSIC,
        sessionStatus = SessionStatus.FINISHED,
        config = GameConfig(),
        totalPlayers = 2,
        totalQuestions = 10,
        viewer = GameHistoryViewer(isHost = isHost, playerId = if (isHost) null else 5)
    )

    private fun user() = User(
        id = 7,
        fullname = "Kiro",
        email = "kiro@example.com",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
}

private class FakeSessionRepository(initial: SessionState) : SessionRepository {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<SessionState> = mutable
    override suspend fun refresh() = Unit
    override fun onAuthenticated(user: User) { mutable.value = SessionState.LoggedIn(user) }
    override fun onSignedOut() { mutable.value = SessionState.Guest }
}
