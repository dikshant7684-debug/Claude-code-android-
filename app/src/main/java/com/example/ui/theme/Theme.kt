package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val NeuCodeDarkColorScheme = darkColorScheme(
  primary = NeuCodePurplePrimary,
  onPrimary = Color.White,
  primaryContainer = NeuCodePurpleDeep,
  onPrimaryContainer = NeuCodeSoftLavender,
  secondary = NeuCodeElectricViolet,
  onSecondary = Color.White,
  secondaryContainer = NeuCodePurpleContainer,
  onSecondaryContainer = NeuCodeTextPrimaryDark,
  tertiary = NeuCodeSoftLavender,
  onTertiary = NeuCodeBgDark,
  background = NeuCodeBgDark,
  onBackground = NeuCodeTextPrimaryDark,
  surface = NeuCodeBgSecondaryDark,
  onSurface = NeuCodeTextPrimaryDark,
  surfaceVariant = NeuCodeSurfacePrimaryDark,
  onSurfaceVariant = NeuCodeTextSecondaryDark,
  outline = NeuCodeBorderDark,
  outlineVariant = NeuCodeBorderActiveDark,
  error = NeuCodeError,
  onError = Color.White,
  errorContainer = NeuCodeErrorBg,
  onErrorContainer = NeuCodeError
)

val NeuCodeLightColorScheme = lightColorScheme(
  primary = Color(0xFF7C3AED),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEDE9FE),
  onPrimaryContainer = Color(0xFF5B21B6),
  secondary = Color(0xFF6D28D9),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF3F0FF),
  onSecondaryContainer = Color(0xFF1E1438),
  tertiary = Color(0xFF8B5CF6),
  onTertiary = Color.White,
  background = NeuCodeBgLight,
  onBackground = NeuCodeTextPrimaryLight,
  surface = NeuCodeBgSecondaryLight,
  onSurface = NeuCodeTextPrimaryLight,
  surfaceVariant = NeuCodeSurfacePrimaryLight,
  onSurfaceVariant = NeuCodeTextSecondaryLight,
  outline = NeuCodeBorderLight,
  outlineVariant = Color(0xFFD3CCE8),
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B)
)

val LightColorScheme = NeuCodeLightColorScheme
val DarkColorScheme = NeuCodeDarkColorScheme
val UltronDarkColorScheme = NeuCodeDarkColorScheme

/**
 * NEU\CODE Theme - Advanced AI Developer Environment
 * Supports seamless app-wide light and dark mode toggling.
 */
@Composable
fun NeuCodeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colors = if (darkTheme) NeuCodeDarkColorScheme else NeuCodeLightColorScheme

  CompositionLocalProvider(
    LocalIsDarkTheme provides darkTheme
  ) {
    MaterialTheme(
      colorScheme = colors,
      typography = ClaudeCodeTypography,
      content = content
    )
  }
}

@Composable
fun UltronTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = NeuCodeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

@Composable
fun ClaudeCodeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = NeuCodeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

@Composable
fun DroidCodeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) = NeuCodeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
