package android.kma.myquizzapp.feature.quiz_manage.presentation.editor

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.UpdateQuizWithAssetsUseCase
import android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz.EditQuizIntent
import android.kma.myquizzapp.feature.quiz_manage.presentation.editquiz.EditQuizViewModel
import androidx.lifecycle.ViewModelStore
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetQuizDetailUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class EditQuizViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var saveQuiz: UpdateQuizWithAssetsUseCase
    private lateinit var vm: EditQuizViewModel
    private lateinit var store: ViewModelStore
    private val quiz = Quiz(id = 1L, quizOwner = 7L, quizName = "Quiz mẫu", quizLanguage = "vi", isPublic = true,
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z")
    private lateinit var getQuiz: GetQuizDetailUseCase
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        saveQuiz = mockk()
        store = ViewModelStore()
        getQuiz = mockk()
        coEvery { getQuiz(1L) } returns Result.Success(quiz)
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private suspend fun create() {
        vm = EditQuizViewModel(getQuiz, saveQuiz, SavedStateHandle(mapOf("quizId" to 1L)))
        store.put("editor", vm)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(EditQuizIntent.AddQuestion)
    }
    private fun validInput(): String {
        vm.onIntent(EditQuizIntent.QuizNameChanged(" Quiz mẫu "))
        vm.onIntent(EditQuizIntent.QuizLanguageChanged(" vi "))
        val id = vm.uiState.value.questions.first().localId
        vm.onIntent(EditQuizIntent.QuestionTextChanged(id, " Câu hỏi "))
        vm.onIntent(EditQuizIntent.OptionChanged(id, 0, " A "))
        vm.onIntent(EditQuizIntent.OptionChanged(id, 1, "B"))
        vm.onIntent(EditQuizIntent.ToggleCorrectIndex(id, 0))
        return id
    }

    @Test fun `validation blocks upload and repeated errors remain observable`() = runTest(dispatcher) {
        create()
        vm.onIntent(EditQuizIntent.Submit)
        val firstAttempt = vm.uiState.value.validationAttempt
        vm.onIntent(EditQuizIntent.Submit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.fieldErrors.isNotEmpty())
        assertEquals(firstAttempt + 1, vm.uiState.value.validationAttempt)
        coVerify(exactly = 0) { saveQuiz(1L, any()) }
    }
    @Test fun `double submit is blocked synchronously and fields cannot change while saving`() = runTest(dispatcher) {
        create()
        val id = validInput()
        val response = CompletableDeferred<Result<Quiz>>()
        coEvery { saveQuiz(1L, any()) } coAnswers { response.await() }
        vm.onIntent(EditQuizIntent.Submit)
        vm.onIntent(EditQuizIntent.Submit)
        vm.onIntent(EditQuizIntent.QuizNameChanged("Không được đổi"))
        vm.onIntent(EditQuizIntent.AddQuestion)
        vm.onIntent(EditQuizIntent.OptionChanged(id, 0, "Không được đổi"))
        assertTrue(vm.uiState.value.isSubmitting)
        assertEquals(" Quiz mẫu ", vm.uiState.value.quizName)
        assertEquals(1, vm.uiState.value.questions.size)
        assertEquals(" A ", vm.uiState.value.questions.first().options.first())
        runCurrent()
        coVerify(exactly = 1) { saveQuiz(1L, any()) }
        response.complete(Result.Success(quiz))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertFalse(vm.uiState.value.isDirty)
    }
    @Test fun `custom mode survives typing a preset and preset selection clears custom mode`() = runTest(dispatcher) {
        create(); val id = validInput()
        vm.onIntent(EditQuizIntent.CustomTimeSelected(id))
        vm.onIntent(EditQuizIntent.TimeLimitChanged(id, "30"))
        assertTrue(vm.uiState.value.questions.first().usesCustomTime)
        vm.onIntent(EditQuizIntent.TimePresetSelected(id, 60))
        assertFalse(vm.uiState.value.questions.first().usesCustomTime)
        assertEquals("60", vm.uiState.value.questions.first().timeLimit)
    }
    @Test fun `options stay within bounds and deletion remaps correct index`() = runTest(dispatcher) {
        create(); val id = validInput()
        repeat(4) { vm.onIntent(EditQuizIntent.AddOption(id)) }
        assertEquals(4, vm.uiState.value.questions.first().options.size)
        vm.onIntent(EditQuizIntent.ToggleCorrectIndex(id, 3))
        vm.onIntent(EditQuizIntent.RemoveOption(id, 1))
        assertEquals(setOf(2), vm.uiState.value.questions.first().correctIndexes)
        vm.onIntent(EditQuizIntent.RemoveOption(id, 99))
        assertEquals(3, vm.uiState.value.questions.first().options.size)
        vm.onIntent(EditQuizIntent.RemoveOption(id, 2))
        assertTrue(vm.uiState.value.questions.first().correctIndexes.isEmpty())
        vm.onIntent(EditQuizIntent.RemoveOption(id, 0))
        assertEquals(2, vm.uiState.value.questions.first().options.size)
    }
    @Test fun `last question cannot be removed and selecting same type keeps answer`() = runTest(dispatcher) {
        create(); val id = validInput()
        vm.onIntent(EditQuizIntent.RemoveQuestion(id))
        assertEquals(1, vm.uiState.value.questions.size)
        vm.onIntent(EditQuizIntent.QuestionTypeChanged(id, vm.uiState.value.questions.first().questionType))
        assertEquals(setOf(0), vm.uiState.value.questions.first().correctIndexes)
    }
    @Test fun `network error preserves draft and permits deliberate retry`() = runTest(dispatcher) {
        create(); validInput()
        coEvery { saveQuiz(1L, any()) } returnsMany listOf(Result.Error(AppError.Network), Result.Success(quiz))
        vm.onIntent(EditQuizIntent.Submit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertTrue(vm.uiState.value.isDirty)
        assertNotNull(vm.uiState.value.errorMessage)
        vm.onIntent(EditQuizIntent.Submit)
        advanceUntilIdle()
        coVerify(exactly = 2) { saveQuiz(1L, any()) }
        assertNull(vm.uiState.value.errorMessage)
    }
    @Test fun `duplicate load intents do not race the initial request`() = runTest(dispatcher) {
        vm = EditQuizViewModel(getQuiz, saveQuiz, SavedStateHandle(mapOf("quizId" to 1L)))
        store.put("editor", vm)
        vm.onIntent(EditQuizIntent.LoadQuiz)
        vm.onIntent(EditQuizIntent.Retry)
        advanceUntilIdle()
        coVerify(exactly = 1) { getQuiz(1L) }
        assertFalse(vm.uiState.value.isLoading)
    }

}
