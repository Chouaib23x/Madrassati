package dz.madrassati.ui.nav

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.core.net.ClassControlClient
import dz.madrassati.core.net.LessonCommandDto
import dz.madrassati.core.net.MediaDto
import dz.madrassati.core.net.SchoolServer
import dz.madrassati.core.session.ActiveStudent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ClassControlViewModel @Inject constructor(
    controlClient: ClassControlClient,
    activeStudent: ActiveStudent,
    server: SchoolServer
) : ViewModel() {
    val locked: StateFlow<Boolean> = controlClient.locked
    val activeStudentId: StateFlow<Long?> = activeStudent.studentId
    val lessonCommands: Flow<LessonCommandDto> = controlClient.lessonCommands
    val media: StateFlow<MediaDto?> = controlClient.media
    val serverHost: StateFlow<String?> = controlClient.host
    val serverRunning: StateFlow<Boolean> = server.running
}