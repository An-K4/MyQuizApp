package android.kma.myquizzapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import android.kma.myquizzapp.feature.lobby.presentation.guestnickname.GuestNicknameScreen
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.HostLobbyScreen
import android.kma.myquizzapp.feature.lobby.presentation.playerlobby.PlayerLobbyScreen

/**
 * Key dùng để chuyển lý do bị buộc rời phòng về màn trước.
 *
 * Màn lobby bị pop mất trước khi kịp hiện snackbar, nên thông báo phải đi kèm
 * backstack entry của màn đích. Để ở đây để bên gửi (game graph) và bên nhận
 * (main graph) dùng chung một hằng, không gõ tay chuỗi ở hai nơi.
 */
const val KEY_LOBBY_EXIT_MESSAGE = "lobbyExitMessage"

/**
 * Game navigation graph.
 * Contains gameplay routes: GuestNickname, PlayerLobby, HostLobby, GamePlay, HostGame, FinalResult.
 */
fun NavGraphBuilder.gameGraph(navController: NavHostController) {
    composable<Route.GuestNickname> {
        // sessionCode do GuestNicknameViewModel đọc từ SavedStateHandle.
        GuestNicknameScreen(
            onNavigateToPlayerLobby = { gameId, playerId, socketToken ->
                navController.navigate(Route.PlayerLobby(gameId, playerId, socketToken)) {
                    // Xóa màn nhập tên khỏi backstack và quay về Trang chủ: từ N19.6
                    // ô nhập mã nằm ngay trên Trang chủ, nên rời phòng là về đúng chỗ
                    // vào lại được — trước đây phải giữ lại màn Join riêng cho việc này.
                    popUpTo<Route.Home> { inclusive = false }
                }
            },
            onExitWithMessage = { message -> navController.popWithMessage(message) },
            onBack = { navController.popBackStack() }
        )
    }

    composable<Route.PlayerLobby> {
        // gameId / playerId / socketToken do PlayerLobbyViewModel đọc từ SavedStateHandle.
        PlayerLobbyScreen(
            onExit = { message -> navController.popWithMessage(message) }
        )
    }

    composable<Route.HostLobby> {
        // Màn lobby host thật ở feature:lobby.
        // gameId / socketToken / sessionCode do HostLobbyViewModel đọc từ
        // SavedStateHandle nên ở đây không cần toRoute nữa.
        HostLobbyScreen(
            onExit = { message -> navController.popWithMessage(message) },
            onNavigateToHostGame = { gameId, socketToken ->
                navController.navigate(Route.HostGame(gameId, socketToken)) {
                    // Trận đã bắt đầu thì không còn đường quay về phòng chờ: back từ
                    // màn điều khiển phải về Trang chủ, không phải về một lobby đã chết.
                    popUpTo<Route.HostLobby> { inclusive = true }
                }
            }
        )
    }

    composable<Route.GamePlay> {
        // TODO: GamePlayScreen() - dùng GameViewModel
        Text("Game Play - Coming Soon")
    }

    composable<Route.HostGame> { entry ->
        // N21 sẽ thay chỗ này bằng HostGameScreen thật (HostGameViewModel riêng, tự
        // mở socket bằng socketToken được truyền vào). Placeholder của N20 cố tình
        // hiển thị đủ tham số nhận được để việc hand-off kiểm tra được bằng mắt.
        val route = entry.toRoute<Route.HostGame>()
        HostGamePlaceholder(
            gameId = route.gameId,
            onBackToHome = {
                navController.navigate(Route.Home) {
                    popUpTo<Route.Home> { inclusive = false }
                }
            }
        )
    }

    composable<Route.FinalResult> {
        // TODO: FinalResultScreen()
        Text("Final Result - Coming Soon")
    }
}

/**
 * Pop màn hiện tại, kèm theo lý do (nếu có) cho màn đích.
 *
 * Phải ghi vào savedStateHandle TRƯỚC khi pop — sau khi pop thì
 * `previousBackStackEntry` đã trỏ sang chỗ khác. Màn đích nào không đọc key này
 * thì giá trị chỉ nằm im, không gây hại.
 */
private fun NavHostController.popWithMessage(message: String?) {
    if (message != null) {
        previousBackStackEntry?.savedStateHandle?.set(KEY_LOBBY_EXIT_MESSAGE, message)
    }
    popBackStack()
}

/**
 * Chỗ đứng tạm của màn điều khiển trận (host).
 *
 * N20 chỉ làm đến "trận đã bắt đầu thật": phần điều khiển câu hỏi thuộc N21.
 * Để một màn có chữ và có đường thoát rõ ràng thay vì một dòng "Coming Soon"
 * lơ lứng, vì host thật sự sẽ dừng ở đây khi test trên máy thật.
 */
@Composable
private fun HostGamePlaceholder(
    gameId: Long,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("Trận đã bắt đầu", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Màn điều khiển trận cho host sẽ được làm ở bước N21. Phiên #$gameId đã " +
                "chuyển sang trạng thái đang chơi trên máy chủ.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onBackToHome) { Text("Về Trang chủ") }
    }
}
