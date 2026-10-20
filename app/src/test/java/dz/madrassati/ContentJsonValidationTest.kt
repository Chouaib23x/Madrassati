package dz.madrassati

import dz.madrassati.data.seed.ContentBundle
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentJsonValidationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val bundle: ContentBundle by lazy {
        val candidates = listOf(
            "app/src/main/assets/content/content.json",
            "src/main/assets/content/content.json"
        )
        val file = candidates.map(::File).firstOrNull(File::exists)
            ?: error("content.json introuvable: $candidates")
        json.decodeFromString(ContentBundle.serializer(), file.readText(Charsets.UTF_8))
    }

    private val allLessons by lazy {
        bundle.subjects.flatMap { it.units }.flatMap { it.lessons }
    }

    private val allActivities by lazy {
        allLessons.flatMap { it.activities }
    }

    @Test
    fun bundleHasExpectedVolumes() {
        assertTrue("version", bundle.version >= 2)
        assertEquals("3 matières", 3, bundle.subjects.size)
        assertTrue("unités", bundle.subjects.flatMap { it.units }.size >= 10)
        assertTrue("leçons", allLessons.size >= 24)
        assertTrue("activités", allActivities.size >= 110)
        for (subject in bundle.subjects) {
            assertTrue(
                "6 leçons minimum pour ${subject.title}",
                subject.units.flatMap { it.lessons }.size >= 6
            )
        }
    }

    @Test
    fun idsAreUniqueAndWellFormed() {
        val subjectIds = bundle.subjects.map { it.id }
        val unitIds = bundle.subjects.flatMap { it.units }.map { it.id }
        val lessonIds = allLessons.map { it.id }
        assertEquals(subjectIds, subjectIds.distinct())
        assertEquals(unitIds, unitIds.distinct())
        assertEquals(lessonIds, lessonIds.distinct())
        assertEquals(setOf(1L, 2L, 3L), subjectIds.toSet())
        assertTrue(unitIds.all { it in 101..105L || it in 201..205L || it in 301..305L })
        assertTrue(lessonIds.all { it in 1001..1015L || it in 2001..2015L || it in 3001..3016L })
    }

    @Test
    fun metadataIsValid() {
        for (subject in bundle.subjects) {
            assertTrue("titre matière", subject.title.isNotBlank())
            assertTrue("couleur matière", subject.colorHex != 0L)
            val units = subject.units
            assertEquals("sortOrder unités", (1..units.size).toList(), units.map { it.sortOrder })
            for (unit in units) {
                assertTrue("titre unité", unit.title.isNotBlank())
                assertEquals(
                    "sortOrder leçons",
                    (1..unit.lessons.size).toList(),
                    unit.lessons.map { it.sortOrder }
                )
                for (lesson in unit.lessons) {
                    assertTrue("titre leçon", lesson.title.isNotBlank())
                    assertTrue("résumé leçon", lesson.summary.isNotBlank())
                    assertTrue(
                        "difficulté leçon ${lesson.title}",
                        Difficulty.entries.any { it.name == lesson.difficulty }
                    )
                    assertEquals(
                        "sortOrder activités",
                        (1..lesson.activities.size).toList(),
                        lesson.activities.map { it.sortOrder }
                    )
                    assertTrue(
                        "≥4 activités pour ${lesson.title} (${lesson.activities.size})",
                        lesson.activities.size >= 4
                    )
                }
            }
        }
    }

    @Test
    fun activityTypesAndDifficultiesAreCovered() {
        val types = allActivities.map { it.type }.toSet()
        for (type in ActivityType.entries.map { it.name }) {
            assertTrue("type $type présent", type in types)
        }
        for (lesson in allLessons) {
            val lessonTypes = lesson.activities.map { it.type }.toSet()
            assertTrue(
                "leçon ${lesson.id} sans contenu de cours",
                "LESSON" in lessonTypes
            )
        }
        val difficulties = allActivities.map { it.difficulty }.toSet()
        for (difficulty in Difficulty.entries.map { it.name }) {
            assertTrue("difficulté activité $difficulty présente", difficulty in difficulties)
        }
    }

    @Test
    fun everyActivityIsValid() {
        for (lesson in allLessons) {
            for (activity in lesson.activities) {
                val ctx = "lesson ${lesson.id} activity #${activity.sortOrder}"
                assertTrue("$ctx: type inconnu", activity.type in ActivityType.entries.map { it.name })
                assertTrue(
                    "$ctx: difficulté inconnue",
                    activity.difficulty in Difficulty.entries.map { it.name }
                )
                val payload = activity.payload
                when (payload) {
                    is ActivityPayload.LessonContent -> {
                        assertEquals("$ctx: LESSON", "LESSON", activity.type)
                        assertTrue("$ctx: ≥1 section", payload.sections.isNotEmpty())
                        payload.sections.forEach { section ->
                            assertTrue("$ctx: titre de section vide", section.heading.isNotBlank())
                            section.paragraphs.forEach { assertTrue("$ctx: paragraphe vide", it.isNotBlank()) }
                        }
                    }
                    is ActivityPayload.Mcq -> {
                        assertEquals("$ctx: QCM", "QCM", activity.type)
                        assertQuestionParts(payload.question, payload.hint, payload.explanation, ctx)
                        assertTrue("$ctx: ≥2 options", payload.options.size >= 2)
                        assertEquals("$ctx: options uniques", payload.options, payload.options.distinct())
                        payload.options.forEach { assertTrue("$ctx: option vide", it.isNotBlank()) }
                        assertTrue("$ctx: ≥1 bonne réponse", payload.correctIndices.isNotEmpty())
                        payload.correctIndices.forEach {
                            assertTrue("$ctx: indice $it hors limites", it in payload.options.indices)
                        }
                    }
                    is ActivityPayload.TrueFalse -> {
                        assertEquals("$ctx: TRUE_FALSE", "TRUE_FALSE", activity.type)
                        assertQuestionParts(payload.question, payload.hint, payload.explanation, ctx)
                    }
                    is ActivityPayload.Matching -> {
                        assertEquals("$ctx: MATCHING", "MATCHING", activity.type)
                        assertQuestionParts(payload.question, payload.hint, payload.explanation, ctx)
                        assertTrue("$ctx: ≥2 paires", payload.pairs.size >= 2)
                        payload.pairs.forEach { (left, right) ->
                            assertTrue("$ctx: paire gauche vide", left.isNotBlank())
                            assertTrue("$ctx: paire droite vide", right.isNotBlank())
                        }
                    }
                    is ActivityPayload.Sorting -> {
                        assertEquals("$ctx: SORTING", "SORTING", activity.type)
                        assertQuestionParts(payload.question, payload.hint, payload.explanation, ctx)
                        assertTrue("$ctx: ≥2 éléments", payload.itemsInOrder.size >= 2)
                        payload.itemsInOrder.forEach { assertTrue("$ctx: élément vide", it.isNotBlank()) }
                        assertEquals(
                            "$ctx: éléments uniques",
                            payload.itemsInOrder,
                            payload.itemsInOrder.distinct()
                        )
                    }
                    is ActivityPayload.Writing -> {
                        assertEquals("$ctx: WRITING", "WRITING", activity.type)
                        assertQuestionParts(payload.question, payload.hint, payload.explanation, ctx)
                        assertTrue("$ctx: ≥1 réponse acceptée", payload.acceptedAnswers.isNotEmpty())
                        payload.acceptedAnswers.forEach { assertTrue("$ctx: réponse vide", it.isNotBlank()) }
                        assertTrue("$ctx: modelAnswer vide", payload.modelAnswer.isNotBlank())
                    }
                }
            }
        }
    }

    private fun assertQuestionParts(question: String, hint: String, explanation: String, ctx: String) {
        assertTrue("$ctx: question vide", question.isNotBlank())
        assertTrue("$ctx: hint vide", hint.isNotBlank())
        assertTrue("$ctx: explanation vide", explanation.isNotBlank())
    }
}
