package dz.madrassati

import dz.madrassati.domain.exercise.ExerciseBuilder
import dz.madrassati.domain.exercise.ExerciseDraft
import dz.madrassati.domain.exercise.ExerciseValidation
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseBuilderTest {

    @Test
    fun emptyQuestionIsRejected() {
        val result = ExerciseBuilder.build(ExerciseDraft(type = ActivityType.QCM, question = "   "))
        assertEquals(
            ExerciseValidation.Invalid(ExerciseValidation.Reason.EMPTY_QUESTION),
            result
        )
    }

    @Test
    fun mcqNeedsTwoOptions() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.QCM,
                question = "سؤال",
                options = listOf("واحد", "  "),
                correctIndices = setOf(0)
            )
        )
        assertEquals(ExerciseValidation.Invalid(ExerciseValidation.Reason.NEED_OPTIONS), result)
    }

    @Test
    fun mcqNeedsCorrectAnswer() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.QCM,
                question = "سؤال",
                options = listOf("أ", "ب"),
                correctIndices = emptySet()
            )
        )
        assertEquals(ExerciseValidation.Invalid(ExerciseValidation.Reason.NEED_CORRECT), result)
    }

    @Test
    fun mcqRemapsIndicesAfterDroppingBlankOptions() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.QCM,
                question = "اختر",
                options = listOf("", "صحيح", "  ", "أيضا صحيح"),
                correctIndices = setOf(1, 3)
            )
        )
        assertTrue(result is ExerciseValidation.Valid)
        val payload = (result as ExerciseValidation.Valid).payload as ActivityPayload.Mcq
        assertEquals(listOf("صحيح", "أيضا صحيح"), payload.options)
        assertEquals(setOf(0, 1), payload.correctIndices)
    }

    @Test
    fun matchingNeedsTwoPairs() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.MATCHING,
                question = "طابق",
                pairs = listOf("أ" to "1", "" to "2")
            )
        )
        assertEquals(ExerciseValidation.Invalid(ExerciseValidation.Reason.NEED_PAIRS), result)
    }

    @Test
    fun sortingNeedsTwoItems() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.SORTING,
                question = "رتب",
                orderedItems = listOf("أ", " ")
            )
        )
        assertEquals(ExerciseValidation.Invalid(ExerciseValidation.Reason.NEED_ITEMS), result)
    }

    @Test
    fun writingNeedsAcceptedAnswer() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.WRITING,
                question = "اكتب",
                acceptedAnswers = listOf("  ")
            )
        )
        assertEquals(ExerciseValidation.Invalid(ExerciseValidation.Reason.NEED_ANSWER), result)
    }

    @Test
    fun writingDefaultsModelAnswerToFirstAccepted() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.WRITING,
                question = "اكتب",
                acceptedAnswers = listOf("  الجواب  ", "جواب آخر")
            )
        )
        assertTrue(result is ExerciseValidation.Valid)
        val payload = (result as ExerciseValidation.Valid).payload as ActivityPayload.Writing
        assertEquals(listOf("الجواب", "جواب آخر"), payload.acceptedAnswers)
        assertEquals("الجواب", payload.modelAnswer)
    }

    @Test
    fun trueFalseBuildsPayload() {
        val result = ExerciseBuilder.build(
            ExerciseDraft(
                type = ActivityType.TRUE_FALSE,
                question = "هل",
                trueFalseCorrect = false
            )
        )
        assertTrue(result is ExerciseValidation.Valid)
        val payload = (result as ExerciseValidation.Valid).payload as ActivityPayload.TrueFalse
        assertEquals(false, payload.correct)
    }
}
