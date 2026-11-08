package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeuCodeDarkColorScheme = darkColorScheme(
  primary = NeuCodePurplePrimary,
  onPrimary = Color.White,
  primaryContainer = NeuCodePurpleDeep,
  onPrimaryContainer = NeuCodeSoftLavender,
  secondary = NeuCodeElectricViolet,
  onSecondary = Color.White,
  secondaryContainer = NeuCodePurpleContainer,
  onSecondaryContainer = NeuCodeTextPrimary,
  tertiary = NeuCodeSoftLavender,
  onTertiary = NeuCodeBg,
  background = NeuCodeBg,
  onBackground = NeuCodeTextPrimary,
  surface = NeuCodeBgSecondary,
  onSurface = NeuCodeTextPrimary,
  surfaceVariant = NeuCodeSurfacePrimary,
  onSurfaceVariant = NeuCodeTextSecondary,
  outline = NeuCodeBorder,
  outlineVariant = NeuCodeBorderActive,
  error = NeuCodeError,
  onError = Color.White,
  errorContainer = NeuCodeErrorBg,
  onErrorContainer = NeuCodeError
)

val LightColorScheme = NeuCodeDarkColorScheme
val DarkColorScheme = NeuCodeDarkColorScheme
val UltronDarkColorScheme = NeuCodeDarkColorScheme

/**
 * NEU\CODE Theme - Advanced AI Developer Environment
 */
@Composable
fun NeuCodeTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = NeuCodeDarkColorScheme,
    typography = ClaudeCodeTypography,
    content = content
  )
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
