package dz.madrassati.domain.runner

import dz.madrassati.domain.correction.CorrectionPolicy
import dz.madrassati.domain.correction.CorrectionState
import dz.madrassati.domain.correction.CorrectionStep
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.LessonActivity

sealed interface Submission {
    data class Correct(val firstTry: Boolean) : Submission
    data class Incorrect(val step: CorrectionStep) : Submission
    data object Ignored : Submission
}

data class ActivityOutcome(
    val activityId: Long,
    val difficulty: Difficulty,
    val wrongAttempts: Int,
    val solved: Boolean,
    val solutionShown: Boolean,
    val seconds: Int
)

class LessonRunner(private val activities: List<LessonActivity>) {

    var index: Int = 0
        private set
    var wrongAttempts: Int = 0
        private set
    var hintShown: Boolean = false
        private set
    var solved: Boolean = false
        private set
    var solutionShown: Boolean = false
        private set

    private val outcomes = mutableListOf<ActivityOutcome>()

    val current: LessonActivity?
        get() = activities.getOrNull(index)

    val isFinished: Boolean
        get() = index >= activities.size

    val isLessonContent: Boolean
        get() = current?.type == ActivityType.LESSON

    val canAdvance: Boolean
        get() = solved || solutionShown || isLessonContent

    val currentIndex: Int
        get() = (index + 1).coerceAtMost(activities.size)

    val total: Int
        get() = activities.size

    val collectedOutcomes: List<ActivityOutcome>
        get() = outcomes.toList()

    fun submit(correct: Boolean): Submission {
        if (isFinished || canAdvance) return Submission.Ignored
        return if (correct) {
            solved = true
            Submission.Correct(firstTry = wrongAttempts == 0)
        } else {
            val previous = CorrectionState(wrongAttempts, hintShown, solutionShown)
            wrongAttempts += 1
            val step = CorrectionPolicy.onWrong(activities[index].difficulty, previous)
            when (step) {
                CorrectionStep.HINT -> hintShown = true
                CorrectionStep.SECOND_CHANCE -> Unit
                CorrectionStep.SOLUTION -> solutionShown = true
            }
            Submission.Incorrect(step)
        }
    }

    fun markSolutionShown() {
        if (!isFinished && !solved) solutionShown = true
    }

    fun advance(seconds: Int): Boolean {
        if (isFinished || !canAdvance) return false
        val activity = activities[index]
        if (activity.type != ActivityType.LESSON) {
            outcomes += ActivityOutcome(
                activityId = activity.id,
                difficulty = activity.difficulty,
                wrongAttempts = wrongAttempts,
                solved = solved,
                solutionShown = solutionShown,
                seconds = seconds.coerceAtLeast(0)
            )
        }
        index += 1
        wrongAttempts = 0
        hintShown = false
        solved = false
        solutionShown = false
        return true
    }
}
