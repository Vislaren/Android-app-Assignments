package ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import logic.DEFAULT_GRADE_RANGES
import logic.buildGradingLogic
import ui.views.HomeView
import ui.views.HomeViewState
import ui.views.VaultView
import ui.views.VaultViewState
import ui.views.SettingsView
import ui.views.VaultFile

// Navigation destination model
private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0
)

private val NAV_ITEMS = listOf(
    NavItem("Home", Icons.Default.Home, Icons.Default.Home),
    NavItem("Vault", Icons.Default.FolderOpen, Icons.Default.Folder),
    NavItem("Settings", Icons.Default.Settings, Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableStateOf(0) }

    // Vault — session-scoped MutableStateList
    val vault = remember { mutableStateListOf<VaultFile>() }

    // Grading configuration state
    var gradeRanges by remember { mutableStateOf(DEFAULT_GRADE_RANGES) }
    val gradingLogic: (Double) -> String by remember(gradeRanges) {
        derivedStateOf { buildGradingLogic(gradeRanges) }
    }

    // STATE HOISTING: Keeps state alive when navigating between views!
    val homeViewState = remember { HomeViewState() }
    val vaultViewState = remember { VaultViewState() }

    AppTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Student Grade Calculator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Powered by Kotlin · Compose for Desktop",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Vault badge
                            if (vault.isNotEmpty()) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "${vault.size} file(s) in vault",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            // Theme toggle
                            IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle theme",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // ── Navigation Rail ───────────────────────────────────
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        NAV_ITEMS.forEachIndexed { index, item ->
                            val badge = if (index == 1 && vault.isNotEmpty()) vault.size else 0
                            NavigationRailItem(
                                selected = selectedNav == index,
                                onClick = { selectedNav = index },
                                icon = {
                                    if (badge > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text("$badge", fontSize = 9.sp)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                if (selectedNav == index) item.selectedIcon else item.icon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            if (selectedNav == index) item.selectedIcon else item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = { Text(item.label, fontSize = 11.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )

                    // ── Main Content ──────────────────────────────────────
                    AnimatedContent(
                        targetState = selectedNav,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "nav_content"
                    ) { nav ->
                        when (nav) {
                            0 -> HomeView(
                                gradingLogic = gradingLogic,
                                onSaveToVault = { fileName, students ->
                                    vault.add(VaultFile(fileName = fileName, students = students))
                                },
                                state = homeViewState // Passed hoisted state
                            )
                            1 -> VaultView(
                                vaultFiles = vault,
                                onDeleteFile = { file -> vault.remove(file) },
                                onClearAll = { vault.clear() },
                                state = vaultViewState // Passed hoisted state
                            )
                            2 -> SettingsView(
                                gradeRanges = gradeRanges,
                                onRangesChange = { newRanges ->
                                    gradeRanges = newRanges
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}