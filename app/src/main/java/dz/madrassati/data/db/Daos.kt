package dz.madrassati.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY sortOrder")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeById(id: Long): Flow<SubjectEntity?>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY subjectId, sortOrder")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE subjectId = :subjectId ORDER BY sortOrder")
    fun observeForSubject(subjectId: Long): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :id")
    fun observeById(id: Long): Flow<UnitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)

    @Query("SELECT COUNT(*) FROM units")
    suspend fun count(): Int

    @Query("DELETE FROM units")
    suspend fun deleteAll()
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY unitId, sortOrder")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE unitId = :unitId ORDER BY sortOrder")
    fun observeForUnit(unitId: Long): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id")
    fun observeById(id: Long): Flow<LessonEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE lessonId = :lessonId ORDER BY sortOrder")
    fun observeForLesson(lessonId: Long): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY sortOrder")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun findById(id: Long): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun count(): Int

    @Insert
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(students: List<StudentEntity>)

    @Query("UPDATE students SET name = :name, avatar = :avatar, colorHex = :colorHex WHERE id = :id")
    suspend fun update(id: Long, name: String, avatar: String, colorHex: Long)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertResult(result: SessionResultEntity): Long

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, endedAt: Long)

    @Query("SELECT * FROM session_results WHERE id = :id")
    suspend fun findResultById(id: Long): SessionResultEntity?
}
