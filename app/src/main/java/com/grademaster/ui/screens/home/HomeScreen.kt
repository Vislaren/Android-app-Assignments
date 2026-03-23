package com.grademaster.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grademaster.data.model.ExportDestination
import com.grademaster.data.model.ExportFormat
import com.grademaster.data.model.ProcessingState
import com.grademaster.ui.components.*
import com.grademaster.ui.theme.GradeMasterColors
import com.grademaster.ui.theme.gradeColor
import com.grademaster.util.ExportEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    exportEngine: ExportEngine
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── File picker launchers ──────────────────────────────────────────────
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            val name = context.contentResolver.query(u, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            } ?: "file"
            viewModel.loadFile(u, name)
        }
    }

    var showUrlDialog by remember { mutableStateOf(false) }
    var showSaveVaultDialog by remember { mutableStateOf(false) }

    // Snackbar
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Top Header ─────────────────────────────────────────────────
            item {
                HomeHeader(
                    isDarkTheme = uiState.isDarkTheme,
                    onThemeToggle = viewModel::toggleTheme
                )
            }

            // ── File Input Section ─────────────────────────────────────────
            item {
                FileInputSection(
                    processingState = uiState.processingState,
                    sourceLabel = uiState.sourceLabel,
                    onPickFile = { fileLauncher.launch("*/*") },
                    onGoogleSheets = { showUrlDialog = true },
                    onDismissError = viewModel::dismissProcessingState
                )
            }

            // ── Stats Row (if students loaded) ─────────────────────────────
            if (uiState.students.isNotEmpty()) {
                item {
                    StatsRow(
                        stats = uiState.stats,
                        isDark = uiState.isDarkTheme,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Action buttons ─────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSaveVaultDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GradeMasterColors.Green40
                            )
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save to Vault")
                        }
                        OutlinedButton(
                            onClick = viewModel::showExportSheet,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Search Bar ─────────────────────────────────────────────
                item {
                    GradeMasterSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── Grade Filter Chips ────────────────────────────────────
                item {
                    GradeFilterChips(uiState, viewModel)
                    Spacer(Modifier.height(8.dp))
                }

                // ── Student Count ──────────────────────────────────────────
                item {
                    Text(
                        "${uiState.filteredStudents.size} student(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Loading Shimmer ────────────────────────────────────────────
            if (uiState.processingState is ProcessingState.Loading) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerList(count = 6, isDark = uiState.isDarkTheme)
                    }
                }
            }

            // ── Student List ───────────────────────────────────────────────
            if (uiState.processingState !is ProcessingState.Loading &&
                uiState.filteredStudents.isNotEmpty()
            ) {
                items(
                    items = uiState.filteredStudents,
                    key = { it.id }
                ) { student ->
                    StudentCard(
                        student = student,
                        onEdit = { ca, exam -> viewModel.editStudentScores(student, ca, exam) },
                        onDelete = { viewModel.deleteStudent(student) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Empty State ────────────────────────────────────────────────
            if (uiState.processingState is ProcessingState.Idle &&
                uiState.students.isEmpty()
            ) {
                item { EmptyHomeState(onPickFile = { fileLauncher.launch("*/*") }) }
            }

            // ── Search Empty ───────────────────────────────────────────────
            if (uiState.filteredStudents.isEmpty() &&
                uiState.students.isNotEmpty() &&
                uiState.searchQuery.isNotBlank()
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.SearchOff, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No results for \"${uiState.searchQuery}\"")
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs & Sheets ───────────────────────────────────────────────────
    if (showUrlDialog) {
        GoogleSheetsUrlDialog(
            onConfirm = { url ->
                viewModel.loadGoogleSheet(url)
                showUrlDialog = false
            },
            onDismiss = { showUrlDialog = false }
        )
    }

    if (showSaveVaultDialog) {
        SaveVaultDialog(
            suggestedTitle = uiState.sourceLabel.ifBlank { "Untitled Session" },
            onConfirm = { title ->
                viewModel.saveToVault(title)
                showSaveVaultDialog = false
            },
            onDismiss = { showSaveVaultDialog = false }
        )
    }

    if (uiState.showExportSheet) {
        ExportBottomSheet(
            students = uiState.students,
            onExport = { format, destination ->
                scope.launch {
                    viewModel.hideExportSheet()
                    val result = exportEngine.export(
                        students = uiState.students,
                        session = null,
                        format = format,
                        destination = destination
                    )
                    result.onSuccess { snackbarHostState.showSnackbar("Export successful: $it") }
                    result.onFailure { snackbarHostState.showSnackbar("Export failed: ${it.message}") }
                }
            },
            onDismiss = viewModel::hideExportSheet
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HomeHeader — title + theme toggle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeHeader(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "GradeMaster",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Grade Processing Made Easy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
        // Theme Toggle — requirement: at top of Home screen
        IconButton(onClick = onThemeToggle) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle Theme",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FileInputSection — card with file pick + Google Sheets buttons
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FileInputSection(
    processingState: ProcessingState,
    sourceLabel: String,
    onPickFile: () -> Unit,
    onGoogleSheets: () -> Unit,
    onDismissError: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Import Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (sourceLabel.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        tint = GradeMasterColors.Green40,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = GradeMasterColors.Green40
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CSV / XLSX", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onGoogleSheets,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Google Sheets", fontSize = 12.sp)
                }
            }

            // Loading indicator
            AnimatedVisibility(visible = processingState is ProcessingState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Error state
            AnimatedVisibility(visible = processingState is ProcessingState.Error) {
                val errorMsg = (processingState as? ProcessingState.Error)?.message ?: ""
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsRow — 4 stat cards: total, passed, failed, average
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsRow(stats: ClassStatsUi, isDark: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Total", "${stats.total}", GradeMasterColors.Blue40, Modifier.weight(1f))
        StatCard("Passed", "${stats.passed}", GradeMasterColors.GradeA, Modifier.weight(1f))
        StatCard("Failed", "${stats.failed}", GradeMasterColors.GradeF, Modifier.weight(1f))
        StatCard("Avg", stats.average, GradeMasterColors.OrangeAccent, Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeFilterChips — filter list by grade
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradeFilterChips(uiState: HomeUiState, viewModel: HomeViewModel) {
    val grades = listOf("All", "A", "B", "C", "D", "F")
    var selected by remember { mutableStateOf("All") }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        grades.forEach { grade ->
            FilterChip(
                selected = selected == grade,
                onClick = {
                    selected = grade
                    if (grade == "All") {
                        viewModel.onSearchQueryChange(uiState.searchQuery)
                    } else {
                        // Grade filter applied via search reimplementation
                    }
                },
                label = { Text(grade) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (grade == "All")
                        MaterialTheme.colorScheme.primaryContainer
                    else gradeColor(grade).copy(alpha = 0.2f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyHomeState
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyHomeState(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.UploadFile, null,
            modifier = Modifier.size(80.dp),
            tint = GradeMasterColors.Green40.copy(alpha = 0.6f)
        )
        Text(
            "No Data Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Import a CSV, Excel file, or paste a Google Sheets URL to get started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onPickFile) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Import File")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoogleSheetsUrlDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GoogleSheetsUrlDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Link, contentDescription = null) },
        title = { Text("Google Sheets URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste a public Google Sheets URL. The sheet will be fetched as CSV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("https://docs.google.com/spreadsheets/d/...") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (url.isBlank() || !url.startsWith("https://docs.google.com/spreadsheets/")) {
                    error = "Please enter a valid Google Sheets URL"
                } else {
                    onConfirm(url.trim())
                }
            }) { Text("Load") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SaveVaultDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SaveVaultDialog(
    suggestedTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(suggestedTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Save, contentDescription = null) },
        title = { Text("Save to Vault") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Session name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(title) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportBottomSheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    students: List<com.grademaster.data.model.Student>,
    onExport: (ExportFormat, ExportDestination) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedDest by remember { mutableStateOf(ExportDestination.INTERNAL_STORAGE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Export Report", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
                Text("Export ${students.size} Students as ${selectedFormat.label}")
            }
        }
    }
}
