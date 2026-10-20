package dz.madrassati.ui.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dz.madrassati.R
import dz.madrassati.core.theme.DifficultyAdvanced
import dz.madrassati.core.theme.DifficultyEasy
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.LessonActivity
import dz.madrassati.ui.common.AppTopBar
import dz.madrassati.ui.common.DifficultyChip
import dz.madrassati.ui.common.LessonCoursePanel
import dz.madrassati.ui.common.MascotGender
import dz.madrassati.ui.common.MascotMood
import dz.madrassati.ui.common.NumberBadge
import dz.madrassati.ui.common.StudentMascot
import dz.madrassati.ui.common.activityTypeName

private val ChoicePalette = listOf(
    Color(0xFF2563EB), Color(0xFF0B6B4F), Color(0xFFD97706),
    Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFF0E7490),
    Color(0xFF4D7C0F), Color(0xFFDC2626)
)

private val LetterBadges = listOf("أ", "ب", "ج", "د", "هـ", "و", "ز", "ح", "ط", "ي")

@Composable
fun LessonRunnerScreen(
    onFinished: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LessonRunnerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resultId.collect { resultId -> onFinished(resultId) }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = state.lessonTitle, onBack = onBack)
        }
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.activity == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.content_coming_soon),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }

            else -> RunnerContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun RunnerContent(
    state: RunnerUiState,
    viewModel: LessonRunnerViewModel,
    modifier: Modifier = Modifier
) {
    val activity = state.activity ?: return
    val payload = activity.payload
    val gender = if (viewModel.studentId % 2L == 0L) MascotGender.GIRL else MascotGender.BOY
    val mood = when {
        state.feedback == RunnerFeedback.CORRECT -> MascotMood.HAPPY
        state.feedback == RunnerFeedback.WRONG -> MascotMood.SAD
        hasSelection(state, payload) -> MascotMood.THINKING
        else -> MascotMood.NEUTRAL
    }

    val selectionReady = when (payload) {
        is ActivityPayload.LessonContent -> true
        is ActivityPayload.Mcq -> state.selectedOptions.isNotEmpty()
        is ActivityPayload.TrueFalse -> state.trueFalse != null
        is ActivityPayload.Writing -> state.writing.isNotBlank()
        is ActivityPayload.Matching -> state.matched.size == payload.pairs.size
        is ActivityPayload.Sorting -> state.ordered.isNotEmpty()
    }
    val canValidate = selectionReady && !state.canNext

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeaderCard(
            gender = gender,
            mood = mood,
            position = state.position,
            total = state.total,
            activity = activity
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                NumberBadge(number = state.position)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = activity.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (payload) {
                is ActivityPayload.LessonContent -> LessonCoursePanel(payload = payload)

                is ActivityPayload.Mcq -> McqOptions(
                    payload = payload,
                    selected = state.selectedOptions,
                    enabled = !state.canNext,
                    onSelect = viewModel::toggleOption
                )

                is ActivityPayload.TrueFalse -> TrueFalseOptions(
                    selected = state.trueFalse,
                    enabled = !state.canNext,
                    onSelect = viewModel::selectTrueFalse
                )

                is ActivityPayload.Writing -> WritingInput(
                    value = state.writing,
                    enabled = !state.canNext,
                    onChange = viewModel::onWritingChange
                )

                is ActivityPayload.Sorting -> SortingList(
                    items = state.ordered,
                    enabled = !state.canNext,
                    onMove = viewModel::moveOrdered
                )

                is ActivityPayload.Matching -> MatchingColumns(
                    activityId = activity.id,
                    pairs = payload.pairs,
                    matched = state.matched,
                    activeLeft = state.activeLeft,
                    enabled = !state.canNext,
                    onSelectLeft = viewModel::selectMatchLeft,
                    onSelectRight = viewModel::selectMatchRight
                )
            }

            state.feedback?.let { feedback ->
                FeedbackBanner(feedback = feedback, gender = gender)
            }

            if (state.hintVisible) {
                HintPanel(text = activity.hint)
            }

            if (state.solutionVisible) {
                SolutionPanel(text = activity.explanation)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (state.activity?.type == ActivityType.LESSON) {
            Button(
                onClick = viewModel::next,
                enabled = state.canNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_start_exercises),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::validate,
                    enabled = canValidate,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_validate),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = viewModel::next,
                    enabled = state.canNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_next),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun hasSelection(state: RunnerUiState, payload: ActivityPayload): Boolean = when (payload) {
    is ActivityPayload.Mcq -> state.selectedOptions.isNotEmpty()
    is ActivityPayload.TrueFalse -> state.trueFalse != null
    is ActivityPayload.Writing -> state.writing.isNotBlank()
    is ActivityPayload.Matching -> state.matched.isNotEmpty() || state.activeLeft != null
    is ActivityPayload.Sorting -> false
    is ActivityPayload.LessonContent -> false
}

@Composable
private fun HeaderCard(
    gender: MascotGender,
    mood: MascotMood,
    position: Int,
    total: Int,
    activity: LessonActivity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentMascot(gender = gender, mood = mood, size = 72.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.activity_index, position, total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (position - 1).coerceAtLeast(0).toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyChip(difficulty = activity.difficulty)
                    Text(
                        text = activityTypeName(activity.type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun McqOptions(
    payload: ActivityPayload.Mcq,
    selected: Set<Int>,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Text(
        text = stringResource(R.string.choose_answer),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    payload.options.forEachIndexed { index, option ->
        val isSelected = index in selected
        val color = ChoicePalette[index % ChoicePalette.size]
        Card(
            onClick = { onSelect(index) },
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) color else Color.White,
                disabledContainerColor = if (isSelected) color else Color.White
            ),
            border = BorderStroke(
                width = if (isSelected) 0.dp else 1.5.dp,
                color = if (isSelected) color else Color(0xFFD5DBD6)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LetterBadges.getOrElse(index) { "•" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else color
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrueFalseOptions(
    selected: Boolean?,
    enabled: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrueFalseButton(
            text = stringResource(R.string.true_label),
            selected = selected == true,
            accent = DifficultyEasy,
            enabled = enabled,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
        TrueFalseButton(
            text = stringResource(R.string.false_label),
            selected = selected == false,
            accent = DifficultyAdvanced,
            enabled = enabled,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrueFalseButton(
    text: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent else Color.White,
            disabledContainerColor = if (selected) accent else Color.White
        ),
        border = BorderStroke(
            width = if (selected) 0.dp else 2.dp,
            color = if (selected) accent else accent.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else accent
            )
        }
    }
}

@Composable
private fun WritingInput(
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(stringResource(R.string.hint_write_answer)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SortingList(
    items: List<String>,
    enabled: Boolean,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    Text(
        text = stringResource(R.string.sort_instruction),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    items.forEachIndexed { index, item ->
        val color = ChoicePalette[index % ChoicePalette.size]
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onMove(index, index - 1) },
                    enabled = enabled && index > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (enabled && index > 0) color else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = { onMove(index, index + 1) },
                    enabled = enabled && index < items.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (enabled && index < items.lastIndex) color else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private enum class CapsuleState { IDLE, ACTIVE, MATCHED }

@Composable
private fun MatchingColumns(
    activityId: Long,
    pairs: List<Pair<String, String>>,
    matched: Map<String, String>,
    activeLeft: String?,
    enabled: Boolean,
    onSelectLeft: (String) -> Unit,
    onSelectRight: (String) -> Unit
) {
    val rightItems = remember(activityId) { pairs.map { it.second }.shuffled() }
    val colorByLeft = remember(activityId) {
        pairs.mapIndexed { index, pair -> pair.first to ChoicePalette[index % ChoicePalette.size] }.toMap()
    }

    var containerPos by remember { mutableStateOf(Offset.Zero) }
    val leftBounds = remember { mutableStateMapOf<String, Rect>() }
    val rightBounds = remember { mutableStateMapOf<String, Rect>() }

    Text(
        text = stringResource(R.string.match_instruction),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerPos = it.positionInRoot() }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pairs.forEach { (left, _) ->
                    val color = colorByLeft[left] ?: ChoicePalette.first()
                    val pairedRight = matched[left]
                    MatchCapsule(
                        text = left,
                        color = color,
                        state = when {
                            pairedRight != null -> CapsuleState.MATCHED
                            activeLeft == left -> CapsuleState.ACTIVE
                            else -> CapsuleState.IDLE
                        },
                        enabled = enabled,
                        onClick = { onSelectLeft(left) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                leftBounds[left] = Rect(coords.positionInRoot(), coords.size.toSize())
                            }
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rightItems.forEach { right ->
                    val used = right in matched.values
                    val owner = matched.entries.firstOrNull { it.value == right }?.key
                    val color = owner?.let { colorByLeft[it] } ?: Color(0xFF9AA5A0)
                    MatchCapsule(
                        text = right,
                        color = color,
                        state = if (used) CapsuleState.MATCHED else CapsuleState.IDLE,
                        enabled = enabled,
                        onClick = { onSelectRight(right) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                rightBounds[right] = Rect(coords.positionInRoot(), coords.size.toSize())
                            }
                    )
                }
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            matched.forEach { (left, right) ->
                val leftRect = leftBounds[left] ?: return@forEach
                val rightRect = rightBounds[right] ?: return@forEach
                val color = colorByLeft[left] ?: return@forEach
                drawLine(
                    color = color,
                    start = Offset(leftRect.left, leftRect.center.y) - containerPos,
                    end = Offset(rightRect.right, rightRect.center.y) - containerPos,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun MatchCapsule(
    text: String,
    color: Color,
    state: CapsuleState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                CapsuleState.MATCHED -> color.copy(alpha = 0.16f)
                CapsuleState.ACTIVE -> color.copy(alpha = 0.22f)
                CapsuleState.IDLE -> Color.White
            },
            disabledContainerColor = when (state) {
                CapsuleState.MATCHED -> color.copy(alpha = 0.16f)
                CapsuleState.IDLE -> Color.White
                else -> color.copy(alpha = 0.22f)
            }
        ),
        border = BorderStroke(
            width = if (state == CapsuleState.IDLE) 1.5.dp else 3.dp,
            color = if (state == CapsuleState.IDLE) Color(0xFFD5DBD6) else color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (state == CapsuleState.IDLE) 1.dp else 2.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (state == CapsuleState.IDLE) MaterialTheme.colorScheme.onSurface else color
            )
        }
    }
}

@Composable
private fun FeedbackBanner(feedback: RunnerFeedback, gender: MascotGender) {
    when (feedback) {
        RunnerFeedback.CORRECT -> Surface(
            color = DifficultyEasy.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentMascot(gender = gender, mood = MascotMood.HAPPY, size = 56.dp)
                Text(
                    text = stringResource(R.string.correct_msg),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DifficultyEasy
                )
            }
        }

        RunnerFeedback.WRONG -> Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudentMascot(gender = gender, mood = MascotMood.SAD, size = 56.dp)
                Text(
                    text = stringResource(R.string.wrong_msg),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HintPanel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.hint_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SolutionPanel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.solution_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
