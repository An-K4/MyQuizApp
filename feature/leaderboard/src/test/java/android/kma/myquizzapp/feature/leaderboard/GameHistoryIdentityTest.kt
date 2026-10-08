package android.kma.myquizzapp.feature.leaderboard

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.datastore.GuestIdentityStore
import android.kma.myquizzapp.feature.leaderboard.domain.*
import android.kma.myquizzapp.feature.leaderboard.presentation.historydetail.*
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class GameHistoryIdentityTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `new identity starts summary immediately and old success is discarded`() = runTest(dispatcher) {
        val f = fixture()
        var calls = 0
        var old: Continuation<Result<GameHistorySummary>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers {
            if (++calls == 1) suspendCoroutine<Result<GameHistorySummary>> { old = it } else Result.Success(summary("B"))
        }
        val vm = f.vm(); runCurrent()
        f.session.onAuthenticated(user(9)); runCurrent()
        assertEquals(2, calls)
        assertEquals("B", vm.uiState.value.summary?.sessionName)
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Success(summary("A"))))
        runCurrent()
        assertEquals("B", vm.uiState.value.summary?.sessionName)
        assertFalse(vm.uiState.value.isSummaryLoading)
    }

    @Test fun `old summary error cannot replace new identity state`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameHistorySummary>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers {
            if (++calls == 1) suspendCoroutine<Result<GameHistorySummary>> { old = it } else Result.Success(summary("B"))
        }
        val vm = f.vm(); runCurrent()
        f.session.onAuthenticated(user(9)); runCurrent()
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Error(AppError.Network))); runCurrent()
        assertEquals("B", vm.uiState.value.summary?.sessionName)
        assertNull(vm.uiState.value.summaryError)
    }

    @Test fun `late answers cannot overwrite new players answers`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameReview>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } returns Result.Success(summary("room", 70))
        coEvery { f.repo.getGameHistoryAnswers(any(), any()) } coAnswers {
            if (++calls == 1) suspendCoroutine<Result<GameReview>> { old = it } else Result.Success(review(88))
        }
        val vm = f.vm(); runCurrent()
        f.session.onAuthenticated(user(9)); runCurrent()
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Success(review(13)))); runCurrent()
        assertEquals(88, vm.uiState.value.review?.playerScore)
        assertFalse(vm.uiState.value.isAnswersLoading)
    }

    @Test fun `stale review disabled error cannot hide a new players answers`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameReview>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } returns Result.Success(summary("room", 70))
        coEvery { f.repo.getGameHistoryAnswers(any(), any()) } coAnswers {
            if (++calls == 1) suspendCoroutine<Result<GameReview>> { old = it } else Result.Success(review(88))
        }
        val vm = f.vm(); runCurrent()
        f.session.onAuthenticated(user(9)); runCurrent()
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Error(AppError.Api("GAME_REVIEW_DISABLED"))))
        runCurrent()
        assertEquals(88, vm.uiState.value.review?.playerScore)
        assertFalse(vm.uiState.value.reviewDisabled)
        assertNull(vm.uiState.value.answersError)
    }

    @Test fun `summary retry is deduped while request is active`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameHistorySummary>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers {
            calls++; suspendCoroutine<Result<GameHistorySummary>> { old = it }
        }
        val vm = f.vm(); runCurrent()
        vm.onIntent(GameHistoryDetailIntent.RetrySummary)
        vm.onIntent(GameHistoryDetailIntent.RetrySummary); runCurrent()
        assertEquals(1, calls)
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Success(summary("A")))); runCurrent()
        assertFalse(vm.uiState.value.isSummaryLoading)
    }

    @Test fun `same user new generation is detected synchronously on intent`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameHistorySummary>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers {
            if (++calls == 1) suspendCoroutine<Result<GameHistorySummary>> { old = it } else Result.Success(summary("new lifetime"))
        }
        val vm = f.vm(); runCurrent()
        f.session.onAuthenticated(user()) // StateFlow equality suppresses this emission
        vm.onIntent(GameHistoryDetailIntent.RetrySummary); runCurrent()
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Success(summary("old lifetime")))); runCurrent()
        assertEquals(2, calls)
        assertEquals("new lifetime", vm.uiState.value.summary?.sessionName)
    }

    @Test fun `profile field update keeps same identity and does not cancel valid summary`() = runTest(dispatcher) {
        val f = fixture(); var calls = 0
        var old: Continuation<Result<GameHistorySummary>>? = null
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers {
            calls++; suspendCoroutine<Result<GameHistorySummary>> { old = it }
        }
        val vm = f.vm(); runCurrent()
        f.session.updateProfile(user().copy(fullname = "Changed")); runCurrent()
        assertEquals(1, calls)
        checkNotNull(old).resumeWith(kotlin.Result.success(Result.Success(summary("valid")))); runCurrent()
        assertEquals("valid", vm.uiState.value.summary?.sessionName)
    }

    @Test fun `unknown session does not issue a request until identity is resolved`() = runTest(dispatcher) {
        val f = fixture(SessionState.Unknown); var calls = 0
        coEvery { f.repo.getGameHistorySummary(any(), any()) } coAnswers { calls++; Result.Success(summary("A")) }
        val vm = f.vm(); runCurrent(); assertEquals(0, calls)
        f.session.onAuthenticated(user()); runCurrent()
        assertEquals(1, calls); assertEquals("A", vm.uiState.value.summary?.sessionName)
    }

    @Test fun `guest summary and answers use stored guest identity not socket token`() = runTest(dispatcher) {
        val f = fixture(SessionState.Guest)
        coEvery { f.guest.getGuestIdOrNull() } returns "guest-fixture"
        coEvery { f.repo.getGameHistorySummary(42, "guest-fixture") } returns Result.Success(summary("Guest", 70))
        coEvery { f.repo.getGameHistoryAnswers(42, "guest-fixture") } returns Result.Success(review(8))
        val vm = f.vm(); runCurrent()
        assertEquals("Guest", vm.uiState.value.summary?.sessionName)
        assertEquals(8, vm.uiState.value.review?.playerScore)
    }

    private fun fixture(initial: SessionState = SessionState.LoggedIn(user())): Fixture =
        Fixture(mockk(), mockk(), IdentitySession(initial))

    private data class Fixture(val repo: GameSessionRepository, val guest: GuestIdentityStore, val session: IdentitySession) {
        fun vm() = GameHistoryDetailViewModel(LoadGameHistorySummaryUseCase(repo, guest),
            LoadGameHistoryAnswersUseCase(repo, guest), ObserveHistorySessionUseCase(session),
            SavedStateHandle(mapOf("sessionId" to 42L)))
    }

    private fun summary(name: String, playerId: Long? = null) = GameHistorySummary(
        sessionId = 42, sessionName = name, gameMode = GameMode.CLASSIC,
        sessionStatus = SessionStatus.FINISHED, config = GameConfig(), totalPlayers = 2,
        totalQuestions = 1, viewer = GameHistoryViewer(playerId == null, playerId)
    )
    private fun review(score: Int) = GameReview(score, 1, 1, 1, emptyList())
    private fun user(id: Long = 7) = User(id, "Fixture", "fixture@example.test", createdAt = "2026-10-01", updatedAt = "2026-10-07")
}

private class IdentitySession(initial: SessionState) : SessionRepository {
    override val state = MutableStateFlow(initial)
    private var generation = 1L
    override fun snapshot() = SessionSnapshot(state.value, generation)
    override suspend fun refresh() = Unit
    override fun onAuthenticated(user: User) { generation++; state.value = SessionState.LoggedIn(user) }
    override fun onSignedOut() { generation++; state.value = SessionState.Guest }
    fun updateProfile(user: User) { state.value = SessionState.LoggedIn(user) }
}
