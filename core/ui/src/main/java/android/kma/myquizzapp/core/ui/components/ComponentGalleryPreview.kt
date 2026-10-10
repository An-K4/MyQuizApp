package android.kma.myquizzapp.core.ui.components

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Fake state only, no ViewModel/navigation/network calls. Used for component review, not an app route. */
@Preview(showBackground = true, widthDp = 390, heightDp = 1000, name = "Components Light")
@Preview(showBackground = true, widthDp = 390, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Components Dark")
@Preview(showBackground = true, widthDp = 320, heightDp = 1400, fontScale = 1.5f, name = "Components Narrow Large Text")
@Composable
private fun ComponentGalleryPreview() {
    MyQuizAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizPrimaryButton("Đăng nhập", {}, Modifier.fillMaxWidth())
                QuizSecondaryButton("Tiếp tục với tư cách Khách", {}, Modifier.fillMaxWidth())
                QuizDangerButton("Đăng xuất", {}, Modifier.fillMaxWidth(), trailingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, Modifier.size(16.dp))
                })
                QuizPrimaryButton("Đang đăng nhập", {}, Modifier.fillMaxWidth(), loading = true)
                QuizSecondaryButton("Chưa thể tiếp tục", {}, Modifier.fillMaxWidth(), enabled = false)
                SixCharacterCodeFieldContent(
                    TextFieldValue("482", TextRange(2)), {}, SixCharacterCodeKind.Otp, true, {},
                )
                SixCharacterCodeFieldContent(
                    TextFieldValue("AB23CD", TextRange(6)), {}, SixCharacterCodeKind.Room, false, {},
                )
                SixCharacterCodeFieldContent(
                    TextFieldValue("123"), {}, SixCharacterCodeKind.Otp, false, {}, errorMessage = "Vui lòng nhập đủ mã",
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
                    AnswerOptionItem("A", "K2", false, {})
                    AnswerOptionItem("B", "Everest", true, {}, enabled = false, feedback = AnswerOptionFeedback.Correct)
                    AnswerOptionItem("C", "Kangchenjunga", false, {})
                    AnswerOptionItem("D", "Lhotse", false, {})
                }
                AnswerOptionItem("A", "Đã chọn, chưa chấm điểm", true, {})
                AnswerOptionItem("D", "Đáp án sai", true, {}, enabled = false, feedback = AnswerOptionFeedback.Incorrect)
                QuizListCard("100 Câu Tiếng Anh Giao Tiếp Thông Dụng", "20 câu hỏi • 500 người đã chơi • Tạo bởi Anh Thư", null, {}, ratingText = "4.9")
                QuizListCard("Quiz chưa có đánh giá", "10 câu hỏi", null, {})
                LeaderboardPlayerCard("🥇", "Minh Anh", "4,120 điểm", initials = "MA", scoreDeltaText = "(+950)")
                LeaderboardPlayerCard("2", "Một tên người chơi dài để kiểm tra xuống dòng", null, initials = "TN", isCurrentPlayer = true)
            }
        }
    }
}
