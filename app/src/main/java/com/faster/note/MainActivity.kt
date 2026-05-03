package com.faster.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.note.data.AppUpdaterService
import com.faster.note.data.local.DataStore
import com.faster.note.ui.about.AboutActivity
import com.faster.note.ui.day.DayViewModel
import com.faster.note.ui.month.MonthViewModel
import com.faster.note.ui.navigation.AppNavHost
import com.faster.note.ui.navigation.Routes
import com.faster.note.ui.settings.SettingsViewModel
import com.faster.note.ui.theme.ScheduleAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataStore.init(applicationContext)
        enableEdgeToEdge()

        var showUpdateDialog by mutableStateOf(false)
        var pendingUpdate by mutableStateOf<AppUpdaterService.UpdateResult?>(null)

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
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        AppNavHost(
                            navController = navController,
                            dayViewModel = dayViewModel,
                            monthViewModel = monthViewModel,
                            settingsViewModel = settingsViewModel,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = it },
                            onCheckUpdate = {
                                lifecycleScope.launch {
                                    val result = AppUpdaterService.checkForUpdate()
                                    if (result != null) {
                                        pendingUpdate = result
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(this@MainActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenAbout = {
                                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                            }
                        )
                    }
                }

                if (showUpdateDialog && pendingUpdate != null) {
                    val update = pendingUpdate!!
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = { Text("发现新版本 ${update.latestVersion}") },
                        text = { Text(update.releaseNotes.ifBlank { "有新版本可用，是否前往更新？" }) },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                                showUpdateDialog = false
                            }) { Text("更新") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateDialog = false }) { Text("稍后") }
                        }
                    )
                }
            }
        }

        // Daily silent check for updates on first launch
        val prefs = getSharedPreferences("app_updater_prefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastCheck = prefs.getString("last_update_check_date", "")
        if (lastCheck != today) {
            lifecycleScope.launch {
                val result = AppUpdaterService.checkForUpdate()
                if (result != null) {
                    pendingUpdate = result
                    showUpdateDialog = true
                }
            }
            prefs.edit().putString("last_update_check_date", today).apply()
        }
    }
}
