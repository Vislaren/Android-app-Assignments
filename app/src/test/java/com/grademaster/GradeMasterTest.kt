package com.grademaster

import com.grademaster.data.model.Grade
import com.grademaster.data.model.GradeRange
import com.grademaster.data.model.Student
import com.grademaster.data.model.Student.Companion.byGrade
import com.grademaster.data.model.Student.Companion.classAverage
import com.grademaster.data.model.Student.Companion.failed
import com.grademaster.data.model.Student.Companion.passed
import com.grademaster.data.model.Student.Companion.searchBy
import com.grademaster.data.model.Student.Companion.topN
import com.grademaster.data.model.Student.Companion.transformWith
import com.grademaster.data.model.ValidationResult
import com.grademaster.util.ClassStats
import com.grademaster.util.GradeCalculator
import com.grademaster.util.StudentTransformer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GradeMasterTest — Comprehensive Unit Test Suite
 *
 * Demonstrates:
 *  1. Lambda passed to custom higher-order function [StudentTransformer.applyTransformation]
 *  2. Collection operations: filter, map, groupBy on Student lists
 *  3. OOP: Student validation, grade assignment, immutable copies
 *  4. GradeCalculator logic with dynamic grade ranges
 * ═══════════════════════════════════════════════════════════════════════════
 */
class GradeMasterTest {

    // ── Test Fixtures ──────────────────────────────────────────────────────
    private val defaultRanges = GradeRange.DEFAULT_RANGES

    private lateinit var students: List<Student>

    @Before
    fun setup() {
        students = listOf(
            Student(1L, 0L, "Alice Johnson",    caScore = 28.0, examScore = 65.0, finalScore = 93.0, grade = "A"),
            Student(2L, 0L, "Bob Smith",        caScore = 25.0, examScore = 50.0, finalScore = 75.0, grade = "B"),
            Student(3L, 0L, "Clara Osei",       caScore = 20.0, examScore = 35.0, finalScore = 55.0, grade = "C"),
            Student(4L, 0L, "David Acheampong", caScore = 15.0, examScore = 28.0, finalScore = 43.0, grade = "D"),
            Student(5L, 0L, "Emeka Nwosu",      caScore = 10.0, examScore = 20.0, finalScore = 30.0, grade = "F"),
            Student(6L, 0L, "Fatima Al-Hassan", caScore = 5.0,  examScore = 15.0, finalScore = 20.0, grade = "F"),
            Student(7L, 0L, "Grace Mensah",     caScore = 30.0, examScore = 62.0, finalScore = 92.0, grade = "A"),
            Student(8L, 0L, "Henry Addo",       caScore = 22.0, examScore = 42.0, finalScore = 64.0, grade = "C")
        )
    }

    // ══════════════════════════════════════════════════════════════════════
    // REQUIREMENT 4a: Lambda passed to custom higher-order function
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `HOF - applyTransformation with custom toSummary lambda`() {
        // LAMBDA DEFINITION — satisfies: "a lambda passed to your custom HOF"
        val toSummaryLambda: (Student) -> String = { student ->
            "[${student.grade}] ${student.name} scored ${student.finalScore}/100"
        }

        // CALL SITE — pass lambda into custom higher-order function
        val summaries: List<String> = StudentTransformer.applyTransformation(
            students = students,
            transformer = toSummaryLambda   // ← lambda argument
        )

        assertEquals(8, summaries.size)
        assertTrue(summaries[0].startsWith("[A] Alice Johnson"))
        assertTrue(summaries[4].startsWith("[F] Emeka Nwosu"))
        println("=== HOF Lambda Output (toSummary) ===")
        summaries.forEach { println("  $it") }
    }

    @Test
    fun `HOF - applyTransformation with CSV row lambda`() {
        // Using a predefined transformer lambda from StudentTransformer
        val csvRows: List<String> = StudentTransformer.applyTransformation(
            students = students,
            transformer = StudentTransformer.toCsvRow   // ← predefined lambda
        )

        assertEquals(8, csvRows.size)
        assertTrue(csvRows[0].contains("Alice Johnson"))
        assertTrue(csvRows[0].contains("93.0"))
        println("=== HOF Lambda Output (toCsvRow) ===")
        csvRows.forEach { println("  $it") }
    }

    @Test
    fun `HOF - applyTransformation with name-grade pair lambda`() {
        val pairs: List<Pair<String, String>> = StudentTransformer.applyTransformation(
            students = students,
            transformer = { Pair(it.name, it.grade) }   // ← inline lambda
        )

        assertEquals(8, pairs.size)
        assertEquals("Alice Johnson" to "A", pairs[0])
        assertEquals("Emeka Nwosu" to "F", pairs[4])
    }

