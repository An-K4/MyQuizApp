package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/** Reuses the approved light tokens; dark is a Material fallback, not a new palette. */
internal data class HistoryDetailColors(
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val border: Color,
    val card: Color,
    val summary: Color,
    val correct: Color,
    val correctSurface: Color,
    val incorrect: Color,
    val incorrectSurface: Color,
    val track: Color
)

@Composable
internal fun historyDetailColors(): HistoryDetailColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return HistoryDetailColors(
        foreground = if (dark) scheme.onSurface else FrontendColors.Foreground,
        muted = if (dark) scheme.onSurfaceVariant else FrontendColors.MutedForeground,
        accent = scheme.primary,
        border = if (dark) scheme.outlineVariant else FrontendColors.Border,
        card = if (dark) scheme.surfaceVariant else FrontendColors.SoftBackground,
        summary = if (dark) scheme.surfaceVariant else FrontendColors.BrandTint,
        correct = if (dark) FrontendColors.Success else FrontendColors.SuccessForeground,
        correctSurface = if (dark) FrontendColors.Success.copy(alpha = 0.12f).compositeOver(scheme.surface)
            else FrontendColors.CorrectAnswerBackground,
        incorrect = if (dark) scheme.onErrorContainer else FrontendColors.Danger,
        incorrectSurface = if (dark) scheme.errorContainer
            else FrontendColors.Danger.copy(alpha = 0.07f).compositeOver(FrontendColors.PageBackground),
        track = if (dark) scheme.surface else FrontendColors.Border
    )
}
