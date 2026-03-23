package com.grademaster.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.grademaster.data.model.ExportFormat
import com.grademaster.data.model.GradeRange
import com.grademaster.ui.theme.GradeMasterColors
import com.grademaster.ui.theme.gradeColor

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "Customize grading behaviour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Appearance ─────────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                SettingsSwitchRow(
                    icon = if (uiState.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    title = "Dark Theme",
                    subtitle = if (uiState.isDarkTheme) "Dark mode is active" else "Light mode is active",
                    checked = uiState.isDarkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme() }
                )
            }

            // ── Grade Ranges ───────────────────────────────────────────────
            SettingsSection(
                title = "Grade Ranges",
                action = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = viewModel::showResetConfirm) {
                            Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            ) {
                Text(
                    "Define score ranges for each letter grade. Changes apply immediately to all loaded and saved data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))

                uiState.gradeRanges
                    .sortedByDescending { it.minScore }
                    .forEach { range ->
                        GradeRangeRow(
                            range = range,
                            onEdit = { viewModel.startEditingRange(range) }
                        )
                    }
            }

            // ── Pass Mark ──────────────────────────────────────────────────
            SettingsSection(title = "Pass Mark") {
                PassMarkSlider(
                    passMark = uiState.passMark,
                    onPassMarkChange = viewModel::setPassMark
                )
            }

            // ── Default Export Format ──────────────────────────────────────
            SettingsSection(title = "Default Export Format") {
                ExportFormatPicker(
                    selectedFormat = uiState.defaultExportFormat,
                    onFormatSelected = viewModel::setDefaultExportFormat
                )
            }

            // ── Integrations ───────────────────────────────────────────────
            SettingsSection(title = "Integrations") {
                SettingsSwitchRow(
                    icon = Icons.Filled.CloudUpload,
                    title = "Google Drive Upload",
                    subtitle = "Enable uploading exports to Google Drive",
                    checked = uiState.isGoogleDriveEnabled,
                    onCheckedChange = { viewModel.toggleGoogleDrive() }
                )
            }

            // ── About ──────────────────────────────────────────────────────
            SettingsSection(title = "About") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("GradeMaster v1.0.0", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Built with Kotlin · Jetpack Compose · MVVM · Room · Hilt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // ── Edit Range Dialog ──────────────────────────────────────────────────
    uiState.editingRange?.let { range ->
        EditGradeRangeDialog(
            range = range,
            onConfirm = { updated -> viewModel.saveRange(range, updated) },
            onDismiss = viewModel::cancelEditingRange
        )
    }

    // ── Reset Confirm ──────────────────────────────────────────────────────
    if (uiState.showResetConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::hideResetConfirm,
            icon = { Icon(Icons.Filled.RestartAlt, null) },
            title = { Text("Reset Grade Ranges") },
            text = { Text("Restore default grade ranges (A≥70, B≥60, C≥50, D≥40, F<40)?") },
            confirmButton = {
                Button(onClick = viewModel::resetRangesToDefault) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideResetConfirm) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsSection — reusable section wrapper with title
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsSwitchRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeRangeRow — displays one grade range with edit button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradeRangeRow(range: GradeRange, onEdit: () -> Unit) {
    val color = gradeColor(range.grade)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Grade letter badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(range.grade, color = color, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${range.minScore.toInt()} – ${range.maxScore.toInt()} marks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            // Visual range bar
            LinearProgressIndicator(
                progress = { (range.maxScore / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Edit range",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PassMarkSlider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PassMarkSlider(passMark: Double, onPassMarkChange: (Double) -> Unit) {
    var sliderValue by remember(passMark) { mutableFloatStateOf(passMark.toFloat()) }
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Pass Mark", style = MaterialTheme.typography.titleSmall)
            Text(
                "${sliderValue.toInt()} / 100",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onPassMarkChange(sliderValue.toDouble()) },
            valueRange = 0f..100f,
            steps = 19, // 0, 5, 10, ..., 100
            colors = SliderDefaults.colors(
                thumbColor = GradeMasterColors.OrangeAccent,
                activeTrackColor = GradeMasterColors.OrangeAccent
            )
        )
        Text(
            "Students scoring ${sliderValue.toInt()} or above are considered passing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportFormatPicker
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExportFormatPicker(selectedFormat: String, onFormatSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExportFormat.entries.forEach { fmt ->
            FilterChip(
                selected = selectedFormat == fmt.label,
                onClick = { onFormatSelected(fmt.label) },
                label = { Text(fmt.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EditGradeRangeDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EditGradeRangeDialog(
    range: GradeRange,
    onConfirm: (GradeRange) -> Unit,
    onDismiss: () -> Unit
) {
    var minInput by remember { mutableStateOf(range.minScore.toInt().toString()) }
    var maxInput by remember { mutableStateOf(range.maxScore.toInt().toString()) }
    var minError by remember { mutableStateOf<String?>(null) }
    var maxError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        val min = minInput.toDoubleOrNull()
        val max = maxInput.toDoubleOrNull()
        minError = when {
            min == null -> "Invalid number"
            min < 0 || min > 100 -> "Must be 0–100"
            else -> null
        }
        maxError = when {
            max == null -> "Invalid number"
            max < 0 || max > 100 -> "Must be 0–100"
            min != null && max <= min -> "Must be > min"
            else -> null
        }
        return minError == null && maxError == null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(gradeColor(range.grade).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(range.grade, fontWeight = FontWeight.ExtraBold,
                            color = gradeColor(range.grade))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Edit Grade ${range.grade} Range",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedTextField(
                    value = minInput,
                    onValueChange = { minInput = it; minError = null },
                    label = { Text("Minimum Score") },
                    isError = minError != null,
                    supportingText = minError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxInput,
                    onValueChange = { maxInput = it; maxError = null },
                    label = { Text("Maximum Score") },
                    isError = maxError != null,
                    supportingText = maxError?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview
                val previewMin = minInput.toDoubleOrNull() ?: range.minScore
                val previewMax = maxInput.toDoubleOrNull() ?: range.maxScore
                Text(
                    "Grade ${range.grade}: ${"%.0f".format(previewMin)} ≤ score ≤ ${"%.0f".format(previewMax)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = gradeColor(range.grade),
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (validate()) {
                            onConfirm(
                                range.copy(
                                    minScore = minInput.toDouble(),
                                    maxScore = maxInput.toDouble()
                                )
                            )
                        }
                    }) { Text("Save") }
                }
            }
        }
    }
}
