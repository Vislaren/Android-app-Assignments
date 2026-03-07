package ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logic.exportToCsv
import model.Student
import ui.OrangeAccent
import ui.components.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

// Data model representing a saved file session
data class VaultFile(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val students: List<Student>,
    val timestamp: Long = System.currentTimeMillis()
)

// STATE CLASS: Prevents data loss on navigation
class VaultViewState {
    var selectedFile by mutableStateOf<VaultFile?>(null)
    var searchQuery by mutableStateOf("")
    var showClearDialog by mutableStateOf(false)
    var exportStatus by mutableStateOf("")
}

@Composable
fun VaultView(
    vaultFiles: List<VaultFile>,
    onDeleteFile: (VaultFile) -> Unit,
    onClearAll: () -> Unit,
    state: VaultViewState
) {
    val coroutineScope = rememberCoroutineScope()

    // If a file was deleted externally, clear the selection
    LaunchedEffect(vaultFiles) {
        if (state.selectedFile != null && !vaultFiles.any { it.id == state.selectedFile?.id }) {
            state.selectedFile = null
        }
    }

    fun exportSelectedFile(file: VaultFile) {
        if (file.students.isEmpty()) return
        val dialog = FileDialog(Frame(), "Export ${file.fileName} to CSV", FileDialog.SAVE)
        // Set a default file name
        dialog.file = file.fileName.replaceAfterLast(".", "csv").takeIf { it.contains(".") } 
            ?: "${file.fileName}_export.csv"
            
        dialog.isVisible = true
        val dir = dialog.directory
        val path = dialog.file
        if (dir != null && path != null) {
            coroutineScope.launch(Dispatchers.IO) {
                File(dir, path).writeText(exportToCsv(file.students))
                withContext(Dispatchers.Main) {
                    state.exportStatus = "✓ Exported ${file.students.size} records to $path"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // ── State 1: Viewing the File List ──
                if (state.selectedFile == null) {
                    SectionHeader(
                        title = "Grade Vault",
                        subtitle = "${vaultFiles.size} file(s) saved this session"
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(vaultFiles.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { state.showClearDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = OrangeAccent
                                )
                            ) {
                                Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear All Files")
                            }
                        }
                    }
                } 
                // ── State 2: Viewing inside a specific File ──
                else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            state.selectedFile = null
                            state.searchQuery = ""
                            state.exportStatus = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.width(8.dp))
                        SectionHeader(
                            title = state.selectedFile!!.fileName,
                            subtitle = "Previewing ${state.selectedFile!!.students.size} records"
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { state.searchQuery = it },
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { exportSelectedFile(state.selectedFile!!) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export File")
                        }
                    }

                    if (state.exportStatus.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(state.exportStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Content Area
        if (state.selectedFile == null) {
            // Render list of files
            if (vaultFiles.isEmpty()) {
                Box(
                    // CRITICAL FIX: Add weight to force bounds
                    modifier = Modifier.weight(1f).fillMaxWidth(), 
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState("Vault is empty.\nProcess students in the Home tab and click 'Save to Vault'.") {
                        Icon(Icons.Default.Folder, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    }
                }
            } else {
                LazyColumn(
                    // CRITICAL FIX: Add weight to allow scrolling
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(vaultFiles, key = { it.id }) { file ->
                        FileCard(
                            file = file,
                            onClick = { state.selectedFile = file },
                            onDelete = { onDeleteFile(file) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        } else {
            // Render students inside the selected file
            val students = state.selectedFile!!.students
            val filteredStudents = if (state.searchQuery.isBlank()) students else students.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                        it.id.contains(state.searchQuery, ignoreCase = true) ||
                        it.grade.contains(state.searchQuery, ignoreCase = true)
            }

            if (students.isNotEmpty()) {
                StatsRow(students)
                Spacer(Modifier.height(6.dp))
                GradeDistributionRow(students)
                Spacer(Modifier.height(6.dp))
            }

            LazyColumn(
                // CRITICAL FIX: Add weight to allow scrolling
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredStudents.isEmpty()) {
                    item {
                        EmptyState("No results for \"${state.searchQuery}\"") {
                            Icon(Icons.Default.SearchOff, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.id + it.name + it.finalScore }) { student ->
                        StudentCard(
                            student = student,
                            onDelete = {} // Individual deletions disabled in file preview mode
                        )
                    }
                }
            }
        }
    }

    // Clear All confirmation dialog
    if (state.showClearDialog) {
        AlertDialog(
            onDismissRequest = { state.showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = OrangeAccent) },
            title = { Text("Clear Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all ${vaultFiles.size} saved files from this session's vault.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        state.showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) { Text("Clear All") }
            },
            dismissButton = {
                OutlinedButton(onClick = { state.showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileCard(file: VaultFile, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss")
    val dateString = dateFormat.format(Date(file.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.InsertDriveFile, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("${file.students.size} students • $dateString", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun GradeDistributionRow(students: List<Student>) {
    val distribution = students.groupBy { it.grade }
        .mapValues { it.value.size }
        .toSortedMap()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Distribution:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        distribution.forEach { (grade, count) ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(grade, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("$count", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}