package dz.madrassati.ui.teacher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.R
import dz.madrassati.core.net.ControlRecord
import dz.madrassati.core.net.LessonCommandDto
import dz.madrassati.core.net.SchoolNet
import dz.madrassati.core.net.SchoolServer
import dz.madrassati.core.theme.DifficultyEasy
import dz.madrassati.core.theme.DifficultyMedium
import dz.madrassati.ui.common.AppTopBar
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class ClassControlViewModel @Inject constructor(
    private val server: SchoolServer
) : ViewModel() {

    val locked: StateFlow<Boolean> = server.locked
    val lesson: StateFlow<LessonCommandDto?> = server.lesson
    val running: StateFlow<Boolean> = server.running

    private val _devices = MutableStateFlow<List<ControlRecord>>(emptyList())
    val devices: StateFlow<List<ControlRecord>> = _devices.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                server.refreshControl()
                _devices.value = server.controlRecords.value
                delay(SchoolNet.CONTROL_INTERVAL_MS)
            }
        }
    }

    fun toggleLock() {
        server.setLocked(!server.locked.value)
    }

    fun stopLesson() {
        server.clearLesson()
    }
}

@Composable
fun ClassControlScreen(
    onBack: () -> Unit,
    viewModel: ClassControlViewModel = hiltViewModel()
) {
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val lesson by viewModel.lesson.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.control_title), onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!running) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.control_needs_server),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                item {
                    Button(
                        onClick = { viewModel.toggleLock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(
                                if (locked) R.string.control_unlock else R.string.control_lock
                            )
                        )
                    }
                }
                item {
                    ControlLessonCard(lesson = lesson, onStop = { viewModel.stopLesson() })
                }
                item {
                    Text(
                        text = stringResource(R.string.control_devices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.control_no_devices),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                items(devices) { record ->
                    ControlDeviceRow(record = record)
                }
            }
        }
    }
}

@Composable
private fun ControlLessonCard(
    lesson: LessonCommandDto?,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.control_lesson),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            if (lesson != null) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.control_stop_lesson))
                }
            } else {
                Text(
                    text = stringResource(R.string.control_lesson_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ControlDeviceRow(record: ControlRecord) {
    val now = System.currentTimeMillis()
    val secondsAgo = ((now - record.lastSeen) / 1000).coerceAtLeast(0)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(presenceColor(secondsAgo))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = seenLabel(secondsAgo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            val lessonTitle = record.lessonTitle
            if (lessonTitle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = lessonTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    val position = record.position ?: 0
                    val total = record.total ?: 0
                    Text(
                        text = stringResource(R.string.activity_index, position, total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                val total = (record.total ?: 0).coerceAtLeast(0)
                val fraction = if (total > 0) {
                    ((record.position ?: 0).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.control_waiting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun seenLabel(secondsAgo: Long): String = when {
    secondsAgo < 60 -> stringResource(R.string.net_seen_seconds, secondsAgo)
    else -> stringResource(R.string.net_seen_minutes, secondsAgo / 60)
}

@Composable
private fun presenceColor(secondsAgo: Long): Color = when {
    secondsAgo < 30 -> DifficultyEasy
    secondsAgo < 90 -> DifficultyMedium
    else -> MaterialTheme.colorScheme.outline
}