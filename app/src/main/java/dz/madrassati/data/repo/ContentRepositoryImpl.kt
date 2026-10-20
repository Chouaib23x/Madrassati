package dz.madrassati.data.repo

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.data.db.ActivityDao
import dz.madrassati.data.db.ActivityEntity
import dz.madrassati.data.db.LessonDao
import dz.madrassati.data.db.LessonEntity
import dz.madrassati.data.db.SubjectDao
import dz.madrassati.data.db.SubjectEntity
import dz.madrassati.data.db.UnitDao
import dz.madrassati.data.db.UnitEntity
import dz.madrassati.data.seed.ContentBundle
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.Lesson
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.model.Subject
import dz.madrassati.domain.model.Unit
import dz.madrassati.domain.repository.ContentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class ContentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectDao: SubjectDao,
    private val unitDao: UnitDao,
    private val lessonDao: LessonDao,
    private val activityDao: ActivityDao,
    private val prefs: SharedPreferences,
    private val json: Json
) : ContentRepository {

    private val customActivities = MutableStateFlow(loadCustomActivities())

    override fun subjects(): Flow<List<Subject>> =
        subjectDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun subject(id: Long): Flow<Subject?> =
        subjectDao.observeById(id).map { it?.toDomain() }

    override fun units(subjectId: Long): Flow<List<Unit>> =
        unitDao.observeForSubject(subjectId).map { list -> list.map { it.toDomain() } }

    override fun allUnits(): Flow<List<Unit>> =
        unitDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun unit(id: Long): Flow<Unit?> =
        unitDao.observeById(id).map { it?.toDomain() }

    override fun lessons(unitId: Long): Flow<List<Lesson>> =
        lessonDao.observeForUnit(unitId).map { list -> list.map { it.toDomain() } }

    override fun allLessons(): Flow<List<Lesson>> =
        lessonDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun lesson(lessonId: Long): Flow<Lesson?> =
        lessonDao.observeById(lessonId).map { it?.toDomain() }

    override fun activities(lessonId: Long): Flow<List<LessonActivity>> =
        combine(
            activityDao.observeForLesson(lessonId),
            customActivities
        ) { seeded, custom ->
            seeded.map { it.toDomain() } +
                custom.filter { it.lessonId == lessonId }
                    .sortedBy { it.sortOrder }
                    .map { it.toDomain() }
        }

    override suspend fun addActivity(
        lessonId: Long,
        type: ActivityType,
        difficulty: Difficulty,
        payload: ActivityPayload
    ): Long {
        val current = customActivities.value
        val id = (current.minOfOrNull { it.id } ?: 0L) - 1L
        val sortOrder = (current.filter { it.lessonId == lessonId }
            .maxOfOrNull { it.sortOrder } ?: 0) + 1
        val dto = CustomActivityDto(
            id = id,
            lessonId = lessonId,
            type = type.name,
            difficulty = difficulty.name,
            sortOrder = sortOrder,
            payload = payload
        )
        persistCustom(current + dto)
        return id
    }

    override suspend fun deleteActivity(activityId: Long) {
        if (activityId >= 0L) return
        persistCustom(customActivities.value.filterNot { it.id == activityId })
    }

    override suspend fun seed() {
        val bundle = loadBundle()
        if (prefs.getInt(KEY_CONTENT_VERSION, 0) == bundle.version && subjectDao.count() > 0) return
        activityDao.deleteAll()
        lessonDao.deleteAll()
        unitDao.deleteAll()
        subjectDao.deleteAll()
        subjectDao.insertAll(
            bundle.subjects.map { SubjectEntity(it.id, it.title, it.colorHex, it.sortOrder) }
        )
        unitDao.insertAll(
            bundle.subjects.flatMap { subject ->
                subject.units.map { unit ->
                    UnitEntity(unit.id, subject.id, unit.title, unit.sortOrder)
                }
            }
        )
        lessonDao.insertAll(
            bundle.subjects.flatMap { subject ->
                subject.units.flatMap { unit ->
                    unit.lessons.map { lesson ->
                        LessonEntity(
                            id = lesson.id,
                            unitId = unit.id,
                            title = lesson.title,
                            summary = lesson.summary,
                            difficulty = lesson.difficulty,
                            sortOrder = lesson.sortOrder
                        )
                    }
                }
            }
        )
        activityDao.insertAll(
            bundle.subjects.flatMap { subject ->
                subject.units.flatMap { unit ->
                    unit.lessons.flatMap { lesson ->
                        lesson.activities.map { activity ->
                            ActivityEntity(
                                id = 0,
                                lessonId = lesson.id,
                                type = activity.type,
                                difficulty = activity.difficulty,
                                sortOrder = activity.sortOrder,
                                payloadJson = json.encodeToString(
                                    ActivityPayload.serializer(),
                                    activity.payload
                                )
                            )
                        }
                    }
                }
            }
        )
        prefs.edit().putInt(KEY_CONTENT_VERSION, bundle.version).apply()
    }

    private fun loadBundle(): ContentBundle {
        val text = context.assets.open(CONTENT_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return json.decodeFromString(ContentBundle.serializer(), text)
    }

    private fun SubjectEntity.toDomain() = Subject(id, title, colorHex, sortOrder)

    private fun UnitEntity.toDomain() = Unit(id, subjectId, title, sortOrder)

    private fun LessonEntity.toDomain() =
        Lesson(id, unitId, title, summary, difficultyOf(difficulty), sortOrder)

    private fun ActivityEntity.toDomain() = LessonActivity(
        id = id,
        lessonId = lessonId,
        type = ActivityType.entries.firstOrNull { it.name == type } ?: ActivityType.QCM,
        difficulty = difficultyOf(difficulty),
        sortOrder = sortOrder,
        payload = parsePayload(payloadJson)
    )

    private fun parsePayload(raw: String): ActivityPayload =
        runCatching { json.decodeFromString(ActivityPayload.serializer(), raw) }
            .getOrElse {
                ActivityPayload.Mcq(
                    question = "",
                    options = emptyList(),
                    correctIndices = emptySet(),
                    hint = "",
                    explanation = ""
                )
            }

    private fun difficultyOf(name: String): Difficulty =
        Difficulty.entries.firstOrNull { it.name == name } ?: Difficulty.EASY

    private fun CustomActivityDto.toDomain() = LessonActivity(
        id = id,
        lessonId = lessonId,
        type = ActivityType.entries.firstOrNull { it.name == type } ?: ActivityType.QCM,
        difficulty = difficultyOf(difficulty),
        sortOrder = sortOrder,
        payload = payload,
        custom = true
    )

    private fun loadCustomActivities(): List<CustomActivityDto> {
        val raw = prefs.getString(KEY_CUSTOM_ACTIVITIES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CustomActivityDto.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun persistCustom(list: List<CustomActivityDto>) {
        customActivities.value = list
        val raw = json.encodeToString(ListSerializer(CustomActivityDto.serializer()), list)
        prefs.edit().putString(KEY_CUSTOM_ACTIVITIES, raw).apply()
    }

    companion object {
        private const val CONTENT_ASSET = "content/content.json"
        private const val KEY_CONTENT_VERSION = "content_version"
        private const val KEY_CUSTOM_ACTIVITIES = "custom_activities"
    }
}
