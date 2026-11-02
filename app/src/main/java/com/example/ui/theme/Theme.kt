package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AMediaDarkScheme = darkColorScheme(
  primary = AMediaCoral,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF3E132B),
  onPrimaryContainer = Color(0xFFFFD9E2),
  secondary = AMediaCyan,
  onSecondary = Color(0xFF002020),
  secondaryContainer = Color(0xFF0E343E),
  onSecondaryContainer = Color(0xFFB3F6F5),
  tertiary = AMediaViolet,
  onTertiary = Color.White,
  background = DarkBg,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceMuted,
  error = Color(0xFFEF4444),
  onError = Color.White,
)

private val AMediaLightScheme = lightColorScheme(
  primary = AMediaCoral,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFE4EC),
  onPrimaryContainer = Color(0xFF650022),
  secondary = Color(0xFF0096C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F7FA),
  onSecondaryContainer = Color(0xFF00363D),
  tertiary = AMediaViolet,
  onTertiary = Color.White,
  background = LightBg,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceMuted,
  error = Color(0xFFDC2626),
  onError = Color.White,
)

val AMediaShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
)

@Composable
fun AMediaTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  MyApplicationTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AMediaDarkScheme else AMediaLightScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AMediaShapes,
    content = content,
  )
}
