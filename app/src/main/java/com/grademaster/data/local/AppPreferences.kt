package com.grademaster.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.grademaster.data.model.GradeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property for Context to access DataStore
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "grademaster_settings")

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_GRADE_RANGES = stringPreferencesKey("grade_ranges")
        val KEY_PASS_MARK = floatPreferencesKey("pass_mark")
        val KEY_DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val KEY_GOOGLE_DRIVE_ENABLED = booleanPreferencesKey("google_drive_enabled")
    }

    private val gson = Gson()

    // ── Dark Theme ─────────────────────────────────────────────────────────
    val isDarkTheme: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_DARK_THEME] ?: false }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DARK_THEME] = isDark }
    }

    // ── Grade Ranges ───────────────────────────────────────────────────────
    val gradeRanges: Flow<List<GradeRange>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val json = prefs[KEY_GRADE_RANGES]
            if (json.isNullOrEmpty()) {
                GradeRange.DEFAULT_RANGES
            } else {
                try {
                    val type = object : TypeToken<List<GradeRange>>() {}.type
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    GradeRange.DEFAULT_RANGES
                }
            }
        }

    suspend fun setGradeRanges(ranges: List<GradeRange>) {
        dataStore.edit { prefs ->
            prefs[KEY_GRADE_RANGES] = gson.toJson(ranges)
        }
    }

    // ── Pass Mark ──────────────────────────────────────────────────────────
    val passMark: Flow<Double> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> (prefs[KEY_PASS_MARK] ?: 40f).toDouble() }

    suspend fun setPassMark(mark: Double) {
        dataStore.edit { prefs -> prefs[KEY_PASS_MARK] = mark.toFloat() }
    }

    // ── Default Export Format ──────────────────────────────────────────────
    val defaultExportFormat: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_DEFAULT_EXPORT_FORMAT] ?: "CSV" }

    suspend fun setDefaultExportFormat(format: String) {
        dataStore.edit { prefs -> prefs[KEY_DEFAULT_EXPORT_FORMAT] = format }
    }

    // ── Google Drive ───────────────────────────────────────────────────────
    val isGoogleDriveEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_GOOGLE_DRIVE_ENABLED] ?: false }

    suspend fun setGoogleDriveEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_GOOGLE_DRIVE_ENABLED] = enabled }
    }
}
