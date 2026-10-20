package dz.madrassati.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.repository.ClassRepository
import dz.madrassati.domain.usecase.AddStudentResult
import dz.madrassati.domain.usecase.AddStudentUseCase
import dz.madrassati.ui.common.AVATARS
import dz.madrassati.ui.common.AppTopBar
import dz.madrassati.ui.common.AvatarView
import dz.madrassati.ui.common.EmptyState
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ClassEvent {
    data class AddFinished(val result: AddStudentResult) : ClassEvent
    data object Saved : ClassEvent
    data object Deleted : ClassEvent
}

@HiltViewModel
class ClassViewModel @Inject constructor(
    private val classRepository: ClassRepository,
    private val addStudentUseCase: AddStudentUseCase
) : ViewModel() {

    val students: StateFlow<List<Student>> = classRepository.students()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = Channel<ClassEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun add(name: String, avatar: String, colorHex: Long) {
        viewModelScope.launch {
            _events.send(ClassEvent.AddFinished(addStudentUseCase(name, avatar, colorHex)))
        }
    }

    fun update(id: Long, name: String, avatar: String, colorHex: Long) {
        viewModelScope.launch {
            classRepository.update(id, name, avatar, colorHex)
            _events.send(ClassEvent.Saved)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            classRepository.remove(id)
            _events.send(ClassEvent.Deleted)
        }
    }
}

@Composable
fun ClassScreen(
    onBack: () -> Unit,
    viewModel: ClassViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var pendingDelete by remember { mutableStateOf<Student?>(null) }

    val savedMessage = stringResource(R.string.saved_ok)
    val deletedMessage = stringResource(R.string.student_deleted)
    val fullMessage = stringResource(R.string.error_class_full)
    val emptyNameMessage = stringResource(R.string.error_name_required)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ClassEvent.AddFinished -> when (event.result) {
                    AddStudentResult.Success -> {
                        showDialog = false
                        snackbarHostState.showSnackbar(savedMessage)
                    }

                    AddStudentResult.EmptyName -> snackbarHostState.showSnackbar(emptyNameMessage)
                    AddStudentResult.Full -> snackbarHostState.showSnackbar(fullMessage)
                }

                ClassEvent.Saved -> {
                    showDialog = false
                    editing = null
                    snackbarHostState.showSnackbar(savedMessage)
                }

                ClassEvent.Deleted -> snackbarHostState.showSnackbar(deletedMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.class_title),
                onBack = onBack
            ) {
                IconButton(onClick = {
                    editing = null
                    showDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = stringResource(R.string.action_add_student)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.students_count, students.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (students.isEmpty()) {
                item {
                    EmptyState(
                        imageRes = R.drawable.state_empty,
                        text = stringResource(R.string.class_empty)
                    )
                }
            }
            items(students) { student ->
                StudentRow(
                    student = student,
                    onEdit = {
                        editing = student
                        showDialog = true
                    },
                    onDelete = { pendingDelete = student }
                )
            }
        }
    }

    if (showDialog) {
        StudentDialog(
            initial = editing,
            onDismiss = {
                showDialog = false
                editing = null
            },
            onSave = { name, avatar, colorHex ->
                val current = editing
                if (current == null) {
                    viewModel.add(name, avatar, colorHex)
                } else {
                    viewModel.update(current.id, name, avatar, colorHex)
                }
            }
        )
    }

    pendingDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(student.id)
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
private fun StudentRow(
    student: Student,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(avatar = student.avatar, colorHex = student.colorHex, size = 52.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit_student),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete_student),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StudentDialog(
    initial: Student?,
    onDismiss: () -> Unit,
    onSave: (name: String, avatar: String, colorHex: Long) -> Unit
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var avatar by rememberSaveable(initial?.id) { mutableStateOf(initial?.avatar ?: AVATARS.first()) }

    val avatarIndex = AVATARS.indexOf(avatar).coerceAtLeast(0)
    val colorHex = initial?.colorHex
        ?: dz.madrassati.ui.common.STUDENT_COLORS[avatarIndex % dz.madrassati.ui.common.STUDENT_COLORS.size]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.action_add_student else R.string.action_edit_student
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.hint_student_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.choose_avatar),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AVATARS) { option ->
                        val selected = option == avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { avatar = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = option, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), avatar, colorHex) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
