package android.kma.myquizzapp.feature.quiz_preview

import android.kma.myquizzapp.core.common.model.AnswerOption
import android.kma.myquizzapp.core.common.model.Question
import android.kma.myquizzapp.core.common.model.QuestionType
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizPreviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<QuizRepository>()
    private val sessionRepository = mockk<SessionRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sessionRepository.state } returns MutableStateFlow(SessionState.Guest)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads quiz then starts countdown and first question`() = runTest(dispatcher) {
        coEvery { repository.getQuizDetail(1L) } returns Result.Success(quiz())
        val viewModel = viewModel()

        runCurrent()
        assertEquals(PreviewPhase.COUNTDOWN, viewModel.uiState.value.phase)

        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(PreviewPhase.QUESTION, viewModel.uiState.value.phase)
        assertEquals(0, viewModel.uiState.value.currentIndex)
        coVerify(exactly = 1) { repository.getQuizDetail(1L) }

        viewModel.onIntent(QuizPreviewIntent.SelectOption(1L))
        advanceTimeBy(2_000)
        runCurrent()
    }

    @Test
    fun `single choice grades locally without game mutation`() = runTest(dispatcher) {
        coEvery { repository.getQuizDetail(1L) } returns Result.Success(quiz())
        val viewModel = viewModel()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()

        viewModel.onIntent(QuizPreviewIntent.SelectOption(1L))

        val state = viewModel.uiState.value
        assertEquals(PreviewPhase.FEEDBACK, state.phase)
        assertEquals(true, state.currentResult?.isCorrect)
        assertEquals(1, state.results.size)
        coVerify(exactly = 1) { repository.getQuizDetail(1L) }

        advanceTimeBy(2_000)
        runCurrent()
    }

    private fun viewModel() = QuizPreviewViewModel(
        quizRepository = repository,
        sessionRepository = sessionRepository,
        savedStateHandle = SavedStateHandle(mapOf("quizId" to 1L))
    )

    private fun quiz() = Quiz(
        id = 1,
        quizOwner = 10,
        quizName = "Quiz thử",
        quizLanguage = "vi",
        isPublic = true,
        createdAt = "2026-10-06T00:00:00Z",
        updatedAt = "2026-10-06T00:00:00Z",
        questions = listOf(
            Question(
                id = 1,
                quizId = 1,
                questionType = QuestionType.MULTIPLE_CHOICE,
                questionText = "Một cộng không?",
                timeLimit = 30,
                answerOptions = listOf(
                    AnswerOption(0, "Không"),
                    AnswerOption(1, "Một")
                ),
                correctAnswer = JsonArray(listOf(JsonPrimitive(1)))
            )
        )
    )
}
