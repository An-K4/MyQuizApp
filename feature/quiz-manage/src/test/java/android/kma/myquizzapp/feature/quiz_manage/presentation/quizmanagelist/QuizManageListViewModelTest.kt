package android.kma.myquizzapp.feature.quiz_manage.presentation.quizmanagelist

import android.util.Log
import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.MyQuizzesSort
import android.kma.myquizzapp.core.common.model.MyQuizzesVisibility
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.DeleteQuizUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetMyQuizzesUseCase
import androidx.lifecycle.ViewModelStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizManageListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: QuizRepository
    private lateinit var viewModel: QuizManageListViewModel
    private lateinit var store: ViewModelStore
    private val target = QuizDeleteTarget(42L, "Quiz mẫu")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Paging on Android checks Log.isLoggable; JVM tests have no Android runtime.
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
        repository = mockk()
        viewModel = QuizManageListViewModel(GetMyQuizzesUseCase(repository), DeleteQuizUseCase(repository))
        store = ViewModelStore().also { it.put("library", viewModel) }
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `confirm without an open confirmation never deletes`() = runTest(dispatcher) {
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.deleteQuiz(any()) }
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `opening and dismissing confirmation does not call delete`() = runTest(dispatcher) {
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        assertEquals(target, viewModel.uiState.value.deleteTarget)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizDismissed)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.deleteTarget)
        coVerify(exactly = 0) { repository.deleteQuiz(any()) }
    }

    @Test
    fun `double confirmation is claimed before coroutine runs`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<Unit>>()
        coEvery { repository.deleteQuiz(target.id) } coAnswers { response.await() }
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        assertTrue(viewModel.uiState.value.isDeleting)
        runCurrent()
        coVerify(exactly = 1) { repository.deleteQuiz(target.id) }
        response.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `in flight delete cannot be dismissed or retargeted`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<Unit>>()
        coEvery { repository.deleteQuiz(target.id) } coAnswers { response.await() }
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizDismissed)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(QuizDeleteTarget(99L, "Quiz khác")))
        assertEquals(target, viewModel.uiState.value.deleteTarget)
        runCurrent()
        response.complete(Result.Success(Unit))
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteQuiz(target.id) }
        coVerify(exactly = 0) { repository.deleteQuiz(99L) }
    }

    @Test
    fun `network failure preserves dialog and allows explicit retry`() = runTest(dispatcher) {
        coEvery { repository.deleteQuiz(target.id) } returnsMany listOf(Result.Error(AppError.Network), Result.Success(Unit))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        assertEquals(target, viewModel.uiState.value.deleteTarget)
        assertEquals(AppError.Network.toUserMessage(), viewModel.uiState.value.deleteError)
        assertFalse(viewModel.uiState.value.isDeleting)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        assertNull(viewModel.uiState.value.deleteError)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.deleteTarget)
        coVerify(exactly = 2) { repository.deleteQuiz(target.id) }
    }

    @Test
    fun `dismiss after failure clears the error without retrying`() = runTest(dispatcher) {
        coEvery { repository.deleteQuiz(target.id) } returns Result.Error(AppError.Network)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        viewModel.onIntent(QuizManageListIntent.DeleteQuizDismissed)
        assertNull(viewModel.uiState.value.deleteTarget)
        assertNull(viewModel.uiState.value.deleteError)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteQuiz(target.id) }
    }

    @Test
    fun `success retains query and creates a new Paging generation`() = runTest(dispatcher) {
        coEvery { repository.deleteQuiz(target.id) } returns Result.Success(Unit)
        viewModel.onIntent(QuizManageListIntent.KeywordChanged("Sinh học"))
        viewModel.onIntent(QuizManageListIntent.VisibilityChanged(MyQuizzesVisibility.PUBLIC))
        viewModel.onIntent(QuizManageListIntent.SortChanged(MyQuizzesSort.NAME_ASC))
        var generations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.quizzes.collect { generations++ }
        }
        runCurrent()
        val beforeDelete = generations
        assertTrue(beforeDelete > 0)
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        runCurrent()
        assertEquals(beforeDelete, generations) // Dialog state must not replace the Pager.
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        runCurrent()
        assertTrue(generations > beforeDelete)
        val state = viewModel.uiState.value
        assertNull(state.deleteTarget)
        assertFalse(state.isDeleting)
        assertEquals("Sinh học", state.keyword)
        assertEquals(MyQuizzesVisibility.PUBLIC, state.visibility)
        assertEquals(MyQuizzesSort.NAME_ASC, state.sort)
        assertEquals(QuizManageListEffect.ShowMessage("Đã xóa quiz."), viewModel.effect.first())
    }

    @Test
    fun `missing quiz clears stale confirmation and refreshes without success message`() = runTest(dispatcher) {
        val error = AppError.Api("QUIZ_NOT_FOUND")
        coEvery { repository.deleteQuiz(target.id) } returns Result.Error(error)
        var generations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.quizzes.collect { generations++ }
        }
        runCurrent()
        val beforeDelete = generations
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        runCurrent()
        assertNull(viewModel.uiState.value.deleteTarget)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(generations > beforeDelete)
        assertEquals(QuizManageListEffect.ShowMessage(error.toUserMessage()), viewModel.effect.first())
    }

    @Test
    fun `transient failure does not replace Paging generation`() = runTest(dispatcher) {
        coEvery { repository.deleteQuiz(target.id) } returns Result.Error(AppError.Server(503))
        var generations = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.quizzes.collect { generations++ }
        }
        runCurrent()
        val beforeDelete = generations
        viewModel.onIntent(QuizManageListIntent.DeleteQuizClicked(target))
        viewModel.onIntent(QuizManageListIntent.DeleteQuizConfirmed)
        runCurrent()
        assertEquals(beforeDelete, generations)
        assertEquals(target, viewModel.uiState.value.deleteTarget)
        assertNotNull(viewModel.uiState.value.deleteError)
    }
}
