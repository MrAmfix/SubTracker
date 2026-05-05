package com.mramfix.subtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mramfix.subtracker.ui.screens.EditSubscriptionScreen
import com.mramfix.subtracker.ui.screens.HomeScreen
import com.mramfix.subtracker.ui.screens.SettingsScreen
import com.mramfix.subtracker.ui.screens.StatsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val EDIT = "edit/{subscriptionId}"

    fun edit(id: Long = 0L): String = "edit/$id"
}

@Composable
fun SubTrackerApp() {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAdd = { navController.navigate(Routes.edit()) },
                    onEdit = { navController.navigate(Routes.edit(it)) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onStats = { navController.navigate(Routes.STATS) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument("subscriptionId") { type = NavType.LongType })
            ) {
                EditSubscriptionScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
