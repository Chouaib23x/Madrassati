package dz.madrassati.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dz.madrassati.R
import dz.madrassati.core.net.SchoolNet
import dz.madrassati.ui.auth.AuthScreen
import dz.madrassati.ui.browse.LessonDetailScreen
import dz.madrassati.ui.browse.LessonListScreen
import dz.madrassati.ui.browse.SubjectGridScreen
import dz.madrassati.ui.browse.UnitListScreen
import dz.madrassati.ui.common.MediaOverlay
import dz.madrassati.ui.kiosk.PinScreen
import dz.madrassati.ui.mode.ModeSelectScreen
import dz.madrassati.ui.mode.WelcomeScreen
import dz.madrassati.ui.student.LessonRunnerScreen
import dz.madrassati.ui.student.ProfilesScreen
import dz.madrassati.ui.student.ScoreScreen
import dz.madrassati.ui.student.StudentWaitScreen
import dz.madrassati.ui.teacher.ClassControlScreen
import dz.madrassati.ui.teacher.ClassScreen
import dz.madrassati.ui.teacher.ExerciseEditorScreen
import dz.madrassati.ui.teacher.NetworkScreen
import dz.madrassati.ui.teacher.SettingsScreen
import dz.madrassati.ui.teacher.TeacherMediaScreen

private fun longArg(name: String) = navArgument(name) { type = NavType.LongType }

