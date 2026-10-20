@file:OptIn(ExperimentalTextApi::class)

package dz.madrassati.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dz.madrassati.R

val BrandGreen = Color(0xFF0B6B4F)
val BrandGreenDark = Color(0xFF053B2A)
val BrandGreenContainer = Color(0xFFD7F0E4)
val BrandGold = Color(0xFFF4C430)
val BrandGoldContainer = Color(0xFFFFF2C6)
val BrandBackground = Color(0xFFF7F8F4)
val BrandInk = Color(0xFF171D1A)

val BrandGradient = Brush.linearGradient(listOf(BrandGreen, BrandGreenDark))
val GoldGradient = Brush.linearGradient(listOf(Color(0xFFF7D774), BrandGold))

val DifficultyEasy = Color(0xFF2E7D32)
val DifficultyMedium = Color(0xFFF9A825)
val DifficultyAdvanced = Color(0xFFC62828)

private val Cairo = FontFamily(
    Font(R.font.cairo, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 400f))),
    Font(R.font.cairo, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 500f))),
    Font(R.font.cairo, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 600f))),
    Font(R.font.cairo, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 700f)))
)

val CairoFontFamily: FontFamily get() = Cairo

private val MadrassatiColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGold,
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = BrandGoldContainer,
    onSecondaryContainer = Color(0xFF3E2E00),
    background = BrandBackground,
    onBackground = BrandInk,
    surface = Color.White,
    onSurface = BrandInk,
    surfaceVariant = Color(0xFFE4E9E2),
    onSurfaceVariant = Color(0xFF414943),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF707973)
)

private val MadrassatiTypography = Typography(
    displayLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

private val MadrassatiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MadrassatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MadrassatiColorScheme,
        typography = MadrassatiTypography,
        shapes = MadrassatiShapes,
        content = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        }
    )
}