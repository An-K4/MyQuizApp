package android.kma.myquizzapp.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.kma.myquizzapp.core.common.model.QuizCard
import coil.compose.AsyncImage

/**
 * Reusable quiz card component.
 * 
 * Displays:
 * - Thumbnail image
 * - Quiz title
 * - Owner info (name + avatar)
 * - Stats (question count, play count)
 */
@Composable
fun QuizCardItem(
    quiz: QuizCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thumbnail. N16: fallback/error placeholder — quiz không có cover (hoặc ảnh lỗi)
            // không hiện ô trống nữa. TODO(polish): thay bằng ảnh mặc định trong res/.
            AsyncImage(
                model = quiz.quizImage,
                contentDescription = quiz.quizName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
                fallback = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_gallery)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = quiz.quizName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Owner info
            if (quiz.owner != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = quiz.owner?.avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    quiz.owner?.fullname?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${quiz.questionCount} câu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${quiz.playCount} lượt chơi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
