package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.StoredGameResult
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameReviewUseCase
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinalResultViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `review is lazy loaded once when player opens it`() = runTest(dispatcher) {
        val stored = StoredGameResult(
            gameId = 7,
            playerId = 9,
            result = GameEnded(reviewEnabled = true),
            mode = GameMode.PRACTICE,
            socketToken = "secret-token"
        )
        val results = FakeResultRepository(stored)
        val games = FakeGameSessionRepository(Result.Success(review()))
        val viewModel = viewModel(results, games)

        assertEquals(0, games.reviewCalls)
        assertTrue(viewModel.uiState.value.isPractice)

        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        runCurrent()

        assertEquals(1, games.reviewCalls)
        assertTrue(viewModel.uiState.value.isReviewVisible)
        assertNotNull(viewModel.uiState.value.review)
        assertNull(viewModel.uiState.value.reviewError)

        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        runCurrent()
        assertEquals(1, games.reviewCalls)
    }

    @Test
    fun `disabled review never calls endpoint`() = runTest(dispatcher) {
        val stored = StoredGameResult(7, 9, GameEnded(reviewEnabled = false), GameMode.SOLO, "token")
        val games = FakeGameSessionRepository(Result.Success(review()))
        val viewModel = viewModel(FakeResultRepository(stored), games)

        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        runCurrent()

        assertEquals(0, games.reviewCalls)
        assertFalse(viewModel.uiState.value.reviewEnabled)
        assertNull(viewModel.uiState.value.review)
    }

    @Test
    fun `review error is shown and retry succeeds`() = runTest(dispatcher) {
        val stored = StoredGameResult(7, 9, GameEnded(reviewEnabled = true), GameMode.SOLO, "token")
        val games = FakeGameSessionRepository(Result.Error(AppError.Network))
        val viewModel = viewModel(FakeResultRepository(stored), games)

        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        runCurrent()
        assertEquals("Không có kết nối mạng", viewModel.uiState.value.reviewError)

        games.result = Result.Success(review())
        viewModel.handleIntent(FinalResultIntent.RetryReview)
        runCurrent()
        assertEquals(2, games.reviewCalls)
        assertNotNull(viewModel.uiState.value.review)
        assertNull(viewModel.uiState.value.reviewError)
    }

    private fun viewModel(results: GameResultRepository, games: GameSessionRepository) =
        FinalResultViewModel(
            results = results,
            loadGameReview = LoadGameReviewUseCase(games),
            savedStateHandle = SavedStateHandle(mapOf("gameId" to 7L, "playerId" to 9L))
        )

    private fun review() = GameReview(
        playerScore = 0,
        correctAnswersCount = 1,
        totalQuestions = 2,
        answeredCount = 1,
        items = emptyList()
    )
}

private class FakeResultRepository(private val value: StoredGameResult?) : GameResultRepository {
    override fun save(value: StoredGameResult) = error("unused")
    override fun get(gameId: Long): StoredGameResult? = value
    override fun clear(gameId: Long) = Unit
}

private class FakeGameSessionRepository(
    var result: Result<GameReview>
) : GameSessionRepository {
    var reviewCalls = 0
    override suspend fun getGameReview(gameId: Long, socketToken: String): Result<GameReview> {
        reviewCalls += 1
        return result
    }
    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> = error("unused")
    override suspend fun createGameSession(params: CreateGameSessionParams): Result<CreateGameSessionResult> = error("unused")
    override suspend fun getHostToken(gameId: Long): Result<String> = error("unused")
    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> = error("unused")
    override suspend fun joinRoom(sessionCode: String, playerName: String?, guestId: String?): Result<JoinRoomResult> = error("unused")
}
