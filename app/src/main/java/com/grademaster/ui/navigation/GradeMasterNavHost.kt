package com.grademaster.ui.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.grademaster.ui.screens.home.HomeScreen
import com.grademaster.ui.screens.home.HomeViewModel
import com.grademaster.ui.screens.settings.SettingsScreen
import com.grademaster.ui.screens.vault.VaultScreen
import com.grademaster.util.ExportEngine

// ─────────────────────────────────────────────────────────────────────────────
// GradeMasterNavHost
//
// NAVIGATION STATE PRESERVATION:
// Each tab uses a single composable instance. The NavHost with
// `launchSingleTop = true` and `restoreState = true` ensures that switching
// tabs does NOT reset the ViewModel state — data previews are preserved.
// ViewModels are scoped to the NavBackStackEntry so they survive tab switches.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradeMasterNavHost(exportEngine: ExportEngine) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { GradeMasterBottomBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            composable(NavItem.Home.route) {
                HomeScreen(exportEngine = exportEngine)
            }
            composable(NavItem.Vault.route) {
                VaultScreen()
            }
            composable(NavItem.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeMasterBottomBar — persistent bottom navigation bar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradeMasterBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavItem.allItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to start destination to avoid back-stack buildup
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when navigating back to a previously visited tab
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
