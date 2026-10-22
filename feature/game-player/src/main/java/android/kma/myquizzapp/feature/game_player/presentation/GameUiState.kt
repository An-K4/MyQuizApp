package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PersonalQuestionResult
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard

enum class GameConnection { CONNECTING, CONNECTED, RECONNECTING, RECONNECT_FAILED }

/** Local feedback within the server's results window; never delays a server transition. */
internal const val CLASSIC_FEEDBACK_DURATION_MS = 2_000L

enum class QuestionOutcome { CORRECT, INCORRECT, HIDDEN }

sealed interface GamePhaseUi {
    data object Connecting : GamePhaseUi
    data class Countdown(val startsAt: String?) : GamePhaseUi
    data object Question : GamePhaseUi
    data object Submitted : GamePhaseUi
    data object Locked : GamePhaseUi
    data class Results(val restoredWithoutDetails: Boolean = false) : GamePhaseUi
    data object Eliminated : GamePhaseUi
    data object PlayerFinished : GamePhaseUi
    data object Finished : GamePhaseUi
}

data class GameUiState(
    val connection: GameConnection = GameConnection.CONNECTING,
    val isSynchronizing: Boolean = false,
    val playerId: Long? = null,
    val sessionStatus: SessionStatus? = null,
    val mode: GameMode? = null,
    val pacing: Pacing = Pacing.HOST,
    val autoAdvance: Boolean = true,
    val showCorrectAnswer: Boolean? = null,
    val showLeaderboard: ShowLeaderboard? = null,
    val phase: GamePhaseUi = GamePhaseUi.Connecting,
    val question: PublicQuestion? = null,
    val selectedOptionId: String? = null,
    val selectedOptionIds: Set<String> = emptySet(),
    val textAnswer: String = "",
    val endsAt: String? = null,
    /** Original duration from question:started; never inferred from reconnect time left. */
    val timeLimitSeconds: Int? = null,
    val matchEndsAt: String? = null,
    val allowAnswerLate: Boolean = false,
    val serverOffsetMs: Long = 0L,
    val remainingSeconds: Int? = null,
    val isInputLocked: Boolean = true,
    val isSubmitting: Boolean = false,
    val isConfirming: Boolean = false,
    val canRequestNext: Boolean = false,
    val isRequestingNext: Boolean = false,
    /** Only ACK/snapshot evidence, never an editable draft, may be graded. */
    val acceptedAnswerKeys: List<String>? = null,
    val hasAnswerAttempt: Boolean = false,
    val classicFeedbackRemainingMs: Long = 0L,
    val isResultsLeaderboardFresh: Boolean = false,
    val results: QuestionResults? = null,
    val personalResult: PersonalQuestionResult? = null,
    /** Latest authoritative phase time, not a locally invented results deadline. */
    val classicPhaseServerTime: String? = null,
    val classicTimingFromSnapshot: Boolean = false,
    val outcome: QuestionOutcome? = null,
    val answeredCount: Int? = null,
    val activePlayers: Int? = null,
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val playerRank: Int? = null,
    val playerScore: Int? = null,
    val totalScore: Int? = null,
    val scoreEarned: Int? = null,
    val streak: Int? = null,
    val wasLate: Boolean = false,
    val timedOut: Boolean = false,
    val lives: Int? = null,
    val matchTimeExpired: Boolean = false,
    val errorMessage: String? = null
) {
    val isClassic: Boolean get() = mode == GameMode.CLASSIC && pacing == Pacing.HOST
    val usesClassicQuestionLayout: Boolean
        get() = isClassic && question != null &&
            (phase == GamePhaseUi.Question || phase == GamePhaseUi.Submitted || phase == GamePhaseUi.Locked ||
                (phase is GamePhaseUi.Results && !showsClassicSummary))
    val skipsClassicSummary: Boolean
        get() = showCorrectAnswer == false && showLeaderboard != null &&
            showLeaderboard != ShowLeaderboard.BETWEEN_QUESTIONS
    val showsClassicSummary: Boolean
        get() = isClassic && phase is GamePhaseUi.Results && !skipsClassicSummary &&
            (classicFeedbackRemainingMs <= 0L || showCorrectAnswer != true ||
                results?.correctAnswers.isNullOrEmpty() || question == null)
    val revealsClassicFeedback: Boolean
        get() = isClassic && phase is GamePhaseUi.Results && showCorrectAnswer == true &&
            !results?.correctAnswers.isNullOrEmpty()
    val visiblePersonalResult: PersonalQuestionResult?
        get() = personalResult?.takeIf { isClassic && phase is GamePhaseUi.Results &&
            showCorrectAnswer == true && it.index == question?.index && it.questionId == question?.id }
    val canShowClassicRank: Boolean
        get() = isClassic && phase is GamePhaseUi.Results && showCorrectAnswer == true &&
            showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS
    val isPaused: Boolean get() = sessionStatus == SessionStatus.PAUSED
    val isSelfPaced: Boolean get() = pacing == Pacing.SELF
    val isAnswerInputEnabled: Boolean get() = !isInputLocked && !isPaused
    val shouldShowNextAction: Boolean
        get() = isSelfPaced && !autoAdvance && phase is GamePhaseUi.Results && canRequestNext
    val hasMatchBudget: Boolean get() = matchEndsAt != null
    val isPersonallyDone: Boolean
        get() = phase == GamePhaseUi.Eliminated || phase == GamePhaseUi.PlayerFinished
    val canShowLiveLeaderboard: Boolean
        get() = leaderboard.isNotEmpty() && when {
            showLeaderboard == ShowLeaderboard.NEVER -> false
            isPersonallyDone -> true
            else -> showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS && phase is GamePhaseUi.Results &&
                (!isClassic || showCorrectAnswer == true)
        }

    val canSubmit: Boolean get() = isAnswerInputEnabled && when (question?.questionType) {
        "multiple_choice" -> selectedOptionId != null
        "multiple_select" -> selectedOptionIds.isNotEmpty()
        "short_answer", "long_answer" -> textAnswer.isNotBlank()
        else -> false
    }

    val submittedAnswerKeys: List<String> get() = when (question?.questionType) {
        "multiple_choice" -> listOfNotNull(selectedOptionId)
        "multiple_select" -> question.answerOptions.map { it.id }.filter(selectedOptionIds::contains)
        "short_answer", "long_answer" -> listOf(textAnswer).filter(String::isNotBlank)
        else -> emptyList()
    }
}
