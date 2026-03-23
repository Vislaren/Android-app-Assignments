package com.grademaster.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.grademaster.data.model.Student
import com.grademaster.ui.theme.GradeMasterColors
import com.grademaster.ui.theme.gradeColor

// ─────────────────────────────────────────────────────────────────────────────
// StudentCard — expandable card with edit/delete controls
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StudentCard(
    student: Student,
    onEdit: (Double, Double) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Main Row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with initial
                StudentAvatar(name = student.name, grade = student.grade)

                Spacer(Modifier.width(12.dp))

                // Name + score
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CA: ${student.caScore}  |  Exam: ${student.examScore}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Grade Badge
                GradeBadge(grade = student.grade, score = student.finalScore)

                Spacer(Modifier.width(8.dp))

                // Expand chevron
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Expanded Section ──────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score chips
                    ScoreChip(label = "CA", value = student.caScore, max = 30.0)
                    ScoreChip(label = "Exam", value = student.examScore, max = 70.0)
                    ScoreChip(label = "Total", value = student.finalScore, max = 100.0)

                    Spacer(Modifier.weight(1f))

                    // Edit Button
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GradeMasterColors.Blue40.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = GradeMasterColors.Blue40,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GradeMasterColors.GradeF.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = GradeMasterColors.GradeF,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Edit Dialog ────────────────────────────────────────────────────────
    if (showEditDialog) {
        EditScoreDialog(
            student = student,
            onConfirm = { ca, exam ->
                onEdit(ca, exam)
                showEditDialog = false
                expanded = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // ── Delete Confirm ─────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = GradeMasterColors.GradeF) },
            title = { Text("Delete Student") },
            text = { Text("Remove \"${student.displayName}\" from this session? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = GradeMasterColors.GradeF)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StudentAvatar — circle with first letter of name
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StudentAvatar(name: String, grade: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(gradeColor(grade).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = gradeColor(grade),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeBadge — pill showing letter grade
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradeBadge(grade: String, score: Double) {
    val color = gradeColor(grade)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = grade,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
        Text(
            text = "%.1f".format(score),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScoreChip — compact chip for a score value
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ScoreChip(label: String, value: Double, max: Double) {
    val pct = (value / max).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        pct >= 0.7f -> GradeMasterColors.GradeA
        pct >= 0.5f -> GradeMasterColors.GradeB
        pct >= 0.4f -> GradeMasterColors.GradeC
        else -> GradeMasterColors.GradeF
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "$value",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EditScoreDialog — dialog to edit CA and Exam scores only
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EditScoreDialog(
    student: Student,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var caInput by remember { mutableStateOf(student.caScore.toString()) }
    var examInput by remember { mutableStateOf(student.examScore.toString()) }
    var caError by remember { mutableStateOf<String?>(null) }
    var examError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        val ca = caInput.toDoubleOrNull()
        val exam = examInput.toDoubleOrNull()
        caError = when {
            ca == null -> "Must be a number"
            ca < 0 || ca > 30 -> "Must be 0 – 30"
            else -> null
        }
        examError = when {
            exam == null -> "Must be a number"
            exam < 0 || exam > 70 -> "Must be 0 – 70"
            else -> null
        }
        return caError == null && examError == null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Edit Scores",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    student.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = caInput,
                    onValueChange = { caInput = it; caError = null },
                    label = { Text("CA Score (0 – 30)") },
                    isError = caError != null,
                    supportingText = caError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examInput,
                    onValueChange = { examInput = it; examError = null },
                    label = { Text("Exam Score (0 – 70)") },
                    isError = examError != null,
                    supportingText = examError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview calculated total
                val previewTotal = (caInput.toDoubleOrNull() ?: 0.0) +
                        (examInput.toDoubleOrNull() ?: 0.0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Total:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "%.1f / 100".format(previewTotal.coerceIn(0.0, 100.0)),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        if (validate()) {
                            onConfirm(
                                caInput.toDouble(),
                                examInput.toDouble()
                            )
                        }
                    }) { Text("Save & Recalculate") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SearchBar — reusable search input
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradeMasterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search students…",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// StatCard — small stat chip used on Home and Vault
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
