package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizWithOwnership
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.DeleteQuizUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizWithOwnershipUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class QuizDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var getQuiz: GetQuizWithOwnershipUseCase
    private lateinit var deleteQuiz: DeleteQuizUseCase
    private lateinit var viewModel: QuizDetailViewModel
    private lateinit var store: ViewModelStore
    private val quiz = Quiz(
        id = 1L, quizOwner = 7L, quizName = "Quiz mẫu", quizLanguage = "vi", isPublic = true,
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        getQuiz = mockk()
        deleteQuiz = mockk()
        store = ViewModelStore()
    }

    /** Create inside runTest, after stubbing, so init cannot run before the test body. */
    private fun createViewModel(isOwner: Boolean = true) {
        coEvery { getQuiz(1L) } returns Result.Success(QuizWithOwnership(quiz, isOwner))
        viewModel = QuizDetailViewModel(getQuiz, deleteQuiz, SavedStateHandle(mapOf("quizId" to 1L)))
        store.put("detail", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `duplicate loads are blocked before coroutine starts`() = runTest(dispatcher) {
        createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
        viewModel.onIntent(QuizDetailIntent.Retry)
        advanceUntilIdle()
        coVerify(exactly = 1) { getQuiz(1L) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `confirmation is required even for owner`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        coVerify(exactly = 0) { deleteQuiz(any()) }
    }

    @Test
    fun `non owner cannot open confirmation or delete through intents`() = runTest(dispatcher) {
        createViewModel(isOwner = false)
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
        coVerify(exactly = 0) { deleteQuiz(any()) }
    }

    @Test
    fun `double tap sends one delete and blocks dismiss and reload while in flight`() = runTest(dispatcher) {
        createViewModel()
        val response = CompletableDeferred<Result<Unit>>()
        coEvery { deleteQuiz(1L) } coAnswers { response.await() }
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizDismissed)
        viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
        assertTrue(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.isConfirmingDelete)
        runCurrent()
        coVerify(exactly = 1) { deleteQuiz(1L) }
        coVerify(exactly = 1) { getQuiz(1L) }
        response.complete(Result.Success(Unit))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.quiz)
        assertFalse(viewModel.uiState.value.isOwner)
        assertEquals(QuizDetailEffect.QuizDeleted, viewModel.effect.first())
    }

    @Test
    fun `transient delete error keeps confirmation for deliberate retry`() = runTest(dispatcher) {
        createViewModel()
        coEvery { deleteQuiz(1L) } returnsMany listOf(Result.Error(AppError.Network), Result.Success(Unit))
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isConfirmingDelete)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals(AppError.Network.toUserMessage(), viewModel.uiState.value.deleteError)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        assertNull(viewModel.uiState.value.deleteError)
        advanceUntilIdle()
        coVerify(exactly = 2) { deleteQuiz(1L) }
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
    }

    @Test
    fun `missing quiz while deleting clears stale data and emits resource missing`() = runTest(dispatcher) {
        createViewModel()
        val error = AppError.Api("QUIZ_NOT_FOUND")
        coEvery { deleteQuiz(1L) } returns Result.Error(error)
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.quiz)
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals(QuizDetailEffect.ResourceMissing(error.toUserMessage()), viewModel.effect.first())
    }

    @Test
    fun `missing quiz on reload clears existing quiz and owner privileges`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()
        val error = AppError.Api("QUIZ_NOT_FOUND")
        coEvery { getQuiz(1L) } returns Result.Error(error)
        viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.quiz)
        assertFalse(viewModel.uiState.value.isOwner)
        assertEquals(QuizDetailEffect.ResourceMissing(error.toUserMessage()), viewModel.effect.first())
    }

    @Test
    fun `load error on stale data blocks delete`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()
        coEvery { getQuiz(1L) } returns Result.Error(AppError.Server(503))
        viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
        coVerify(exactly = 0) { deleteQuiz(any()) }
    }

    @Test
    fun `dismiss clears delete error and does not retry`() = runTest(dispatcher) {
        createViewModel()
        coEvery { deleteQuiz(1L) } returns Result.Error(AppError.Network)
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizDismissed)
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
        assertNull(viewModel.uiState.value.deleteError)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        advanceUntilIdle()
        coVerify(exactly = 1) { deleteQuiz(1L) }
    }

    @Test
    fun `reload dismisses stale confirmation before rechecking ownership`() = runTest(dispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.onIntent(QuizDetailIntent.DeleteQuizClicked)
        assertTrue(viewModel.uiState.value.isConfirmingDelete)
        coEvery { getQuiz(1L) } returns Result.Success(QuizWithOwnership(quiz, false))
        viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isConfirmingDelete)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isOwner)
        viewModel.onIntent(QuizDetailIntent.DeleteQuizConfirmed)
        coVerify(exactly = 0) { deleteQuiz(any()) }
    }

}
