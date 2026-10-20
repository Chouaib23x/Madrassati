package dz.madrassati.domain.scoring

import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.runner.ActivityOutcome

data class SessionSummary(
    val score: Int,
    val maxScore: Int,
    val masteryPercent: Int,
    val totalSeconds: Int,
    val difficultyActivityIds: List<Long>,
    val solvedFirstTry: Int,
    val totalActivities: Int
)

object SessionScorer {

    fun maxPoints(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 10
        Difficulty.MEDIUM -> 13
        Difficulty.ADVANCED -> 15
    }

    fun summarize(outcomes: List<ActivityOutcome>): SessionSummary {
        var score = 0
        var max = 0
        var totalSeconds = 0
        val difficultyIds = mutableListOf<Long>()

        for (outcome in outcomes) {
            val base = maxPoints(outcome.difficulty)
            max += base
            totalSeconds += outcome.seconds
            score += when {
                outcome.solved && outcome.wrongAttempts == 0 -> base
                outcome.solved -> 6
                else -> 0
            }
            if (!outcome.solved || outcome.wrongAttempts >= 2) {
                difficultyIds += outcome.activityId
            }
        }

        val mastery = if (max == 0) 0 else (score * 100 / max)
        val firstTry = outcomes.count { it.solved && it.wrongAttempts == 0 }

        return SessionSummary(
            score = score,
            maxScore = max,
            masteryPercent = mastery,
            totalSeconds = totalSeconds,
            difficultyActivityIds = difficultyIds,
            solvedFirstTry = firstTry,
            totalActivities = outcomes.size
        )
    }
}
