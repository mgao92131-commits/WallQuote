package com.example.wallquote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wallquote.ui.editor.EditorScreen
import com.example.wallquote.ui.home.HomeScreen
import com.example.wallquote.ui.style.CustomStyleEditorScreen
import com.example.wallquote.ui.style.CustomStylesListScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "collection_editor"
    const val EDITOR_ARG_ID = "collectionId"
    const val CUSTOM_STYLES = "custom_styles"
    const val CUSTOM_STYLE_EDITOR = "custom_style_editor"
    const val CUSTOM_STYLE_EDITOR_ARG_ID = "styleId"

    fun editorRoute(collectionId: Long?): String =
        if (collectionId == null) "$EDITOR?${EDITOR_ARG_ID}=-1"
        else "$EDITOR?${EDITOR_ARG_ID}=$collectionId"

    fun customStyleEditorRoute(styleId: Long?): String =
        if (styleId == null) "$CUSTOM_STYLE_EDITOR?${CUSTOM_STYLE_EDITOR_ARG_ID}=-1"
        else "$CUSTOM_STYLE_EDITOR?${CUSTOM_STYLE_EDITOR_ARG_ID}=$styleId"
}

@Composable
fun WallQuoteNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewCollection = { navController.navigate(Routes.editorRoute(null)) },
                onEditCollection = { id -> navController.navigate(Routes.editorRoute(id)) },
                onManageCustomStyles = { navController.navigate(Routes.CUSTOM_STYLES) },
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
                onManageCustomStyles = { navController.navigate(Routes.CUSTOM_STYLES) },
            )
        }
        composable(Routes.CUSTOM_STYLES) {
            CustomStylesListScreen(
                onBack = { navController.popBackStack() },
                onCreate = { navController.navigate(Routes.customStyleEditorRoute(null)) },
                onEdit = { id -> navController.navigate(Routes.customStyleEditorRoute(id)) },
            )
        }
        composable(
            route = "${Routes.CUSTOM_STYLE_EDITOR}?${Routes.CUSTOM_STYLE_EDITOR_ARG_ID}=" +
                "{${Routes.CUSTOM_STYLE_EDITOR_ARG_ID}}",
            arguments = listOf(
                navArgument(Routes.CUSTOM_STYLE_EDITOR_ARG_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val rawId = entry.arguments?.getLong(Routes.CUSTOM_STYLE_EDITOR_ARG_ID) ?: -1L
            CustomStyleEditorScreen(
                styleId = rawId.takeIf { it >= 0 },
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
