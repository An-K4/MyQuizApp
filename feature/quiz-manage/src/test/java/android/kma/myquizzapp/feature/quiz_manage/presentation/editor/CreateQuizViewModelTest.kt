package android.kma.myquizzapp.feature.quiz_manage.presentation.editor

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.CreateQuizWithAssetsUseCase
import android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz.CreateQuizIntent
import android.kma.myquizzapp.feature.quiz_manage.presentation.createquiz.CreateQuizViewModel
import androidx.lifecycle.ViewModelStore
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateQuizViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var saveQuiz: CreateQuizWithAssetsUseCase
    private lateinit var vm: CreateQuizViewModel
    private lateinit var store: ViewModelStore
    private val quiz = Quiz(id = 1L, quizOwner = 7L, quizName = "Quiz mẫu", quizLanguage = "vi", isPublic = true,
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z")
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        saveQuiz = mockk()
        store = ViewModelStore()
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun create() { vm = CreateQuizViewModel(saveQuiz); store.put("editor", vm) }
    private fun validInput(): String {
        vm.onIntent(CreateQuizIntent.QuizNameChanged(" Quiz mẫu "))
        vm.onIntent(CreateQuizIntent.QuizLanguageChanged(" vi "))
        val id = vm.uiState.value.questions.first().localId
        vm.onIntent(CreateQuizIntent.QuestionTextChanged(id, " Câu hỏi "))
        vm.onIntent(CreateQuizIntent.OptionChanged(id, 0, " A "))
        vm.onIntent(CreateQuizIntent.OptionChanged(id, 1, "B"))
        vm.onIntent(CreateQuizIntent.ToggleCorrectIndex(id, 0))
        return id
    }

    @Test fun `validation blocks upload and repeated errors remain observable`() = runTest(dispatcher) {
        create()
        vm.onIntent(CreateQuizIntent.Submit)
        val firstAttempt = vm.uiState.value.validationAttempt
        vm.onIntent(CreateQuizIntent.Submit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.fieldErrors.isNotEmpty())
        assertEquals(firstAttempt + 1, vm.uiState.value.validationAttempt)
        coVerify(exactly = 0) { saveQuiz(any()) }
    }
    @Test fun `double submit is blocked synchronously and fields cannot change while saving`() = runTest(dispatcher) {
        create()
        val id = validInput()
        val response = CompletableDeferred<Result<Quiz>>()
        coEvery { saveQuiz(any()) } coAnswers { response.await() }
        vm.onIntent(CreateQuizIntent.Submit)
        vm.onIntent(CreateQuizIntent.Submit)
        vm.onIntent(CreateQuizIntent.QuizNameChanged("Không được đổi"))
        vm.onIntent(CreateQuizIntent.AddQuestion)
        vm.onIntent(CreateQuizIntent.OptionChanged(id, 0, "Không được đổi"))
        assertTrue(vm.uiState.value.isSubmitting)
        assertEquals(" Quiz mẫu ", vm.uiState.value.quizName)
        assertEquals(1, vm.uiState.value.questions.size)
        assertEquals(" A ", vm.uiState.value.questions.first().options.first())
        runCurrent()
        coVerify(exactly = 1) { saveQuiz(any()) }
        response.complete(Result.Success(quiz))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertFalse(vm.uiState.value.isDirty)
    }
    @Test fun `custom mode survives typing a preset and preset selection clears custom mode`() = runTest(dispatcher) {
        create(); val id = validInput()
        vm.onIntent(CreateQuizIntent.CustomTimeSelected(id))
        vm.onIntent(CreateQuizIntent.TimeLimitChanged(id, "30"))
        assertTrue(vm.uiState.value.questions.first().usesCustomTime)
        vm.onIntent(CreateQuizIntent.TimePresetSelected(id, 60))
        assertFalse(vm.uiState.value.questions.first().usesCustomTime)
        assertEquals("60", vm.uiState.value.questions.first().timeLimit)
    }
    @Test fun `options stay within bounds and deletion remaps correct index`() = runTest(dispatcher) {
        create(); val id = validInput()
        repeat(4) { vm.onIntent(CreateQuizIntent.AddOption(id)) }
        assertEquals(4, vm.uiState.value.questions.first().options.size)
        vm.onIntent(CreateQuizIntent.ToggleCorrectIndex(id, 3))
        vm.onIntent(CreateQuizIntent.RemoveOption(id, 1))
        assertEquals(setOf(2), vm.uiState.value.questions.first().correctIndexes)
        vm.onIntent(CreateQuizIntent.RemoveOption(id, 99))
        assertEquals(3, vm.uiState.value.questions.first().options.size)
        vm.onIntent(CreateQuizIntent.RemoveOption(id, 2))
        assertTrue(vm.uiState.value.questions.first().correctIndexes.isEmpty())
        vm.onIntent(CreateQuizIntent.RemoveOption(id, 0))
        assertEquals(2, vm.uiState.value.questions.first().options.size)
    }
    @Test fun `last question cannot be removed and selecting same type keeps answer`() = runTest(dispatcher) {
        create(); val id = validInput()
        vm.onIntent(CreateQuizIntent.RemoveQuestion(id))
        assertEquals(1, vm.uiState.value.questions.size)
        vm.onIntent(CreateQuizIntent.QuestionTypeChanged(id, vm.uiState.value.questions.first().questionType))
        assertEquals(setOf(0), vm.uiState.value.questions.first().correctIndexes)
    }
    @Test fun `network error preserves draft and permits deliberate retry`() = runTest(dispatcher) {
        create(); validInput()
        coEvery { saveQuiz(any()) } returnsMany listOf(Result.Error(AppError.Network), Result.Success(quiz))
        vm.onIntent(CreateQuizIntent.Submit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSubmitting)
        assertTrue(vm.uiState.value.isDirty)
        assertNotNull(vm.uiState.value.errorMessage)
        vm.onIntent(CreateQuizIntent.Submit)
        advanceUntilIdle()
        coVerify(exactly = 2) { saveQuiz(any()) }
        assertNull(vm.uiState.value.errorMessage)
    }
}
