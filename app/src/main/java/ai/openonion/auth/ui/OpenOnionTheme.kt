package ai.openonion.auth.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OpenOnionColors = lightColorScheme(
    primary = Color(0xFF9A4F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDBA),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF6E5B45),
    background = Color(0xFFF7F2E8),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFEFE4D7),
    onSurface = Color(0xFF211A14),
)

@Composable
fun OpenOnionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OpenOnionColors,
        content = content,
    )
}
