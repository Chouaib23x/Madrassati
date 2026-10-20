package dz.madrassati

import dz.madrassati.domain.correction.CorrectionPolicy
import dz.madrassati.domain.correction.CorrectionState
import dz.madrassati.domain.correction.CorrectionStep
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectionPolicyTest {

    @Test
    fun easyFirstWrongShowsHint() {
        val step = CorrectionPolicy.onWrong(
            dz.madrassati.domain.model.Difficulty.EASY,
            CorrectionState(wrongAttempts = 0, hintShown = false)
        )
        assertEquals(CorrectionStep.HINT, step)
    }

    @Test
    fun easySecondWrongShowsSolution() {
        val step = CorrectionPolicy.onWrong(
            dz.madrassati.domain.model.Difficulty.EASY,
            CorrectionState(wrongAttempts = 1, hintShown = true)
        )
        assertEquals(CorrectionStep.SOLUTION, step)
    }

    @Test
    fun mediumFirstWrongGivesSecondChance() {
        val step = CorrectionPolicy.onWrong(
            dz.madrassati.domain.model.Difficulty.MEDIUM,
            CorrectionState(wrongAttempts = 0)
        )
        assertEquals(CorrectionStep.SECOND_CHANCE, step)
    }

    @Test
    fun mediumSecondWrongShowsSolution() {
        val step = CorrectionPolicy.onWrong(
            dz.madrassati.domain.model.Difficulty.MEDIUM,
            CorrectionState(wrongAttempts = 1)
        )
        assertEquals(CorrectionStep.SOLUTION, step)
    }

    @Test
    fun advancedWrongImmediatelyShowsSolution() {
        val step = CorrectionPolicy.onWrong(
            dz.madrassati.domain.model.Difficulty.ADVANCED,
            CorrectionState(wrongAttempts = 0)
        )
        assertEquals(CorrectionStep.SOLUTION, step)
    }
}
