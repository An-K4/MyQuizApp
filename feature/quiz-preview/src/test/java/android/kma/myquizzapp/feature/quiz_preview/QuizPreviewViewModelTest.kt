package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.feature.quiz_preview.domain.usecase.LoadQuizPreviewUseCase

import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizPreviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<QuizRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads quiz then starts countdown and first question`() = runTest(dispatcher) {
        withPreview { vm ->
            runCurrent()
            assertEquals(PreviewPhase.COUNTDOWN, vm.uiState.value.phase)
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            assertEquals(0, vm.uiState.value.currentIndex)
            coVerify(exactly = 1) { repository.getQuizDetail(1L) }
        }
    }

    @Test
    fun `single choice grades locally without game mutation`() = runTest(dispatcher) {
        withPreview { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            val state = vm.uiState.value
            assertEquals(PreviewPhase.FEEDBACK, state.phase)
            assertEquals(true, state.currentResult?.isCorrect)
            assertEquals(1, state.results.size)
            coVerify(exactly = 1) { repository.getQuizDetail(1L) }
        }
    }

    @Test
    fun `feedback remains until manual next even after old reveal duration`() = runTest(dispatcher) {
        withPreview { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            val remaining = vm.uiState.value.remainingTimeMillis
            advanceTimeBy(20_000)
            runCurrent()
            assertEquals(PreviewPhase.FEEDBACK, vm.uiState.value.phase)
            assertEquals(remaining, vm.uiState.value.remainingTimeMillis)
            vm.onIntent(QuizPreviewIntent.Next)
            assertEquals(PreviewPhase.FINISHED, vm.uiState.value.phase)
        }
    }

    @Test
    fun `multiple select sends once and locks input during feedback`() = runTest(dispatcher) {
        withPreview(listOf(question(type = QuestionType.MULTIPLE_SELECT))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.ToggleOption(1L))
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            assertTrue(vm.uiState.value.canSubmit)
            vm.onIntent(QuizPreviewIntent.Submit)
            val score = vm.uiState.value.totalScore
            vm.onIntent(QuizPreviewIntent.Submit)
            vm.onIntent(QuizPreviewIntent.ToggleOption(0L))
            assertEquals(1, vm.uiState.value.results.size)
            assertEquals(setOf(1L), vm.uiState.value.selectedOptionIds)
            assertEquals(score, vm.uiState.value.totalScore)
        }
    }

    @Test
    fun `short and long answers require text and lock after submission`() = runTest(dispatcher) {
        for (type in listOf(QuestionType.SHORT_ANSWER, QuestionType.LONG_ANSWER)) {
            withPreview(listOf(question(type = type))) { vm ->
                begin(vm)
                assertFalse(vm.uiState.value.canSubmit)
                vm.onIntent(QuizPreviewIntent.Submit)
                assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
                vm.onIntent(QuizPreviewIntent.ChangeText("  một  "))
                vm.onIntent(QuizPreviewIntent.Submit)
                assertEquals(true, vm.uiState.value.currentResult?.isCorrect)
                vm.onIntent(QuizPreviewIntent.ChangeText("Khác"))
                assertEquals("  một  ", vm.uiState.value.textAnswer)
                assertEquals(1, vm.uiState.value.results.size)
            }
        }
    }

    @Test
    fun `skip reveals answer without submitting partial selection and waits for next`() = runTest(dispatcher) {
        withPreview(listOf(question(type = QuestionType.MULTIPLE_SELECT), question(id = 2))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.ToggleOption(1L))
            vm.onIntent(QuizPreviewIntent.Skip)
            assertEquals(PreviewPhase.FEEDBACK, vm.uiState.value.phase)
            assertEquals(false, vm.uiState.value.currentResult?.isAnswered)
            assertNull(vm.uiState.value.currentResult?.submittedAnswer)
            assertEquals("Một", vm.uiState.value.currentResult?.correctAnswer)
            assertEquals(0, vm.uiState.value.totalScore)
            vm.onIntent(QuizPreviewIntent.Skip)
            assertEquals(1, vm.uiState.value.results.size)
            vm.onIntent(QuizPreviewIntent.Next)
            assertEquals(1, vm.uiState.value.currentIndex)
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            assertTrue(vm.uiState.value.selectedOptionIds.isEmpty())
            vm.onIntent(QuizPreviewIntent.Next)
            assertEquals(1, vm.uiState.value.currentIndex)
        }
    }

    @Test
    fun `expired timed question stays answerable and receives late score`() = runTest(dispatcher) {
        withPreview(listOf(question(limit = 10))) { vm ->
            begin(vm)
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(0, vm.uiState.value.remainingSeconds)
            assertEquals(0L, vm.uiState.value.remainingTimeMillis)
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertEquals(true, vm.uiState.value.currentResult?.isLate)
            assertEquals(900, vm.uiState.value.totalScore)
        }
    }

    @Test
    fun `untimed question has no fallback deadline late penalty or speed bonus`() = runTest(dispatcher) {
        withPreview(listOf(question(limit = 0))) { vm ->
            begin(vm)
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            assertFalse(vm.uiState.value.isLate)
            assertEquals(0, vm.uiState.value.remainingSeconds)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertEquals(false, vm.uiState.value.currentResult?.isLate)
            assertEquals(1_000, vm.uiState.value.totalScore)
        }
    }

    @Test
    fun `duplicate load requests are claimed before coroutine launch`() = runTest(dispatcher) {
        withPreview { vm ->
            vm.onIntent(QuizPreviewIntent.Retry)
            vm.onIntent(QuizPreviewIntent.Retry)
            runCurrent()
            coVerify(exactly = 1) { repository.getQuizDetail(1L) }
        }
    }

    @Test
    fun `invalid option ids and question type intents cannot mutate selection`() = runTest(dispatcher) {
        withPreview { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.SelectOption(99L))
            vm.onIntent(QuizPreviewIntent.ToggleOption(1L))
            vm.onIntent(QuizPreviewIntent.ChangeText("Không hợp lệ"))
            assertEquals(PreviewPhase.QUESTION, vm.uiState.value.phase)
            assertTrue(vm.uiState.value.selectedOptionIds.isEmpty())
            assertEquals("", vm.uiState.value.textAnswer)
        }
    }

    @Test
    fun `total score accumulates only once per question and restart resets run`() = runTest(dispatcher) {
        withPreview(listOf(question(limit = 0), question(id = 2, limit = 0))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertEquals(1_000, vm.uiState.value.totalScore)
            vm.onIntent(QuizPreviewIntent.Next)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertEquals(2_000, vm.uiState.value.totalScore)
            vm.onIntent(QuizPreviewIntent.Next)
            assertEquals(PreviewPhase.FINISHED, vm.uiState.value.phase)
            vm.onIntent(QuizPreviewIntent.Restart)
            assertEquals(PreviewPhase.COUNTDOWN, vm.uiState.value.phase)
            assertEquals(0, vm.uiState.value.totalScore)
            assertTrue(vm.uiState.value.results.isEmpty())
        }
    }

    @Test
    fun `missing key remains ungradable and never awards points`() = runTest(dispatcher) {
        withPreview(listOf(question().copy(correctAnswer = null))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertNull(vm.uiState.value.currentResult?.isCorrect)
            assertEquals(0, vm.uiState.value.totalScore)
        }
    }

    @Test
    fun `review preserves exact selected ids across next question and restart`() = runTest(dispatcher) {
        withPreview(listOf(question(type = QuestionType.MULTIPLE_SELECT), question(id = 2))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.ToggleOption(0L))
            vm.onIntent(QuizPreviewIntent.ToggleOption(1L))
            vm.onIntent(QuizPreviewIntent.Submit)
            val first = vm.uiState.value.results.first()
            assertEquals(setOf(0L, 1L), first.selectedOptionIds)
            vm.onIntent(QuizPreviewIntent.Next)
            vm.onIntent(QuizPreviewIntent.SelectOption(1L))
            assertEquals(setOf(0L, 1L), vm.uiState.value.results[0].selectedOptionIds)
            assertEquals(setOf(1L), vm.uiState.value.results[1].selectedOptionIds)
            vm.onIntent(QuizPreviewIntent.Next)
            vm.onIntent(QuizPreviewIntent.Restart)
            assertTrue(vm.uiState.value.results.isEmpty())
            assertEquals(setOf(0L, 1L), first.selectedOptionIds)
        }
    }

    @Test
    fun `skip does not preserve abandoned choices for review`() = runTest(dispatcher) {
        withPreview(listOf(question(type = QuestionType.MULTIPLE_SELECT))) { vm ->
            begin(vm)
            vm.onIntent(QuizPreviewIntent.ToggleOption(0L))
            vm.onIntent(QuizPreviewIntent.Skip)
            assertTrue(vm.uiState.value.results.single().selectedOptionIds.isEmpty())
        }
    }

    private fun TestScope.begin(vm: QuizPreviewViewModel) {
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
    }

    // Mock setup precedes construction INSIDE runTest; clock uses the same virtual scheduler.
    private fun TestScope.withPreview(
        questions: List<Question> = listOf(question()),
        block: TestScope.(QuizPreviewViewModel) -> Unit,
    ) {
        coEvery { repository.getQuizDetail(1L) } returns Result.Success(quiz(questions))
        val vm = QuizPreviewViewModel(
            LoadQuizPreviewUseCase(repository),
            SavedStateHandle(mapOf("quizId" to 1L)),
            monotonicMillis = { testScheduler.currentTime + 1_000L },
        )
        try { block(vm) } finally { vm.viewModelScope.cancel() }
    }

    private fun question(id: Long = 1, type: QuestionType = QuestionType.MULTIPLE_CHOICE, limit: Int = 30): Question {
        val choice = type in setOf(QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_SELECT)
        return Question(
            id = id, quizId = 1, questionType = type, questionText = "Một cộng không?", timeLimit = limit,
            answerOptions = if (choice) listOf(AnswerOption(0, "Không"), AnswerOption(1, "Một")) else null,
            correctAnswer = if (choice) JsonArray(listOf(JsonPrimitive(1))) else JsonPrimitive("Một"),
        )
    }

    private fun quiz(questions: List<Question>) = Quiz(
        id = 1, quizOwner = 10, quizName = "Quiz thử", quizLanguage = "vi", isPublic = true,
        createdAt = "2026-10-06T00:00:00Z", updatedAt = "2026-10-06T00:00:00Z", questions = questions,
    )
}
