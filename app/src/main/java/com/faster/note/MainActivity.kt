package com.faster.note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.note.ui.about.AboutActivity
import com.faster.note.ui.day.DayViewModel
import com.faster.note.ui.month.MonthViewModel
import com.faster.note.ui.navigation.AppNavHost
import com.faster.note.ui.navigation.Routes
import com.faster.note.ui.settings.SettingsViewModel
import com.faster.note.ui.theme.ScheduleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            val dayViewModel: DayViewModel = viewModel()
            val monthViewModel: MonthViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()

            ScheduleAppTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomBarVisible = currentDestination?.route in listOf(
                    Routes.DAY, Routes.MONTH, Routes.SETTINGS
                )

                Scaffold(
                    bottomBar = {
                        AnimatedVisibility(
                            visible = bottomBarVisible,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it }
                        ) {
                            NavigationBar {
                                data class NavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
                                val items = listOf(
                                    NavItem(Routes.DAY, "日视图", Icons.Filled.Today, Icons.Outlined.Today),
                                    NavItem(Routes.MONTH, "月视图", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
                                    NavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
                                )
                                items.forEach { (route, label, selectedIcon, unselectedIcon) ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(if (selected) selectedIcon else unselectedIcon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AppNavHost(
                            navController = navController,
                            dayViewModel = dayViewModel,
                            monthViewModel = monthViewModel,
                            settingsViewModel = settingsViewModel,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = it },
                            onOpenAbout = {
                                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }
}
