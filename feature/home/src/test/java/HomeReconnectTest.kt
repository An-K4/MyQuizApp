package android.kma.myquizzapp.feature.home.presentation

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.network.NetworkMonitor
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.auth.domain.usecase.ObserveSessionUseCase
import android.kma.myquizzapp.feature.home.domain.usecase.GetHomeContentUseCase
import androidx.lifecycle.ViewModelStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeReconnectTest {
    private val dispatcher = StandardTestDispatcher()
    private val online = MutableStateFlow(false)
    private val network = object : NetworkMonitor { override val isOnline = online }
    private val repo = mockk<QuizRepository>()
    private val session = mockk<SessionRepository>()
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { session.state } returns MutableStateFlow(SessionState.Unknown)
    }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }

    private fun makeViewModel(): HomeViewModel = HomeViewModel(
        GetHomeContentUseCase(repo), ObserveSessionUseCase(session), network
    ).also { store.put("home", it) }

    @Test fun `reconnect reloads once and manual retry remains available`() = runTest(dispatcher) {
        coEvery { repo.getHomeContent() } returns Result.Error(AppError.Network)
        val vm = makeViewModel()
        runCurrent()
        coVerify(exactly = 1) { repo.getHomeContent() }
        vm.onIntent(HomeIntent.Retry)
        vm.onIntent(HomeIntent.Retry)
        runCurrent()
        coVerify(exactly = 2) { repo.getHomeContent() }
        online.value = true
        runCurrent()
        coVerify(exactly = 3) { repo.getHomeContent() }
        runCurrent()
        coVerify(exactly = 3) { repo.getHomeContent() }
    }

    @Test fun `reconnect during old request schedules one fresh read`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Result<List<HomeSection>>>()
        var calls = 0
        coEvery { repo.getHomeContent() } coAnswers {
            calls++
            if (calls == 1) pending.await() else Result.Success(emptyList())
        }
        makeViewModel()
        runCurrent()
        online.value = true
        runCurrent()
        coVerify(exactly = 1) { repo.getHomeContent() }
        pending.complete(Result.Error(AppError.Network))
        runCurrent()
        coVerify(exactly = 2) { repo.getHomeContent() }
    }
}
