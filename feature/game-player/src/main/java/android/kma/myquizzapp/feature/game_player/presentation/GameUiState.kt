package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.common.model.ShowLeaderboard

enum class GameConnection { CONNECTING, CONNECTED, RECONNECTING, RECONNECT_FAILED }

enum class QuestionOutcome { CORRECT, INCORRECT, HIDDEN }

sealed interface GamePhaseUi {
    data object Connecting : GamePhaseUi
    data class Countdown(val startsAt: String?) : GamePhaseUi
    data object Question : GamePhaseUi
    data object Submitted : GamePhaseUi
    data object Locked : GamePhaseUi
    data class Results(val restoredWithoutDetails: Boolean = false) : GamePhaseUi
    data object Finished : GamePhaseUi
}

data class GameUiState(
    val connection: GameConnection = GameConnection.CONNECTING,
    val playerId: Long? = null,
    val sessionStatus: SessionStatus? = null,
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
    val matchEndsAt: String? = null,
    val allowAnswerLate: Boolean = false,
    val serverOffsetMs: Long = 0L,
    val remainingSeconds: Int? = null,
    val isInputLocked: Boolean = true,
    val isSubmitting: Boolean = false,
    val isConfirming: Boolean = false,
    val canRequestNext: Boolean = false,
    val isRequestingNext: Boolean = false,
    val results: QuestionResults? = null,
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
    val lives: Int? = null,
    val errorMessage: String? = null
) {
    val isPaused: Boolean get() = sessionStatus == SessionStatus.PAUSED
    val isSelfPaced: Boolean get() = pacing == Pacing.SELF
    val isAnswerInputEnabled: Boolean get() = !isInputLocked && !isPaused
    val shouldShowNextAction: Boolean
        get() = isSelfPaced && !autoAdvance && phase is GamePhaseUi.Results && canRequestNext
    val canShowLiveLeaderboard: Boolean
        get() = showLeaderboard == ShowLeaderboard.BETWEEN_QUESTIONS &&
            phase is GamePhaseUi.Results && leaderboard.isNotEmpty()

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
