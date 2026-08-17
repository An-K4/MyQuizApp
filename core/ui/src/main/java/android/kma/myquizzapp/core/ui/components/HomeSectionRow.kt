package android.kma.myquizzapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.kma.myquizzapp.core.common.model.HomeSection

/**
 * Horizontal section component for home screen.
 * 
 * Displays:
 * - Section title
 * - Horizontal scrolling row of quiz cards
 */
@Composable
fun HomeSectionRow(
    section: HomeSection,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Section title
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Horizontal scrolling row of quiz cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = section.items,
                key = { it.id }
            ) { quiz ->
                QuizCardItem(
                    quiz = quiz,
                    onClick = { onQuizClick(quiz.id) }
                )
            }
        }
    }
}