    @Test
    fun `HOF - applyTransformation with composed preProcess and transformer lambdas`() {
        // Compose two lambdas: normalise name, then format
        val normalised: List<String> = StudentTransformer.applyTransformation(
            students = students,
            preProcess = StudentTransformer.normalizeName,  // ← lambda 1
            transformer = { "${it.name} [${it.grade}]" }   // ← lambda 2
        )
        assertTrue(normalised[0].startsWith("Alice Johnson"))
    }

    @Test
    fun `HOF - Student transformWith extension function`() {
        // Using the extension HOF defined in Student companion
        val grades: List<String> = with(Student) {
            students.transformWith { it.grade }
        }
        assertEquals(listOf("A", "B", "C", "D", "F", "F", "A", "C"), grades)
    }

    // ══════════════════════════════════════════════════════════════════════
    // REQUIREMENT 4b: Collection operations (filter / map / group)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `COLLECTION - filter students who passed`() {
        val passing: List<Student> = with(Student) { students.passed(passMark = 40.0) }

        assertEquals(6, passing.size)
        assertTrue(passing.all { it.finalScore >= 40.0 })
        println("=== Passed Students ===")
        passing.forEach { println("  ${it.name}: ${it.finalScore}") }
    }

    @Test
    fun `COLLECTION - filter students who failed`() {
        val failing: List<Student> = with(Student) { students.failed(passMark = 40.0) }

        assertEquals(2, failing.size)
        assertTrue(failing.all { it.finalScore < 40.0 })
        assertEquals("Emeka Nwosu", failing[0].name)
        assertEquals("Fatima Al-Hassan", failing[1].name)
        println("=== Failed Students ===")
        failing.forEach { println("  ${it.name}: ${it.finalScore}") }
    }

    @Test
    fun `COLLECTION - filter by grade using higher-order function`() {
        val gradeA = with(Student) { students.byGrade(Grade.A) }
        val gradeF = with(Student) { students.byGrade(Grade.F) }

        assertEquals(2, gradeA.size)
        assertEquals(2, gradeF.size)
        assertTrue(gradeA.all { it.grade == "A" })
        assertTrue(gradeF.all { it.grade == "F" })
    }

    @Test
    fun `COLLECTION - map to extract scores`() {
        val totalScores: List<Double> = students.map { it.finalScore }
        val caScores: List<Double> = students.map { it.caScore }

        assertEquals(8, totalScores.size)
        assertEquals(93.0, totalScores[0], 0.001)
        assertEquals(28.0, caScores[0], 0.001)
    }

    @Test
    fun `COLLECTION - groupBy grade`() {
        val grouped: Map<String, List<Student>> = students.groupBy { it.grade }

        assertTrue(grouped.containsKey("A"))
        assertTrue(grouped.containsKey("F"))
        assertEquals(2, grouped["A"]!!.size)
        assertEquals(2, grouped["F"]!!.size)
        println("=== Grade Distribution ===")
        grouped.forEach { (grade, list) ->
            println("  $grade: ${list.size} student(s) — ${list.map { it.name }}")
        }
    }

    @Test
    fun `COLLECTION - topN students`() {
        val top3 = with(Student) { students.topN(3) }

        assertEquals(3, top3.size)
        assertEquals("Alice Johnson", top3[0].name)   // 93.0
        assertEquals("Grace Mensah", top3[1].name)    // 92.0
        assertEquals("Bob Smith", top3[2].name)       // 75.0
    }

    @Test
    fun `COLLECTION - classAverage`() {
        val avg = with(Student) { students.classAverage() }

        val expected = students.sumOf { it.finalScore } / students.size
        assertEquals(expected, avg, 0.001)
        println("Class average: %.2f".format(avg))
    }

    @Test
    fun `COLLECTION - searchBy name`() {
        val results = with(Student) { students.searchBy("osei") }

        assertEquals(1, results.size)
        assertEquals("Clara Osei", results[0].name)
    }

    @Test
    fun `COLLECTION - chained operations: top passing students formatted`() {
        // CHAIN: filter → sort → take → map  (functional pipeline)
        val report: List<String> = students
            .filter { it.isPassing }                       // keep passing
            .sortedByDescending { it.finalScore }          // rank by score
            .take(3)                                       // top 3
            .mapIndexed { rank, s ->                       // format with rank
                "#${rank + 1}: ${s.name} — ${s.finalScore} (${s.grade})"
            }

        assertEquals(3, report.size)
        assertTrue(report[0].startsWith("#1:"))
        println("=== Top 3 Passing Students ===")
        report.forEach { println("  $it") }
    }

    // ══════════════════════════════════════════════════════════════════════
    // OOP: Validation tests
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `VALIDATION - valid student passes`() {
        val student = Student(name = "Test Student", caScore = 25.0, examScore = 60.0,
            finalScore = 85.0, grade = "A")
        val result = student.validate()
        assertTrue(result.isValid)
        assertFalse(result is ValidationResult.Invalid)
    }

    @Test
    fun `VALIDATION - CA score over 30 fails`() {
        val student = Student(name = "Cheater", caScore = 35.0, examScore = 50.0,
            finalScore = 85.0, grade = "A")
        val result = student.validate()
        assertFalse(result.isValid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("CA") })
    }

    @Test
    fun `VALIDATION - Exam score over 70 fails`() {
        val student = Student(name = "Cheater", caScore = 20.0, examScore = 75.0,
            finalScore = 95.0, grade = "A")
        val result = student.validate()
        assertFalse(result.isValid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("Exam") })
    }

    @Test
    fun `VALIDATION - blank name fails`() {
        val student = Student(name = "", caScore = 20.0, examScore = 50.0,
            finalScore = 70.0, grade = "B")
        val result = student.validate()
        assertFalse(result.isValid)
        assertTrue((result as ValidationResult.Invalid).errors.any { it.contains("name") })
    }

    // ══════════════════════════════════════════════════════════════════════
    // GradeCalculator tests — dynamic ranges
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `GRADE_ENGINE - calculates grade from default ranges`() {
        assertEquals("A", GradeCalculator.calculateGrade(90.0, defaultRanges))
        assertEquals("B", GradeCalculator.calculateGrade(65.0, defaultRanges))
        assertEquals("C", GradeCalculator.calculateGrade(55.0, defaultRanges))
        assertEquals("D", GradeCalculator.calculateGrade(45.0, defaultRanges))
        assertEquals("F", GradeCalculator.calculateGrade(30.0, defaultRanges))
        assertEquals("F", GradeCalculator.calculateGrade(0.0, defaultRanges))
    }

    @Test
    fun `GRADE_ENGINE - recalculates all with custom ranges`() {
        val strictRanges = listOf(
            GradeRange("A", 80.0, 100.0),
            GradeRange("B", 65.0, 79.99),
            GradeRange("C", 55.0, 64.99),
            GradeRange("D", 45.0, 54.99),
            GradeRange("F", 0.0, 44.99)
        )
        val recalculated = GradeCalculator.recalculateAll(students, strictRanges)

        // Alice 93.0 → A (unchanged), Bob 75.0 → B (unchanged),
        // Henry 64.0 → C (was C before — still correct), David 43.0 → F (was D)
        assertEquals("A", recalculated.first { it.name == "Alice Johnson" }.grade)
        assertEquals("F", recalculated.first { it.name == "David Acheampong" }.grade)
        println("=== Recalculated with strict ranges ===")
        recalculated.forEach { println("  ${it.name}: ${it.finalScore} → ${it.grade}") }
    }

    @Test
    fun `GRADE_ENGINE - computeStats produces correct summary`() {
        val stats: ClassStats = GradeCalculator.computeStats(students, passMark = 40.0)

        assertEquals(8, stats.totalStudents)
        assertEquals(6, stats.passed)
        assertEquals(2, stats.failed)
        assertEquals("Alice Johnson", stats.highest?.name)
        assertEquals("Fatima Al-Hassan", stats.lowest?.name)
        val passRate = stats.passRate
        assertEquals(75.0, passRate, 0.1)
        println("Pass rate: %.1f%%".format(passRate))
    }

    @Test
    fun `GRADE_ENGINE - computeStats on empty list returns empty`() {
        val stats = GradeCalculator.computeStats(emptyList())
        assertEquals(0, stats.totalStudents)
        assertEquals(0.0, stats.average, 0.001)
    }

    // ══════════════════════════════════════════════════════════════════════
    // Student.withGrade — immutable update
    // ══════════════════════════════════════════════════════════════════════

    @Test
    fun `withGrade - reassigns grade based on ranges`() {
        val student = Student(name = "Test", caScore = 30.0, examScore = 45.0, finalScore = 75.0, grade = "F")
        val updated = student.withGrade(defaultRanges)
        assertEquals("B", updated.grade)
    }

    @Test
    fun `withGrade - original is unchanged (immutability)`() {
        val original = Student(name = "Test", caScore = 28.0, examScore = 65.0,
            finalScore = 93.0, grade = "F")
        original.withGrade(defaultRanges)
        assertEquals("F", original.grade) // original must be unmodified
    }

    // ══════════════════════════════════════════════════════════════════════
    // main() — standalone demonstration (runs outside Android)
    // ══════════════════════════════════════════════════════════════════════

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("╔══════════════════════════════════════════════════════╗")
            println("║           GradeMaster — Code Demonstration           ║")
            println("╚══════════════════════════════════════════════════════╝")

            val ranges = GradeRange.DEFAULT_RANGES

            val sampleStudents = listOf(
                Student(1, 0, "Alice Johnson",    28.0, 65.0, 93.0, "A"),
                Student(2, 0, "Bob Smith",        25.0, 50.0, 75.0, "B"),
                Student(3, 0, "Clara Osei",       20.0, 35.0, 55.0, "C"),
                Student(4, 0, "David Acheampong", 15.0, 28.0, 43.0, "D"),
                Student(5, 0, "Emeka Nwosu",      10.0, 20.0, 30.0, "F"),
                Student(6, 0, "Fatima Al-Hassan",  5.0, 15.0, 20.0, "F"),
                Student(7, 0, "Grace Mensah",     30.0, 62.0, 92.0, "A"),
                Student(8, 0, "Henry Addo",       22.0, 42.0, 64.0, "C")
            )

            // ── 1. Lambda passed to custom HOF ─────────────────────────────
            println("\n▶ 1. Custom HOF: applyTransformation with lambda")
            val reportLambda: (Student) -> String = { s ->
                "%-20s CA:%-5.1f Exam:%-5.1f Total:%-6.1f Grade:%s"
                    .format(s.name, s.caScore, s.examScore, s.finalScore, s.grade)
            }
            val formattedReport = StudentTransformer.applyTransformation(
                students = sampleStudents,
                transformer = reportLambda   // ← lambda passed to HOF
            )
            formattedReport.forEach { println("   $it") }

            // ── 2. Collection: filter students who failed ───────────────────
            println("\n▶ 2. Collection — filter: students who FAILED (score < 40)")
            val failedStudents = with(Student) { sampleStudents.failed(passMark = 40.0) }
            failedStudents.forEach { println("   ✗ ${it.name} — ${it.finalScore}") }

            // ── 3. Collection: filter students who passed ───────────────────
            println("\n▶ 3. Collection — filter: students who PASSED (score ≥ 40)")
            val passedStudents = with(Student) { sampleStudents.passed(passMark = 40.0) }
            passedStudents.forEach { println("   ✓ ${it.name} — ${it.finalScore}") }

            // ── 4. Collection: groupBy + map summary ───────────────────────
            println("\n▶ 4. Collection — groupBy grade + map to count")
            val distribution = sampleStudents
                .groupBy { it.grade }
                .mapValues { (_, list) -> list.size }
                .toSortedMap()
            distribution.forEach { (grade, count) ->
                println("   $grade: ${"█".repeat(count)} ($count)")
            }

            // ── 5. Class statistics ────────────────────────────────────────
            println("\n▶ 5. Class Statistics")
            val stats = GradeCalculator.computeStats(sampleStudents)
            println("   Total: ${stats.totalStudents}")
            println("   Passed: ${stats.passed}  Failed: ${stats.failed}")
            println("   Pass Rate: %.1f%%".format(stats.passRate))
            println("   Average: %.2f".format(stats.average))
            println("   Top Student: ${stats.highest?.name} (${stats.highest?.finalScore})")

            // ── 6. Dynamic grade recalculation ─────────────────────────────
            println("\n▶ 6. Dynamic Recalculation (strict ranges: A≥80)")
            val strictRanges = listOf(
                GradeRange("A", 80.0, 100.0),
                GradeRange("B", 65.0, 79.99),
                GradeRange("C", 55.0, 64.99),
                GradeRange("D", 45.0, 54.99),
                GradeRange("F", 0.0, 44.99)
            )
            val recalculated = GradeCalculator.recalculateAll(sampleStudents, strictRanges)
            recalculated.forEach { s ->
                val original = sampleStudents.first { it.id == s.id }
                val changed = if (original.grade != s.grade) " ← was ${original.grade}" else ""
                println("   ${s.name}: ${s.finalScore} → ${s.grade}$changed")
            }

            println("\n╔══════════════════════════════════════════════════════╗")
            println("║                  Demonstration Complete              ║")
            println("╚══════════════════════════════════════════════════════╝")
        }
    }
}
