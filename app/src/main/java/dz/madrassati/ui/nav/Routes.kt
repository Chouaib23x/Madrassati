package dz.madrassati.ui.nav

object Routes {

    const val WELCOME = "welcome"

    const val MODE = "mode"

    const val TEACHER_LOGIN = "teacher/login"
    const val TEACHER_HOME = "teacher/home"
    const val TEACHER_UNITS = "teacher/units/{subjectId}"
    const val TEACHER_LESSONS = "teacher/lessons/{unitId}"
    const val TEACHER_LESSON = "teacher/lesson/{lessonId}"
    const val TEACHER_EXERCISE = "teacher/exercise/{lessonId}"
    const val TEACHER_CLASS = "teacher/class"
    const val TEACHER_SETTINGS = "teacher/settings"
    const val TEACHER_NETWORK = "teacher/network"
    const val TEACHER_CONTROL = "teacher/control"
    const val TEACHER_MEDIA = "teacher/media"

    const val STUDENT_WAIT = "student/wait"
    const val STUDENT_PROFILES = "student/profiles"
    const val STUDENT_SUBJECTS = "student/subjects/{studentId}"
    const val STUDENT_UNITS = "student/units/{subjectId}/{studentId}"
    const val STUDENT_LESSONS = "student/lessons/{unitId}/{studentId}"
    const val STUDENT_RUN = "student/run/{lessonId}/{studentId}"
    const val STUDENT_SCORE = "student/score/{resultId}"

    const val PIN = "pin"

    const val ARG_SUBJECT_ID = "subjectId"
    const val ARG_UNIT_ID = "unitId"
    const val ARG_LESSON_ID = "lessonId"
    const val ARG_STUDENT_ID = "studentId"
    const val ARG_RESULT_ID = "resultId"

    fun teacherUnits(subjectId: Long) = "teacher/units/$subjectId"

    fun teacherLessons(unitId: Long) = "teacher/lessons/$unitId"

    fun teacherLesson(lessonId: Long) = "teacher/lesson/$lessonId"

    fun teacherExercise(lessonId: Long) = "teacher/exercise/$lessonId"

    fun studentSubjects(studentId: Long) = "student/subjects/$studentId"

    fun studentUnits(subjectId: Long, studentId: Long) = "student/units/$subjectId/$studentId"

    fun studentLessons(unitId: Long, studentId: Long) = "student/lessons/$unitId/$studentId"

    fun studentRun(lessonId: Long, studentId: Long) = "student/run/$lessonId/$studentId"

    fun studentScore(resultId: Long) = "student/score/$resultId"
}
