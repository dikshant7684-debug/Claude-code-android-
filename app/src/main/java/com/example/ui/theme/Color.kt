package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = compositionLocalOf { true }

// =========================================================================
// NEU\CODE NEBULA PURPLE + CHARCOAL BLACK & CLEAN LIGHT DESIGN SYSTEM
// =========================================================================

// RAW DARK PALETTE TOKENS
val NeuCodeBgDark = Color(0xFF09090D)
val NeuCodeBgSecondaryDark = Color(0xFF101014)
val NeuCodeBgTertiaryDark = Color(0xFF17151E)
val NeuCodeSurfacePrimaryDark = Color(0xFF181720)
val NeuCodeSurfaceSecondaryDark = Color(0xFF201D29)
val NeuCodeSurfaceElevatedDark = Color(0xFF282333)
val NeuCodeTextPrimaryDark = Color(0xFFF5F3FA)
val NeuCodeTextSecondaryDark = Color(0xFFAAA4B8)
val NeuCodeTextMutedDark = Color(0xFF777181)
val NeuCodeBorderDark = Color(0xFF302B3A)
val NeuCodeBorderActiveDark = Color(0xFF6941A5)

// RAW LIGHT PALETTE TOKENS
val NeuCodeBgLight = Color(0xFFF8F7FC)
val NeuCodeBgSecondaryLight = Color(0xFFFFFFFF)
val NeuCodeBgTertiaryLight = Color(0xFFF1EFF9)
val NeuCodeSurfacePrimaryLight = Color(0xFFFFFFFF)
val NeuCodeSurfaceSecondaryLight = Color(0xFFF3F0FA)
val NeuCodeSurfaceElevatedLight = Color(0xFFEDE9F6)
val NeuCodeTextPrimaryLight = Color(0xFF161324)
val NeuCodeTextSecondaryLight = Color(0xFF5D5770)
val NeuCodeTextMutedLight = Color(0xFF867E9C)
val NeuCodeBorderLight = Color(0xFFE2DEF2)
val NeuCodeBorderActiveLight = Color(0xFF8B5CF6)

// NEBULA PURPLE ACCENTS (Consistent brand accents across both themes)
val NeuCodePurplePrimary = Color(0xFF8B5CF6)     // Primary Purple: #8B5CF6
val NeuCodeElectricViolet = Color(0xFFA78BFA)    // Electric Violet: #A78BFA
val NeuCodePurpleDeep = Color(0xFF5B21B6)        // Deep Purple: #5B21B6
val NeuCodeSoftLavender = Color(0xFFC4B5FD)      // Soft Lavender: #C4B5FD
val NeuCodeMutedPurple = Color(0xFF6D4CB3)       // Muted Purple: #6D4CB3
val NeuCodePurpleContainer = Color(0xFF251B38)   // Subtle purple container
val NeuCodeAmbientGlow = Color(0x228B5CF6)       // Ambient purple glow

// STATUS COLORS
val NeuCodeSuccess = Color(0xFF4ADE80)           // Success: #4ADE80
val NeuCodeSuccessBg = Color(0xFF122818)
val NeuCodeWarning = Color(0xFFFBBF24)           // Warning: #FBBF24
val NeuCodeWarningBg = Color(0xFF2D230D)
val NeuCodeError = Color(0xFFF87171)             // Error: #F87171
val NeuCodeErrorBg = Color(0xFF301518)
val NeuCodeInfo = Color(0xFF60A5FA)              // Information: #60A5FA
val NeuCodeInfoBg = Color(0xFF142033)

val NeuCodeLavender = NeuCodeSoftLavender
val NeuCodeStatusSuccess = NeuCodeSuccess
val NeuCodeStatusWarning = NeuCodeWarning
val NeuCodeStatusError = NeuCodeError

// INTELLIGENCE CORE COLORS
val NeuCoreCenter = NeuCodeElectricViolet
val NeuCoreGlow = NeuCodePurplePrimary
val NeuCoreRingOuter = NeuCodePurpleDeep
val NeuCoreRingInner = NeuCodeMutedPurple

// DYNAMIC COMPOSE THEME GETTERS (Work across all pages and adapt to Theme toggle)
val NeuCodeBg: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBgDark else NeuCodeBgLight

val NeuCodeBgSecondary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBgSecondaryDark else NeuCodeBgSecondaryLight

val NeuCodeBgTertiary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBgTertiaryDark else NeuCodeBgTertiaryLight

val NeuCodeSurfacePrimary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeSurfacePrimaryDark else NeuCodeSurfacePrimaryLight

val NeuCodeSurfaceSecondary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeSurfaceSecondaryDark else NeuCodeSurfaceSecondaryLight

val NeuCodeSurfaceElevated: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeSurfaceElevatedDark else NeuCodeSurfaceElevatedLight

val NeuCodeTextPrimary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextPrimaryDark else NeuCodeTextPrimaryLight

val NeuCodeTextSecondary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextSecondaryDark else NeuCodeTextSecondaryLight

val NeuCodeTextMuted: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextMutedDark else NeuCodeTextMutedLight

val NeuCodeBorder: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBorderDark else NeuCodeBorderLight

val NeuCodeBorderActive: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBorderActiveDark else NeuCodeBorderActiveLight

// USER & ASSISTANT BUBBLE ACCENTS
val NeuUserBubble: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF1F1A2E) else Color(0xFFEDE9FE)

val NeuUserBubbleBorder: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBorderActiveDark else Color(0xFFC4B5FD)

