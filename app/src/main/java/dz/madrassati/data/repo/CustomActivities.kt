package dz.madrassati.data.repo

import dz.madrassati.domain.model.ActivityPayload
import kotlinx.serialization.Serializable

@Serializable
data class CustomActivityDto(
    val id: Long,
    val lessonId: Long,
    val type: String,
    val difficulty: String,
    val sortOrder: Int,
    val payload: ActivityPayload
)
