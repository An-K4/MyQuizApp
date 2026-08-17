package android.kma.myquizzapp.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme với primary #7B61FF
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    
    outline = Outline,
    outlineVariant = OutlineVariant
)

/**
 * Dark color scheme với background đen
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = OnPrimaryContainer,
    onPrimaryContainer = PrimaryContainer,
    
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = OnSecondaryContainer,
    onSecondaryContainer = SecondaryContainer,
    
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = OnTertiaryContainer,
    onTertiaryContainer = TertiaryContainer,
    
    error = Error,
    onError = OnError,
    errorContainer = OnErrorContainer,
    onErrorContainer = ErrorContainer,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = OnSurfaceVariant,
    onSurfaceVariant = SurfaceVariant,
    
    outline = Outline,
    outlineVariant = OutlineVariant
)

/**
 * MyQuizApp Material3 Theme
 * 
 * @param darkTheme Auto-detect dark mode từ system
 * @param content Screen content
 */
@Composable
fun MyQuizAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
