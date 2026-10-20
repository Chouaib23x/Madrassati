package dz.madrassati.domain.exercise

import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType

data class ExerciseDraft(
    val type: ActivityType = ActivityType.QCM,
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val correctIndices: Set<Int> = emptySet(),
    val trueFalseCorrect: Boolean = true,
    val pairs: List<Pair<String, String>> = listOf("" to "", "" to ""),
    val orderedItems: List<String> = listOf("", ""),
    val acceptedAnswers: List<String> = listOf(""),
    val modelAnswer: String = "",
    val hint: String = "",
    val explanation: String = ""
)

sealed interface ExerciseValidation {

    data class Valid(val payload: ActivityPayload) : ExerciseValidation

    data class Invalid(val reason: Reason) : ExerciseValidation

    enum class Reason {
        EMPTY_QUESTION,
        NEED_OPTIONS,
        NEED_CORRECT,
        NEED_PAIRS,
        NEED_ITEMS,
        NEED_ANSWER,
        NO_LESSON
    }
}

object ExerciseBuilder {

    fun build(draft: ExerciseDraft): ExerciseValidation {
        val question = draft.question.trim()
        if (question.isEmpty()) {
            return invalid(ExerciseValidation.Reason.EMPTY_QUESTION)
        }
        return when (draft.type) {
            ActivityType.LESSON -> invalid(ExerciseValidation.Reason.EMPTY_QUESTION)

            ActivityType.QCM -> {
                val keptIndices = draft.options.indices.filter { draft.options[it].isNotBlank() }
                val options = keptIndices.map { draft.options[it].trim() }
                if (options.size < 2) return invalid(ExerciseValidation.Reason.NEED_OPTIONS)
                val correct = draft.correctIndices
                    .filter { it in keptIndices }
                    .map { keptIndices.indexOf(it) }
                    .toSet()
                if (correct.isEmpty()) return invalid(ExerciseValidation.Reason.NEED_CORRECT)
                valid(
                    ActivityPayload.Mcq(
                        question = question,
                        options = options,
                        correctIndices = correct,
                        hint = draft.hint.trim(),
                        explanation = draft.explanation.trim()
                    )
                )
            }

            ActivityType.TRUE_FALSE -> valid(
                ActivityPayload.TrueFalse(
                    question = question,
                    correct = draft.trueFalseCorrect,
                    hint = draft.hint.trim(),
                    explanation = draft.explanation.trim()
                )
            )

            ActivityType.MATCHING -> {
                val pairs = draft.pairs
                    .map { it.first.trim() to it.second.trim() }
                    .filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
                if (pairs.size < 2) return invalid(ExerciseValidation.Reason.NEED_PAIRS)
                valid(
                    ActivityPayload.Matching(
                        question = question,
                        pairs = pairs,
                        hint = draft.hint.trim(),
                        explanation = draft.explanation.trim()
                    )
                )
            }

            ActivityType.SORTING -> {
                val items = draft.orderedItems.map { it.trim() }.filter { it.isNotEmpty() }
                if (items.size < 2) return invalid(ExerciseValidation.Reason.NEED_ITEMS)
                valid(
                    ActivityPayload.Sorting(
                        question = question,
                        itemsInOrder = items,
                        hint = draft.hint.trim(),
                        explanation = draft.explanation.trim()
                    )
                )
            }

            ActivityType.WRITING -> {
                val answers = draft.acceptedAnswers
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                if (answers.isEmpty()) return invalid(ExerciseValidation.Reason.NEED_ANSWER)
                valid(
                    ActivityPayload.Writing(
                        question = question,
                        acceptedAnswers = answers,
                        modelAnswer = draft.modelAnswer.trim().ifEmpty { answers.first() },
                        hint = draft.hint.trim(),
                        explanation = draft.explanation.trim()
                    )
                )
            }
        }
    }

    private fun valid(payload: ActivityPayload) = ExerciseValidation.Valid(payload)

    private fun invalid(reason: ExerciseValidation.Reason) = ExerciseValidation.Invalid(reason)
}
