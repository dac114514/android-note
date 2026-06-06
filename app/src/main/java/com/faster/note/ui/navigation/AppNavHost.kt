package com.faster.note.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.faster.note.ui.day.DayScreen
import com.faster.note.ui.day.DayViewModel
import com.faster.note.ui.month.MonthScreen
import com.faster.note.ui.month.MonthViewModel
import com.faster.note.ui.ai.AiChatScreen
import com.faster.note.ui.ai.AiChatViewModel
import com.faster.note.ui.ai.AiSettingsScreen
import com.faster.note.ui.settings.SettingsScreen
import com.faster.note.ui.settings.SettingsViewModel

object Routes {
    const val DAY = "day"
    const val MONTH = "month"
    const val SETTINGS = "settings"
    const val AI_CHAT = "ai_chat"
    const val AI_SETTINGS = "ai_settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    dayViewModel: DayViewModel,
    monthViewModel: MonthViewModel,
    settingsViewModel: SettingsViewModel,
    aiChatViewModel: AiChatViewModel,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Routes.DAY) {
        composable(Routes.DAY) {
            DayScreen(
                viewModel = dayViewModel,
                onNavigateToMonth = { navController.navigate(Routes.MONTH) },
                onNavigateToAiChat = { navController.navigate(Routes.AI_CHAT) }
            )
        }
        composable(Routes.MONTH) {
            MonthScreen(
                viewModel = monthViewModel,
                onDaySelected = { year, month, day ->
                    dayViewModel.goToDate(year, month, day)
                },
                onNavigateToDay = { year, month, day ->
                    dayViewModel.goToDate(year, month, day)
                    navController.navigate(Routes.DAY) {
                        popUpTo(Routes.DAY) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.MONTH) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onToggleDarkMode = onToggleDarkMode,
                onCheckUpdate = onCheckUpdate,
                onOpenAbout = onOpenAbout
            )
        }
        composable(Routes.AI_CHAT) {
            AiChatScreen(
                viewModel = aiChatViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDay = { year, month, day ->
                    dayViewModel.goToDate(year, month, day)
                    navController.navigate(Routes.DAY) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = { navController.navigate(Routes.AI_SETTINGS) }
            )
        }
        composable(Routes.AI_SETTINGS) {
            AiSettingsScreen(
                viewModel = aiChatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
