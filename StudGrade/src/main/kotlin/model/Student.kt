package model

import kotlinx.serialization.Serializable

/**
 * Core Student data class with validation and utility functions.
 */
@Serializable
data class Student(
    val name: String,
    val id: String,
    val caScore: Double,
    val examScore: Double,
    val finalScore: Double = 0.0,
    val grade: String = ""
) {
    /**
     * Validates and calculates the total final score.
     * CA must be ≤ 30, Exam must be ≤ 70.
     * Returns a new Student with finalScore set, or throws if invalid.
     */
    fun calculateTotal(): Student {
        require(caScore in 0.0..30.0) {
            "CA score for '$name' must be between 0 and 30. Got: $caScore"
        }
        require(examScore in 0.0..70.0) {
            "Exam score for '$name' must be between 0 and 70. Got: $examScore"
        }
        val total = caScore + examScore
        return this.copy(finalScore = total)
    }

    /**
     * Formats student data as a CSV row.
     */
    fun asCsvRow(): String {
        val safeName = if (name.contains(",")) "\"$name\"" else name
        return "$safeName,$id,${"%.1f".format(caScore)},${"%.1f".format(examScore)},${"%.1f".format(finalScore)},$grade"
    }
}

/**
 * CSV header row for export.
 */
val CSV_HEADER = "Name,ID,CA Score,Exam Score,Final Score,Grade"
