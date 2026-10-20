package dz.madrassati

import dz.madrassati.domain.correction.CorrectionStep
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.runner.LessonRunner
import dz.madrassati.domain.runner.Submission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRunnerTest {

    private fun mcq(id: Long, difficulty: Difficulty): LessonActivity = LessonActivity(
        id = id,
        lessonId = 1,
        type = ActivityType.QCM,
        difficulty = difficulty,
        sortOrder = id.toInt(),
        payload = ActivityPayload.Mcq(
            question = "سؤال $id",
            options = listOf("أ", "ب"),
            correctIndices = setOf(0),
            hint = "تلميح",
            explanation = "شرح"
        )
    )

    @Test
    fun correctFirstTryMarksSolved() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY)))
        val submission = runner.submit(correct = true)
        assertEquals(Submission.Correct(firstTry = true), submission)
        assertTrue(runner.canAdvance)
    }

    @Test
    fun correctAfterWrongIsNotFirstTry() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.MEDIUM)))
        runner.submit(correct = false)
        val submission = runner.submit(correct = true)
        assertEquals(Submission.Correct(firstTry = false), submission)
    }

    @Test
    fun easyWrongShowsHintThenSolution() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY)))

        val first = runner.submit(correct = false)
        assertTrue(first is Submission.Incorrect)
        assertEquals(CorrectionStep.HINT, (first as Submission.Incorrect).step)
        assertTrue(runner.hintShown)
        assertFalse(runner.canAdvance)

        val second = runner.submit(correct = false)
        assertTrue(second is Submission.Incorrect)
        assertEquals(CorrectionStep.SOLUTION, (second as Submission.Incorrect).step)
        assertTrue(runner.solutionShown)
        assertTrue(runner.canAdvance)
    }

    @Test
    fun mediumWrongGivesSecondChance() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.MEDIUM)))
        val submission = runner.submit(correct = false)
        assertEquals(CorrectionStep.SECOND_CHANCE, (submission as Submission.Incorrect).step)
        assertFalse(runner.canAdvance)
    }

    @Test
    fun submitAfterSolvedIsIgnored() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY)))
        runner.submit(correct = true)
        assertEquals(Submission.Ignored, runner.submit(correct = false))
    }

    @Test
    fun advanceMovesToNextActivity() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY), mcq(2, Difficulty.EASY)))
        runner.submit(correct = true)
        assertTrue(runner.advance(seconds = 5))
        assertEquals(2, runner.currentIndex)
        assertFalse(runner.isFinished)
        assertEquals(1, runner.collectedOutcomes.size)
        assertEquals(5, runner.collectedOutcomes[0].seconds)
    }

    @Test
    fun advanceWithoutSolutionIsRefused() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY)))
        assertFalse(runner.advance(seconds = 5))
        assertEquals(1, runner.currentIndex)
    }

    @Test
    fun finishingAllActivitiesMarksFinished() {
        val runner = LessonRunner(listOf(mcq(1, Difficulty.EASY)))
        runner.submit(correct = true)
        runner.advance(seconds = 3)
        assertTrue(runner.isFinished)
        assertFalse(runner.advance(seconds = 1))
    }
}
