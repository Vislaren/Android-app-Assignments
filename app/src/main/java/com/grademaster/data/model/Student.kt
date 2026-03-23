package com.grademaster.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// Grade Enum — all possible letter grades
// ─────────────────────────────────────────────────────────────────────────────
enum class Grade(val label: String, val description: String) {
    A("A", "Excellent"),
    B("B", "Very Good"),
    C("C", "Good"),
    D("D", "Pass"),
    F("F", "Fail");

    companion object {
        fun fromLabel(label: String): Grade =
            entries.firstOrNull { it.label == label } ?: F
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Validation Result — sealed class for expressive error handling
// ─────────────────────────────────────────────────────────────────────────────
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

// ─────────────────────────────────────────────────────────────────────────────
// Student — core data class with embedded validation & functional utilities
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long = 0L,   // links student to a saved vault session
    val name: String = "",
    val caScore: Double = 0.0,  // Continuous Assessment: max 30
    val examScore: Double = 0.0, // Exam score: max 70
    val finalScore: Double = caScore + examScore,
    val grade: String = Grade.F.label
) {
    // ── Computed Properties ────────────────────────────────────────────────
    val displayName: String get() = name.trim().ifBlank { "Unknown Student" }
    val isPassing: Boolean get() = finalScore >= 40.0
    val gradeEnum: Grade get() = Grade.fromLabel(grade)

    // ── OOP: Validation Logic ──────────────────────────────────────────────
    fun validate(): ValidationResult {
        val errors = buildList {
            if (name.isBlank()) add("Student name cannot be empty.")
            if (caScore < 0 || caScore > 30)
                add("CA score ($caScore) must be between 0 and 30.")
            if (examScore < 0 || examScore > 70)
                add("Exam score ($examScore) must be between 0 and 70.")
            if (finalScore < 0 || finalScore > 100)
                add("Final score ($finalScore) must be between 0 and 100.")
        }
        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    // ── Functional: Copy with recalculated grade ───────────────────────────
    fun withGrade(gradeRanges: List<GradeRange>): Student {
        val calculatedGrade = gradeRanges
            .sortedByDescending { it.minScore }
            .firstOrNull { finalScore >= it.minScore }
            ?.grade ?: Grade.F.label
        return copy(grade = calculatedGrade)
    }

    // ── Summary string for display ─────────────────────────────────────────
    fun toSummaryString(): String =
        "[$grade] $displayName — CA: $caScore | Exam: $examScore | Total: $finalScore"

    companion object {
        const val MAX_CA = 30.0
        const val MAX_EXAM = 70.0
        const val MAX_TOTAL = 100.0

        // ── Functional: Mandatory custom higher-order function ─────────────
        // Accepts a lambda (transformer) to format or transform student data.
        // This satisfies the "custom higher-order function" requirement.
        fun <T> List<Student>.transformWith(transformer: (Student) -> T): List<T> =
            this.map(transformer)

        // ── Functional: Filter helpers ─────────────────────────────────────
        fun List<Student>.passed(passMark: Double = 40.0): List<Student> =
            filter { it.finalScore >= passMark }

        fun List<Student>.failed(passMark: Double = 40.0): List<Student> =
            filter { it.finalScore < passMark }

        fun List<Student>.byGrade(grade: Grade): List<Student> =
            filter { it.grade == grade.label }

        fun List<Student>.topN(n: Int): List<Student> =
            sortedByDescending { it.finalScore }.take(n)

        fun List<Student>.classAverage(): Double =
            if (isEmpty()) 0.0 else sumOf { it.finalScore } / size

        fun List<Student>.searchBy(query: String): List<Student> =
            filter { it.name.contains(query, ignoreCase = true) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeRange — used in Settings to define dynamic grade boundaries
// ─────────────────────────────────────────────────────────────────────────────
data class GradeRange(
    val grade: String,
    val minScore: Double,
    val maxScore: Double,
    val color: Long = 0xFF4CAF50 // default green
) {
    fun contains(score: Double): Boolean = score in minScore..maxScore

    override fun toString(): String =
        "$grade: ${minScore.toInt()} – ${maxScore.toInt()}"

    companion object {
        val DEFAULT_RANGES = listOf(
            GradeRange("A", 70.0, 100.0, 0xFF4CAF50),
            GradeRange("B", 60.0, 69.99, 0xFF2196F3),
            GradeRange("C", 50.0, 59.99, 0xFFFF9800),
            GradeRange("D", 40.0, 49.99, 0xFFFF5722),
            GradeRange("F", 0.0, 39.99, 0xFFF44336)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSession — groups a set of students saved to the vault
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "vault_sessions")
data class VaultSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val source: String = "",         // file name / URL origin
    val savedAt: Long = System.currentTimeMillis(),
    val studentCount: Int = 0,
    val averageScore: Double = 0.0,
    val passRate: Double = 0.0
) {
    val formattedDate: String get() {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(savedAt))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProcessingState — UI state for file loading
// ─────────────────────────────────────────────────────────────────────────────
sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Loading : ProcessingState()
    data class Success(val message: String = "Processed successfully") : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportFormat — supported export types
// ─────────────────────────────────────────────────────────────────────────────
enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV", "csv", "text/csv"),
    EXCEL("Excel", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("PDF", "pdf", "application/pdf")
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportDestination — where to send the exported file
// ─────────────────────────────────────────────────────────────────────────────
enum class ExportDestination(val label: String) {
    INTERNAL_STORAGE("Save to Device"),
    GOOGLE_DRIVE("Upload to Google Drive"),
    SHARE("Share via..."),
}
