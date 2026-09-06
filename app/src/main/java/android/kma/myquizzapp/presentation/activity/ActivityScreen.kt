package android.kma.myquizzapp.presentation.activity

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Tab "Hoạt động" — PLACEHOLDER của N19.5.
 *
 * N19.5 chỉ dụng khung điều hướng; màn lịch sử thật làm sau. Ghi lại sẵn
 * hợp đồng backend đã audit (5/9) để khi làm thật không phải đào lại:
 *
 * - `GET /v1/games/history` nhận `role` = `played` | `hosted` (default `played`),
 *   `cursor` (chuỗi opaque, ≤300 ký tự, sai định dạng → 400), `limit` 1..50
 *   (default 20), `include_total` = đúng literal `"true"`/`"false"`.
 * - KHÔNG có `role=all`: muốn chip "Tất cả" thì phải gọi 2 lượt và trộn 2 dòng
 *   cursor riêng biệt — dễ trùng/thiếu item khi cuộn. Web cũng chỉ có 2 chip.
 * - Thứ tự sort cố định `(coalesce(finished_at, created_at) DESC, id DESC)`.
 * - Khách (guest) xem được lịch sử của mình qua header `x-guest-id`, và với
 *   khách thì `hosted` vô nghĩa (host bắt buộc đăng nhập) → chỉ hiện 1 chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Hoạt động") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Sắp có", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Lịch sử các trận bạn đã chơi và đã tổ chức sẽ hiện ở đây.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActivityScreenPreview() {
    MyQuizAppTheme {
        ActivityScreen()
    }
}
