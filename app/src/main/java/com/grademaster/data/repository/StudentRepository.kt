package com.grademaster.data.repository

import com.grademaster.data.local.AppPreferences
import com.grademaster.data.local.StudentDao
import com.grademaster.data.local.VaultSessionDao
import com.grademaster.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val vaultSessionDao: VaultSessionDao,
    private val appPreferences: AppPreferences
) {

    // ── Settings Flows ─────────────────────────────────────────────────────
    val isDarkTheme: Flow<Boolean> = appPreferences.isDarkTheme
    val gradeRanges: Flow<List<GradeRange>> = appPreferences.gradeRanges
    val passMark: Flow<Double> = appPreferences.passMark
    val defaultExportFormat: Flow<String> = appPreferences.defaultExportFormat
    val isGoogleDriveEnabled: Flow<Boolean> = appPreferences.isGoogleDriveEnabled

    // ── Students ───────────────────────────────────────────────────────────
    fun getStudentsBySession(sessionId: Long): Flow<List<Student>> =
        studentDao.getStudentsBySession(sessionId)

    fun searchStudentsInSession(sessionId: Long, query: String): Flow<List<Student>> =
        studentDao.searchStudentsInSession(sessionId, query)

    suspend fun insertStudents(students: List<Student>): List<Long> =
        studentDao.insertStudents(students)

    suspend fun insertStudent(student: Student): Long =
        studentDao.insertStudent(student)

    suspend fun updateStudent(student: Student) =
        studentDao.updateStudent(student)

    suspend fun deleteStudent(student: Student) =
        studentDao.deleteStudent(student)

    suspend fun deleteStudentsBySession(sessionId: Long) =
        studentDao.deleteStudentsBySession(sessionId)

    // ── Vault Sessions ─────────────────────────────────────────────────────
    val allVaultSessions: Flow<List<VaultSession>> = vaultSessionDao.getAllSessions()

    fun searchVaultSessions(query: String): Flow<List<VaultSession>> =
        vaultSessionDao.searchSessions(query)

    suspend fun getSessionById(id: Long): VaultSession? =
        vaultSessionDao.getSessionById(id)

    suspend fun createVaultSession(
        students: List<Student>,
        title: String,
        source: String
    ): Long {
        val passMark = appPreferences.passMark.first()
        val passed = students.count { it.finalScore >= passMark }
        val passRate = if (students.isNotEmpty()) passed.toDouble() / students.size * 100 else 0.0
        val avgScore = if (students.isNotEmpty()) students.sumOf { it.finalScore } / students.size else 0.0

        val session = VaultSession(
            title = title,
            source = source,
            studentCount = students.size,
            averageScore = avgScore,
            passRate = passRate
        )
        val sessionId = vaultSessionDao.insertSession(session)

        // Persist students under this session
        val studentsWithSession = students.map { it.copy(sessionId = sessionId) }
        studentDao.insertStudents(studentsWithSession)

        return sessionId
    }

    suspend fun deleteVaultSession(session: VaultSession) {
        studentDao.deleteStudentsBySession(session.id)
        vaultSessionDao.deleteSession(session)
    }

    // ── Grade Calculation (using current settings) ─────────────────────────
    suspend fun assignGrades(students: List<Student>): List<Student> {
        val ranges = appPreferences.gradeRanges.first()
        return students.map { student ->
            val recalculated = student.copy(
                finalScore = student.caScore + student.examScore
            )
            recalculated.withGrade(ranges)
        }
    }

    suspend fun updateStudentScores(
        student: Student,
        newCaScore: Double,
        newExamScore: Double
    ): Student {
        val ranges = appPreferences.gradeRanges.first()
        val updated = student.copy(
            caScore = newCaScore,
            examScore = newExamScore,
            finalScore = newCaScore + newExamScore
        ).withGrade(ranges)
        studentDao.updateStudent(updated)
        return updated
    }

    // ── Settings Mutations ─────────────────────────────────────────────────
    suspend fun setDarkTheme(isDark: Boolean) = appPreferences.setDarkTheme(isDark)
    suspend fun setGradeRanges(ranges: List<GradeRange>) = appPreferences.setGradeRanges(ranges)
    suspend fun setPassMark(mark: Double) = appPreferences.setPassMark(mark)
    suspend fun setDefaultExportFormat(format: String) = appPreferences.setDefaultExportFormat(format)
    suspend fun setGoogleDriveEnabled(enabled: Boolean) = appPreferences.setGoogleDriveEnabled(enabled)

    // ── Functional Utilities ───────────────────────────────────────────────
    // Applies a custom higher-order function from the Student model
    fun <T> transformStudents(
        students: List<Student>,
        transformer: (Student) -> T
    ): List<T> = with(Student) { students.transformWith(transformer) }

    fun filterStudents(
        students: List<Student>,
        predicate: (Student) -> Boolean
    ): List<Student> = students.filter(predicate)
}
