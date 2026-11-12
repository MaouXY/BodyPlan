package com.maou.bodyplanapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Pine = Color(0xFF123D36)
val Mint = Color(0xFFD5F3DF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B53), onPrimary = Color.White,
    primaryContainer = Mint, onPrimaryContainer = Pine,
    secondary = Color(0xFFAC4C35), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7DE), onSecondaryContainer = Color(0xFF713322),
    tertiary = Color(0xFF476B91),
    background = Color(0xFFF5F7F6), onBackground = Color(0xFF182C26),
    surface = Color.White, onSurface = Color(0xFF182C26),
    surfaceVariant = Color(0xFFEBF0ED), onSurfaceVariant = Color(0xFF62716B),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFF0F4F1),
    outline = Color(0xFF81948A), outlineVariant = Color(0xFFDEE6E1),
    error = Color(0xFFB43838),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF94DDB6), onPrimary = Pine,
    primaryContainer = Color(0xFF214D3D), onPrimaryContainer = Mint,
    secondary = Color(0xFFF4AF99), secondaryContainer = Color(0xFF573528),
    onSecondaryContainer = Color(0xFFFFDFD3),
    background = Color(0xFF111B17), onBackground = Color(0xFFE4ECE6),
    surface = Color(0xFF19251F), onSurface = Color(0xFFE4ECE6),
    surfaceVariant = Color(0xFF28382F), onSurfaceVariant = Color(0xFFACBEB2),
    surfaceContainer = Color(0xFF19251F), surfaceContainerHigh = Color(0xFF24332B),
    outlineVariant = Color(0xFF35483D),
)

@Composable
fun BodyPlanAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(16.dp),
        ),
        content = content,
    )
}
