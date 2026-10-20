package dz.madrassati.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dz.madrassati.R
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Lesson
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.domain.model.Subject
import dz.madrassati.domain.model.Unit as LessonUnit
import dz.madrassati.ui.common.AppTopBar
import dz.madrassati.ui.common.ClassroomBackdrop
import dz.madrassati.ui.common.DifficultyChip
import dz.madrassati.ui.common.LessonCoursePanel
import dz.madrassati.ui.common.NumberBadge
import dz.madrassati.ui.common.SubjectCard
import dz.madrassati.ui.common.activityTypeName

@Composable
fun SubjectGridScreen(
    title: String,
    onSubject: (Long) -> Unit,
    onClass: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onControl: (() -> Unit)? = null,
    onMedia: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(title = title, onBack = onBack) {
                if (onMedia != null) {
                    IconButton(onClick = onMedia) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = stringResource(R.string.media_title)
                        )
                    }
                }
                if (onControl != null) {
                    IconButton(onClick = onControl) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.control_title)
                        )
                    }
                }
                if (onClass != null) {
                    IconButton(onClick = onClass) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = stringResource(R.string.action_class)
                        )
                    }
                }
                if (onSettings != null) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ClassroomBackdrop()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (subjects.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.state_empty),
                            contentDescription = null,
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.content_coming_soon),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                subjects.forEach { subject ->
                    SubjectCard(subject = subject) { onSubject(subject.id) }
                }
            }
        }
    }
}

@Composable
fun UnitListScreen(
    onUnit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: UnitsViewModel = hiltViewModel()
) {
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = subject?.title ?: stringResource(R.string.units_title),
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(units) { index, unit ->
                UnitRow(position = index + 1, unit = unit) { onUnit(unit.id) }
            }
        }
    }
}

@Composable
private fun UnitRow(position: Int, unit: LessonUnit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberBadge(position)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LessonListScreen(
    onLesson: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val unit by viewModel.unit.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = unit?.title ?: stringResource(R.string.lessons_title),
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(lessons) { index, lesson ->
                LessonRow(position = index + 1, lesson = lesson) { onLesson(lesson.id) }
            }
        }
    }
}

@Composable
private fun LessonRow(position: Int, lesson: Lesson, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberBadge(position)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyChip(difficulty = lesson.difficulty)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = lesson.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LessonDetailScreen(
    onBack: () -> Unit,
    onAddExercise: (Long) -> Unit = {},
    viewModel: LessonDetailViewModel = hiltViewModel()
) {
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val serverRunning by viewModel.serverRunning.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sentMessage = stringResource(R.string.control_lesson_sent)
    var pendingDelete by remember { mutableStateOf<LessonActivity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.sent.collect {
            snackbarHostState.showSnackbar(sentMessage)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = lesson?.title ?: stringResource(R.string.lessons_title),
                onBack = onBack
            ) {
                IconButton(onClick = { onAddExercise(lesson?.id ?: 0L) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.action_add_exercise)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val course = activities.firstOrNull { it.type == ActivityType.LESSON }
            ?.payload as? ActivityPayload.LessonContent
        val exercises = activities.filter { it.type != ActivityType.LESSON }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = { viewModel.sendLesson() },
                    enabled = lesson != null && serverRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.control_send_lesson))
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.lesson_overview),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            lesson?.let { DifficultyChip(difficulty = it.difficulty) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = lesson?.summary.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            if (course != null) {
                item {
                    Text(
                        text = stringResource(R.string.lesson_content_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    LessonCoursePanel(payload = course)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.activities_count, exercises.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            itemsIndexed(exercises, key = { _, activity -> activity.id }) { _, activity ->
                ActivityPreviewRow(
                    activity = activity,
                    onDelete = if (activity.custom) ({ pendingDelete = activity }) else null
                )
            }
        }
    }

    pendingDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.action_delete_exercise)) },
            text = { Text(stringResource(R.string.exercise_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivity(activity.id)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ActivityPreviewRow(
    activity: LessonActivity,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activityTypeName(activity.type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (activity.custom) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.exercise_custom_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete_exercise),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DifficultyChip(difficulty = activity.difficulty)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = activity.question,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
