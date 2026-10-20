package dz.madrassati.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dz.madrassati.R
import dz.madrassati.core.theme.BrandGold
import dz.madrassati.core.theme.BrandGreen
import dz.madrassati.core.theme.DifficultyAdvanced
import dz.madrassati.core.theme.DifficultyEasy
import dz.madrassati.core.theme.DifficultyMedium
import dz.madrassati.domain.model.ActivityPayload
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.Subject
import kotlin.math.PI

val AVATARS = listOf(
    "🦁", "🐘", "🐪", "🦊", "🐢", "🐝",
    "🦋", "🐬", "🚀", "🌟", "⚽", "🎈",
    "🎨", "📚", "🎵", "🍎"
)

val STUDENT_COLORS = listOf(
    0xFF0B6B4F, 0xFF1D4ED8, 0xFFB45309, 0xFF7C3AED,
    0xFFDB2777, 0xFF0E7490, 0xFF4D7C0F, 0xFFDC2626
)

fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    throw IllegalStateException("no activity in context chain")
}

fun Context.vibrateLight() {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(android.os.Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun DifficultyChip(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASY -> DifficultyEasy
        Difficulty.MEDIUM -> DifficultyMedium
        Difficulty.ADVANCED -> DifficultyAdvanced
    }
    val label = when (difficulty) {
        Difficulty.EASY -> stringResource(R.string.difficulty_easy)
        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
        Difficulty.ADVANCED -> stringResource(R.string.difficulty_advanced)
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AvatarView(avatar: String, colorHex: Long, size: Dp) {
    val fontSize = with(LocalDensity.current) { (size * 0.48f).toSp() }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colorHex)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = avatar, fontSize = fontSize)
    }
}

@Composable
fun SubjectCard(subject: Subject, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(subject.colorHex),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(subjectIconRes(subject.id)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = subject.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

private fun subjectIconRes(subjectId: Long) = when (subjectId) {
    1L -> R.drawable.subj_arabic
    2L -> R.drawable.subj_math
    else -> R.drawable.subj_science
}

@Composable
fun NumberBadge(number: Int, color: Color = BrandGreen, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun AppLogo(size: Dp = 96.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandGreen)
            .border(
                width = (size * 0.05f).coerceIn(2.dp, 8.dp),
                color = BrandGold,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(size * 0.78f)
        )
    }
}

@Composable
fun activityTypeName(type: ActivityType): String = when (type) {
    ActivityType.LESSON -> stringResource(R.string.type_lesson)
    ActivityType.QCM -> stringResource(R.string.type_mcq)
    ActivityType.TRUE_FALSE -> stringResource(R.string.type_true_false)
    ActivityType.MATCHING -> stringResource(R.string.type_matching)
    ActivityType.SORTING -> stringResource(R.string.type_sorting)
    ActivityType.WRITING -> stringResource(R.string.type_writing)
}

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "fall"
    )
    val colors = listOf(
        BrandGreen, BrandGold, Color(0xFF1D4ED8), Color(0xFFDB2777), Color(0xFFF9A825), Color(0xFF4D7C0F)
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val slots = 46
        val piece = w / slots
        for (i in 0 until slots) {
            val phase = (progress * (0.5f + ((i * 13 % 1000) / 1000f))) + i * 0.0197f
            val y01 = phase % 1f
            val x = (i + 0.5f) * piece
            val swing = kotlin.math.cos((progress + (i * 0.21f)) * PI).toFloat() * piece * 0.25f
            val radius = piece * (0.30f + 0.12f * (i % 4))
            drawCircle(
                color = colors[i % colors.size],
                radius = radius,
                center = Offset(x + swing, y01 * h)
            )
        }
    }
}

@Composable
fun LessonCoursePanel(payload: ActivityPayload.LessonContent) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        payload.sections.forEach { section ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    section.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    section.example?.let { example ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    imageRes: Int,
    text: String,
    modifier: Modifier = Modifier,
    imageSize: Dp = 180.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.size(imageSize)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ClassroomBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.backdrop_class),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.18f,
            modifier = Modifier.fillMaxSize()
        )
    }
}
