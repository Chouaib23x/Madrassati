package dz.madrassati.ui.student

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.core.net.ClassControlClient
import dz.madrassati.domain.correction.CorrectionStep
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.repository.ContentRepository
import dz.madrassati.domain.repository.SessionRepository
import dz.madrassati.domain.runner.ActivityOutcome
import dz.madrassati.domain.runner.LessonRunner
import dz.madrassati.domain.runner.Submission
import dz.madrassati.domain.scoring.SessionScorer
import dz.madrassati.ui.common.vibrateLight
import dz.madrassati.ui.nav.Routes
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RunnerFeedback { CORRECT, WRONG }

data class RunnerUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val lessonTitle: String = "",
    val position: Int = 0,
    val total: Int = 0,
    val activity: LessonActivity? = null,
    val selectedOptions: Set<Int> = emptySet(),
    val trueFalse: Boolean? = null,
    val writing: String = "",
    val ordered: List<String> = emptyList(),
    val matched: Map<String, String> = emptyMap(),
    val activeLeft: String? = null,
    val feedback: RunnerFeedback? = null,
    val wrongStep: CorrectionStep? = null,
    val hintVisible: Boolean = false,
    val solutionVisible: Boolean = false,
    val canNext: Boolean = false
)

@HiltViewModel
class LessonRunnerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val contentRepository: ContentRepository,
    private val sessionRepository: SessionRepository,
    private val controlClient: ClassControlClient
) : ViewModel() {

    private val lessonId: Long = savedStateHandle.get<Long>(Routes.ARG_LESSON_ID) ?: -1L
    val studentId: Long = savedStateHandle.get<Long>(Routes.ARG_STUDENT_ID) ?: -1L

    private var runner: LessonRunner? = null
    private var sessionId: Long = -1L
    private var lessonTitle: String = ""
    private var activityStartMs: Long = 0L

    private val _state = MutableStateFlow(RunnerUiState())
    val state: StateFlow<RunnerUiState> = _state.asStateFlow()

    private val _resultId = Channel<Long>(Channel.BUFFERED)
    val resultId = _resultId.receiveAsFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val lesson = contentRepository.lesson(lessonId).first()
                val local = contentRepository.activities(lessonId).first()
                val remote = controlClient.fetchExercises(lessonId)
                    .filter { candidate -> local.none { it.id == candidate.id } }
                val activities = local + remote
                if (activities.isEmpty()) {
                    _state.update { it.copy(loading = false, failed = true) }
                    return@runCatching
                }
                runner = LessonRunner(activities)
                sessionId = sessionRepository.startSession(studentId, lessonId)
                activityStartMs = System.currentTimeMillis()
                lessonTitle = lesson?.title ?: ""
                _state.update { it.copy(lessonTitle = lessonTitle) }
                resetResponse()
                _state.update { it.copy(loading = false) }
            }.onFailure {
                _state.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun toggleOption(index: Int) {
        if (_state.value.canNext) return
        _state.update { s ->
            val next = if (index in s.selectedOptions) {
                s.selectedOptions - index
            } else {
                s.selectedOptions + index
            }
            s.copy(selectedOptions = next)
        }
    }

    fun selectTrueFalse(value: Boolean) {
        if (_state.value.canNext) return
        _state.update { it.copy(trueFalse = value) }
    }

    fun onWritingChange(text: String) {
        if (_state.value.canNext) return
        _state.update { it.copy(writing = text) }
    }

    fun moveOrdered(fromIndex: Int, toIndex: Int) {
        if (_state.value.canNext) return
        _state.update { s ->
            val list = s.ordered.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                list.add(toIndex, list.removeAt(fromIndex))
            }
            s.copy(ordered = list)
        }
    }

    fun selectMatchLeft(item: String) {
        if (_state.value.canNext) return
        _state.update { s -> s.copy(activeLeft = if (s.activeLeft == item) null else item) }
    }

    fun selectMatchRight(item: String) {
        if (_state.value.canNext) return
        _state.update { s ->
            val existingLeft = s.matched.entries.firstOrNull { it.value == item }?.key
            if (existingLeft != null) {
                s.copy(matched = s.matched - existingLeft)
            } else {
                val left = s.activeLeft ?: return@update s
                val newMatched = s.matched.toMutableMap()
                newMatched[left] = item
                s.copy(matched = newMatched, activeLeft = null)
            }
        }
    }

    fun validate() {
        val r = runner ?: return
        val activity = r.current ?: return
        val correct = isCurrentCorrect(activity)
        when (val submission = r.submit(correct)) {
            is Submission.Correct -> _state.update {
                it.copy(
                    feedback = RunnerFeedback.CORRECT,
                    wrongStep = null,
                    hintVisible = r.hintShown,
                    solutionVisible = r.solutionShown
                )
            }

            is Submission.Incorrect -> {
                context.vibrateLight()
                _state.update {
                    it.copy(
                        feedback = RunnerFeedback.WRONG,
                        wrongStep = submission.step,
                        hintVisible = r.hintShown,
                        solutionVisible = r.solutionShown
                    )
                }
            }

            Submission.Ignored -> Unit
        }
        _state.update { it.copy(canNext = r.canAdvance) }
    }

    fun next() {
        val r = runner ?: return
        if (!r.canAdvance) return
        val seconds = ((System.currentTimeMillis() - activityStartMs) / 1000).toInt()
        if (r.advance(seconds)) {
            activityStartMs = System.currentTimeMillis()
            resetResponse()
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        val r = runner ?: return
        reportProgress(r.total, r.total)
        viewModelScope.launch {
            val summary = SessionScorer.summarize(r.collectedOutcomes)
            val resultId = sessionRepository.finishSession(sessionId, studentId, lessonId, summary)
            _resultId.send(resultId)
        }
    }

    private fun resetResponse() {
        val r = runner ?: return
        val activity = r.current
        val payload = activity?.payload
        _state.update {
            it.copy(
                activity = activity,
                position = r.currentIndex,
                total = r.total,
                selectedOptions = emptySet(),
                trueFalse = null,
                writing = "",
                ordered = if (payload is ActivityPayload.Sorting) {
                    payload.itemsInOrder.shuffled()
                } else {
                    emptyList()
                },
                matched = emptyMap(),
                activeLeft = null,
                feedback = null,
                wrongStep = null,
                hintVisible = r.hintShown,
                solutionVisible = r.solutionShown,
                canNext = r.canAdvance
            )
        }
        reportProgress(r.currentIndex + 1, r.total)
    }

    private fun reportProgress(position: Int, total: Int) {
        if (lessonTitle.isBlank()) return
        controlClient.reportProgress(lessonId, lessonTitle, position, total)
    }

    private fun isCurrentCorrect(activity: LessonActivity): Boolean {
        val current = _state.value
        return when (val payload = activity.payload) {
            is ActivityPayload.LessonContent -> false
            is ActivityPayload.Mcq -> current.selectedOptions == payload.correctIndices
            is ActivityPayload.TrueFalse -> current.trueFalse == payload.correct
            is ActivityPayload.Writing ->
                payload.acceptedAnswers.any { normalize(it) == normalize(current.writing) }

            is ActivityPayload.Sorting -> current.ordered == payload.itemsInOrder
            is ActivityPayload.Matching ->
                payload.pairs.all { (left, right) -> current.matched[left] == right }
        }
    }

    private fun normalize(text: String): String =
        text.trim()
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
            .replace('إ', 'ا')
            .replace('أ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .lowercase()
}
