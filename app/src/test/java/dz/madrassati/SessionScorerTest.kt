package dz.madrassati

import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.runner.ActivityOutcome
import dz.madrassati.domain.scoring.SessionScorer
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionScorerTest {

    private fun outcome(
        id: Long,
        difficulty: Difficulty,
        solved: Boolean,
        wrongAttempts: Int,
        seconds: Int = 10
    ) = ActivityOutcome(
        activityId = id,
        difficulty = difficulty,
        wrongAttempts = wrongAttempts,
        solved = solved,
        solutionShown = !solved,
        seconds = seconds
    )

    @Test
    fun firstTryEarnsFullPoints() {
        val summary = SessionScorer.summarize(
            listOf(
                outcome(1, Difficulty.EASY, solved = true, wrongAttempts = 0),
                outcome(2, Difficulty.MEDIUM, solved = true, wrongAttempts = 0),
                outcome(3, Difficulty.ADVANCED, solved = true, wrongAttempts = 0)
            )
        )
        assertEquals(38, summary.score)
        assertEquals(38, summary.maxScore)
        assertEquals(100, summary.masteryPercent)
        assertEquals(3, summary.solvedFirstTry)
        assertEquals(0, summary.difficultyActivityIds.size)
    }

    @Test
    fun solvedAfterErrorEarnsSixPoints() {
        val summary = SessionScorer.summarize(
            listOf(outcome(1, Difficulty.ADVANCED, solved = true, wrongAttempts = 2))
        )
        assertEquals(6, summary.score)
        assertEquals(15, summary.maxScore)
        assertEquals(40, summary.masteryPercent)
    }

    @Test
    fun unsolvedEarnsZeroAndIsDifficult() {
        val summary = SessionScorer.summarize(
            listOf(outcome(1, Difficulty.EASY, solved = false, wrongAttempts = 3))
        )
        assertEquals(0, summary.score)
        assertEquals(10, summary.maxScore)
        assertEquals(0, summary.masteryPercent)
        assertEquals(listOf(1L), summary.difficultyActivityIds)
    }

    @Test
    fun timeAndTotalsAreSummed() {
        val summary = SessionScorer.summarize(
            listOf(
                outcome(1, Difficulty.EASY, solved = true, wrongAttempts = 0, seconds = 7),
                outcome(2, Difficulty.EASY, solved = true, wrongAttempts = 1, seconds = 9)
            )
        )
        assertEquals(16, summary.totalSeconds)
        assertEquals(2, summary.totalActivities)
        assertEquals(16, summary.score)
        assertEquals(20, summary.maxScore)
        assertEquals(80, summary.masteryPercent)
    }

    @Test
    fun emptySummariesAreZero() {
        val summary = SessionScorer.summarize(emptyList())
        assertEquals(0, summary.score)
        assertEquals(0, summary.maxScore)
        assertEquals(0, summary.masteryPercent)
    }
}
