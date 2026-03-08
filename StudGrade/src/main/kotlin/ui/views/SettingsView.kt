package ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import logic.GradeRange
import logic.DEFAULT_GRADE_RANGES
import ui.OrangeAccent
import ui.components.SectionHeader

@Composable
fun SettingsView(
    gradeRanges: List<GradeRange>,
    onRangesChange: (List<GradeRange>) -> Unit
) {
    // Local editable state — synced to parent on apply
    var editableRanges by remember(gradeRanges) {
        mutableStateOf(gradeRanges.map { it.copy() })
    }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var savedMessage by remember { mutableStateOf("") }

    fun validate(): Boolean {
        val errors = mutableListOf<String>()
        editableRanges.forEachIndexed { i, range ->
            if (range.label.isBlank()) errors.add("Row ${i + 1}: Grade label cannot be empty")
            if (range.min < 0 || range.max > 100)
                errors.add("Row ${i + 1}: Scores must be between 0 and 100")
            if (range.min > range.max)
                errors.add("Row ${i + 1}: Min cannot exceed Max")
        }
        validationErrors = errors
        return errors.isEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionHeader(
                        title = "Grading System",
                        subtitle = "Define score ranges for each grade label"
                    )
                    Spacer(Modifier.height(16.dp))

                    // Column headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Grade", modifier = Modifier.width(70.dp),
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Min Score", modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Max Score", modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(40.dp)) // space for delete button
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    editableRanges.forEachIndexed { index, range ->
                        GradeRangeRow(
                            range = range,
                            onLabelChange = { newLabel ->
                                editableRanges = editableRanges.toMutableList().also {
                                    it[index] = it[index].copy(label = newLabel)
                                }
                            },
                            onMinChange = { newMin ->
                                editableRanges = editableRanges.toMutableList().also {
                                    it[index] = it[index].copy(min = newMin)
                                }
                            },
                            onMaxChange = { newMax ->
                                editableRanges = editableRanges.toMutableList().also {
                                    it[index] = it[index].copy(max = newMax)
                                }
                            },
                            onDelete = {
                                editableRanges = editableRanges.toMutableList().also {
                                    it.removeAt(index)
                                }
                            },
                            canDelete = editableRanges.size > 1
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Add row button
                    OutlinedButton(
                        onClick = {
                            editableRanges = editableRanges + GradeRange("", 0.0, 0.0)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Grade Level")
                    }

                    // Validation errors
                    if (validationErrors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        validationErrors.forEach { err ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(err, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Apply / Reset
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (validate()) {
                                    onRangesChange(editableRanges)
                                    savedMessage = "✓ Grading logic updated and applied!"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Apply Changes", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                editableRanges = DEFAULT_GRADE_RANGES.map { it.copy() }
                                onRangesChange(DEFAULT_GRADE_RANGES)
                                savedMessage = "Reset to defaults"
                                validationErrors = emptyList()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Defaults")
                        }
                    }

                    if (savedMessage.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(savedMessage, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Preview section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Grading Table", fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    gradeRanges.forEach { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grade ${range.label}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                            Text("${"%.0f".format(range.min)} – ${"%.0f".format(range.max)}",
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }

        // About section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("About Student Grade Calculator",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Version 1.0.0 · Built with Kotlin & Compose for Desktop\n" +
                        "Supports CSV and Excel (.xlsx) file imports.\n" +
                        "CA scores capped at 30 · Exam scores capped at 70.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeRangeRow(
    range: GradeRange,
    onLabelChange: (String) -> Unit,
    onMinChange: (Double) -> Unit,
    onMaxChange: (Double) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = range.label,
            onValueChange = onLabelChange,
            modifier = Modifier.width(70.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("A", fontSize = 13.sp) }
        )

        OutlinedTextField(
            value = if (range.min == 0.0 && range.label.isEmpty()) "" else "%.0f".format(range.min),
            onValueChange = { it.toDoubleOrNull()?.let(onMinChange) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("0", fontSize = 13.sp) }
        )

        OutlinedTextField(
            value = if (range.max == 0.0 && range.label.isEmpty()) "" else "%.0f".format(range.max),
            onValueChange = { it.toDoubleOrNull()?.let(onMaxChange) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("100", fontSize = 13.sp) }
        )

        IconButton(
            onClick = onDelete,
            enabled = canDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "Remove",
                tint = if (canDelete) OrangeAccent
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
