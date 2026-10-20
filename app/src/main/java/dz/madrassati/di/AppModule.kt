package dz.madrassati.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dz.madrassati.data.db.ActivityDao
import dz.madrassati.data.db.LessonDao
import dz.madrassati.data.db.MadrassatiDatabase
import dz.madrassati.data.db.SessionDao
import dz.madrassati.data.db.StudentDao
import dz.madrassati.data.db.SubjectDao
import dz.madrassati.data.db.UnitDao
import dz.madrassati.data.repo.AuthRepositoryImpl
import dz.madrassati.data.repo.ClassRepositoryImpl
import dz.madrassati.data.repo.ContentRepositoryImpl
import dz.madrassati.data.repo.SessionRepositoryImpl
import dz.madrassati.domain.repository.AuthRepository
import dz.madrassati.domain.repository.ClassRepository
import dz.madrassati.domain.repository.ContentRepository
import dz.madrassati.domain.repository.SessionRepository
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideSecurePrefs(@ApplicationContext context: Context): SharedPreferences {
        val fallback = context.getSharedPreferences("madrassati_prefs", Context.MODE_PRIVATE)
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "madrassati_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrDefault(fallback)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MadrassatiDatabase =
        androidx.room.Room.databaseBuilder(
            context,
            MadrassatiDatabase::class.java,
            MadrassatiDatabase.NAME
        ).build()

    @Provides fun provideSubjectDao(db: MadrassatiDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideUnitDao(db: MadrassatiDatabase): UnitDao = db.unitDao()
    @Provides fun provideLessonDao(db: MadrassatiDatabase): LessonDao = db.lessonDao()
    @Provides fun provideActivityDao(db: MadrassatiDatabase): ActivityDao = db.activityDao()
    @Provides fun provideStudentDao(db: MadrassatiDatabase): StudentDao = db.studentDao()
    @Provides fun provideSessionDao(db: MadrassatiDatabase): SessionDao = db.sessionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    abstract fun bindClassRepository(impl: ClassRepositoryImpl): ClassRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
