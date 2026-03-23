package com.grademaster.util

import kotlin.collections.emptyList
import android.content.Context
import android.net.Uri
import com.grademaster.data.model.GradeRange
import com.grademaster.data.model.ProcessingState
import com.grademaster.data.model.Student
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext


@Singleton
class FileProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Column name aliases for auto-detection
        private val CA_ALIASES = setOf(
            "ca", "continuous assessment", "ca score", "continuous_assessment",
            "c.a", "coursework", "test", "ca marks", "continuous"
        )
        private val EXAM_ALIASES = setOf(
            "exam", "examination", "exam score", "exam_score",
            "final exam", "e.x", "exam marks", "exams"
        )
        private val NAME_ALIASES = setOf(
            "name", "student name", "full name", "student_name",
            "fullname", "student", "pupil"
        )
    }

    // ── Main entry: detect file type and parse ─────────────────────────────
    fun processFile(
        uri: Uri,
        gradeRanges: List<GradeRange>
    ): Flow<Pair<ProcessingState, List<Student>>> = flow {
        emit(Pair(ProcessingState.Loading, emptyList()))
        try {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val fileName = getFileName(uri)
            val students = when {
                mimeType.contains("spreadsheet") || fileName.endsWith(".xlsx", ignoreCase = true) ->
                    parseXlsx(uri, gradeRanges)
                mimeType.contains("csv") || fileName.endsWith(".csv", ignoreCase = true) ->
                    parseCsv(uri, gradeRanges)
                else -> throw IllegalArgumentException("Unsupported file format: $fileName")
            }
            emit(Pair(ProcessingState.Success("${students.size} students loaded"), students))
        } catch (e: Exception) {
            emit(Pair(ProcessingState.Error(e.message ?: "Unknown error"), emptyList()))
        }
    }.flowOn(Dispatchers.IO)

    fun processGoogleSheetsUrl(
        url: String,
        gradeRanges: List<GradeRange>
    ): Flow<Pair<ProcessingState, List<Student>>> = flow {
        emit(Pair(ProcessingState.Loading, emptyList()))
        try {
            val csvUrl = convertSheetsUrlToCsv(url)
            val students = parseCsvFromUrl(csvUrl, gradeRanges)
            emit(Pair(ProcessingState.Success("${students.size} students loaded from Google Sheets"), students))
        } catch (e: Exception) {
            val errorMessage = "Failed to load Google Sheet: ${e.message}"
            emit(Pair(ProcessingState.Error(errorMessage), emptyList()))
        }
    }.flowOn(Dispatchers.IO)

    // ── XLSX Parser ────────────────────────────────────────────────────────
    private suspend fun parseXlsx(uri: Uri, gradeRanges: List<GradeRange>): List<Student> =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sheet = workbook.getSheetAt(0)
                val rows = sheet.iterator().asSequence().toList()
                if (rows.isEmpty()) return@withContext emptyList()

                val headerRow = rows.first()
                val headers = (0 until headerRow.lastCellNum).map { idx ->
                    headerRow.getCell(idx)?.toString()?.trim()?.lowercase() ?: ""
                }

                val nameCol = findColumn(headers, NAME_ALIASES)
                val caCol = findColumn(headers, CA_ALIASES)
                val examCol = findColumn(headers, EXAM_ALIASES)

                rows.drop(1).mapIndexedNotNull { index, row ->
                    try {
                        val name = row.getCell(nameCol)?.toString()?.trim() ?: return@mapIndexedNotNull null
                        if (name.isBlank()) return@mapIndexedNotNull null
                        val ca = row.getCell(caCol)?.numericCellValue ?: 0.0
                        val exam = row.getCell(examCol)?.numericCellValue ?: 0.0
                        buildStudent(index.toLong(), name, ca, exam, gradeRanges)
                    } catch (e: Exception) {
                        null
                    }
                }
            } ?: emptyList()
        }

    // ── CSV Parser (from Uri) ──────────────────────────────────────────────
    private suspend fun parseCsv(uri: Uri, gradeRanges: List<GradeRange>): List<Student> =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = CSVReader(InputStreamReader(stream))
                val allRows = reader.readAll()
                parseCsvRows(allRows, gradeRanges)
            } ?: emptyList()
        }

    // ── CSV Parser (from URL string) ───────────────────────────────────────
    private suspend fun parseCsvFromUrl(url: String, gradeRanges: List<GradeRange>): List<Student> =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection()
            connection.connect()
            val reader = CSVReader(BufferedReader(InputStreamReader(connection.getInputStream())))
            val allRows = reader.readAll()
            parseCsvRows(allRows, gradeRanges)
        }

    private fun parseCsvRows(allRows: List<Array<String>>, gradeRanges: List<GradeRange>): List<Student> {
        if (allRows.isEmpty()) return emptyList()
        val headers = allRows.first().map { it.trim().lowercase() }
        val nameCol = findColumn(headers, NAME_ALIASES)
        val caCol = findColumn(headers, CA_ALIASES)
        val examCol = findColumn(headers, EXAM_ALIASES)

        return allRows.drop(1).mapIndexedNotNull { index, row ->
            if (row.size <= maxOf(nameCol, caCol, examCol)) return@mapIndexedNotNull null
            val name = row[nameCol].trim()
            if (name.isBlank()) return@mapIndexedNotNull null
            val ca = row[caCol].trim().toDoubleOrNull() ?: 0.0
            val exam = row[examCol].trim().toDoubleOrNull() ?: 0.0
            buildStudent(index.toLong(), name, ca, exam, gradeRanges)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private fun findColumn(headers: List<String>, aliases: Set<String>): Int {
        val idx = headers.indexOfFirst { h -> aliases.any { alias -> h.contains(alias) } }
        return if (idx == -1) 0 else idx
    }

    private fun buildStudent(
        index: Long,
        name: String,
        ca: Double,
        exam: Double,
        gradeRanges: List<GradeRange>
    ): Student {
        val total = (ca + exam).coerceIn(0.0, 100.0)
        val grade = GradeCalculator.calculateGrade(total, gradeRanges)
        return Student(
            id = index,
            name = name,
            caScore = ca.coerceIn(0.0, Student.MAX_CA),
            examScore = exam.coerceIn(0.0, Student.MAX_EXAM),
            finalScore = total,
            grade = grade
        )
    }

    private fun convertSheetsUrlToCsv(url: String): String {
        // Converts "https://docs.google.com/spreadsheets/d/{ID}/edit#gid=0"
        // to       "https://docs.google.com/spreadsheets/d/{ID}/export?format=csv"
        val idRegex = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        val match = idRegex.find(url)
            ?: throw IllegalArgumentException("Invalid Google Sheets URL: $url")
        val sheetId = match.groupValues[1]
        return "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"
    }

    private fun getFileName(uri: Uri): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else ""
        } ?: uri.lastPathSegment ?: ""
    }

    fun isGoogleSheetsUrl(text: String): Boolean =
        text.startsWith("https://docs.google.com/spreadsheets/")
}

private fun Pair<ProcessingState, List<Student>>.emit(
    pair: Pair<ProcessingState, List<Student>>
) { /* placeholder; real emit is via Flow */ }
