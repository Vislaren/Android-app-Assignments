package logic

import model.Student
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import java.io.File

/**
 * Parses an .xlsx or .xls file into a list of raw Students.
 * Expects columns: Name | ID | CA Score | Exam Score
 * Returns Pair<students, errors>
 */
fun parseExcelFile(file: File): Pair<List<Student>, List<String>> {
    val errors = mutableListOf<String>()
    val students = mutableListOf<Student>()

    try {
        val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)

        val startRow = if (
            sheet.getRow(0)?.getCell(0)?.stringCellValue
                ?.lowercase()?.contains("name") == true
        ) 1 else 0

        for (rowIdx in startRow..sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx) ?: continue
            try {
                fun cellStr(col: Int) = row.getCell(col)?.let {
                    when (it.cellType) {
                        CellType.NUMERIC -> it.numericCellValue.toString()
                        else -> it.stringCellValue.trim()
                    }
                } ?: ""

                fun cellNum(col: Int): Double = row.getCell(col)?.let {
                    when (it.cellType) {
                        CellType.NUMERIC -> it.numericCellValue
                        CellType.STRING -> it.stringCellValue.trim().toDouble()
                        else -> throw NumberFormatException("Empty cell")
                    }
                } ?: throw NumberFormatException("Missing cell at column $col")

                students.add(
                    Student(
                        name = cellStr(0),
                        id = cellStr(1),
                        caScore = cellNum(2),
                        examScore = cellNum(3)
                    )
                )
            } catch (e: Exception) {
                errors.add("Row ${rowIdx + 1}: ${e.message}")
            }
        }
        workbook.close()
    } catch (e: Exception) {
        errors.add("Failed to open Excel file: ${e.message}")
    }

    return Pair(students, errors)
}
