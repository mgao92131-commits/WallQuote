package com.example.wallquote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wallquote.ui.editor.EditorScreen
import com.example.wallquote.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "collection_editor"
    const val EDITOR_ARG_ID = "collectionId"

    fun editorRoute(collectionId: Long?): String =
        if (collectionId == null) "$EDITOR?${EDITOR_ARG_ID}=-1"
        else "$EDITOR?${EDITOR_ARG_ID}=$collectionId"
}

@Composable
fun WallQuoteNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewCollection = { navController.navigate(Routes.editorRoute(null)) },
                onEditCollection = { id -> navController.navigate(Routes.editorRoute(id)) },
            )
        }
        composable(
            route = "${Routes.EDITOR}?${Routes.EDITOR_ARG_ID}={${Routes.EDITOR_ARG_ID}}",
            arguments = listOf(
                navArgument(Routes.EDITOR_ARG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val rawId = entry.arguments?.getLong(Routes.EDITOR_ARG_ID) ?: -1L
            val collectionId = rawId.takeIf { it >= 0 }
            EditorScreen(
                collectionId = collectionId,
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
