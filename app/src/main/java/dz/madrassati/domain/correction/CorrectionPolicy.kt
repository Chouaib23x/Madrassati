package dz.madrassati.domain.correction

import dz.madrassati.domain.model.Difficulty

enum class CorrectionStep { HINT, SECOND_CHANCE, SOLUTION }

data class CorrectionState(
    val wrongAttempts: Int = 0,
    val hintShown: Boolean = false,
    val solutionShown: Boolean = false
)

object CorrectionPolicy {

    fun onWrong(difficulty: Difficulty, previous: CorrectionState): CorrectionStep =
        when (difficulty) {
            Difficulty.EASY ->
                if (!previous.hintShown) CorrectionStep.HINT else CorrectionStep.SOLUTION
            Difficulty.MEDIUM ->
                if (previous.wrongAttempts == 0) CorrectionStep.SECOND_CHANCE else CorrectionStep.SOLUTION
            Difficulty.ADVANCED ->
                CorrectionStep.SOLUTION
        }
}
