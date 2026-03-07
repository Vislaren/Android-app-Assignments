package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.Student
import ui.OrangeAccent

// ── Grade Badge ──────────────────────────────────────────────────

@Composable
fun GradeBadge(grade: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (grade) {
        "A"  -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "B"  -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "C"  -> Color(0xFFFFF176) to Color(0xFF333000)
        "D"  -> Color(0xFFFFCC80) to Color(0xFF3E1500)
        else -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (grade.startsWith("ERR")) "ERR" else grade,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// ── Student Card ─────────────────────────────────────────────────

@Composable
fun StudentCard(
    student: Student,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isError = student.finalScore < 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            // Name + ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${student.id}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Score chips
            if (!isError) {
                ScoreChip(label = "CA", value = student.caScore, max = 30.0)
                Spacer(Modifier.width(6.dp))
                ScoreChip(label = "EX", value = student.examScore, max = 70.0)
                Spacer(Modifier.width(6.dp))
                ScoreChip(label = "TOT", value = student.finalScore, max = 100.0, bold = true)
                Spacer(Modifier.width(8.dp))
            } else {
                Text(
                    text = student.grade.removePrefix("ERR: "),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.width(160.dp)
                )
            }

            GradeBadge(grade = student.grade)

            if (onDelete != null) {
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = OrangeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(
    label: String,
    value: Double,
    max: Double,
    bold: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "%.0f".format(value),
            fontSize = if (bold) 14.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Search Bar ───────────────────────────────────────────────────

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search students…",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear",
                        modifier = Modifier.size(18.dp))
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        modifier = modifier.height(52.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}

// ── Stats Row ────────────────────────────────────────────────────

@Composable
fun StatsRow(students: List<Student>) {
    if (students.isEmpty()) return
    val valid = students.filter { it.finalScore >= 0 }
    val avg = if (valid.isNotEmpty()) valid.map { it.finalScore }.average() else 0.0
    val highest = valid.maxOfOrNull { it.finalScore } ?: 0.0
    val passing = valid.count { it.finalScore >= 40 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Total", "${students.size}", Modifier.weight(1f))
        StatCard("Avg Score", "%.1f".format(avg), Modifier.weight(1f))
        StatCard("Highest", "%.1f".format(highest), Modifier.weight(1f))
        StatCard("Passing", "$passing", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary)
            Text(label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────

@Composable
fun EmptyState(message: String, icon: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Section Header ───────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String = "") {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}
