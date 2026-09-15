package android.kma.myquizzapp.feature.leaderboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FinalResultScreen(onHome: () -> Unit, modifier: Modifier = Modifier, viewModel: FinalResultViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FinalResultScreenContent(state, { viewModel.consume(); onHome() }, modifier)
}

@Composable
fun FinalResultScreenContent(state: FinalResultUiState, onHome: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Kết quả trận đấu", style = MaterialTheme.typography.headlineMedium) }
            when {
                state.isMissing -> item { Text("Kết quả tạm thời không còn sau khi ứng dụng được khởi động lại.") }
                state.isLeaderboardHidden -> item { Text("Host đã ẩn bảng xếp hạng của trận này.") }
                else -> {
                    state.currentPlayer?.let { me -> item {
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                            Text("Thành tích của bạn", style = MaterialTheme.typography.titleMedium)
                            Text("Hạng ${me.rank} • ${me.playerScore} điểm")
                            me.correctAnswersCount?.let { Text("$it câu đúng") }
                        } }
                    } }
                    items(state.leaderboard, key = { it.id }) { row ->
                        Card(Modifier.fillMaxWidth()) { Text(
                            "#${row.rank}  ${row.playerName} — ${row.playerScore} điểm" + if (row.id == state.playerId) "  (Bạn)" else "",
                            Modifier.padding(14.dp)
                        ) }
                    }
                }
            }
            item { Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Về trang chủ") } }
        }
    }
}
