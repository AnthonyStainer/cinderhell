package dev.cinderhell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object CinderhellPalette {
    val Void = Color(0xFF090807)
    val Soot = Color(0xFF12100E)
    val Iron = Color(0xFF211C18)
    val RaisedIron = Color(0xFF302720)
    val Ember = Color(0xFFFF9D16)
    val EmberBright = Color(0xFFFFBC55)
    val EmberDeep = Color(0xFF9F4515)
    val Ash = Color(0xFFFFF0DC)
    val MutedAsh = Color(0xFFCDBDAA)
    val CoalLine = Color(0xFF5A483B)
    val Success = Color(0xFF70D6A2)
    val Warning = Color(0xFFFFC15A)
    val Error = Color(0xFFFF837A)
    val Info = Color(0xFF83C9FF)
}

internal object CinderhellSpacing {
    val PageHorizontal = 28.dp
    val PageVertical = 18.dp
    val Section = 18.dp
    val Card = 16.dp
    val Element = 10.dp
}

private val CinderhellColors = darkColorScheme(
    primary = CinderhellPalette.Ember,
    onPrimary = CinderhellPalette.Void,
    primaryContainer = CinderhellPalette.EmberDeep,
    onPrimaryContainer = CinderhellPalette.Ash,
    secondary = CinderhellPalette.RaisedIron,
    onSecondary = CinderhellPalette.Ash,
    secondaryContainer = CinderhellPalette.Iron,
    onSecondaryContainer = CinderhellPalette.MutedAsh,
    tertiary = CinderhellPalette.EmberBright,
    background = CinderhellPalette.Void,
    onBackground = CinderhellPalette.Ash,
    surface = CinderhellPalette.Soot,
    onSurface = CinderhellPalette.Ash,
    surfaceVariant = CinderhellPalette.Iron,
    onSurfaceVariant = CinderhellPalette.MutedAsh,
    outline = CinderhellPalette.CoalLine,
    error = CinderhellPalette.Error,
    onError = CinderhellPalette.Void,
)

private val CinderhellTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.5.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

private val CinderhellShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

@Composable
fun CinderhellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinderhellColors,
        typography = CinderhellTypography,
        shapes = CinderhellShapes,
        content = content,
    )
}
