package android.kma.myquizzapp.navigation

import androidx.navigation.NavHostController

/** Navigation only: callers finish session cleanup in their use cases first. */
fun NavHostController.resetMainGraphAfterSessionEnd(message: String? = null) {
    clearBackStack<Route.Home>()
    clearBackStack<Route.MyQuizzes>()
    clearBackStack<Route.Activity>()
    clearBackStack<Route.Profile>()
    navigate(Route.MainGraph) {
        popUpTo<Route.MainGraph> { inclusive = true; saveState = false }
        launchSingleTop = true
        restoreState = false
    }
    if (message != null) {
        getBackStackEntry<Route.Home>().savedStateHandle[KEY_LOBBY_EXIT_MESSAGE] = message
    }
}