// CLAUDE / WORKSPACE DYNAMIC THEME TOKENS
val ClaudeWarmBg: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBgDark else NeuCodeBgLight

val ClaudePureWhite: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeSurfacePrimaryDark else NeuCodeSurfacePrimaryLight

val ClaudeSurfaceSubtle: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBgSecondaryDark else NeuCodeSurfaceSecondaryLight

val ClaudeBorder: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBorderDark else NeuCodeBorderLight

val ClaudeBorderLight: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeBorderActiveDark else NeuCodeBorderLight

val ClaudeTextPrimary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextPrimaryDark else NeuCodeTextPrimaryLight

val ClaudeTextSecondary: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextSecondaryDark else NeuCodeTextSecondaryLight

val ClaudeTextMuted: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeTextMutedDark else NeuCodeTextMutedLight

val ClaudeTerracotta = NeuCodePurplePrimary
val ClaudeTerracottaDark = NeuCodePurpleDeep
val ClaudeTerracottaLight = NeuCodeElectricViolet
val ClaudeTerracottaContainer = NeuCodePurpleContainer
val ClaudeSuccess = NeuCodeSuccess
val ClaudeSuccessBg = NeuCodeSuccessBg
val ClaudeWarning = NeuCodeWarning
val ClaudeWarningBg = NeuCodeWarningBg
val ClaudeError = NeuCodeError
val ClaudeErrorBg = NeuCodeErrorBg

val ClaudeTerminalBg: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF0C0B12) else Color(0xFF1E1B29)
val ClaudeTerminalSurface: Color
  @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) NeuCodeSurfacePrimaryDark else Color(0xFF282438)
val ClaudeTerminalText: Color
  @Composable @ReadOnlyComposable get() = Color(0xFFF5F3FA)

// SYNTAX HIGHLIGHTING
val SyntaxKeyword = NeuCodeElectricViolet
val SyntaxFunction = NeuCodeSoftLavender
val SyntaxString = Color(0xFF86EFAC)
val SyntaxComment: Color
  @Composable @ReadOnlyComposable get() = NeuCodeTextMuted
val SyntaxNumber = Color(0xFFFDE047)
val SyntaxType = Color(0xFF93C5FD)

// BACKWARDS-COMPATIBILITY TOKENS
val UltronBackground: Color @Composable @ReadOnlyComposable get() = NeuCodeBg
val UltronBackgroundSecondary: Color @Composable @ReadOnlyComposable get() = NeuCodeBgSecondary
val UltronCard: Color @Composable @ReadOnlyComposable get() = NeuCodeSurfacePrimary
val UltronRedPrimary = NeuCodePurplePrimary
val UltronRedAccent = NeuCodeElectricViolet
val UltronRedDeep = NeuCodePurpleDeep
val UltronTextPrimary: Color @Composable @ReadOnlyComposable get() = NeuCodeTextPrimary
val UltronTextSecondary: Color @Composable @ReadOnlyComposable get() = NeuCodeTextSecondary
val UltronBorder: Color @Composable @ReadOnlyComposable get() = NeuCodeBorder
val UltronBorderAccent: Color @Composable @ReadOnlyComposable get() = NeuCodeBorderActive
val UltronGlassPanel: Color @Composable @ReadOnlyComposable get() = NeuCodeSurfaceSecondary
val UltronRedAmbient = NeuCodeAmbientGlow
val UltronCoreCenter = NeuCoreCenter
val UltronCoreRing = NeuCoreRingOuter

val DarkBackground = NeuCodeBgDark
val DarkSurface = NeuCodeBgSecondaryDark
val DarkSurfaceVariant = NeuCodeSurfacePrimaryDark
val DarkOnBackground = NeuCodeTextPrimaryDark
val DarkOnSurface = NeuCodeTextPrimaryDark
val DarkOnSurfaceVariant = NeuCodeTextSecondaryDark
val DarkOutline = NeuCodeBorderDark
val DarkOutlineVariant = NeuCodeBorderActiveDark
val DarkUserBubble = Color(0xFF1F1A2E)
val DarkUserBubbleBorder = NeuCodeBorderActiveDark

val LightBackground = NeuCodeBgLight
val LightSurface = NeuCodeBgSecondaryLight
val LightSurfaceVariant = NeuCodeSurfacePrimaryLight
val LightOnBackground = NeuCodeTextPrimaryLight
val LightOnSurface = NeuCodeTextPrimaryLight
val LightOnSurfaceVariant = NeuCodeTextSecondaryLight
val LightOutline = NeuCodeBorderLight
val LightOutlineVariant = NeuCodeBorderActiveLight
val LightUserBubble = Color(0xFFEDE9FE)
val LightUserBubbleBorder = Color(0xFFC4B5FD)

val DroidOrange = NeuCodePurplePrimary
val DroidOrangeDark = NeuCodePurpleDeep
val DroidOrangeLight = NeuCodeElectricViolet
val DroidOrangeContainer = NeuCodePurpleContainer
val DroidOrangeDisabled = Color(0xFF433D52)
val DroidOrangeSubtle = Color(0xFF1C1626)

val ColorSuccess = NeuCodeSuccess
val ColorSuccessBg = NeuCodeSuccessBg
val ColorWarning = NeuCodeWarning
val ColorWarningBg = NeuCodeWarningBg
val ColorError = NeuCodeError
val ColorErrorBg = NeuCodeErrorBg
