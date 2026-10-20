package dz.madrassati.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Difficulty { EASY, MEDIUM, ADVANCED }

enum class ActivityType { LESSON, QCM, TRUE_FALSE, MATCHING, SORTING, WRITING }

@Serializable
sealed interface ActivityPayload {

    @Serializable
    @SerialName("lesson")
    data class LessonContent(
        val sections: List<LessonSection>
    ) : ActivityPayload

    @Serializable
    @SerialName("mcq")
    data class Mcq(
        val question: String,
        val options: List<String>,
        val correctIndices: Set<Int>,
        val hint: String,
        val explanation: String
    ) : ActivityPayload

    @Serializable
    @SerialName("true_false")
    data class TrueFalse(
        val question: String,
        val correct: Boolean,
        val hint: String,
        val explanation: String
    ) : ActivityPayload

    @Serializable
    @SerialName("matching")
    data class Matching(
        val question: String,
        val pairs: List<Pair<String, String>>,
        val hint: String,
        val explanation: String
    ) : ActivityPayload

    @Serializable
    @SerialName("sorting")
    data class Sorting(
        val question: String,
        val itemsInOrder: List<String>,
        val hint: String,
        val explanation: String
    ) : ActivityPayload

    @Serializable
    @SerialName("writing")
    data class Writing(
        val question: String,
        val acceptedAnswers: List<String>,
        val modelAnswer: String,
        val hint: String,
        val explanation: String
    ) : ActivityPayload
}

@Serializable
data class LessonSection(
    val heading: String,
    val paragraphs: List<String>,
    val example: String? = null
)

data class Subject(
    val id: Long,
    val title: String,
    val colorHex: Long,
    val sortOrder: Int
)

data class Unit(
    val id: Long,
    val subjectId: Long,
    val title: String,
    val sortOrder: Int
)

data class Lesson(
    val id: Long,
    val unitId: Long,
    val title: String,
    val summary: String,
    val difficulty: Difficulty,
    val sortOrder: Int
)

data class LessonActivity(
    val id: Long,
    val lessonId: Long,
    val type: ActivityType,
    val difficulty: Difficulty,
    val sortOrder: Int,
    val payload: ActivityPayload,
    val custom: Boolean = false
) {
    val question: String
        get() = when (val p = payload) {
            is ActivityPayload.LessonContent -> p.sections.firstOrNull()?.heading.orEmpty()
            is ActivityPayload.Mcq -> p.question
            is ActivityPayload.TrueFalse -> p.question
            is ActivityPayload.Matching -> p.question
            is ActivityPayload.Sorting -> p.question
            is ActivityPayload.Writing -> p.question
        }

    val hint: String
        get() = when (val p = payload) {
            is ActivityPayload.LessonContent -> ""
            is ActivityPayload.Mcq -> p.hint
            is ActivityPayload.TrueFalse -> p.hint
            is ActivityPayload.Matching -> p.hint
            is ActivityPayload.Sorting -> p.hint
            is ActivityPayload.Writing -> p.hint
        }

    val explanation: String
        get() = when (val p = payload) {
            is ActivityPayload.LessonContent -> ""
            is ActivityPayload.Mcq -> p.explanation
            is ActivityPayload.TrueFalse -> p.explanation
            is ActivityPayload.Matching -> p.explanation
            is ActivityPayload.Sorting -> p.explanation
            is ActivityPayload.Writing -> p.explanation
        }
}

data class Student(
    val id: Long,
    val name: String,
    val avatar: String,
    val colorHex: Long,
    val sortOrder: Int
)

data class SessionResult(
    val id: Long,
    val sessionId: Long,
    val studentId: Long,
    val lessonId: Long,
    val score: Int,
    val maxScore: Int,
    val masteryPercent: Int,
    val totalSeconds: Int,
    val difficultyActivityIds: List<Long>,
    val solvedFirstTry: Int,
    val totalActivities: Int
)
