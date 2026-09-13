package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults

enum class GameConnection { CONNECTING, CONNECTED, RECONNECTING }

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
    val phase: GamePhaseUi = GamePhaseUi.Connecting,
    val question: PublicQuestion? = null,
    val selectedOptionId: String? = null,
    val selectedOptionIds: Set<String> = emptySet(),
    val textAnswer: String = "",
    val endsAt: String? = null,
    val serverOffsetMs: Long = 0L,
    val remainingSeconds: Int? = null,
    val isInputLocked: Boolean = true,
    val isSubmitting: Boolean = false,
    val isConfirming: Boolean = false,
    val results: QuestionResults? = null,
    val lives: Int? = null,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean get() = !isInputLocked && when (question?.questionType) {
        "multiple_choice" -> selectedOptionId != null
        "multiple_select" -> selectedOptionIds.isNotEmpty()
        "short_answer", "long_answer" -> textAnswer.isNotBlank()
        else -> false
    }
}

sealed interface GameIntent {
    data class SelectOption(val id: String) : GameIntent
    data class ToggleOption(val id: String) : GameIntent
    data class ChangeText(val value: String) : GameIntent
    data object Submit : GameIntent
    data object Retry : GameIntent
    data object Sync : GameIntent
    data object Leave : GameIntent
    data object DeadlineReached : GameIntent
    data object ErrorShown : GameIntent
}

sealed interface GameEffect {
    data class Exit(val message: String?) : GameEffect
}
