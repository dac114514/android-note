package com.faster.note.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.faster.note.ui.editor.NoteEditScreen
import com.faster.note.ui.folders.FolderScreen
import com.faster.note.ui.notes.NoteListScreen
import com.faster.note.ui.search.SearchScreen
import com.faster.note.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val FOLDERS = "folders"
    const val NOTE_EDIT = "note/{noteId}"
    const val NOTE_NEW = "note/new"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun noteEdit(id: Long) = "note/$id"
}

@Composable
fun NoteNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.NOTES) {
        composable(Routes.NOTES) {
            NoteListScreen(
                onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                onNewNote = { navController.navigate(Routes.NOTE_NEW) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) }
            )
        }
        composable(
            route = Routes.NOTE_EDIT,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteEditScreen(noteId = noteId, onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTE_NEW) {
            NoteEditScreen(noteId = null, onBack = { navController.popBackStack() })
        }
        composable(Routes.FOLDERS) {
            FolderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onOpenNote = { id -> navController.navigate(Routes.noteEdit(id)) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
