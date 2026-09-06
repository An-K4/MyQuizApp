package android.kma.myquizzapp.core.ui.components

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
    onSeeMore: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            if (onSeeMore != null) {
                TextButton(onClick = onSeeMore) { Text("Xem thêm") }
            }
        }

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
