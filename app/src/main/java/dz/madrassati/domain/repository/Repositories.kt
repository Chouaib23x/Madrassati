package dz.madrassati.domain.repository

import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.Lesson
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.model.SessionResult
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.model.Subject
import dz.madrassati.domain.model.Unit
import dz.madrassati.domain.scoring.SessionSummary
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun subjects(): Flow<List<Subject>>
    fun subject(id: Long): Flow<Subject?>
    fun units(subjectId: Long): Flow<List<Unit>>
    fun allUnits(): Flow<List<Unit>>
    fun unit(id: Long): Flow<Unit?>
    fun lessons(unitId: Long): Flow<List<Lesson>>
    fun allLessons(): Flow<List<Lesson>>
    fun lesson(lessonId: Long): Flow<Lesson?>
    fun activities(lessonId: Long): Flow<List<LessonActivity>>
    suspend fun addActivity(
        lessonId: Long,
        type: ActivityType,
        difficulty: Difficulty,
        payload: ActivityPayload
    ): Long
    suspend fun deleteActivity(activityId: Long)
    suspend fun seed()
}

interface ClassRepository {
    fun students(): Flow<List<Student>>
    suspend fun student(id: Long): Student?
    suspend fun count(): Int
    suspend fun add(name: String, avatar: String, colorHex: Long): Boolean
    suspend fun update(id: Long, name: String, avatar: String, colorHex: Long): Boolean
    suspend fun remove(id: Long)
    suspend fun importFromServer(students: List<Student>)
}

interface SessionRepository {
    suspend fun startSession(studentId: Long, lessonId: Long): Long
    suspend fun finishSession(
        sessionId: Long,
        studentId: Long,
        lessonId: Long,
        summary: SessionSummary
    ): Long
    suspend fun result(resultId: Long): SessionResult?
}

interface AuthRepository {
    suspend fun hasAccount(): Boolean
    suspend fun createAccount(username: String, password: String): Boolean
    suspend fun login(username: String, password: String): Boolean
    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean
    suspend fun resetPassword(newPassword: String)
    fun rememberedUsername(): String
    suspend fun setRememberedUsername(username: String?)
    fun hasPin(): Boolean
    fun ensureDefaultPin()
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
}
