package dz.madrassati.ui.teacher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.domain.exercise.ExerciseBuilder
import dz.madrassati.domain.exercise.ExerciseDraft
import dz.madrassati.domain.exercise.ExerciseValidation
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.repository.ContentRepository
import dz.madrassati.ui.nav.Routes
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LessonOption(val id: Long, val label: String)

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data class Error(val reason: ExerciseValidation.Reason) : EditorEvent
}

@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialLessonId: Long =
        savedStateHandle.get<Long>(Routes.ARG_LESSON_ID)?.takeIf { it > 0L } ?: 0L

    val lessons: StateFlow<List<LessonOption>> = combine(
        contentRepository.subjects(),
        contentRepository.allUnits(),
        contentRepository.allLessons()
    ) { subjects, units, lessons ->
        val subjectById = subjects.associateBy { it.id }
        val unitById = units.associateBy { it.id }
        lessons.map { lesson ->
            val unit = unitById[lesson.unitId]
            val subject = unit?.let { subjectById[it.subjectId] }
            val prefix = listOfNotNull(subject?.title, unit?.title).joinToString(" · ")
            LessonOption(
                id = lesson.id,
                label = if (prefix.isEmpty()) lesson.title else "$prefix · ${lesson.title}"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun initialLesson(): Long = initialLessonId

    fun save(
        lessonId: Long,
        type: ActivityType,
        difficulty: Difficulty,
        draft: ExerciseDraft
    ) {
        if (lessonId <= 0L) {
            viewModelScope.launch { _events.send(EditorEvent.Error(ExerciseValidation.Reason.NO_LESSON)) }
            return
        }
        when (val result = ExerciseBuilder.build(draft.copy(type = type))) {
            is ExerciseValidation.Valid -> viewModelScope.launch {
                contentRepository.addActivity(lessonId, type, difficulty, result.payload)
                _events.send(EditorEvent.Saved)
            }

            is ExerciseValidation.Invalid -> viewModelScope.launch {
                _events.send(EditorEvent.Error(result.reason))
            }
        }
    }
}
