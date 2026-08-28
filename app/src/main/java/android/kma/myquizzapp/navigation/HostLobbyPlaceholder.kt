package android.kma.myquizzapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Placeholder tạm để kiểm tra hand-off Create Room → Host Lobby trong N17. */
@Composable
internal fun HostLobbyPlaceholder(
    gameId: Long,
    sessionCode: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Phòng đã tạo", style = MaterialTheme.typography.headlineSmall)
                Text("Mã tham gia: $sessionCode", style = MaterialTheme.typography.titleLarge)
                Text("Game ID: $gameId")
                Text(
                    "Host token đã được cấp. Lobby thật sẽ được triển khai ở bước tiếp theo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Quay lại chi tiết quiz")
                }
            }
        }
    }
}
