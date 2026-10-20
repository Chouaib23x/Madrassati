package dz.madrassati.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.core.net.SchoolServer
import dz.madrassati.domain.model.Lesson
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.model.Subject
import dz.madrassati.domain.model.Unit
import dz.madrassati.domain.repository.ContentRepository
import dz.madrassati.ui.nav.Routes
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    contentRepository: ContentRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = contentRepository.subjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class UnitsViewModel @Inject constructor(
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val subjectId: Long = savedStateHandle.get<Long>(Routes.ARG_SUBJECT_ID) ?: -1L

    val subject: StateFlow<Subject?> = contentRepository.subject(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val units: StateFlow<List<Unit>> = contentRepository.units(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class LessonsViewModel @Inject constructor(
    contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val unitId: Long = savedStateHandle.get<Long>(Routes.ARG_UNIT_ID) ?: -1L

    val unit: StateFlow<Unit?> = contentRepository.unit(unitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lessons: StateFlow<List<Lesson>> = contentRepository.lessons(unitId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class LessonDetailViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    savedStateHandle: SavedStateHandle,
    private val server: SchoolServer
) : ViewModel() {

    private val lessonId: Long = savedStateHandle.get<Long>(Routes.ARG_LESSON_ID) ?: -1L

    val lesson: StateFlow<Lesson?> = contentRepository.lesson(lessonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activities: StateFlow<List<LessonActivity>> = contentRepository.activities(lessonId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverRunning: StateFlow<Boolean> = server.running

    private val _sent = Channel<kotlin.Unit>(Channel.BUFFERED)
    val sent = _sent.receiveAsFlow()

    fun sendLesson() {
        val lesson = lesson.value ?: return
        server.setLesson(lesson.id, lesson.title)
        viewModelScope.launch { _sent.send(kotlin.Unit) }
    }

    fun deleteActivity(activityId: Long) {
        if (activityId >= 0L) return
        viewModelScope.launch { contentRepository.deleteActivity(activityId) }
    }
}
