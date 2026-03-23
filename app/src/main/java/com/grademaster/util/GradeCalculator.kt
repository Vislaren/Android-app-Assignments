package com.grademaster.util

import com.grademaster.data.model.Grade
import com.grademaster.data.model.GradeRange
import com.grademaster.data.model.Student

// ─────────────────────────────────────────────────────────────────────────────
// GradeCalculator — pure calculation functions
// ─────────────────────────────────────────────────────────────────────────────
object GradeCalculator {

    /**
     * Calculate the grade for a given total score based on dynamic ranges.
     */
    fun calculateGrade(totalScore: Double, ranges: List<GradeRange>): String {
        return ranges
            .sortedByDescending { it.minScore }
            .firstOrNull { totalScore >= it.minScore && totalScore <= it.maxScore }
            ?.grade ?: Grade.F.label
    }

    /**
     * Recalculate all students' grades using the provided grade ranges.
     * Uses a map() higher-order function internally.
     */
    fun recalculateAll(
        students: List<Student>,
        ranges: List<GradeRange>
    ): List<Student> = students.map { student ->
        val total = student.caScore + student.examScore
        val grade = calculateGrade(total, ranges)
        student.copy(finalScore = total, grade = grade)
    }

    /**
     * Class statistics: pass rate, average, grade distribution
     */
    fun computeStats(students: List<Student>, passMark: Double = 40.0): ClassStats {
        if (students.isEmpty()) return ClassStats.empty()

        val passed = students.filter { it.finalScore >= passMark }
        val failed = students.filter { it.finalScore < passMark }
        val average = students.sumOf { it.finalScore } / students.size
        val highest = students.maxByOrNull { it.finalScore }
        val lowest = students.minByOrNull { it.finalScore }

        val gradeDistribution = students
            .groupBy { it.grade }
            .mapValues { (_, v) -> v.size }

        return ClassStats(
            totalStudents = students.size,
            passed = passed.size,
            failed = failed.size,
            passRate = passed.size.toDouble() / students.size * 100,
            average = average,
            highest = highest,
            lowest = lowest,
            gradeDistribution = gradeDistribution
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ClassStats — summary data for a session
// ─────────────────────────────────────────────────────────────────────────────
data class ClassStats(
    val totalStudents: Int,
    val passed: Int,
    val failed: Int,
    val passRate: Double,
    val average: Double,
    val highest: Student?,
    val lowest: Student?,
    val gradeDistribution: Map<String, Int>
) {
    companion object {
        fun empty() = ClassStats(0, 0, 0, 0.0, 0.0, null, null, emptyMap())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StudentTransformer — mandatory custom higher-order function namespace
// ─────────────────────────────────────────────────────────────────────────────
object StudentTransformer {

    /**
     * MANDATORY CUSTOM HIGHER-ORDER FUNCTION.
     *
     * `applyTransformation` accepts a lambda `transformer: (Student) -> T`
     * and returns a mapped result list. The caller decides how to format or
     * transform each Student at the call site (see [StudentProcessorDemo]).
     *
     * @param students  The source list of students.
     * @param transformer A lambda that converts a [Student] into any type [T].
     * @return           A list of transformed values.
     */
    fun <T> applyTransformation(
        students: List<Student>,
        transformer: (Student) -> T
    ): List<T> = students.map(transformer)

    /**
     * Overload: chain two transformations (compose two lambdas).
     *
     * First applies [preProcess] to filter or modify, then [transformer].
     */
    fun <T> applyTransformation(
        students: List<Student>,
        preProcess: (Student) -> Student,
        transformer: (Student) -> T
    ): List<T> = students.map(preProcess).map(transformer)

    // ── Predefined transformers (lambdas that can be passed around) ────────

    /** Formats a Student as a CSV row string. */
    val toCsvRow: (Student) -> String = { s ->
        "${s.id},\"${s.name}\",${s.caScore},${s.examScore},${s.finalScore},${s.grade}"
    }

    /** Formats a Student as a summary display string. */
    val toDisplaySummary: (Student) -> String = { s ->
        "[${s.grade}] ${s.name} — ${s.finalScore}/100 (CA: ${s.caScore}, Exam: ${s.examScore})"
    }

    /** Extracts (name, grade) pairs for quick display. */
    val toNameGradePair: (Student) -> Pair<String, String> = { s ->
        Pair(s.name, s.grade)
    }

    /** Normalises the name field. */
    val normalizeName: (Student) -> Student = { s ->
        s.copy(name = s.name.trim().split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        )
    }
}
