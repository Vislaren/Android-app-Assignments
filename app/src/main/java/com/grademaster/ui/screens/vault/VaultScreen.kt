package com.grademaster.ui.screens.vault

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grademaster.data.model.*
import com.grademaster.ui.components.*
import com.grademaster.ui.theme.GradeMasterColors
import com.grademaster.ui.theme.gradeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Header ─────────────────────────────────────────────────────
            VaultHeader(
                selectedSession = uiState.selectedSession,
                onBack = viewModel::clearSelectedSession
            )

            // ── Session Detail View ────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.selectedSession,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "vault_navigation"
            ) { selectedSession ->
                if (selectedSession != null) {
                    SessionDetailView(
                        session = selectedSession,
                        students = uiState.filteredSessionStudents,
                        searchQuery = uiState.studentSearchQuery,
                        onSearchChange = viewModel::onStudentSearchQueryChange,
                        onDeleteStudent = viewModel::deleteStudentFromSession,
                        onExport = viewModel::showExportSheet,
                        isDark = uiState.isDarkTheme
                    )
                } else {
                    SessionListView(
                        sessions = uiState.filteredSessions,
                        searchQuery = uiState.searchQuery,
                        onSearchChange = viewModel::onSearchQueryChange,
                        onSelectSession = viewModel::selectSession,
                        onDeleteSession = viewModel::deleteSession,
                        isLoading = uiState.isLoading,
                        isDark = uiState.isDarkTheme
                    )
                }
            }
        }
    }

    // ── Export Bottom Sheet ────────────────────────────────────────────────
    if (uiState.showExportSheet) {
        ExportSheet(
            sessionTitle = uiState.selectedSession?.title ?: "",
            onExport = viewModel::exportSession,
            onDismiss = viewModel::hideExportSheet
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultHeader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VaultHeader(
    selectedSession: VaultSession?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = selectedSession != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (selectedSession != null) selectedSession.title else "Vault",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selectedSession != null) {
                Text(
                    text = "${selectedSession.studentCount} students · ${selectedSession.formattedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    "Saved Grade Sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
        // Vault icon
        if (selectedSession == null) {
            Icon(
                Icons.Outlined.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SessionListView — list of saved sessions with search
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SessionListView(
    sessions: List<VaultSession>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectSession: (VaultSession) -> Unit,
    onDeleteSession: (VaultSession) -> Unit,
    isLoading: Boolean,
    isDark: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            GradeMasterSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search saved sessions…",
                modifier = Modifier.padding(16.dp)
            )
        }

        if (isLoading) {
            items(4) {
                VaultSessionShimmer(isDark = isDark)
                Spacer(Modifier.height(12.dp))
            }
        } else if (sessions.isEmpty()) {
            item { EmptyVaultState() }
        } else {
            item {
                Text(
                    "${sessions.size} session(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
            items(sessions, key = { it.id }) { session ->
                VaultSessionCard(
                    session = session,
                    onClick = { onSelectSession(session) },
                    onDelete = { onDeleteSession(session) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSessionCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VaultSessionCard(
    session: VaultSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = session.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = GradeMasterColors.GradeF.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Source label
            if (session.source.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Source,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        session.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Stats chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VaultStatChip(
                    label = "Students",
                    value = "${session.studentCount}",
                    color = GradeMasterColors.Blue40,
                    modifier = Modifier.weight(1f)
                )
                VaultStatChip(
                    label = "Average",
                    value = "%.1f".format(session.averageScore),
                    color = GradeMasterColors.OrangeAccent,
                    modifier = Modifier.weight(1f)
                )
                VaultStatChip(
                    label = "Pass Rate",
                    value = "%.0f%%".format(session.passRate),
                    color = if (session.passRate >= 50) GradeMasterColors.GradeA else GradeMasterColors.GradeF,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(Icons.Filled.DeleteForever, null, tint = GradeMasterColors.GradeF)
            },
            title = { Text("Delete Session") },
            text = {
                Text("Delete \"${session.title}\" and all ${session.studentCount} student records? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = GradeMasterColors.GradeF)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VaultStatChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SessionDetailView — shows students within a selected vault session
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SessionDetailView(
    session: VaultSession,
    students: List<Student>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onExport: () -> Unit,
    isDark: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Session summary banner
        item {
            SessionSummaryBanner(session = session, onExport = onExport)
        }

        // Search
        item {
            GradeMasterSearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search students in session…",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            Text(
                "${students.size} student(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Students (read-only view in vault — delete only)
        items(students, key = { it.id }) { student ->
            StudentCard(
                student = student,
                onEdit = { _, _ -> /* Vault is read-only for scores */ },
                onDelete = { onDeleteStudent(student) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (students.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No students found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryBanner(session: VaultSession, onExport: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Average Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    "%.2f / 100".format(session.averageScore),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Pass Rate: %.1f%%".format(session.passRate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyVaultState
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyVaultState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.Archive, null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Text(
            "Vault is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Process data on the Home tab and tap \"Save to Vault\" to store results here.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportSheet (Vault context)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    sessionTitle: String,
    onExport: (ExportFormat, ExportDestination) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedDest by remember { mutableStateOf(ExportDestination.INTERNAL_STORAGE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Export \"$sessionTitle\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)

            Text("Format", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = { Text(fmt.label) }
                    )
                }
            }

            Text("Destination", style = MaterialTheme.typography.labelLarge)
            ExportDestination.entries.forEach { dest ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedDest == dest) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { selectedDest = dest }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedDest == dest, onClick = { selectedDest = dest })
                    Spacer(Modifier.width(8.dp))
                    Text(dest.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onExport(selectedFormat, selectedDest) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileDownload, null)
                Spacer(Modifier.width(8.dp))
                Text("Export as ${selectedFormat.label}")
            }
        }
    }
}
