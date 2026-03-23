package com.grademaster.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

// ─────────────────────────────────────────────────────────────────────────────
// NavItem — sealed class representing each bottom nav destination
// ─────────────────────────────────────────────────────────────────────────────
sealed class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : NavItem(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Vault : NavItem(
        route = "vault",
        label = "Vault",
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive
    )

    data object Settings : NavItem(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        val allItems = listOf(Home, Vault, Settings)
    }
}
