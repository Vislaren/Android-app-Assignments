package com.grademaster.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grademaster.data.model.ExportFormat
import com.grademaster.data.model.GradeRange
import com.grademaster.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Settings UI State
// ─────────────────────────────────────────────────────────────────────────────
data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val gradeRanges: List<GradeRange> = GradeRange.DEFAULT_RANGES,
    val passMark: Double = 40.0,
    val defaultExportFormat: String = ExportFormat.CSV.label,
    val isGoogleDriveEnabled: Boolean = false,
    val editingRange: GradeRange? = null,    // currently being edited
    val showResetConfirm: Boolean = false,
    val snackbarMessage: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// SettingsViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.isDarkTheme,
                repository.gradeRanges,
                repository.passMark,
                repository.defaultExportFormat,
                repository.isGoogleDriveEnabled
            ) { dark, ranges, pass, exportFmt, drive ->
                SettingsUiState(
                    isDarkTheme = dark,
                    gradeRanges = ranges,
                    passMark = pass,
                    defaultExportFormat = exportFmt,
                    isGoogleDriveEnabled = drive
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    // ── Theme ──────────────────────────────────────────────────────────────
    fun toggleDarkTheme() {
        viewModelScope.launch {
            repository.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    // ── Grade Ranges ───────────────────────────────────────────────────────
    fun startEditingRange(range: GradeRange) {
        _uiState.update { it.copy(editingRange = range) }
    }

    fun cancelEditingRange() {
        _uiState.update { it.copy(editingRange = null) }
    }

    fun saveRange(original: GradeRange, updated: GradeRange) {
        viewModelScope.launch {
            val currentRanges = _uiState.value.gradeRanges.toMutableList()
            val idx = currentRanges.indexOfFirst { it.grade == original.grade }
            if (idx >= 0) {
                currentRanges[idx] = updated
                // Validate no overlaps before saving
                if (validateRanges(currentRanges)) {
                    repository.setGradeRanges(currentRanges)
                    _uiState.update { it.copy(
                        editingRange = null,
                        snackbarMessage = "Grade ${updated.grade} range updated. All grades recalculated."
                    ) }
                } else {
                    _uiState.update { it.copy(snackbarMessage = "Error: Grade ranges overlap. Please fix.") }
                }
            }
        }
    }

    fun resetRangesToDefault() {
        viewModelScope.launch {
            repository.setGradeRanges(GradeRange.DEFAULT_RANGES)
            _uiState.update { it.copy(
                showResetConfirm = false,
                snackbarMessage = "Grade ranges reset to default."
            ) }
        }
    }

    // ── Pass Mark ──────────────────────────────────────────────────────────
    fun setPassMark(mark: Double) {
        viewModelScope.launch {
            repository.setPassMark(mark.coerceIn(0.0, 100.0))
        }
    }

    // ── Export Format ──────────────────────────────────────────────────────
    fun setDefaultExportFormat(format: String) {
        viewModelScope.launch {
            repository.setDefaultExportFormat(format)
        }
    }

    // ── Google Drive ───────────────────────────────────────────────────────
    fun toggleGoogleDrive() {
        viewModelScope.launch {
            repository.setGoogleDriveEnabled(!_uiState.value.isGoogleDriveEnabled)
        }
    }

    // ── Reset Dialog ───────────────────────────────────────────────────────
    fun showResetConfirm() = _uiState.update { it.copy(showResetConfirm = true) }
    fun hideResetConfirm() = _uiState.update { it.copy(showResetConfirm = false) }

    // ── Snackbar ───────────────────────────────────────────────────────────
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    // ── Validation ─────────────────────────────────────────────────────────
    private fun validateRanges(ranges: List<GradeRange>): Boolean {
        val sorted = ranges.sortedBy { it.minScore }
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].maxScore >= sorted[i + 1].minScore) return false
        }
        return true
    }
}
