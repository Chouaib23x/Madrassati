package dz.madrassati.data.seed

import dz.madrassati.domain.model.ActivityPayload
import kotlinx.serialization.Serializable

@Serializable
data class ContentBundle(
    val version: Int,
    val subjects: List<ContentSubject>
)

@Serializable
data class ContentSubject(
    val id: Long,
    val title: String,
    val colorHex: Long,
    val sortOrder: Int,
    val units: List<ContentUnit>
)

@Serializable
data class ContentUnit(
    val id: Long,
    val title: String,
    val sortOrder: Int,
    val lessons: List<ContentLesson>
)

@Serializable
data class ContentLesson(
    val id: Long,
    val title: String,
    val summary: String,
    val difficulty: String,
    val sortOrder: Int,
    val activities: List<ContentActivity>
)

@Serializable
data class ContentActivity(
    val type: String,
    val difficulty: String,
    val sortOrder: Int,
    val payload: ActivityPayload
)