@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    val controlViewModel: ClassControlViewModel = hiltViewModel()
    val locked by controlViewModel.locked.collectAsStateWithLifecycle()
    val activeStudentId by controlViewModel.activeStudentId.collectAsStateWithLifecycle()
    val media by controlViewModel.media.collectAsStateWithLifecycle()
    val serverHost by controlViewModel.serverHost.collectAsStateWithLifecycle()
    val serverRunning by controlViewModel.serverRunning.collectAsStateWithLifecycle()
    var dismissedMediaSeq by remember { mutableLongStateOf(-1L) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val onStudentScreen = backStackEntry?.destination?.route?.startsWith("student") == true

    LaunchedEffect(Unit) {
        controlViewModel.lessonCommands.collect { command ->
            if (controlViewModel.serverRunning.value) return@collect
            val studentId = controlViewModel.activeStudentId.value ?: return@collect
            nav.navigate(Routes.studentRun(command.lessonId, studentId)) {
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.WELCOME) {

            composable(Routes.WELCOME) {
                BackHandler(enabled = true) {}
                WelcomeScreen(
                    onStart = {
                        nav.navigate(Routes.MODE) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.MODE) {
                BackHandler(enabled = true) {}
                ModeSelectScreen(
                    onTeacher = { nav.navigate(Routes.TEACHER_LOGIN) },
                    onStudent = { nav.navigate(Routes.STUDENT_WAIT) }
                )
            }

            composable(Routes.TEACHER_LOGIN) {
                AuthScreen(
                    onLoggedIn = {
                        nav.navigate(Routes.TEACHER_HOME) {
                            popUpTo(Routes.TEACHER_LOGIN) { inclusive = true }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.TEACHER_HOME) {
                SubjectGridScreen(
                    title = stringResource(R.string.teacher_home_title),
                    onSubject = { subjectId -> nav.navigate(Routes.teacherUnits(subjectId)) },
                    onClass = { nav.navigate(Routes.TEACHER_CLASS) },
                    onSettings = { nav.navigate(Routes.TEACHER_SETTINGS) },
                    onControl = { nav.navigate(Routes.TEACHER_CONTROL) },
                    onMedia = { nav.navigate(Routes.TEACHER_MEDIA) }
                )
            }

            composable(
                Routes.TEACHER_UNITS,
                arguments = listOf(longArg(Routes.ARG_SUBJECT_ID))
            ) {
                UnitListScreen(
                    onUnit = { unitId -> nav.navigate(Routes.teacherLessons(unitId)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.TEACHER_LESSONS,
                arguments = listOf(longArg(Routes.ARG_UNIT_ID))
            ) {
                LessonListScreen(
                    onLesson = { lessonId -> nav.navigate(Routes.teacherLesson(lessonId)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.TEACHER_LESSON,
                arguments = listOf(longArg(Routes.ARG_LESSON_ID))
            ) {
                LessonDetailScreen(
                    onBack = { nav.popBackStack() },
                    onAddExercise = { lessonId -> nav.navigate(Routes.teacherExercise(lessonId)) }
                )
            }

            composable(
                Routes.TEACHER_EXERCISE,
                arguments = listOf(longArg(Routes.ARG_LESSON_ID))
            ) {
                ExerciseEditorScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() }
                )
            }

            composable(Routes.TEACHER_CLASS) {
                ClassScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.TEACHER_SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onUnlockDevice = { nav.navigate(Routes.PIN) },
                    onNetwork = { nav.navigate(Routes.TEACHER_NETWORK) }
                )
            }

            composable(Routes.TEACHER_NETWORK) {
                NetworkScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.TEACHER_CONTROL) {
                ClassControlScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.TEACHER_MEDIA) {
                TeacherMediaScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.STUDENT_WAIT) {
                StudentWaitScreen(
                    onChooseName = { nav.navigate(Routes.STUDENT_PROFILES) },
                    onUnlockGesture = { nav.navigate(Routes.PIN) }
                )
            }

            composable(Routes.STUDENT_PROFILES) {
                ProfilesScreen(
                    onSelect = { studentId -> nav.navigate(Routes.studentSubjects(studentId)) },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.STUDENT_SUBJECTS,
                arguments = listOf(longArg(Routes.ARG_STUDENT_ID))
            ) { entry ->
                val studentId = entry.arguments?.getLong(Routes.ARG_STUDENT_ID) ?: return@composable
                SubjectGridScreen(
                    title = stringResource(R.string.subjects_title),
                    onSubject = { subjectId ->
                        nav.navigate(Routes.studentUnits(subjectId, studentId))
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.STUDENT_UNITS,
                arguments = listOf(longArg(Routes.ARG_SUBJECT_ID), longArg(Routes.ARG_STUDENT_ID))
            ) { entry ->
                val studentId = entry.arguments?.getLong(Routes.ARG_STUDENT_ID) ?: return@composable
                UnitListScreen(
                    onUnit = { unitId ->
                        nav.navigate(Routes.studentLessons(unitId, studentId))
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.STUDENT_LESSONS,
                arguments = listOf(longArg(Routes.ARG_UNIT_ID), longArg(Routes.ARG_STUDENT_ID))
            ) { entry ->
                val studentId = entry.arguments?.getLong(Routes.ARG_STUDENT_ID) ?: return@composable
                LessonListScreen(
                    onLesson = { lessonId ->
                        nav.navigate(Routes.studentRun(lessonId, studentId))
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.STUDENT_RUN,
                arguments = listOf(longArg(Routes.ARG_LESSON_ID), longArg(Routes.ARG_STUDENT_ID))
            ) {
                LessonRunnerScreen(
                    onFinished = { resultId ->
                        nav.navigate(Routes.studentScore(resultId)) {
                            popUpTo(Routes.STUDENT_RUN) { inclusive = true }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(
                Routes.STUDENT_SCORE,
                arguments = listOf(longArg(Routes.ARG_RESULT_ID))
            ) {
                ScoreScreen(
                    onRetry = { lessonId, studentId ->
                        nav.navigate(Routes.studentRun(lessonId, studentId)) {
                            popUpTo(Routes.STUDENT_SCORE) { inclusive = true }
                        }
                    },
                    onHome = {
                        nav.navigate(Routes.STUDENT_WAIT) {
                            popUpTo(Routes.STUDENT_WAIT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Routes.PIN) {
                PinScreen(
                    onUnlocked = {
                        nav.popBackStack(Routes.MODE, inclusive = false)
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }

        if (locked && activeStudentId != null && !serverRunning && onStudentScreen) {
            ClassLockOverlay()
        }

        val currentMedia = media
        if (currentMedia != null && activeStudentId != null && !serverRunning && onStudentScreen &&
            serverHost != null && currentMedia.seq != dismissedMediaSeq
        ) {
            MediaOverlay(
                media = currentMedia,
                url = "http://$serverHost:${SchoolNet.PORT}${SchoolNet.PATH_MEDIA_FILE}?seq=${currentMedia.seq}",
                onClose = { dismissedMediaSeq = currentMedia.seq }
            )
        }
    }
}

@Composable
private fun ClassLockOverlay() {
    BackHandler(enabled = true) {}
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.padding(top = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(72.dp)
                )
            }
            Text(
                text = stringResource(R.string.control_locked_screen),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}