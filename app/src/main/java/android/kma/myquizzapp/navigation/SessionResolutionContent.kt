package android.kma.myquizzapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Display-only recovery for an unresolved session; never treats it as a guest. */
@Composable
fun SessionResolutionContent(state: SessionRecoveryState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            SessionRecoveryState.Checking, SessionRecoveryState.Resolved -> CircularProgressIndicator()
            SessionRecoveryState.Offline -> {
                Text(
                    "Không có kết nối. Phiên sẽ được kiểm tra lại khi mạng trở về.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Thử lại")
                }
            }
            SessionRecoveryState.Unavailable -> {
                Text(
                    "Chưa thể kiểm tra phiên. Hãy thử lại khi kết nối ổn định.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Thử lại")
                }
            }
        }
    }
}
