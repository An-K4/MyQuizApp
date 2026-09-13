package android.kma.myquizzapp.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import android.kma.myquizzapp.feature.game_host.presentation.hostgame.HostGameScreen
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

    composable<Route.HostGame> {
        // gameId / socketToken do HostGameViewModel đọc từ SavedStateHandle.
        //
        // Rời màn này là pop về Trang chủ chứ không navigate: HostLobby đã bị pop
        // inclusive khi trận bắt đầu, nên Trang chủ đang là entry ngay dưới —
        // popWithMessage vừa quay về đúng chỗ vừa gửi kèm được lý do bị buộc thoát
        // (token hỏng, phòng biến mất) để Trang chủ hiện snackbar.
        HostGameScreen(
            onExit = { message -> navController.popWithMessage(message) }
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
