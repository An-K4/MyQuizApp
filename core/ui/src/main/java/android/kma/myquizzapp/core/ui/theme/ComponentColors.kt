package android.kma.myquizzapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Light colors match design/image.png; dark roles are provisional Material fallbacks. */
internal object ComponentColors {
    private val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val softSurface: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val foreground: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    val selectedSurface: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
    val selectedForeground: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Primary
    val successSurface: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.CorrectAnswerBackground
    val successForeground: Color
        @Composable get() = if (isDark) FrontendColors.Success else FrontendColors.SuccessForeground
    val dangerSurface: Color
        @Composable get() = MaterialTheme.colorScheme.errorContainer
    val dangerForeground: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.onErrorContainer else FrontendColors.Danger
    val warning: Color
        @Composable get() = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Warning
}
