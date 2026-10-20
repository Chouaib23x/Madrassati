package dz.madrassati.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dz.madrassati.core.theme.BrandGold
import dz.madrassati.core.theme.BrandGreen

enum class MascotGender { BOY, GIRL }

enum class MascotMood { NEUTRAL, THINKING, HAPPY, SAD }

private val Skin = Color(0xFFF2C9A0)
private val SkinShade = Color(0xFFE2B58C)
private val HairBoy = Color(0xFF3B2A20)
private val HairGirl = Color(0xFF5A3A2A)
private val EyeInk = Color(0xFF2B2B2B)
private val Cheek = Color(0xFFF39AA0)
private val Ribbon = Color(0xFFE85D8A)
private val Tear = Color(0xFF6FB7E8)

@Composable
fun StudentMascot(
    gender: MascotGender,
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val pop by animateFloatAsState(
        targetValue = when (mood) {
            MascotMood.HAPPY -> 1.07f
            MascotMood.SAD -> 0.96f
            else -> 1f
        },
        animationSpec = spring(),
        label = "mascot-pop"
    )
    Canvas(modifier = modifier.size(size)) {
        scale(pop, pivot = center) {
            drawMascot(gender, mood)
        }
    }
}

private fun DrawScope.drawMascot(gender: MascotGender, mood: MascotMood) {
    val s = size.minDimension
    val cx = s / 2f
    val headR = s * 0.27f
    val headCy = s * 0.42f
    val bodyTop = headCy + headR * 0.95f

    drawRoundRect(
        color = SkinShade,
        topLeft = Offset(cx - s * 0.065f, bodyTop - s * 0.16f),
        size = Size(s * 0.13f, s * 0.22f),
        cornerRadius = CornerRadius(s * 0.05f, s * 0.05f)
    )
    drawRoundRect(
        color = BrandGreen,
        topLeft = Offset(s * 0.19f, bodyTop),
        size = Size(s * 0.62f, s - bodyTop),
        cornerRadius = CornerRadius(s * 0.20f, s * 0.20f)
    )
    val collar = Path().apply {
        moveTo(cx - s * 0.11f, bodyTop + s * 0.005f)
        lineTo(cx + s * 0.11f, bodyTop + s * 0.005f)
        lineTo(cx, bodyTop + s * 0.12f)
        close()
    }
    drawPath(collar, Color.White)
    drawCircle(
        color = BrandGold,
        radius = s * 0.028f,
        center = Offset(cx, bodyTop + s * 0.075f)
    )

    if (gender == MascotGender.GIRL) {
        drawCircle(HairGirl, headR * 0.42f, Offset(cx - headR * 1.25f, headCy + s * 0.05f))
        drawCircle(HairGirl, headR * 0.42f, Offset(cx + headR * 1.25f, headCy + s * 0.05f))
        drawCircle(
            Ribbon,
            headR * 0.16f,
            Offset(cx - headR * 0.95f, headCy - headR * 0.95f)
        )
    }

    drawCircle(Skin, headR, Offset(cx, headCy))

    val hairColor = if (gender == MascotGender.BOY) HairBoy else HairGirl
    drawArc(
        color = hairColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - headR * 1.05f, headCy - headR * 1.05f),
        size = Size(headR * 2.1f, headR * 2.1f)
    )
    if (gender == MascotGender.GIRL) {
        drawArc(
            color = hairColor,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - headR * 1.0f, headCy - headR * 1.02f),
            size = Size(headR * 2.0f, headR * 2.0f),
            style = Stroke(width = s * 0.03f, cap = StrokeCap.Round)
        )
    }

    val eyeDx = s * 0.10f
    val eyeY = headCy - s * 0.005f
    val eyeR = s * 0.030f

    when (mood) {
        MascotMood.HAPPY -> {
            drawHappyEye(cx - eyeDx, eyeY, eyeR, s)
            drawHappyEye(cx + eyeDx, eyeY, eyeR, s)
            drawCircle(Cheek.copy(alpha = 0.55f), s * 0.038f, Offset(cx - s * 0.17f, eyeY + s * 0.075f))
            drawCircle(Cheek.copy(alpha = 0.55f), s * 0.038f, Offset(cx + s * 0.17f, eyeY + s * 0.075f))
            drawArc(
                color = EyeInk,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.13f, eyeY + s * 0.02f),
                size = Size(s * 0.26f, s * 0.17f),
                style = Stroke(width = s * 0.035f, cap = StrokeCap.Round)
            )
        }

        MascotMood.NEUTRAL -> {
            drawCircle(EyeInk, eyeR, Offset(cx - eyeDx, eyeY))
            drawCircle(EyeInk, eyeR, Offset(cx + eyeDx, eyeY))
            drawArc(
                color = EyeInk,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.075f, eyeY + s * 0.055f),
                size = Size(s * 0.15f, s * 0.10f),
                style = Stroke(width = s * 0.026f, cap = StrokeCap.Round)
            )
        }

        MascotMood.THINKING -> {
            drawCircle(EyeInk, eyeR, Offset(cx - eyeDx, eyeY))
            drawCircle(EyeInk, eyeR, Offset(cx + eyeDx, eyeY))
            drawLine(
                color = hairColor,
                start = Offset(cx - eyeDx - eyeR * 1.4f, eyeY - eyeR * 3.4f),
                end = Offset(cx - eyeDx + eyeR * 1.2f, eyeY - eyeR * 2.9f),
                strokeWidth = s * 0.024f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = EyeInk,
                start = Offset(cx - s * 0.06f, eyeY + s * 0.075f),
                end = Offset(cx + s * 0.07f, eyeY + s * 0.075f),
                strokeWidth = s * 0.024f,
                cap = StrokeCap.Round
            )
            drawCircle(EyeInk.copy(alpha = 0.35f), s * 0.018f, Offset(cx + s * 0.24f, eyeY - s * 0.13f))
            drawCircle(EyeInk.copy(alpha = 0.25f), s * 0.012f, Offset(cx + s * 0.30f, eyeY - s * 0.19f))
        }

        MascotMood.SAD -> {
            drawCircle(EyeInk, eyeR * 0.92f, Offset(cx - eyeDx, eyeY))
            drawCircle(EyeInk, eyeR * 0.92f, Offset(cx + eyeDx, eyeY))
            drawLine(
                color = hairColor,
                start = Offset(cx - eyeDx - eyeR * 1.3f, eyeY - eyeR * 2.9f),
                end = Offset(cx - eyeDx + eyeR * 1.3f, eyeY - eyeR * 3.5f),
                strokeWidth = s * 0.024f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = hairColor,
                start = Offset(cx + eyeDx - eyeR * 1.3f, eyeY - eyeR * 3.5f),
                end = Offset(cx + eyeDx + eyeR * 1.3f, eyeY - eyeR * 2.9f),
                strokeWidth = s * 0.024f,
                cap = StrokeCap.Round
            )
            val drop = Path().apply {
                moveTo(cx + eyeDx, eyeY + eyeR * 1.4f)
                quadraticBezierTo(
                    cx + eyeDx + s * 0.030f,
                    eyeY + eyeR * 4.2f,
                    cx + eyeDx,
                    eyeY + eyeR * 4.6f
                )
                quadraticBezierTo(
                    cx + eyeDx - s * 0.030f,
                    eyeY + eyeR * 4.2f,
                    cx + eyeDx,
                    eyeY + eyeR * 1.4f
                )
                close()
            }
            drawPath(drop, Tear)
            drawArc(
                color = EyeInk,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.10f, eyeY + s * 0.09f),
                size = Size(s * 0.20f, s * 0.13f),
                style = Stroke(width = s * 0.030f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawHappyEye(cx: Float, cy: Float, r: Float, s: Float) {
    drawArc(
        color = EyeInk,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - r * 1.2f, cy - r),
        size = Size(r * 2.4f, r * 1.8f),
        style = Stroke(width = s * 0.030f, cap = StrokeCap.Round)
    )
}
