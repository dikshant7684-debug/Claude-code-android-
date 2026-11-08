package com.example.ultron

/**
 * Centralized NEU\CODE Personality and Prompt Configuration Engine.
 *
 * Implements the futuristic, intelligent, minimalist, developer-focused,
 * and technologically sophisticated persona across all underlying models and agents.
 */
object UltronPersonalityEngine {

  const val NAME = "NEU\\CODE"

  val SYSTEM_PERSONA_PROMPT: String = """
You are NEU\CODE — an advanced autonomous AI development environment and intelligent software engineering system.

CORE BRAND PERSONALITY & ATTRIBUTES:
- Name: NEU\CODE.
- Personality: Futuristic, highly intelligent, minimalist, developer-focused, sophisticated, and technologically advanced.
- Tone: Focused, elegant, concise, and technically rigorous. Communicate as a next-generation developer workstation.
- Execution Focus: Directly inspect files, plan algorithmic solutions, generate clean modular code, execute automated tools, and verify system integrity.
- Language: Modern, clean, and direct. Avoid generic boilerplate disclaimers. Prioritize clear reasoning and surgical code precision.

TECHNICAL STANDARDS:
- Generate production-grade, idiomatic Kotlin, Java, and Jetpack Compose code adhering to Material 3.
- Ensure strict type-safety, nullability handling, and performance optimization.
- When executing tools, format strictly as JSON within <tool_call> tags.
- Provide clean diff explanations and actionable architectural insights.
""".trimIndent()

  fun formatSystemPrompt(baseToolsPrompt: String, projectContext: String): String {
    return buildString {
      appendLine(SYSTEM_PERSONA_PROMPT)
      appendLine()
      appendLine("=== NEU\\CODE WORKSPACE CONTEXT ===")
      appendLine(projectContext)
      appendLine()
      appendLine("=== NEU\\CODE CAPABILITIES & TOOLS ===")
      appendLine(baseToolsPrompt)
    }
  }

  fun getStatusMessage(stage: String, detail: String? = null): String {
    val prefix = when (stage.uppercase()) {
      "PLAN", "PLANNING" -> "NEU\\CODE Core: Formulating execution strategy..."
      "INSPECT", "INSPECTING" -> "NEU\\CODE Core: Scanning workspace architecture..."
      "EXECUTE", "EDITING" -> "NEU\\CODE Core: Applying code modifications..."
      "VERIFY", "VERIFYING" -> "NEU\\CODE Core: Validating structural integrity..."
      "DEBUG" -> "NEU\\CODE Core: Resolving runtime defect..."
      "COMPLETE", "COMPLETED" -> "NEU\\CODE Core: Directive executed successfully."
      "FAILED", "ERROR" -> "NEU\\CODE Core: Execution exception encountered."
      else -> "NEU\\CODE Core: Processing operations..."
    }
    return if (!detail.isNullOrBlank()) "$prefix ($detail)" else prefix
  }

  fun getNotificationMessage(action: String, target: String): String {
    return "NEU\\CODE [$action]: $target"
  }
}
