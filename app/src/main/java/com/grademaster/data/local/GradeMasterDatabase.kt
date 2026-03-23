package com.grademaster.data.local

import androidx.room.*
import com.grademaster.data.model.Student
import com.grademaster.data.model.VaultSession
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Student DAO
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getStudentsBySession(sessionId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE sessionId = :sessionId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchStudentsInSession(sessionId: Long, query: String): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY finalScore DESC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("DELETE FROM students WHERE sessionId = :sessionId")
    suspend fun deleteStudentsBySession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM students WHERE sessionId = :sessionId")
    suspend fun countStudentsInSession(sessionId: Long): Int

    @Query("SELECT AVG(finalScore) FROM students WHERE sessionId = :sessionId")
    suspend fun averageScoreInSession(sessionId: Long): Double?

    @Query("SELECT COUNT(*) FROM students WHERE sessionId = :sessionId AND finalScore >= :passMark")
    suspend fun countPassedInSession(sessionId: Long, passMark: Double = 40.0): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSession DAO
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface VaultSessionDao {

    @Query("SELECT * FROM vault_sessions ORDER BY savedAt DESC")
    fun getAllSessions(): Flow<List<VaultSession>>

    @Query("SELECT * FROM vault_sessions WHERE title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun searchSessions(query: String): Flow<List<VaultSession>>

    @Query("SELECT * FROM vault_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): VaultSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: VaultSession): Long

    @Update
    suspend fun updateSession(session: VaultSession)

    @Delete
    suspend fun deleteSession(session: VaultSession)

    @Query("DELETE FROM vault_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}

// ─────────────────────────────────────────────────────────────────────────────
// Room Database
// ─────────────────────────────────────────────────────────────────────────────
@Database(
    entities = [Student::class, VaultSession::class],
    version = 1,
    exportSchema = false
)
abstract class GradeMasterDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun vaultSessionDao(): VaultSessionDao

    companion object {
        const val DATABASE_NAME = "grademaster_db"
    }
}
