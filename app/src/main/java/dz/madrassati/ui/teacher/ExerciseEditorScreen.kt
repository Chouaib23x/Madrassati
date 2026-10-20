package dz.madrassati.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dz.madrassati.R
import dz.madrassati.domain.exercise.ExerciseDraft
import dz.madrassati.domain.exercise.ExerciseValidation
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.ui.common.AppTopBar
import dz.madrassati.ui.common.activityTypeName

@Composable
fun ExerciseEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ExerciseEditorViewModel = hiltViewModel()
) {
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var lessonId by remember { mutableStateOf(viewModel.initialLesson()) }
    var type by remember { mutableStateOf(ActivityType.QCM) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var question by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var trueFalseCorrect by remember { mutableStateOf(true) }
    var modelAnswer by remember { mutableStateOf("") }

    val options = remember { mutableStateListOf("", "") }
    var correctIndices by remember { mutableStateOf(emptySet<Int>()) }
    val lefts = remember { mutableStateListOf("", "") }
    val rights = remember { mutableStateListOf("", "") }
    val orderedItems = remember { mutableStateListOf("", "") }
    val answers = remember { mutableStateListOf("") }
    var showLessonPicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }

    val selectedLessonLabel = lessons.firstOrNull { it.id == lessonId }?.label
    val noLessonMark = stringResource(R.string.exercise_choose_lesson)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> onSaved()
                is EditorEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(reasonMessageRes(event.reason)))
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.exercise_new_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel(stringResource(R.string.exercise_lesson))
            OutlinedButton(
                onClick = { showLessonPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedLessonLabel ?: noLessonMark)
            }

            SectionLabel(stringResource(R.string.exercise_type))
            OutlinedButton(
                onClick = { showTypePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(activityTypeName(type))
            }

            SectionLabel(stringResource(R.string.exercise_difficulty))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { candidate ->
                    FilterChip(
                        selected = difficulty == candidate,
                        onClick = { difficulty = candidate },
                        label = { Text(difficultyName(candidate)) }
                    )
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text(stringResource(R.string.exercise_question)) },
                modifier = Modifier.fillMaxWidth()
            )

            when (type) {
                ActivityType.QCM -> McqEditor(
                    options = options,
                    correctIndices = correctIndices,
                    onToggleCorrect = { index ->
                        correctIndices = if (index in correctIndices) {
                            correctIndices - index
                        } else {
                            correctIndices + index
                        }
                    },
                    onRemoveOption = { index ->
                        options.removeAt(index)
                        correctIndices = correctIndices
                            .filter { it != index }
                            .map { if (it > index) it - 1 else it }
                            .toSet()
                    },
                    onAddOption = { options.add("") }
                )

                ActivityType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = trueFalseCorrect,
                        onClick = { trueFalseCorrect = true },
                        label = { Text(stringResource(R.string.exercise_true)) }
                    )
                    FilterChip(
                        selected = !trueFalseCorrect,
                        onClick = { trueFalseCorrect = false },
                        label = { Text(stringResource(R.string.exercise_false)) }
                    )
                }

                ActivityType.MATCHING -> PairsEditor(
                    lefts = lefts,
                    rights = rights,
                    onRemove = { index ->
                        lefts.removeAt(index)
                        rights.removeAt(index)
                    },
                    onAdd = {
                        lefts.add("")
                        rights.add("")
                    }
                )

                ActivityType.SORTING -> ItemsEditor(
                    items = orderedItems,
                    onRemove = { orderedItems.removeAt(it) },
                    onAdd = { orderedItems.add("") }
                )

                ActivityType.WRITING -> AnswersEditor(
                    answers = answers,
                    modelAnswer = modelAnswer,
                    onModelChange = { modelAnswer = it },
                    onRemove = { answers.removeAt(it) },
                    onAdd = { answers.add("") }
                )

                ActivityType.LESSON -> Unit
            }

            OutlinedTextField(
                value = hint,
                onValueChange = { hint = it },
                label = { Text(stringResource(R.string.exercise_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                label = { Text(stringResource(R.string.exercise_explanation)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val draft = ExerciseDraft(
                        type = type,
                        question = question,
                        options = options.toList(),
                        correctIndices = correctIndices,
                        trueFalseCorrect = trueFalseCorrect,
                        pairs = lefts.zip(rights),
                        orderedItems = orderedItems.toList(),
                        acceptedAnswers = answers.toList(),
                        modelAnswer = modelAnswer,
                        hint = hint,
                        explanation = explanation
                    )
                    viewModel.save(lessonId, type, difficulty, draft)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.exercise_save),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showLessonPicker) {
        AlertDialog(
            onDismissRequest = { showLessonPicker = false },
            title = { Text(stringResource(R.string.exercise_choose_lesson)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(lessons, key = { it.id }) { option ->
                        TextButton(
                            onClick = {
                                lessonId = option.id
                                showLessonPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLessonPicker = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text(stringResource(R.string.exercise_type)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        ActivityType.entries.filter { it != ActivityType.LESSON },
                        key = { it.name }
                    ) { candidate ->
                        TextButton(
                            onClick = {
                                type = candidate
                                showTypePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activityTypeName(candidate),
                                color = if (candidate == type) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (candidate == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypePicker = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun McqEditor(
    options: MutableList<String>,
    correctIndices: Set<Int>,
    onToggleCorrect: (Int) -> Unit,
    onRemoveOption: (Int) -> Unit,
    onAddOption: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.exercise_options))
        options.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = index in correctIndices,
                    onCheckedChange = { onToggleCorrect(index) }
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { options[index] = it },
                    label = { Text(stringResource(R.string.exercise_option_hint)) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemoveOption(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
        OutlinedButton(onClick = onAddOption, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.exercise_add_option))
        }
    }
}

@Composable
private fun PairsEditor(
    lefts: MutableList<String>,
    rights: MutableList<String>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.exercise_pairs))
        lefts.indices.forEach { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = lefts[index],
                    onValueChange = { lefts[index] = it },
                    label = { Text(stringResource(R.string.exercise_left_hint)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = rights[index],
                    onValueChange = { rights[index] = it },
                    label = { Text(stringResource(R.string.exercise_right_hint)) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.exercise_add_pair))
        }
    }
}

@Composable
private fun ItemsEditor(
    items: MutableList<String>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.exercise_items))
        items.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { items[index] = it },
                    label = { Text("${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.exercise_add_item))
        }
    }
}

@Composable
private fun AnswersEditor(
    answers: MutableList<String>,
    modelAnswer: String,
    onModelChange: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.exercise_answers))
        answers.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { answers[index] = it },
                    label = { Text(stringResource(R.string.exercise_answer_hint)) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.exercise_add_answer))
        }
        OutlinedTextField(
            value = modelAnswer,
            onValueChange = onModelChange,
            label = { Text(stringResource(R.string.exercise_model)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun difficultyName(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.difficulty_easy)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.ADVANCED -> stringResource(R.string.difficulty_advanced)
}

private fun reasonMessageRes(reason: ExerciseValidation.Reason): Int = when (reason) {
    ExerciseValidation.Reason.EMPTY_QUESTION -> R.string.exercise_err_question
    ExerciseValidation.Reason.NEED_OPTIONS -> R.string.exercise_err_options
    ExerciseValidation.Reason.NEED_CORRECT -> R.string.exercise_err_correct
    ExerciseValidation.Reason.NEED_PAIRS -> R.string.exercise_err_pairs
    ExerciseValidation.Reason.NEED_ITEMS -> R.string.exercise_err_items
    ExerciseValidation.Reason.NEED_ANSWER -> R.string.exercise_err_answer
    ExerciseValidation.Reason.NO_LESSON -> R.string.exercise_err_lesson
}
