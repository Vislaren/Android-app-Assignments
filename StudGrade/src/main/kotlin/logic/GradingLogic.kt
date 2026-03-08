package logic

import model.Student
import model.CSV_HEADER

/**
 * Grading range configuration.
 */
data class GradeRange(
    val label: String,
    val min: Double,
    val max: Double
)

/**
 * Default grading ranges used on startup.
 */
val DEFAULT_GRADE_RANGES = listOf(
    GradeRange("A", 70.0, 100.0),
    GradeRange("B", 60.0, 69.9),
    GradeRange("C", 50.0, 59.9),
    GradeRange("D", 40.0, 49.9),
    GradeRange("F", 0.0, 39.9)
)

/**
 * Builds a grading lambda from a list of GradeRange entries.
 */
fun buildGradingLogic(ranges: List<GradeRange>): (Double) -> String {
    return { score: Double ->
        ranges.firstOrNull { score >= it.min && score <= it.max }?.label ?: "F"
    }
}

/**
 * Higher-order function: processes a list of students by
 * calculating totals and assigning grades via a provided lambda.
 *
 * @param list           Raw student list (without finalScore/grade set).
 * @param gradingLogic   Lambda that maps a Double score → grade String.
 * @return               New list of fully-processed students.
 */
fun processStudents(
    list: List<Student>,
    gradingLogic: (Double) -> String
): List<Student> {
    return list.map { student ->
        try {
            val withTotal = student.calculateTotal()
            withTotal.copy(grade = gradingLogic(withTotal.finalScore))
        } catch (e: IllegalArgumentException) {
            // Return student with error marker if validation fails
            student.copy(
                finalScore = -1.0,
                grade = "ERR: ${e.message?.take(40)}"
            )
        }
    }
}

/**
 * Filters students by a minimum final score threshold.
 * Demonstrates use of .filter with a lambda.
 */
fun filterByMinScore(students: List<Student>, minScore: Double): List<Student> =
    students.filter { it.finalScore >= minScore }

/**
 * Filters "Distinction" students (grade == "A") — used in main() demo.
 */
fun filterDistinction(students: List<Student>): List<Student> =
    students.filter { it.grade == "A" }

/**
 * Parses a CSV string (multi-line) into a list of raw Students.
 * Expected columns: name, id, caScore, examScore
 */
fun parseCsvContent(content: String): Pair<List<Student>, List<String>> {
    val errors = mutableListOf<String>()
    val students = mutableListOf<Student>()

    val lines = content.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val dataLines = if (lines.firstOrNull()
            ?.lowercase()
            ?.contains("name") == true
    ) lines.drop(1) else lines

    dataLines.forEachIndexed { index, line ->
        try {
            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (cols.size < 4) {
                errors.add("Row ${index + 1}: Expected at least 4 columns, got ${cols.size}")
                return@forEachIndexed
            }
            students.add(
                Student(
                    name = cols[0],
                    id = cols[1],
                    caScore = cols[2].toDouble(),
                    examScore = cols[3].toDouble()
                )
            )
        } catch (e: Exception) {
            errors.add("Row ${index + 1}: ${e.message}")
        }
    }
    return Pair(students, errors)
}

/**
 * Converts a processed list of students to a full CSV string.
 */
fun exportToCsv(students: List<Student>): String {
    val sb = StringBuilder()
    sb.appendLine(CSV_HEADER)
    students.forEach { sb.appendLine(it.asCsvRow()) }
    return sb.toString()
}
