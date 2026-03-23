package com.grademaster.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grademaster.data.model.*
import com.grademaster.data.repository.StudentRepository
import com.grademaster.util.ExportEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Vault UI State
// ─────────────────────────────────────────────────────────────────────────────
data class VaultUiState(
    val sessions: List<VaultSession> = emptyList(),
    val searchQuery: String = "",
    val filteredSessions: List<VaultSession> = emptyList(),
    val selectedSession: VaultSession? = null,
    val selectedSessionStudents: List<Student> = emptyList(),
    val filteredSessionStudents: List<Student> = emptyList(),
    val studentSearchQuery: String = "",
    val isLoading: Boolean = true,
    val isDarkTheme: Boolean = false,
    val showExportSheet: Boolean = false,
    val snackbarMessage: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// VaultViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val exportEngine: ExportEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allVaultSessions,
                repository.isDarkTheme
            ) { sessions, dark -> Pair(sessions, dark) }
                .collect { (sessions, dark) ->
                    _uiState.update { state ->
                        state.copy(
                            sessions = sessions,
                            filteredSessions = applySearch(sessions, state.searchQuery),
                            isDarkTheme = dark,
                            isLoading = false
                        )
                    }
                }
        }
    }

    // ── Session Selection ──────────────────────────────────────────────────
    fun selectSession(session: VaultSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedSession = session, isLoading = true) }
            repository.getStudentsBySession(session.id)
                .collect { students ->
                    _uiState.update { state ->
                        state.copy(
                            selectedSessionStudents = students,
                            filteredSessionStudents = applyStudentSearch(students, state.studentSearchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun clearSelectedSession() {
        _uiState.update { it.copy(selectedSession = null, selectedSessionStudents = emptyList()) }
    }

    // ── Search ─────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredSessions = applySearch(state.sessions, query)
            )
        }
    }

    fun onStudentSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                studentSearchQuery = query,
                filteredSessionStudents = applyStudentSearch(state.selectedSessionStudents, query)
            )
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────
    fun deleteSession(session: VaultSession) {
        viewModelScope.launch {
            repository.deleteVaultSession(session)
            if (_uiState.value.selectedSession?.id == session.id) {
                _uiState.update { it.copy(selectedSession = null) }
            }
            _uiState.update { it.copy(snackbarMessage = "\"${session.title}\" deleted") }
        }
    }

    fun deleteStudentFromSession(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _uiState.update { it.copy(snackbarMessage = "${student.displayName} removed") }
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────
    fun showExportSheet() = _uiState.update { it.copy(showExportSheet = true) }
    fun hideExportSheet() = _uiState.update { it.copy(showExportSheet = false) }

    fun exportSession(format: ExportFormat, destination: ExportDestination) {
        viewModelScope.launch {
            val session = _uiState.value.selectedSession ?: return@launch
            val students = _uiState.value.selectedSessionStudents
            val result = exportEngine.export(students, session, format, destination)
            result.onSuccess { path ->
                _uiState.update { it.copy(snackbarMessage = "Exported: $path") }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(snackbarMessage = "Export failed: ${e.message}") }
            }
            hideExportSheet()
        }
    }

    // ── Snackbar ───────────────────────────────────────────────────────────
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    // ── Private Helpers ────────────────────────────────────────────────────
    private fun applySearch(sessions: List<VaultSession>, query: String): List<VaultSession> =
        if (query.isBlank()) sessions
        else sessions.filter { it.title.contains(query, ignoreCase = true) }

    private fun applyStudentSearch(students: List<Student>, query: String): List<Student> =
        if (query.isBlank()) students
        else students.filter { it.name.contains(query, ignoreCase = true) }
}
