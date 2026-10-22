package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.PersonalQuestionResult
import android.kma.myquizzapp.core.common.model.QuestionResults
import java.time.Instant

internal fun String?.serverMillis(): Long? = this?.let {
    runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
}

internal fun newerServerTime(old: String?, incoming: String?): String? =
    if ((incoming.serverMillis() ?: Long.MIN_VALUE) >= (old.serverMillis() ?: Long.MIN_VALUE)) incoming ?: old else old

/** Shared by realtime and player:sync. Assignment only: never increment scores/streak locally. */
internal fun GameUiState.reconcileClassicPersonalResult(
    result: PersonalQuestionResult?,
    restored: Boolean = false
): GameUiState {
    result ?: return this
    if (!isClassic || showCorrectAnswer != true || phase == GamePhaseUi.Finished ||
        phase == GamePhaseUi.Eliminated || phase == GamePhaseUi.PlayerFinished ||
        phase is GamePhaseUi.Countdown || question?.index != result.index || question.id != result.questionId
    ) return this
    val resultTime = result.serverTime.serverMillis() ?: return this
    if (resultTime < (personalResult?.serverTime.serverMillis() ?: Long.MIN_VALUE)) return this
    // A delayed closure may not reopen a newer active snapshot of the same question.
    if (phase !is GamePhaseUi.Results && resultTime < (classicPhaseServerTime.serverMillis() ?: Long.MIN_VALUE)) return this
    val alreadyClosed = phase is GamePhaseUi.Results
    return copy(
        personalResult = result,
        acceptedAnswerKeys = acceptedAnswerKeys.takeIf { result.answered },
        phase = if (alreadyClosed) phase else GamePhaseUi.Results(restoredWithoutDetails = results == null),
        totalScore = result.totalScore,
        scoreEarned = result.scoreEarned,
        streak = result.streak,
        outcome = when (result.isCorrect) {
            true -> QuestionOutcome.CORRECT
            false -> QuestionOutcome.INCORRECT
            null -> QuestionOutcome.HIDDEN
        },
        // Snapshot/common timing wins. In particular a late private event cannot unpause a clock.
        endsAt = if (!restored && !alreadyClosed && !isPaused) result.nextQuestionAt else endsAt,
        classicPhaseServerTime = if (restored || alreadyClosed) classicPhaseServerTime
            else newerServerTime(classicPhaseServerTime, result.serverTime),
        classicFeedbackRemainingMs = if (restored) 0L else classicFeedbackRemainingMs,
        hasAnswerAttempt = result.answered,
        isInputLocked = true,
        isSubmitting = false,
        isConfirming = false
    )
}

internal fun GameUiState.reconcileClassicCommonResult(result: QuestionResults, serverTime: String?): GameUiState {
    if (phase == GamePhaseUi.Finished || question?.index != result.index ||
        (result.questionId != null && question.id != result.questionId)
    ) return this
    val alreadyClosed = phase is GamePhaseUi.Results
    if (!alreadyClosed && (serverTime.serverMillis() ?: Long.MAX_VALUE) <
        (classicPhaseServerTime.serverMillis() ?: Long.MIN_VALUE)) return this
    val feedback = resolveQuestionFeedback(showCorrectAnswer, question.questionType,
        acceptedAnswerKeys.orEmpty(), result)
    return copy(
        phase = GamePhaseUi.Results(),
        results = feedback.results,
        outcome = visiblePersonalResult?.let { outcome }
            ?: if (acceptedAnswerKeys == null) QuestionOutcome.HIDDEN else feedback.outcome,
        endsAt = if (alreadyClosed || classicTimingFromSnapshot || isPaused) endsAt else result.nextQuestionAt,
        classicPhaseServerTime = newerServerTime(classicPhaseServerTime, serverTime),
        classicFeedbackRemainingMs = when {
            alreadyClosed -> classicFeedbackRemainingMs
            showCorrectAnswer == true && feedback.results.correctAnswers.isNotEmpty() -> minOf(CLASSIC_FEEDBACK_DURATION_MS,
                result.nextQuestionAt.serverMillis()?.let { deadline ->
                    serverTime.serverMillis()?.let { (deadline - it).coerceAtLeast(0L) }
                } ?: CLASSIC_FEEDBACK_DURATION_MS)
            else -> 0L
        },
        isResultsLeaderboardFresh = alreadyClosed && isResultsLeaderboardFresh,
        leaderboard = if (alreadyClosed) leaderboard else emptyList(),
        playerRank = playerRank.takeIf { alreadyClosed },
        playerScore = playerScore.takeIf { alreadyClosed },
        isInputLocked = true,
        isSubmitting = false,
        isConfirming = false
    )
}
