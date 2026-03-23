package com.grademaster.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grademaster.data.model.*
import com.grademaster.data.repository.StudentRepository
import com.grademaster.util.FileProcessor
import com.grademaster.util.GradeCalculator
import com.grademaster.util.StudentTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Home UI State
// ─────────────────────────────────────────────────────────────────────────────
data class HomeUiState(
    val students: List<Student> = emptyList(),
    val filteredStudents: List<Student> = emptyList(),
    val searchQuery: String = "",
    val processingState: ProcessingState = ProcessingState.Idle,
    val sourceLabel: String = "",
    val stats: ClassStatsUi = ClassStatsUi(),
    val isDarkTheme: Boolean = false,
    val gradeRanges: List<GradeRange> = GradeRange.DEFAULT_RANGES,
    val showExportSheet: Boolean = false,
    val exportInProgress: Boolean = false,
    val snackbarMessage: String? = null
)

data class ClassStatsUi(
    val total: Int = 0,
    val passed: Int = 0,
    val failed: Int = 0,
    val average: String = "0.0",
    val passRate: String = "0%"
)

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val fileProcessor: FileProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe theme and grade ranges from settings
        viewModelScope.launch {
            combine(
                repository.isDarkTheme,
                repository.gradeRanges
            ) { dark, ranges -> Pair(dark, ranges) }
                .collect { (dark, ranges) ->
                    val currentStudents = _uiState.value.students
                    // When grade ranges change, recalculate all grades automatically
                    val recalculated = if (currentStudents.isNotEmpty()) {
                        GradeCalculator.recalculateAll(currentStudents, ranges)
                    } else currentStudents
                    _uiState.update { state ->
                        state.copy(
                            isDarkTheme = dark,
                            gradeRanges = ranges,
                            students = recalculated,
                            filteredStudents = applySearch(recalculated, state.searchQuery),
                            stats = computeStats(recalculated)
                        )
                    }
                }
        }
    }

    // ── File Loading ───────────────────────────────────────────────────────
    fun loadFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            val ranges = _uiState.value.gradeRanges
            fileProcessor.processFile(uri, ranges).collect { (state, students) ->
                _uiState.update { current ->
                    current.copy(
                        processingState = state,
                        students = if (students.isNotEmpty()) students else current.students,
                        filteredStudents = applySearch(
                            if (students.isNotEmpty()) students else current.students,
                            current.searchQuery
                        ),
                        sourceLabel = if (students.isNotEmpty()) fileName else current.sourceLabel,
                        stats = computeStats(if (students.isNotEmpty()) students else current.students)
                    )
                }
            }
        }
    }

    fun loadGoogleSheet(url: String) {
        viewModelScope.launch {
            val ranges = _uiState.value.gradeRanges
            fileProcessor.processGoogleSheetsUrl(url, ranges).collect { (state, students) ->
                _uiState.update { current ->
                    current.copy(
                        processingState = state,
                        students = if (students.isNotEmpty()) students else current.students,
                        filteredStudents = applySearch(
                            if (students.isNotEmpty()) students else current.students,
                            current.searchQuery
                        ),
                        sourceLabel = if (students.isNotEmpty()) "Google Sheets" else current.sourceLabel,
                        stats = computeStats(if (students.isNotEmpty()) students else current.students)
                    )
                }
            }
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        val students = _uiState.value.students
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredStudents = applySearch(students, query)
            )
        }
    }

    // ── Student Actions ────────────────────────────────────────────────────
    fun editStudentScores(student: Student, newCa: Double, newExam: Double) {
        viewModelScope.launch {
            val ranges = _uiState.value.gradeRanges
            val updated = student.copy(
                caScore = newCa,
                examScore = newExam,
                finalScore = newCa + newExam
            ).withGrade(ranges)

            val newList = _uiState.value.students.map { s ->
                if (s.id == student.id) updated else s
            }
            _uiState.update { state ->
                state.copy(
                    students = newList,
                    filteredStudents = applySearch(newList, state.searchQuery),
                    stats = computeStats(newList)
                )
            }
        }
    }

    fun deleteStudent(student: Student) {
        val newList = _uiState.value.students.filterNot { it.id == student.id }
        _uiState.update { state ->
            state.copy(
                students = newList,
                filteredStudents = applySearch(newList, state.searchQuery),
                stats = computeStats(newList),
                snackbarMessage = "${student.displayName} removed"
            )
        }
    }

    // ── Save to Vault ──────────────────────────────────────────────────────
    fun saveToVault(title: String) {
        viewModelScope.launch {
            val students = _uiState.value.students
            if (students.isEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "No data to save") }
                return@launch
            }
            _uiState.update { it.copy(exportInProgress = true) }
            try {
                repository.createVaultSession(
                    students = students,
                    title = title.ifBlank { "Session ${System.currentTimeMillis()}" },
                    source = _uiState.value.sourceLabel
                )
                _uiState.update { it.copy(exportInProgress = false, snackbarMessage = "Saved to Vault!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportInProgress = false, snackbarMessage = "Save failed: ${e.message}") }
            }
        }
    }

    // ── Theme Toggle ───────────────────────────────────────────────────────
    fun toggleTheme() {
        viewModelScope.launch {
            repository.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    // ── Export Sheet ───────────────────────────────────────────────────────
    fun showExportSheet() = _uiState.update { it.copy(showExportSheet = true) }
    fun hideExportSheet() = _uiState.update { it.copy(showExportSheet = false) }

    // ── Snackbar ───────────────────────────────────────────────────────────
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun dismissProcessingState() = _uiState.update { it.copy(processingState = ProcessingState.Idle) }

    // ── FUNCTIONAL DEMONSTRATION: custom higher-order function ─────────────
    fun getFormattedSummaries(): List<String> =
        StudentTransformer.applyTransformation(
            students = _uiState.value.students,
            transformer = StudentTransformer.toDisplaySummary
        )

    fun getPassingStudents(passMark: Double = 40.0): List<Student> =
        repository.filterStudents(_uiState.value.students) { it.finalScore >= passMark }

    fun getFailingStudents(passMark: Double = 40.0): List<Student> =
        repository.filterStudents(_uiState.value.students) { it.finalScore < passMark }

    // ── Private Helpers ────────────────────────────────────────────────────
    private fun applySearch(students: List<Student>, query: String): List<Student> =
        if (query.isBlank()) students
        else students.filter { it.name.contains(query, ignoreCase = true) }

    private fun computeStats(students: List<Student>): ClassStatsUi {
        if (students.isEmpty()) return ClassStatsUi()
        val passed = students.count { it.isPassing }
        val avg = students.sumOf { it.finalScore } / students.size
        val passRate = passed.toDouble() / students.size * 100
        return ClassStatsUi(
            total = students.size,
            passed = passed,
            failed = students.size - passed,
            average = "%.1f".format(avg),
            passRate = "%.1f%%".format(passRate)
        )
    }
}
