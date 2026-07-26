package dev.cinderhell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinderhellColors = darkColorScheme(
    primary = Color(0xFFFFB000),
    secondary = Color(0xFFC77400),
    background = Color(0xFF120E0B),
    surface = Color(0xFF211914),
    onPrimary = Color(0xFF1D1300),
    onBackground = Color(0xFFFFF2E4),
    onSurface = Color(0xFFFFF2E4),
)

@Composable
fun CinderhellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinderhellColors,
        content = content,
    )
}
