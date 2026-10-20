package dz.madrassati.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        UnitEntity::class,
        LessonEntity::class,
        ActivityEntity::class,
        StudentEntity::class,
        SessionEntity::class,
        SessionResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MadrassatiDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun unitDao(): UnitDao
    abstract fun lessonDao(): LessonDao
    abstract fun activityDao(): ActivityDao
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "madrassati.db"
    }
}
