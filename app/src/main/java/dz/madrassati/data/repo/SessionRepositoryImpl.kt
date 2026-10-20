package dz.madrassati.data.repo

import dz.madrassati.data.db.SessionDao
import dz.madrassati.data.db.SessionEntity
import dz.madrassati.data.db.SessionResultEntity
import dz.madrassati.domain.model.SessionResult
import dz.madrassati.domain.repository.SessionRepository
import dz.madrassati.domain.scoring.SessionSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val json: Json
) : SessionRepository {

    override suspend fun startSession(studentId: Long, lessonId: Long): Long =
        sessionDao.insertSession(
            SessionEntity(
                studentId = studentId,
                lessonId = lessonId,
                startedAt = System.currentTimeMillis(),
                endedAt = null
            )
        )

    override suspend fun finishSession(
        sessionId: Long,
        studentId: Long,
        lessonId: Long,
        summary: SessionSummary
    ): Long {
        sessionDao.closeSession(sessionId, System.currentTimeMillis())
        return sessionDao.insertResult(
            SessionResultEntity(
                sessionId = sessionId,
                studentId = studentId,
                lessonId = lessonId,
                score = summary.score,
                maxScore = summary.maxScore,
                masteryPercent = summary.masteryPercent,
                totalSeconds = summary.totalSeconds,
                difficultyActivityIdsJson = json.encodeToString(summary.difficultyActivityIds),
                solvedFirstTry = summary.solvedFirstTry,
                totalActivities = summary.totalActivities,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun result(resultId: Long): SessionResult? =
        sessionDao.findResultById(resultId)?.let { entity ->
            SessionResult(
                id = entity.id,
                sessionId = entity.sessionId,
                studentId = entity.studentId,
                lessonId = entity.lessonId,
                score = entity.score,
                maxScore = entity.maxScore,
                masteryPercent = entity.masteryPercent,
                totalSeconds = entity.totalSeconds,
                difficultyActivityIds = runCatching {
                    json.decodeFromString<List<Long>>(entity.difficultyActivityIdsJson)
                }.getOrDefault(emptyList()),
                solvedFirstTry = entity.solvedFirstTry,
                totalActivities = entity.totalActivities
            )
        }
}
