package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import android.kma.myquizzapp.core.common.model.QuizCard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.kma.myquizzapp.core.ui.theme.ComponentColors
import android.kma.myquizzapp.core.common.model.HomeSection

/**
 * Một section nằm ngang trên Trang chủ: tiêu đề + LazyRow các thế quiz.
 *
 * @param onSeeMore nếu khác null thì hiện nút "Xem thêm" căn phải, ngang hàng
 *   tiêu đề (N19.6).
 *
 *   Vì sao là `(() -> Unit)?` chứ không phải `showSeeMore: Boolean` đi kèm một
 *   callback luôn có: hai tham số có thể lệch nhau (bật cờ mà quên truyền hành
 *   động → nút bấm không làm gì). Một tham số nullable thì không lệch được.
 *
 *   Quyết định section nào có nút KHÔNG thuộc component này — nó phụ thuộc
 *   vào việc backend có endpoint phân trang tương ứng hay không, xem
 *   `HomeSection.hasSeeMore` ở feature:home.
 */
@Composable
fun HomeSectionRow(
    section: HomeSection,
    onQuizClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSeeMore: (() -> Unit)? = null,
    eyebrowText: String? = null,
) {
    Column(modifier = modifier) {
        if (eyebrowText != null) {
            Text(
                eyebrowText, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                color = ComponentColors.foreground,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            if (onSeeMore != null) {
                TextButton(onClick = onSeeMore) {
                    Text("Xem thêm", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // Horizontal scrolling row of quiz cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = section.items,
                key = { it.id }
            ) { quiz ->
                DiscoveryQuizCard(
                    title = quiz.quizName,
                    categoryText = quiz.quizCategory,
                    metadataText = "${quiz.questionCount} câu • ${quiz.playCount} lượt chơi",
                    imageUrl = quiz.quizImage,
                    onClick = { onQuizClick(quiz.id) },
                    modifier = Modifier.width(220.dp),
                )
            }
        }
    }
}

@Preview(name = "Home Section States Light", showBackground = true, widthDp = 390)
@Preview(name = "Home Section States Dark", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeSectionRowPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val quiz = QuizCard(
                    id = 1L,
                    quizName = "100 Câu Tiếng Anh Giao Tiếp Thông Dụng",
                    quizLanguage = "vi",
                    quizOwnerId = 1L,
                    owner = android.kma.myquizzapp.core.common.model.QuizOwner(1L, "Anh Thư"),
                    questionCount = 20,
                    playCount = 500,
                    completionRate = 0.85,
                    createdAt = "2026-10-10T00:00:00Z",
                )
                val section = HomeSection("preview", "Quiz nổi bật", "featured", listOf(quiz))
                HomeSectionRow(section, {}, onSeeMore = {})
                HomeSectionRow(section.copy(title = "Section không có nút Xem thêm", items = emptyList()), {})
            }
        }
    }
}
