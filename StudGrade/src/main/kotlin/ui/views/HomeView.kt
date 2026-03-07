package ui.views

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logic.*
import model.Student
import ui.OrangeAccent
import ui.components.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

enum class InputMode { NONE, FILE, SHEETS }

// STATE CLASS: Prevents data loss on navigation
class HomeViewState {
    var inputMode by mutableStateOf(InputMode.NONE)
    var googleSheetUrl by mutableStateOf("")
    var rawStudents by mutableStateOf<List<Student>>(emptyList())
    var processedStudents by mutableStateOf<List<Student>>(emptyList())
    var parseErrors by mutableStateOf<List<String>>(emptyList())
    var isProcessing by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var statusMessage by mutableStateOf("")
    var selectedFileName by mutableStateOf("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    gradingLogic: (Double) -> String,
    onSaveToVault: (String, List<Student>) -> Unit,
    state: HomeViewState
) {
    val coroutineScope = rememberCoroutineScope()

    val filteredStudents by remember(state.processedStudents, state.searchQuery) {
        derivedStateOf {
            if (state.searchQuery.isBlank()) state.processedStudents
            else state.processedStudents.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                        it.id.contains(state.searchQuery, ignoreCase = true) ||
                        it.grade.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }

    // ── File Picker ───────────────────────────────────────────
    fun openFilePicker() {
        val dialog = FileDialog(Frame(), "Select Student Data File", FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name ->
            name.endsWith(".csv", true) || name.endsWith(".xlsx", true) || name.endsWith(".xls", true)
        }
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        if (dir != null && file != null) {
            val selectedFile = File(dir, file)
            state.selectedFileName = selectedFile.name
            coroutineScope.launch {
                state.isProcessing = true
                state.statusMessage = "Parsing file…"
                val (students, errors) = withContext(Dispatchers.IO) {
                    if (file.endsWith(".csv", true)) {
                        parseCsvContent(selectedFile.readText())
                    } else {
                        parseExcelFile(selectedFile)
                    }
                }
                state.rawStudents = students
                state.parseErrors = errors
                state.isProcessing = false
                state.statusMessage = if (errors.isEmpty())
                    "✓ Loaded ${students.size} students from $file"
                else
                    "Loaded ${students.size} students with ${errors.size} warning(s)"
                state.inputMode = InputMode.FILE
            }
        }
    }

    // ── Process ───────────────────────────────────────────────
    fun processData() {
        if (state.rawStudents.isEmpty()) {
            state.statusMessage = "⚠ No student data to process"
            return
        }
        coroutineScope.launch {
            state.isProcessing = true
            state.statusMessage = "Calculating scores…"
            delay(600) // simulate processing
            state.processedStudents = withContext(Dispatchers.Default) {
                processStudents(state.rawStudents, gradingLogic)
            }
            state.isProcessing = false
            val errorCount = state.processedStudents.count { it.finalScore < 0 }
            state.statusMessage = if (errorCount == 0)
                "✓ Processed ${state.processedStudents.size} students successfully"
            else
                "⚠ Processed ${state.processedStudents.size} students ($errorCount errors)"
        }
    }

    // ── Export ────────────────────────────────────────────────
    fun exportToCsvFile() {
        if (state.processedStudents.isEmpty()) {
            state.statusMessage = "Nothing to export"
            return
        }
        val dialog = FileDialog(Frame(), "Save CSV File", FileDialog.SAVE)
        dialog.file = "grades_export.csv"
        dialog.isVisible = true
        val dir = dialog.directory
        val file = dialog.file
        if (dir != null && file != null) {
            coroutineScope.launch(Dispatchers.IO) {
                File(dir, file).writeText(exportToCsv(state.processedStudents))
                withContext(Dispatchers.Main) {
                    state.statusMessage = "✓ Exported to $file"
                }
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        // Input panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(
                    title = "Import Student Data",
                    subtitle = "Choose a source to load student scores"
                )
                Spacer(Modifier.height(12.dp))

                // Source buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { openFilePicker() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Browse File (.csv / .xlsx)")
                    }

                    OutlinedButton(
                        onClick = { state.inputMode = InputMode.SHEETS },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Google Sheets URL")
                    }
                }

                // Google Sheets URL input
                AnimatedVisibility(visible = state.inputMode == InputMode.SHEETS) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.googleSheetUrl,
                            onValueChange = { state.googleSheetUrl = it },
                            label = { Text("Google Sheets CSV Export URL") },
                            placeholder = { Text("https://docs.google.com/spreadsheets/…/export?format=csv") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (state.googleSheetUrl.isNotEmpty()) {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            state.isProcessing = true
                                            state.statusMessage = "Fetching from Google Sheets…"
                                            try {
                                                val content = withContext(Dispatchers.IO) {
                                                    java.net.URL(state.googleSheetUrl).readText()
                                                }
                                                val (students, errors) = parseCsvContent(content)
                                                state.rawStudents = students
                                                state.parseErrors = errors
                                                state.selectedFileName = "Google_Sheets_Data"
                                                state.statusMessage = "✓ Fetched ${students.size} students"
                                            } catch (e: Exception) {
                                                state.statusMessage = "✗ Failed: ${e.message}"
                                            }
                                            state.isProcessing = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Fetch")
                                    }
                                }
                            }
                        )
                    }
                }

                // File info
                AnimatedVisibility(visible = state.selectedFileName.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${state.rawStudents.size} students loaded from ${state.selectedFileName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { processData() },
                        enabled = state.rawStudents.isNotEmpty() && !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calculate Grades", fontWeight = FontWeight.SemiBold)
                    }

                    AnimatedVisibility(visible = state.processedStudents.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    val name = state.selectedFileName.takeIf { it.isNotEmpty() } ?: "Unnamed_Export"
                                    onSaveToVault(name, state.processedStudents) 
                                    state.statusMessage = "✓ Saved as $name to Vault"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save to Vault")
                            }

                            OutlinedButton(
                                onClick = { exportToCsvFile() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                        }
                    }

                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Progress bar
                AnimatedVisibility(visible = state.isProcessing) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Parse errors
                if (state.parseErrors.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    state.parseErrors.take(3).forEach { err ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(err, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (state.parseErrors.size > 3) {
                        Text("… and ${state.parseErrors.size - 3} more warnings", fontSize = 11.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }

                // Status bar
                if (state.statusMessage.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.statusMessage,
                        fontSize = 12.sp,
                        color = if (state.statusMessage.startsWith("✗")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Results panel
        AnimatedVisibility(
            visible = state.processedStudents.isNotEmpty(),
            // CRITICAL FIX: Forces the column to take only remaining space, making LazyColumn scrollable
            modifier = Modifier.weight(1f) 
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stats
                StatsRow(state.processedStudents)

                Spacer(Modifier.height(4.dp))

                // Search + count
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = { state.searchQuery = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${filteredStudents.size} result(s)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Student list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredStudents.isEmpty()) {
                        item {
                            EmptyState("No students match your search") {
                                Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                            }
                        }
                    } else {
                        items(filteredStudents, key = { it.id + it.name }) { student ->
                            StudentCard(student = student)
                        }
                    }
                }
            }
        }

        // Initial empty state
        if (state.processedStudents.isEmpty() && !state.isProcessing) {
            Box(
                // CRITICAL FIX: Ensures this box expands, preventing layout measurement issues
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("Import a file and click Calculate to see results") {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                }
            }
        }
    }
}