package android.kma.myquizzapp.feature.leaderboard.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameResults
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.repository.GameResultRepository
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.StoredGameResult
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameResultsUseCase
import android.kma.myquizzapp.feature.leaderboard.domain.LoadGameReviewUseCase
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
        val games = FakeGameSessionRepository(reviewResult = Result.Success(review()))
        val viewModel = viewModel(results, games)

        assertEquals(0, games.reviewCalls)
        assertEquals(0, games.resultsCalls)
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
        val games = FakeGameSessionRepository(reviewResult = Result.Success(review()))
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
        val games = FakeGameSessionRepository(reviewResult = Result.Error(AppError.Network))
        val viewModel = viewModel(FakeResultRepository(stored), games)

        viewModel.handleIntent(FinalResultIntent.ToggleReview)
        runCurrent()
        assertEquals("Không có kết nối mạng", viewModel.uiState.value.reviewError)

        games.reviewResult = Result.Success(review())
        viewModel.handleIntent(FinalResultIntent.RetryReview)
        runCurrent()
        assertEquals(2, games.reviewCalls)
        assertNotNull(viewModel.uiState.value.review)
        assertNull(viewModel.uiState.value.reviewError)
    }

    @Test
    fun `missing transient result is restored from REST`() = runTest(dispatcher) {
        val restored = GameResults(
            mode = GameMode.SOLO,
            reviewEnabled = true,
            leaderboard = listOf(LeaderboardRow(rank = 1, id = 9, playerName = "Kiro", playerScore = 1200)),
            perQuestion = listOf(QuestionStat(questionId = 3, questionIndex = 0, answerCount = 2, correctCount = 1))
        )
        val games = FakeGameSessionRepository(resultsResult = Result.Success(restored))
        val viewModel = viewModel(FakeResultRepository(null), games)

        runCurrent()

        assertEquals(1, games.resultsCalls)
        assertFalse(viewModel.uiState.value.isResultLoading)
        assertEquals(GameMode.SOLO, viewModel.uiState.value.mode)
        assertEquals(1, viewModel.uiState.value.leaderboard.size)
        assertEquals(1, viewModel.uiState.value.questionStats.size)
        assertTrue(viewModel.uiState.value.reviewEnabled)
    }

    @Test
    fun `REST fallback never exposes leaderboard hidden by config`() = runTest(dispatcher) {
        val restored = GameResults(
            showLeaderboard = ShowLeaderboard.NEVER,
            leaderboard = listOf(LeaderboardRow(rank = 1, id = 9, playerName = "Kiro", playerScore = 1200))
        )
        val games = FakeGameSessionRepository(resultsResult = Result.Success(restored))
        val viewModel = viewModel(FakeResultRepository(null), games)

        runCurrent()

        assertTrue(viewModel.uiState.value.isLeaderboardHidden)
        assertTrue(viewModel.uiState.value.leaderboard.isEmpty())
    }

    @Test
    fun `missing game result emits terminal navigation effect`() = runTest(dispatcher) {
        val games = FakeGameSessionRepository(
            resultsResult = Result.Error(AppError.Api("GAME_ROOM_NOT_FOUND"))
        )
        val viewModel = viewModel(FakeResultRepository(null), games)

        runCurrent()

        assertEquals(
            FinalResultEffect.ResourceMissing("Không tìm thấy phòng chơi"),
            viewModel.effect.first()
        )
        assertNull(viewModel.uiState.value.resultError)
    }

    @Test
    fun `result load error is shown and retry succeeds`() = runTest(dispatcher) {
        val games = FakeGameSessionRepository(resultsResult = Result.Error(AppError.Network))
        val viewModel = viewModel(FakeResultRepository(null), games)

        runCurrent()
        assertEquals("Không có kết nối mạng", viewModel.uiState.value.resultError)

        games.resultsResult = Result.Success(GameResults(mode = GameMode.SOLO))
        viewModel.handleIntent(FinalResultIntent.RetryResult)
        runCurrent()

        assertEquals(2, games.resultsCalls)
        assertNull(viewModel.uiState.value.resultError)
        assertNotNull(viewModel.uiState.value.result)
    }

    private fun viewModel(results: GameResultRepository, games: GameSessionRepository) =
        FinalResultViewModel(
            results = results,
            loadGameResults = LoadGameResultsUseCase(games),
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
    var reviewResult: Result<GameReview> = Result.Success(
        GameReview(0, 0, 0, 0, emptyList())
    ),
    var resultsResult: Result<GameResults> = Result.Success(GameResults())
) : GameSessionRepository {
    var reviewCalls = 0
    var resultsCalls = 0

    override suspend fun getGameHistory(
        role: android.kma.myquizzapp.core.common.model.GameHistoryRole,
        cursor: String?,
        limit: Int,
        guestId: String?
    ): Result<List<android.kma.myquizzapp.core.common.model.GameHistoryItem>> = error("unused")

    override suspend fun getGameResults(gameId: Long): Result<GameResults> {
        resultsCalls += 1
        return resultsResult
    }

    override suspend fun getGameReview(gameId: Long, socketToken: String): Result<GameReview> {
        reviewCalls += 1
        return reviewResult
    }

    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> = error("unused")
    override suspend fun createGameSession(params: CreateGameSessionParams): Result<CreateGameSessionResult> = error("unused")
    override suspend fun getHostToken(gameId: Long): Result<String> = error("unused")
    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> = error("unused")
    override suspend fun joinRoom(sessionCode: String, playerName: String?, guestId: String?): Result<JoinRoomResult> = error("unused")
}
