package com.grademaster.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.grademaster.data.model.ExportDestination
import com.grademaster.data.model.ExportFormat
import com.grademaster.data.model.Student
import com.grademaster.data.model.VaultSession
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val AUTHORITY = "com.grademaster.fileprovider"
        private val CSV_HEADER = arrayOf("ID", "Name", "CA Score", "Exam Score", "Final Score", "Grade")
    }

    // ── Main Export Dispatcher ─────────────────────────────────────────────
    suspend fun export(
        students: List<Student>,
        session: VaultSession?,
        format: ExportFormat,
        destination: ExportDestination
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = when (format) {
                ExportFormat.CSV -> exportCsv(students, session)
                ExportFormat.EXCEL -> exportExcel(students, session)
                ExportFormat.PDF -> exportPdf(students, session)
            }
            when (destination) {
                ExportDestination.INTERNAL_STORAGE -> Result.success("Saved to ${file.absolutePath}")
                ExportDestination.SHARE -> {
                    shareFile(file, format.mimeType)
                    Result.success("Share intent launched")
                }
                ExportDestination.GOOGLE_DRIVE -> {
                    // Google Drive upload requires OAuth — trigger auth flow from UI
                    Result.success(file.absolutePath) // Return path for Drive upload
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── CSV Export ─────────────────────────────────────────────────────────
    private fun exportCsv(students: List<Student>, session: VaultSession?): File {
        val file = createOutputFile(session?.title ?: "export", "csv")
        PrintWriter(file).use { writer ->
            writer.println(CSV_HEADER.joinToString(","))
            students.forEach { s ->
                writer.println(
                    "${s.id},\"${s.name}\",${s.caScore},${s.examScore},${s.finalScore},${s.grade}"
                )
            }
            // Summary footer
            if (students.isNotEmpty()) {
                writer.println()
                writer.println("\"Total Students\",${students.size}")
                writer.println("\"Average Score\",%.2f".format(students.sumOf { it.finalScore } / students.size))
                writer.println("\"Pass Rate\",${session?.passRate?.let { "%.1f%%".format(it) } ?: "N/A"}")
            }
        }
        return file
    }

    // ── Excel Export ───────────────────────────────────────────────────────
    private fun exportExcel(students: List<Student>, session: VaultSession?): File {
        val file = createOutputFile(session?.title ?: "export", "xlsx")
        val workbook = XSSFWorkbook()

        // Style helpers
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().also {
                it.bold = true
                it.color = org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.WHITE.index
            }
            setFont(font)
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.DARK_GREEN.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }

        // Students sheet
        val sheet = workbook.createSheet(session?.title ?: "Students")
        val headerRow = sheet.createRow(0)
        CSV_HEADER.forEachIndexed { i, title ->
            headerRow.createCell(i).also {
                it.setCellValue(title)
                it.cellStyle = headerStyle
            }
        }
        students.forEachIndexed { rowIdx, s ->
            val row = sheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(s.id.toDouble())
            row.createCell(1).setCellValue(s.name)
            row.createCell(2).setCellValue(s.caScore)
            row.createCell(3).setCellValue(s.examScore)
            row.createCell(4).setCellValue(s.finalScore)
            row.createCell(5).setCellValue(s.grade)
        }
        CSV_HEADER.indices.forEach { sheet.autoSizeColumn(it) }

        // Summary sheet
        val summarySheet = workbook.createSheet("Summary")
        val stats = listOf(
            "Report Generated" to SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()),
            "Source" to (session?.source ?: "N/A"),
            "Total Students" to students.size.toString(),
            "Average Score" to if (students.isNotEmpty())
                "%.2f".format(students.sumOf { it.finalScore } / students.size) else "0",
            "Pass Rate" to (session?.passRate?.let { "%.1f%%" }?.format(session.passRate) ?: "N/A"),
            "Grade A" to students.count { it.grade == "A" }.toString(),
            "Grade B" to students.count { it.grade == "B" }.toString(),
            "Grade C" to students.count { it.grade == "C" }.toString(),
            "Grade D" to students.count { it.grade == "D" }.toString(),
            "Grade F" to students.count { it.grade == "F" }.toString()
        )
        stats.forEachIndexed { i, (label, value) ->
            val row = summarySheet.createRow(i)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(value)
        }

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    // ── PDF Export ─────────────────────────────────────────────────────────
    private fun exportPdf(students: List<Student>, session: VaultSession?): File {
        val file = createOutputFile(session?.title ?: "export", "pdf")
        val writer = PdfWriter(file)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)

        // Title
        document.add(
            Paragraph("GradeMaster Report")
                .setBold()
                .setFontSize(20f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f)
        )
        session?.let { s ->
            document.add(
                Paragraph("${s.title}  |  ${s.formattedDate}")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(16f)
            )
        }

        // Stats row
        val statsTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
            .useAllAvailableWidth()
            .setMarginBottom(16f)

        listOf(
            "Total Students" to students.size.toString(),
            "Average" to if (students.isNotEmpty())
                "%.1f".format(students.sumOf { it.finalScore } / students.size) else "0",
            "Highest" to (students.maxByOrNull { it.finalScore }?.finalScore?.let { "%.1f".format(it) } ?: "N/A"),
            "Pass Rate" to (session?.passRate?.let { "%.1f%%".format(it) } ?: "N/A")
        ).forEach { (label, value) ->
            statsTable.addCell(
                Cell().add(Paragraph(label).setFontSize(8f).setFontColor(ColorConstants.GRAY))
                .add(Paragraph(value).setBold().setFontSize(14f))
                .setPadding(8f)
                .setBorder(com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1f)) // Set border here
                .setBorderRadius(com.itextpdf.layout.properties.BorderRadius(4f)) // Set radius here
            )
        }
        document.add(statsTable)

        // Student table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(5f, 30f, 12f, 12f, 12f, 10f)))
            .useAllAvailableWidth()

        // Header
        listOf("#", "Name", "CA", "Exam", "Total", "Grade").forEach { label ->
            table.addHeaderCell(
                Cell().add(Paragraph(label).setBold().setFontSize(9f).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(ColorConstants.DARK_GRAY)
                    .setPadding(6f)
            )
        }

        // Rows
        students.forEachIndexed { i, s ->
            val bgColor = if (i % 2 == 0) ColorConstants.WHITE else ColorConstants.LIGHT_GRAY
            val gradeColor = when (s.grade) {
                "A" -> ColorConstants.GREEN
                "B" -> ColorConstants.BLUE
                "F" -> ColorConstants.RED
                else -> ColorConstants.BLACK
            }
            listOf(
                (i + 1).toString(),
                s.name,
                s.caScore.toString(),
                s.examScore.toString(),
                s.finalScore.toString(),
                s.grade
            ).forEachIndexed { colIdx, value ->
                val cell = Cell().add(
                    Paragraph(value).setFontSize(9f)
                        .also { if (colIdx == 5) it.setFontColor(gradeColor).setBold() }
                )
                    .setBackgroundColor(bgColor)
                    .setPadding(5f)
                table.addCell(cell)
            }
        }
        document.add(table)
        document.close()
        return file
    }

    // ── Share Intent ───────────────────────────────────────────────────────
    private fun shareFile(file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share GradeMaster Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ── File Helpers ───────────────────────────────────────────────────────
    private fun createOutputFile(baseName: String, extension: String): File {
        val sanitized = baseName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(context.getExternalFilesDir(null), "GradeMaster").also { it.mkdirs() }
        return File(dir, "${sanitized}_$timestamp.$extension")
    }

    fun getExportsDirectory(): File =
        File(context.getExternalFilesDir(null), "GradeMaster").also { it.mkdirs() }
}
