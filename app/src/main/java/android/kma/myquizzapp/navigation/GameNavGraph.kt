package android.kma.myquizzapp.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import android.kma.myquizzapp.feature.lobby.presentation.hostlobby.HostLobbyScreen

/**
 * Game navigation graph.
 * Contains gameplay routes: PlayerLobby, HostLobby, GamePlay, HostGame, FinalResult.
 */
fun NavGraphBuilder.gameGraph(navController: NavHostController) {
    composable<Route.PlayerLobby> {
        // TODO: PlayerLobbyScreen() - guest hoặc authenticated
        Text("Player Lobby - Coming Soon")
    }

    composable<Route.HostLobby> {
        // Màn lobby host thật ở feature:lobby.
        // gameId / socketToken / sessionCode do HostLobbyViewModel đọc từ
        // SavedStateHandle nên ở đây không cần toRoute nữa.
        HostLobbyScreen(
            // message != null là trường hợp bị buộc rời phòng (token sai, phòng
            // không còn). TODO N19: hiển thị message này ở màn đích sau khi pop.
            onExit = { navController.popBackStack() }
        )
    }

    composable<Route.GamePlay> {
        // TODO: GamePlayScreen() - dùng GameViewModel
        Text("Game Play - Coming Soon")
    }

    composable<Route.HostGame> {
        // TODO: HostGameScreen() - dùng HostGameViewModel riêng
        Text("Host Game - Coming Soon")
    }

    composable<Route.FinalResult> {
        // TODO: FinalResultScreen()
        Text("Final Result - Coming Soon")
    }
}
