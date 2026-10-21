package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.EliminatedPlayer
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameResults
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.HostAnswerReceived
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.HostPlayerProgress
import android.kma.myquizzapp.core.common.model.HostQuestion
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PlayerFinished
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HostGameViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var socket: FakeHostGameSocketRepository
    private lateinit var sessions: FakeGameSessionRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: HostGameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        socket = FakeHostGameSocketRepository()
        sessions = FakeGameSessionRepository()
        savedStateHandle = SavedStateHandle(
            mapOf("gameId" to 1L, "socketToken" to "host-token")
        )
        viewModel = HostGameViewModel(socket, sessions, savedStateHandle)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `start layout covers initial unknown and countdown but not active or finished`() {
        val initial = HostGameUiState()
        assertTrue(initial.usesStartCountdownLayout)
        assertTrue(initial.copy(connection = HostGameConnection.CONNECTED,
            sessionStatus = SessionStatus.ACTIVE).usesStartCountdownLayout)
        assertTrue(initial.copy(phase = GamePhase.COUNTDOWN).usesStartCountdownLayout)
        assertTrue(initial.copy(phase = GamePhase.COUNTDOWN,
            sessionStatus = SessionStatus.PAUSED).usesStartCountdownLayout)
        assertFalse(initial.copy(phase = GamePhase.QUESTION_ACTIVE).usesStartCountdownLayout)
        assertFalse(initial.copy(phase = GamePhase.FINISHED).usesStartCountdownLayout)
        assertFalse(initial.copy(sessionStatus = SessionStatus.FINISHED).usesStartCountdownLayout)
    }

    @Test
    fun `countdown supports pause resume end but never manual advance`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(mode = GameMode.CLASSIC,
            sessionStatus = SessionStatus.ACTIVE, phase = GamePhase.COUNTDOWN,
            countdownStartsAt = "2026-10-20T04:00:03Z")))
        runCurrent()
        assertTrue(viewModel.uiState.value.canPauseOrResume)
        assertTrue(viewModel.uiState.value.canEndGame)
        assertFalse(viewModel.uiState.value.canAdvance)
        viewModel.onIntent(HostGameIntent.PauseOrResume)
        runCurrent()
        assertEquals(1, socket.pauseCalls)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(mode = GameMode.CLASSIC,
            sessionStatus = SessionStatus.PAUSED, phase = GamePhase.COUNTDOWN)))
        runCurrent()
        assertTrue(viewModel.uiState.value.isPaused)
        assertEquals(null, viewModel.uiState.value.countdownTargetEpochMs)
        viewModel.onIntent(HostGameIntent.PauseOrResume)
        runCurrent()
        assertEquals(1, socket.resumeCalls)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(mode = GameMode.CLASSIC,
            sessionStatus = SessionStatus.ACTIVE, phase = GamePhase.COUNTDOWN,
            countdownStartsAt = "2026-10-20T04:00:20Z")))
        runCurrent()
        assertEquals(GamePhase.COUNTDOWN, viewModel.uiState.value.phase)
        assertEquals(java.time.Instant.parse("2026-10-20T04:00:20Z").toEpochMilli(),
            viewModel.uiState.value.countdownTargetEpochMs)
        viewModel.onIntent(HostGameIntent.ConfirmEndGame)
        runCurrent()
        assertEquals(1, socket.endCalls)
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `connected rejoins host room after every reconnect`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertEquals(2, socket.joinCalls)
        assertEquals(HostGameConnection.CONNECTED, viewModel.uiState.value.connection)
    }

    @Test
    fun `reconnect exhaustion exposes retry and retry starts a fresh connection`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed("connect", "CLIENT_RECONNECT_EXHAUSTED"))
        runCurrent()
        assertEquals(HostGameConnection.RECONNECT_FAILED, viewModel.uiState.value.connection)

        viewModel.onIntent(HostGameIntent.Retry)
        runCurrent()
        assertEquals(HostGameConnection.CONNECTING, viewModel.uiState.value.connection)
        assertEquals(listOf("host-token", "host-token"), socket.tokens)
    }

    @Test
    fun `invalid host token is refreshed before reconnecting`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Failed("connect", "GAME_TOKEN_INVALID"))
        runCurrent()

        assertEquals(1, sessions.hostTokenCalls)
        assertEquals("new-host-token", savedStateHandle.get<String>("socketToken"))
        assertEquals(listOf("host-token", "new-host-token"), socket.tokens)
    }

    @Test
    fun `server disconnect exits instead of waiting forever`() = runTest(dispatcher) {
        runCurrent()
        viewModel.effects.test {
            socket.emit(GameEvent.Disconnected(DisconnectReason.SERVER_DISCONNECT))
            runCurrent()
            assertEquals(HostGameEffect.ExitGame("Máy chủ đã đóng kết nối tới phòng này"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `late answer progress from previous question is ignored`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(hostQuestion(index = 1, answers = listOf("b")))
        socket.emit(GameEvent.HostAnswerReceivedEvent(HostAnswerReceived(0, 2, 2, 7, "Late", true)))
        runCurrent()
        assertEquals(0, viewModel.uiState.value.answeredCount)
        assertEquals(0, viewModel.uiState.value.activePlayers)
    }

    @Test
    fun `new question hides previously revealed answer`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(hostQuestion(0, listOf("a")))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ToggleAnswerKey)
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        socket.emit(hostQuestion(1, listOf("b")))
        runCurrent()
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(listOf("b"), viewModel.uiState.value.correctAnswers)
    }

    @Test
    fun `fresh reconnect snapshot does not invent host answer key`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            index = 0,
            totalQuestions = 2,
            question = question(0)
        )))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasAnswerKey)
        assertTrue(viewModel.uiState.value.correctAnswers.isEmpty())
    }

    @Test
    fun `self paced snapshot ignores fake shared question and deadline`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            mode = GameMode.SURVIVAL,
            config = GameConfig(
                timing = GameConfig.Timing(autoAdvance = false),
                flow = GameConfig.Flow(pacing = Pacing.SELF, lives = 3)
            ),
            index = 4,
            totalQuestions = 10,
            question = question(1),
            endsAt = "2026-09-10T14:02:00.000Z"
        )))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isSelfPaced)
        assertEquals(GameMode.SURVIVAL, state.mode)
        assertEquals(0, state.index)
        assertEquals(null, state.question)
        assertEquals(null, state.deadlineEpochMs)
        assertFalse(state.isManualAdvanceVisible)
        assertFalse(state.canRevealAnswer)
    }

    @Test
    fun `self paced progress merges into baseline and preserves survival fields`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.HostLeaderboardUpdated(HostLeaderboard(
            rows = listOf(HostLeaderboardRow(
                rank = 1,
                id = 31,
                playerName = "Kiro",
                playerScore = 1000,
                answeredCount = 2,
                correctCount = 2,
                wrongCount = 0,
                unansweredCount = 8,
                totalQuestions = 10,
                currentQuestionIndex = 2,
                streak = 2,
                lives = 3
            )),
            totalQuestions = 10,
            answeredTotal = 2
        )))
        socket.emit(GameEvent.HostPlayerProgressUpdated(HostPlayerProgress(
            id = 31,
            playerName = "Kiro",
            currentQuestionIndex = 3,
            playerScore = 1700,
            correctAnswersCount = 2,
            status = "connected",
            totalQuestions = 10
        )))
        runCurrent()

        val state = viewModel.uiState.value
        val row = state.leaderboard.rows.single()
        assertEquals(1700, row.playerScore)
        assertEquals(3, row.answeredCount)
        assertEquals(2, row.correctCount)
        assertEquals(1, row.wrongCount)
        assertEquals(7, row.unansweredCount)
        assertEquals(2, row.streak)
        assertEquals(3, row.lives)
        assertEquals(3, state.leaderboard.answeredTotal)
    }

    @Test
    fun `self paced terminal events update host rows`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.HostLeaderboardUpdated(HostLeaderboard(
            rows = listOf(
                HostLeaderboardRow(1, 31, "Kiro", 1000, lives = 1),
                HostLeaderboardRow(2, 32, "An", 900, lives = 2)
            )
        )))
        socket.emit(GameEvent.PlayerEliminated(EliminatedPlayer(31, "Kiro")))
        socket.emit(GameEvent.PlayerFinishedEvent(PlayerFinished(
            id = 32,
            playerScore = 2200,
            correctAnswersCount = 4,
            status = "finished",
            playerName = "An"
        )))
        runCurrent()

        val rows = viewModel.uiState.value.leaderboard.rows.associateBy { it.id }
        assertEquals("eliminated", rows.getValue(31).status)
        assertEquals(0, rows.getValue(31).lives)
        assertEquals("finished", rows.getValue(32).status)
        assertEquals(2200, rows.getValue(32).playerScore)
        assertEquals(4, rows.getValue(32).correctCount)
    }

    @Test
    fun `fire and forget command is guarded against double tap then reopens`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(
            sessionStatus = SessionStatus.ACTIVE,
            phase = GamePhase.QUESTION_ACTIVE,
            config = GameConfig(timing = GameConfig.Timing(autoAdvance = false)),
            index = 0,
            totalQuestions = 2,
            question = question(0)
        )))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceQuestion)
        viewModel.onIntent(HostGameIntent.AdvanceQuestion)
        runCurrent()
        assertEquals(1, socket.nextCalls)
        assertTrue(viewModel.uiState.value.isSendingCommand)
        advanceTimeBy(800)
        runCurrent()
        assertFalse(viewModel.uiState.value.isSendingCommand)
    }

    @Test
    fun `classic fresh question starts zero against known connected roster`() = runTest(dispatcher) {
        startClassic()
        socket.emit(roster(1, 2))
        socket.emit(hostQuestion(0, listOf("b")))
        runCurrent()
        assertEquals(HostQuestionProgress(0, 2), hostQuestionProgress(viewModel.uiState.value))
        socket.emit(answerProgress(0, 1, 2))
        socket.emit(hostQuestion(1, listOf("a")))
        socket.emit(answerProgress(0, 2, 2))
        runCurrent()
        assertEquals(HostQuestionProgress(0, 2), hostQuestionProgress(viewModel.uiState.value))
    }

    @Test
    fun `classic partial answer progress survives leaderboard refresh with unchanged roster`() = runTest(dispatcher) {
        startClassic()
        socket.emit(roster(1, 2))
        socket.emit(hostQuestion(0, listOf("b")))
        runCurrent()
        assertEquals(HostQuestionProgress(0, 2), hostQuestionProgress(viewModel.uiState.value))
        socket.emit(answerProgress(0, 1, 2))
        socket.emit(roster(1, 2))
        runCurrent()
        val progress = hostQuestionProgress(viewModel.uiState.value)!!
        assertEquals(1, progress.answered)
        assertEquals(1, progress.pending)
        assertEquals(50, progress.percent)
        assertEquals(GamePhase.QUESTION_ACTIVE, viewModel.uiState.value.phase)
    }

    @Test
    fun `classic single player completion follows immediate server results without artificial hold`() = runTest(dispatcher) {
        startClassic()
        socket.emit(roster(1))
        socket.emit(hostQuestion(0, listOf("b")))
        runCurrent()
        assertEquals(HostQuestionProgress(0, 1), hostQuestionProgress(viewModel.uiState.value))
        socket.emit(answerProgress(0, 1, 1))
        socket.emit(GameEvent.QuestionLocked(0, QuestionLockReason.ALL_ANSWERED))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0)))
        runCurrent()
        assertEquals(1, viewModel.uiState.value.answeredCount)
        assertEquals(GamePhase.SHOWING_RESULTS, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.usesClassicQuestionLayout)
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `classic reconnect counts cannot be inferred from cumulative leaderboard`() = runTest(dispatcher) {
        startClassic()
        socket.emit(GameEvent.HostLeaderboardUpdated(HostLeaderboard(rows = listOf(
            HostLeaderboardRow(1, 1, "A", 9500, answeredCount = 10, correctCount = 10),
            HostLeaderboardRow(2, 2, "B", 8000, answeredCount = 9, correctCount = 8)
        ), answeredTotal = 19)))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasAnswerProgress)
        assertEquals(null, hostQuestionProgress(viewModel.uiState.value))
        socket.emit(answerProgress(0, 1, 2))
        runCurrent()
        assertEquals(HostQuestionProgress(1, 2), hostQuestionProgress(viewModel.uiState.value))
    }

    @Test
    fun `classic roster replacement with same size invalidates current question counts`() = runTest(dispatcher) {
        startClassic()
        socket.emit(roster(1, 2))
        socket.emit(answerProgress(0, 1, 2))
        runCurrent()
        assertTrue(viewModel.uiState.value.hasAnswerProgress)
        socket.emit(roster(1, 3))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasAnswerProgress)
        assertEquals(null, hostQuestionProgress(viewModel.uiState.value))
        socket.emit(answerProgress(0, 1, 2))
        runCurrent()
        assertEquals(HostQuestionProgress(1, 2), hostQuestionProgress(viewModel.uiState.value))
    }

    @Test
    fun `classic reconnect needs a new snapshot before allowing commands`() = runTest(dispatcher) {
        startClassic()
        socket.emit(answerProgress(0, 1, 2))
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        runCurrent()
        assertFalse(viewModel.uiState.value.isStateSynchronized)
        assertFalse(viewModel.uiState.value.canCloseClassicQuestion)
        assertFalse(viewModel.uiState.value.canPauseOrResume)
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(0, socket.nextCalls)
        socket.emit(classicSnapshot())
        runCurrent()
        assertTrue(viewModel.uiState.value.canCloseClassicQuestion)
        assertEquals(null, hostQuestionProgress(viewModel.uiState.value))
    }

    @Test
    fun `classic same question pause resume preserves known counts but invents no frozen timer`() = runTest(dispatcher) {
        startClassic()
        socket.emit(answerProgress(0, 1, 2))
        socket.emit(classicSnapshot(status = SessionStatus.PAUSED, endsAt = null))
        runCurrent()
        assertEquals(HostQuestionProgress(1, 2), hostQuestionProgress(viewModel.uiState.value))
        assertFalse(viewModel.uiState.value.canCloseClassicQuestion)
        assertEquals(null, hostQuestionTime(viewModel.uiState.value, 0).seconds)
        socket.emit(classicSnapshot())
        runCurrent()
        assertTrue(viewModel.uiState.value.canCloseClassicQuestion)
        assertEquals(HostQuestionProgress(1, 2), hostQuestionProgress(viewModel.uiState.value))
    }

    @Test
    fun `classic confirmed close sends once and waits for server phase`() = runTest(dispatcher) {
        startClassic()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(1, socket.nextCalls)
        assertEquals(GamePhase.QUESTION_ACTIVE, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.isSendingCommand)
    }

    @Test
    fun `classic old close confirmation cannot advance the results phase`() = runTest(dispatcher) {
        startClassic()
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0)))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(0, socket.nextCalls)
        assertEquals(GamePhase.SHOWING_RESULTS, viewModel.uiState.value.phase)
    }

    @Test
    fun `classic old close confirmation cannot close a newer question`() = runTest(dispatcher) {
        startClassic()
        socket.emit(hostQuestion(1, listOf("b")))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(0, socket.nextCalls)
        assertEquals(1, viewModel.uiState.value.index)
    }

    @Test
    fun `classic close rejects wrong question id auto mode and pause`() = runTest(dispatcher) {
        startClassic()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 99))
        socket.emit(classicSnapshot(autoAdvance = true))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        socket.emit(classicSnapshot(status = SessionStatus.PAUSED))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `classic lock before confirmation cannot trigger next and does not claim all answered`() = runTest(dispatcher) {
        startClassic()
        socket.emit(GameEvent.QuestionLocked(0, QuestionLockReason.ALL_ANSWERED))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ConfirmCloseQuestion(0, 10))
        runCurrent()
        assertEquals(0, socket.nextCalls)
        assertEquals("Câu hỏi đã đóng. Đang chờ kết quả từ máy chủ…", hostQuestionStatus(viewModel.uiState.value))
    }

    @Test
    fun `classic end confirmation and leaving are different socket operations`() = runTest(dispatcher) {
        startClassic()
        viewModel.onIntent(HostGameIntent.RequestEndGame)
        runCurrent()
        assertTrue(viewModel.uiState.value.isEndDialogOpen)
        assertEquals(0, socket.endCalls)
        viewModel.onIntent(HostGameIntent.DismissEndDialog)
        runCurrent()
        assertEquals(0, socket.endCalls)
        viewModel.onIntent(HostGameIntent.RequestEndGame)
        viewModel.onIntent(HostGameIntent.ConfirmEndGame)
        runCurrent()
        assertEquals(1, socket.endCalls)
        assertEquals(0, socket.disconnectCalls)
        viewModel.onIntent(HostGameIntent.LeaveGame)
        runCurrent()
        assertEquals(1, socket.disconnectCalls)
        assertEquals(1, socket.endCalls)
    }

    @Test
    fun `classic pause resume waits for authoritative snapshot`() = runTest(dispatcher) {
        startClassic()
        viewModel.onIntent(HostGameIntent.PauseOrResume)
        runCurrent()
        assertEquals(1, socket.pauseCalls)
        assertFalse(viewModel.uiState.value.isPaused)
        socket.emit(classicSnapshot(status = SessionStatus.PAUSED, endsAt = null))
        runCurrent()
        viewModel.onIntent(HostGameIntent.PauseOrResume)
        runCurrent()
        assertEquals(1, socket.resumeCalls)
        assertTrue(viewModel.uiState.value.isPaused)
    }

    @Test
    fun `classic malformed clock never becomes an untimed question`() = runTest(dispatcher) {
        startClassic()
        socket.emit(classicSnapshot(endsAt = "invalid-deadline"))
        runCurrent()
        assertFalse(viewModel.uiState.value.isQuestionClockKnown)
        assertEquals("Chờ đồng bộ thời gian", hostQuestionTime(viewModel.uiState.value, 0).label)
        socket.emit(classicSnapshot(endsAt = null))
        runCurrent()
        assertTrue(viewModel.uiState.value.isQuestionClockKnown)
        assertEquals("Không giới hạn thời gian", hostQuestionTime(viewModel.uiState.value, 0).label)
    }

    @Test
    fun `classic results reveal key according to actual config and reset on next question`() = runTest(dispatcher) {
        startClassic()
        socket.emit(hostQuestion(0, listOf("a")))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        assertTrue(viewModel.uiState.value.usesClassicRoundLayout)
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(listOf("a"), viewModel.uiState.value.correctAnswers)
        socket.emit(hostQuestion(1, listOf("b")))
        runCurrent()
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        assertFalse(viewModel.uiState.value.hasResultsLeaderboard)
        assertEquals(null, viewModel.uiState.value.results)
    }

    @Test
    fun `hidden config does not auto reveal cached key but host can reveal it manually`() = runTest(dispatcher) {
        startClassic()
        socket.emit(classicSnapshot(showCorrect = false))
        socket.emit(hostQuestion(0, listOf("a")))
        runCurrent()
        viewModel.onIntent(HostGameIntent.ToggleAnswerKey)
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        assertTrue(viewModel.uiState.value.hasAnswerKey)
        viewModel.onIntent(HostGameIntent.ToggleAnswerKey)
        runCurrent()
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(null, hostResultColumns(viewModel.uiState.value))
    }

    @Test
    fun `missing config does not default to auto reveal until a trusted snapshot arrives`() = runTest(dispatcher) {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(GameEvent.StateSnapshot(GameSnapshot(mode = GameMode.CLASSIC,
            phase = GamePhase.QUESTION_ACTIVE, sessionStatus = SessionStatus.ACTIVE,
            index = 0, question = question(0))))
        socket.emit(hostQuestion(0, listOf("a")))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        assertEquals(null, viewModel.uiState.value.showCorrectAnswer)
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        socket.emit(classicSnapshot(phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
    }

    @Test
    fun `explicit hide is idempotent and survives duplicate results and same question snapshot`() = runTest(dispatcher) {
        startClassic()
        socket.emit(hostQuestion(0, listOf("a")))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        viewModel.onIntent(HostGameIntent.HideAnswerKey)
        viewModel.onIntent(HostGameIntent.HideAnswerKey)
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        socket.emit(classicSnapshot(phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        assertFalse(viewModel.uiState.value.isAnswerRevealed)
        assertTrue(viewModel.uiState.value.hasAnswerKey)
    }

    @Test
    fun `classic rejects stale index and mismatched question id results`() = runTest(dispatcher) {
        startClassic()
        socket.emit(hostQuestion(1, listOf("b")))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10, listOf("a"))))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(1, 99, listOf("a"))))
        runCurrent()
        assertEquals(GamePhase.QUESTION_ACTIVE, viewModel.uiState.value.phase)
        assertEquals(listOf("b"), viewModel.uiState.value.correctAnswers)
        assertEquals(null, viewModel.uiState.value.results)
    }

    @Test
    fun `late results cannot reopen a finished classic match`() = runTest(dispatcher) {
        startClassic()
        socket.emit(classicSnapshot(status = SessionStatus.FINISHED, phase = GamePhase.FINISHED))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        assertTrue(viewModel.uiState.value.isFinished)
        assertEquals(GamePhase.FINISHED, viewModel.uiState.value.phase)
    }

    @Test
    fun `results wait for a post result leaderboard and reset freshness after reconnect`() = runTest(dispatcher) {
        startClassic()
        socket.emit(roster(1, 2))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasResultsLeaderboard)
        socket.emit(roster(1, 2))
        runCurrent()
        assertTrue(viewModel.uiState.value.hasResultsLeaderboard)
        socket.emit(classicSnapshot(phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        assertTrue(viewModel.uiState.value.hasResultsLeaderboard)
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        socket.emit(GameEvent.Connected)
        socket.emit(classicSnapshot(phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        assertFalse(viewModel.uiState.value.hasResultsLeaderboard)
    }

    @Test
    fun `stale next result action cannot close active or newer questions`() = runTest(dispatcher) {
        startClassic()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(1))
        socket.emit(hostQuestion(1, listOf("b")))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        runCurrent()
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `manual next from results sends once and waits for authoritative next question`() = runTest(dispatcher) {
        startClassic()
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10)))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        runCurrent()
        assertEquals(1, socket.nextCalls)
        assertEquals(GamePhase.SHOWING_RESULTS, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.isSendingCommand)
    }

    @Test
    fun `results next is blocked for auto paused or unsynchronized sessions`() = runTest(dispatcher) {
        startClassic()
        socket.emit(classicSnapshot(autoAdvance = true, phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        socket.emit(classicSnapshot(status = SessionStatus.PAUSED, phase = GamePhase.SHOWING_RESULTS))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        socket.emit(GameEvent.Disconnected(DisconnectReason.TRANSPORT))
        runCurrent()
        viewModel.onIntent(HostGameIntent.AdvanceFromResults(0))
        runCurrent()
        assertEquals(0, socket.nextCalls)
    }

    @Test
    fun `results clock replaces old next deadline after pause resume and never falls back to stale payload`() = runTest(dispatcher) {
        startClassic()
        socket.emit(classicSnapshot(autoAdvance = true))
        socket.emit(GameEvent.QuestionResultsReceived(QuestionResults(0, 10,
            nextQuestionAt = "1970-01-01T00:00:05Z")))
        runCurrent()
        assertEquals(4, hostQuestionTime(viewModel.uiState.value, 1000).seconds)
        socket.emit(classicSnapshot(autoAdvance = true, status = SessionStatus.PAUSED,
            phase = GamePhase.SHOWING_RESULTS, endsAt = null))
        runCurrent()
        assertEquals(null, hostQuestionTime(viewModel.uiState.value, 1000).seconds)
        socket.emit(classicSnapshot(autoAdvance = true, phase = GamePhase.SHOWING_RESULTS,
            endsAt = "1970-01-01T00:00:20Z"))
        runCurrent()
        assertEquals(19, hostQuestionTime(viewModel.uiState.value, 1000).seconds)
        socket.emit(classicSnapshot(autoAdvance = true, phase = GamePhase.SHOWING_RESULTS, endsAt = "invalid"))
        runCurrent()
        assertEquals(null, hostQuestionTime(viewModel.uiState.value, 1000).seconds)
    }

    @Test
    fun `result leaderboard visibility follows each received flow configuration`() = runTest(dispatcher) {
        startClassic()
        for (setting in ShowLeaderboard.entries) {
            socket.emit(classicSnapshot(phase = GamePhase.SHOWING_RESULTS, board = setting))
            runCurrent()
            assertEquals(setting, viewModel.uiState.value.showLeaderboard)
            assertEquals(setting == ShowLeaderboard.BETWEEN_QUESTIONS, hostShowsResultsLeaderboard(viewModel.uiState.value))
        }
    }

    private suspend fun TestScope.startClassic() {
        runCurrent()
        socket.emit(GameEvent.Connected)
        socket.emit(classicSnapshot())
        runCurrent()
    }

    private fun classicSnapshot(
        autoAdvance: Boolean = false,
        status: SessionStatus = SessionStatus.ACTIVE,
        endsAt: String? = "2026-10-20T04:00:30Z",
        phase: GamePhase = GamePhase.QUESTION_ACTIVE,
        showCorrect: Boolean = true,
        board: ShowLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
    ) = GameEvent.StateSnapshot(GameSnapshot(
        mode = GameMode.CLASSIC, sessionStatus = status, phase = phase,
        config = GameConfig(timing = GameConfig.Timing(autoAdvance = autoAdvance),
            flow = GameConfig.Flow(showCorrectAnswer = showCorrect, showLeaderboard = board)),
        index = 0, totalQuestions = 2, question = question(0), endsAt = endsAt,
    ))

    private fun roster(vararg ids: Long) = GameEvent.HostLeaderboardUpdated(HostLeaderboard(
        rows = ids.mapIndexed { index, id -> HostLeaderboardRow(index + 1, id, "Player $id", 1000) },
    ))

    private fun answerProgress(index: Int, answered: Int, total: Int) =
        GameEvent.HostAnswerReceivedEvent(HostAnswerReceived(index, answered, total, 1, "Player", true))

    private fun hostQuestion(index: Int, answers: List<String>) = GameEvent.HostQuestionReceived(
        HostQuestion(question(index), answers, totalQuestions = 2)
    )

    private fun question(index: Int) = PublicQuestion(
        index = index,
        total = 2,
        id = 10L + index,
        questionType = "multiple_choice",
        questionText = "Question $index",
        answerOptions = listOf(PublicAnswerOption("a", "A"), PublicAnswerOption("b", "B"))
    )
}

private class FakeHostGameSocketRepository : HostGameSocketRepository {
    private val events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val tokens = mutableListOf<String>()
    var joinCalls = 0
    var nextCalls = 0
    var pauseCalls = 0
    var resumeCalls = 0
    var endCalls = 0
    var disconnectCalls = 0
    override fun events(socketToken: String): Flow<GameEvent> {
        tokens += socketToken
        return events
    }
    suspend fun emit(event: GameEvent) { events.emit(event) }
    override suspend fun joinLobby() { joinCalls++ }
    override suspend fun disconnect() { disconnectCalls++ }
    override suspend fun startGame() = Unit
    override suspend fun nextQuestion() { nextCalls++ }
    override suspend fun pauseGame() { pauseCalls++ }
    override suspend fun resumeGame() { resumeCalls++ }
    override suspend fun endGame() { endCalls++ }
    override suspend fun updateConfig(patch: Map<GameConfigKey, GameConfigValue>): Result<ConfigUpdateAck> =
        error("Not used by HostGameViewModel tests")
}

private class FakeGameSessionRepository : GameSessionRepository {
    var hostTokenCalls = 0

    override suspend fun getHostToken(gameId: Long): Result<String> {
        hostTokenCalls++
        return Result.Success("new-host-token")
    }

    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> = error("Not used")
    override suspend fun createGameSession(
        params: CreateGameSessionParams
    ): Result<CreateGameSessionResult> = error("Not used")
    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> = error("Not used")
    override suspend fun joinRoom(
        sessionCode: String,
        playerName: String?,
        guestId: String?
    ): Result<JoinRoomResult> = error("Not used")
    override suspend fun getGameHistory(
        role: android.kma.myquizzapp.core.common.model.GameHistoryRole,
        cursor: String?,
        limit: Int,
        guestId: String?,
        includeTotal: Boolean
    ): Result<List<android.kma.myquizzapp.core.common.model.GameHistoryItem>> = error("Not used")
    override suspend fun getGameHistorySummary(gameId: Long, guestId: String?): Result<android.kma.myquizzapp.core.common.model.GameHistorySummary> = error("Not used")
    override suspend fun getGameHistoryAnswers(gameId: Long, guestId: String?): Result<GameReview> = error("Not used")
    override suspend fun getGameResults(gameId: Long): Result<GameResults> = error("Not used")
    override suspend fun getGameReview(
        gameId: Long,
        socketToken: String
    ): Result<GameReview> = error("Not used")
}
