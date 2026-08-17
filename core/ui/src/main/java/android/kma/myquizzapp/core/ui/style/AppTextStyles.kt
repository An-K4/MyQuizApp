@file:Suppress("unused")
package android.kma.myquizzapp.core.ui.style

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Centralized text styles for the entire app
 * Provides consistent typography across all features
 * 
 * Usage:
 * ```
 * Text("Hello", style = AppTextStyles.buttonText)
 * Text("Link", style = AppTextStyles.linkText)
 * ```
 */
object AppTextStyles {
    
    /** Large title for screen headers */
    val titleLarge: TextStyle
        @Composable get() = MaterialTheme.typography.headlineLarge
    
    /** Medium body text for descriptions and subtitles */
    val bodyMedium: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium
    
    /** Bold, prominent text for buttons - stands out as accent! */
    val buttonText: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold
        )
    
    /** Bold text for links and clickable elements - stands out! */
    val linkText: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold
        )
    
    /** Small caption text for secondary information */
    val caption: TextStyle
        @Composable get() = MaterialTheme.typography.bodySmall
}
