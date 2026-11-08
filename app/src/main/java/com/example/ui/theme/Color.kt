package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// NEU\CODE NEBULA PURPLE + CHARCOAL BLACK DESIGN SYSTEM
// =========================================================================

// PRIMARY BACKGROUNDS
val NeuCodeBg = Color(0xFF09090D)                // Main Background: #09090D
val NeuCodeBgSecondary = Color(0xFF101014)       // Secondary Background: #101014
val NeuCodeBgTertiary = Color(0xFF17151E)        // Tertiary Background: #17151E

// SURFACE COLORS
val NeuCodeSurfacePrimary = Color(0xFF181720)    // Surface Primary: #181720
val NeuCodeSurfaceSecondary = Color(0xFF201D29)  // Surface Secondary: #201D29
val NeuCodeSurfaceElevated = Color(0xFF282333)   // Elevated Surface: #282333

// NEBULA PURPLE ACCENTS
val NeuCodePurplePrimary = Color(0xFF8B5CF6)     // Primary Purple: #8B5CF6
val NeuCodeElectricViolet = Color(0xFFA78BFA)    // Electric Violet: #A78BFA
val NeuCodePurpleDeep = Color(0xFF5B21B6)        // Deep Purple: #5B21B6
val NeuCodeSoftLavender = Color(0xFFC4B5FD)      // Soft Lavender: #C4B5FD
val NeuCodeMutedPurple = Color(0xFF6D4CB3)       // Muted Purple: #6D4CB3
val NeuCodePurpleContainer = Color(0xFF251B38)   // Subtle purple container
val NeuCodeAmbientGlow = Color(0x228B5CF6)       // Ambient purple glow

// TEXT COLORS
val NeuCodeTextPrimary = Color(0xFFF5F3FA)       // Primary Text: #F5F3FA
val NeuCodeTextSecondary = Color(0xFFAAA4B8)     // Secondary Text: #AAA4B8
val NeuCodeTextMuted = Color(0xFF777181)         // Muted Text: #777181

// BORDERS
val NeuCodeBorder = Color(0xFF302B3A)            // Default: #302B3A
val NeuCodeBorderActive = Color(0xFF6941A5)      // Active: #6941A5

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

// USER & ASSISTANT BUBBLE ACCENTS
val NeuUserBubble = Color(0xFF1F1A2E)
val NeuUserBubbleBorder = NeuCodeBorderActive

// SYNTAX HIGHLIGHTING (Nebula Violet & Cosmic Accents)
val SyntaxKeyword = NeuCodeElectricViolet
val SyntaxFunction = NeuCodeSoftLavender
val SyntaxString = Color(0xFF86EFAC)
val SyntaxComment = NeuCodeTextMuted
val SyntaxNumber = Color(0xFFFDE047)
val SyntaxType = Color(0xFF93C5FD)

// =========================================================================
// BACKWARDS-COMPATIBILITY TOKENS MAPPED TO NEU\CODE
// =========================================================================
val UltronBackground = NeuCodeBg
val UltronBackgroundSecondary = NeuCodeBgSecondary
val UltronCard = NeuCodeSurfacePrimary
val UltronRedPrimary = NeuCodePurplePrimary
val UltronRedAccent = NeuCodeElectricViolet
val UltronRedDeep = NeuCodePurpleDeep
val UltronTextPrimary = NeuCodeTextPrimary
val UltronTextSecondary = NeuCodeTextSecondary
val UltronBorder = NeuCodeBorder
val UltronBorderAccent = NeuCodeBorderActive
val UltronGlassPanel = NeuCodeSurfaceSecondary
val UltronRedAmbient = NeuCodeAmbientGlow
val UltronCoreCenter = NeuCoreCenter
val UltronCoreRing = NeuCoreRingOuter

val DarkBackground = NeuCodeBg
val DarkSurface = NeuCodeBgSecondary
val DarkSurfaceVariant = NeuCodeSurfacePrimary
val DarkOnBackground = NeuCodeTextPrimary
val DarkOnSurface = NeuCodeTextPrimary
val DarkOnSurfaceVariant = NeuCodeTextSecondary
val DarkOutline = NeuCodeBorder
val DarkOutlineVariant = NeuCodeBorderActive
val DarkUserBubble = NeuUserBubble
val DarkUserBubbleBorder = NeuUserBubbleBorder

val LightBackground = NeuCodeBg
val LightSurface = NeuCodeBgSecondary
val LightSurfaceVariant = NeuCodeSurfacePrimary
val LightOnBackground = NeuCodeTextPrimary
val LightOnSurface = NeuCodeTextPrimary
val LightOnSurfaceVariant = NeuCodeTextSecondary
val LightOutline = NeuCodeBorder
val LightOutlineVariant = NeuCodeBorderActive
val LightUserBubble = NeuUserBubble
val LightUserBubbleBorder = NeuUserBubbleBorder

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

val ClaudeWarmBg = NeuCodeBg
val ClaudePureWhite = NeuCodeSurfacePrimary
val ClaudeSurfaceSubtle = NeuCodeBgSecondary
val ClaudeBorder = NeuCodeBorder
val ClaudeBorderLight = NeuCodeBorderActive
val ClaudeTextPrimary = NeuCodeTextPrimary
val ClaudeTextSecondary = NeuCodeTextSecondary
val ClaudeTextMuted = NeuCodeTextMuted
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
val ClaudeTerminalBg = Color(0xFF0C0B12)
val ClaudeTerminalSurface = NeuCodeSurfacePrimary
val ClaudeTerminalText = NeuCodeTextPrimary
