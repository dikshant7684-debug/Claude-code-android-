package com.example.agent.tools.impl

import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.ToolCategoryProvider
import com.example.agent.tools.ToolDefinition
import com.example.agent.workspace.WorkspaceManager
import com.example.build.BuildWorkflowManager
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Production-ready Debugging Tools for diagnosing build failures, stack traces,
 * runtime crashes, memory leaks, performance bottlenecks, and automated fixes.
 */
class DebuggingToolProvider(
  private val workspaceManager: WorkspaceManager,
  private val buildManager: BuildWorkflowManager
) : ToolCategoryProvider {

  override val categoryName: String = "Debugging"

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "inspect_error",
      category = categoryName,
      description = "Inspect an error message or analyze current build diagnostics in detail.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("error_text", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "analyze_stack_trace",
      category = categoryName,
      description = "Parse and analyze an exception stack trace to locate the origin and root cause.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("stack_trace") })
        put("properties", JSONObject().apply {
          put("stack_trace", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_build_errors",
      category = categoryName,
      description = "Run compiler checks and detect all build diagnostics, syntax errors, and missing symbols.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "detect_runtime_errors",
      category = categoryName,
      description = "Scan a log snippet or runtime trace for fatal exceptions, NullPointerExceptions, or ANRs.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("log_snippet") })
        put("properties", JSONObject().apply {
          put("log_snippet", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "find_error_location",
      category = categoryName,
      description = "View the exact source code window surrounding an error line with context.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("line_number") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("line_number", JSONObject().apply { put("type", "integer") })
          put("window_size", JSONObject().apply { put("type", "integer"); put("description", "Lines of context (default 10)") })
        })
      }
    ),
    ToolDefinition(
      name = "suggest_fix",
      category = categoryName,
      description = "Suggest automated fixes and resolutions for a specific error or compilation diagnostic.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("error_message") })
        put("properties", JSONObject().apply {
          put("error_message", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "apply_error_fix",
      category = categoryName,
      description = "Automatically apply a suggested patch (e.g. adding missing import or fixing unresolved reference).",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("fix_type") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("fix_type", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("add_import"); put("fix_typo") }) })
          put("symbol", JSONObject().apply { put("type", "string") })
          put("replacement", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "inspect_logs",
      category = categoryName,
      description = "Inspect recent application and agent execution logs.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("filter_tag", JSONObject().apply { put("type", "string") })
          put("max_lines", JSONObject().apply { put("type", "integer") })
        })
      }
    ),
    ToolDefinition(
      name = "diagnose_crash",
      category = categoryName,
      description = "Perform deep diagnostic analysis on an Android crash report or tombstone.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("crash_dump") })
        put("properties", JSONObject().apply {
          put("crash_dump", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_memory_issues",
      category = categoryName,
      description = "Detect potential memory leaks, unclosed streams, or static context holding in source code.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "detect_performance_issues",
      category = categoryName,
      description = "Detect UI thread blocking, expensive operations in composables/loops, and performance bottlenecks.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    )
  )

  override suspend fun execute(
    name: String,
    args: JSONObject,
    project: Project,
    autoApproveWrites: Boolean,
    autoApproveCommands: Boolean
  ): AutonomousToolResult? = withContext(Dispatchers.IO) {
    val root = File(project.rootPath)

    when (name) {
      "inspect_error" -> {
        val errorText = args.optString("error_text")
        if (errorText.isNotBlank()) {
          val diagnosis = diagnoseError(errorText)
          AutonomousToolResult.Success(name, diagnosis)
        } else {
          val buildResult = buildManager.checkAndBuild(project)
          val out = if (buildResult.diagnostics.isEmpty()) {
            "No build diagnostics or compiler errors found in project."
          } else {
            buildResult.diagnostics.joinToString("\n") { "[${it.severity}] ${it.filePath}:${it.line} - ${it.message}" }
          }
          AutonomousToolResult.Success(name, out)
        }
      }

      "analyze_stack_trace" -> {
        val trace = args.optString("stack_trace")
        if (trace.isBlank()) return@withContext AutonomousToolResult.Error(name, "stack_trace is required.")

        val lines = trace.lines().filter { it.isNotBlank() }
        val header = lines.firstOrNull() ?: "Unknown exception"
        val frames = lines.drop(1).filter { it.trim().startsWith("at ") }
        val appFrame = frames.firstOrNull { it.contains("com.example") || (!it.contains("android.") && !it.contains("java.")) }
        val cause = lines.lastOrNull { it.trim().startsWith("Caused by:") }

        val report = buildString {
          appendLine("=== Stack Trace Analysis ===")
          appendLine("Exception Header: $header")
          if (cause != null) appendLine("Root Cause: $cause")
          if (appFrame != null) {
            appendLine("First Application Frame: ${appFrame.trim()}")
            val fileMatch = Regex("""\(([\w.]+):(\d+)\)""").find(appFrame)
            if (fileMatch != null) {
              appendLine("  Source: ${fileMatch.groupValues[1]} at line ${fileMatch.groupValues[2]}")
            }
          }
          appendLine("Total Frames: ${frames.size}")
        }
        AutonomousToolResult.Success(name, report)
      }

      "detect_build_errors" -> {
        val result = buildManager.checkAndBuild(project)
        val text = buildString {
          appendLine("Build Status: ${if (result.isSuccess) "PASSED" else "FAILED"}")
          appendLine(result.message)
          if (result.diagnostics.isNotEmpty()) {
            appendLine("Found ${result.diagnostics.size} issues:")
            result.diagnostics.forEach { diag ->
              appendLine("  • [${diag.severity}] ${diag.filePath}:${diag.line} -> ${diag.message}")
            }
          }
        }
        if (result.isSuccess) AutonomousToolResult.Success(name, text)
        else AutonomousToolResult.Error(name, text)
      }

      "detect_runtime_errors" -> {
        val snippet = args.optString("log_snippet")
        if (snippet.isBlank()) return@withContext AutonomousToolResult.Error(name, "log_snippet is required.")
        val fatal = snippet.lines().filter {
          it.contains("FATAL EXCEPTION", ignoreCase = true) ||
            it.contains("NullPointerException", ignoreCase = true) ||
            it.contains("ANR in", ignoreCase = true) ||
            it.contains("AndroidRuntime", ignoreCase = true)
        }
        val out = if (fatal.isEmpty()) "No runtime crashes or fatal exceptions found in log snippet."
        else "Detected Runtime Issues:\n" + fatal.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "find_error_location" -> {
        val relPath = args.optString("relative_path")
        val lineNum = args.optInt("line_number", 1)
        val window = args.optInt("window_size", 10)
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")

        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")

        val lines = file.readLines()
        val start = (lineNum - window).coerceAtLeast(1)
        val end = (lineNum + window).coerceAtMost(lines.size)

        val snippet = buildString {
          appendLine("=== Location: $relPath (Target Line: $lineNum) ===")
          for (i in start..end) {
            val lineContent = lines[i - 1]
            val marker = if (i == lineNum) ">>> " else "    "
            appendLine("$marker${"%4d".format(i)} | $lineContent")
          }
        }
        AutonomousToolResult.Success(name, snippet)
      }

      "suggest_fix" -> {
        val errMsg = args.optString("error_message")
        val relPath = args.optString("relative_path")
        if (errMsg.isBlank()) return@withContext AutonomousToolResult.Error(name, "error_message is required.")

        val suggestion = buildString {
          appendLine("=== Diagnostic & Recommended Fix ===")
          appendLine("Error: $errMsg")
          when {
            errMsg.contains("Unresolved reference", ignoreCase = true) -> {
              val sym = Regex("""'(\w+)'""").find(errMsg)?.groupValues?.get(1) ?: "symbol"
              appendLine("Issue: Missing import or undefined symbol '$sym'.")
              appendLine("Recommended Action: Add `import com.example...$sym` or declare `$sym` in scope.")
            }
            errMsg.contains("Type mismatch", ignoreCase = true) -> {
              appendLine("Issue: Incompatible types provided in expression or assignment.")
              appendLine("Recommended Action: Cast or transform the value to the expected type.")
            }
            errMsg.contains("NullPointerException", ignoreCase = true) -> {
              appendLine("Issue: Attempted to dereference a null pointer.")
              appendLine("Recommended Action: Use safe call `?.` or elvis operator `?:` to handle null gracefully.")
            }
            errMsg.contains("Missing AndroidManifest.xml", ignoreCase = true) -> {
              appendLine("Issue: Required manifest file is missing.")
              appendLine("Recommended Action: Create `app/src/main/AndroidManifest.xml` with package declaration.")
            }
            else -> {
              appendLine("Recommended Action: Inspect lines at origin and ensure correct syntax, type matching, and dependencies.")
            }
          }
        }
        AutonomousToolResult.Success(name, suggestion)
      }

      "apply_error_fix" -> {
        val relPath = args.optString("relative_path")
        val fixType = args.optString("fix_type")
        val symbol = args.optString("symbol")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")

        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")

        val orig = file.readText(Charsets.UTF_8)
        val updated = when (fixType) {
          "add_import" -> {
            val lines = orig.lines().toMutableList()
            val lastImportIdx = lines.indexOfLast { it.trim().startsWith("import ") }
            val insertIdx = if (lastImportIdx != -1) lastImportIdx + 1 else lines.indexOfFirst { it.trim().startsWith("package ") } + 1
            lines.add(insertIdx.coerceIn(0, lines.size), "import $symbol")
            lines.joinToString("\n")
          }
          "fix_typo" -> {
            val replacement = args.optString("replacement")
            orig.replace(symbol, replacement)
          }
          else -> orig
        }

        if (orig == updated) {
          AutonomousToolResult.Error(name, "Could not automatically apply fix type: $fixType")
        } else {
          file.writeText(updated, Charsets.UTF_8)
          AutonomousToolResult.Success(name, "Applied fix '$fixType' to $relPath", listOf(relPath))
        }
      }

      "inspect_logs" -> {
        val tag = args.optString("filter_tag", "")
        val maxLines = args.optInt("max_lines", 30)
        val manifest = workspaceManager.getOperationHistory()
        val text = buildString {
          appendLine("=== Execution Audit Trail (${manifest.size} records) ===")
          manifest.takeLast(maxLines).forEach { op ->
            if (tag.isBlank() || op.description.contains(tag, ignoreCase = true)) {
              appendLine("[${op.operation.name}] ${op.relativePath}: ${op.description}")
            }
          }
        }
        AutonomousToolResult.Success(name, text)
      }

      "diagnose_crash" -> {
        val dump = args.optString("crash_dump")
        val lines = dump.lines()
        val fatalHeader = lines.find { it.contains("FATAL EXCEPTION") } ?: "Fatal Exception"
        val process = lines.find { it.contains("Process:") } ?: "Unknown process"
        val cause = lines.find { it.contains("Caused by:") } ?: lines.find { it.contains("Exception") } ?: "Unspecified error"

        val diagnosis = buildString {
          appendLine("=== Crash Diagnosis ===")
          appendLine("Process: $process")
          appendLine("Fatal Header: $fatalHeader")
          appendLine("Core Cause: $cause")
          appendLine("Severity: CRITICAL")
          appendLine("Recommendation: Inspect call hierarchy leading to: $cause")
        }
        AutonomousToolResult.Success(name, diagnosis)
      }

      "detect_memory_issues" -> {
        val files = root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") && !it.path.contains("/build/") }.take(30).toList()
        val warnings = mutableListOf<String>()

        files.forEach { file ->
          val rel = file.relativeTo(root).path
          val lines = file.readLines()
          lines.forEachIndexed { i, line ->
            if (line.contains("static var context:") || line.contains("static Context ") || line.contains("var context: Context?")) {
              warnings.add("$rel:${i + 1}: [MEMORY LEAK] Static reference to Android Context can prevent Activity garbage collection")
            }
            if ((line.contains("FileInputStream(") || line.contains("FileOutputStream(")) && !lines.subList(i, (i + 15).coerceAtMost(lines.size)).any { it.contains(".use {") || it.contains(".close()") }) {
              warnings.add("$rel:${i + 1}: [UNCLOSED STREAM] Potential unclosed file stream")
            }
          }
        }
        val out = if (warnings.isEmpty()) "Memory Audit Passed: No static Context leaks or unclosed stream anti-patterns found."
        else "Potential Memory Issues (${warnings.size}):\n" + warnings.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "detect_performance_issues" -> {
        val files = root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") && !it.path.contains("/build/") }.take(30).toList()
        val warnings = mutableListOf<String>()

        files.forEach { file ->
          val rel = file.relativeTo(root).path
          file.useLines { lines ->
            lines.forEachIndexed { i, line ->
              if (line.contains("@Composable") && line.contains("Thread.sleep")) {
                warnings.add("$rel:${i + 1}: [UI FREEZE] Thread.sleep called inside Composable")
              }
              if (line.contains("File(") && line.contains(".readText()") && !line.contains("withContext(Dispatchers.IO)")) {
                warnings.add("$rel:${i + 1}: [MAIN THREAD IO] Blocking disk read without withContext(Dispatchers.IO)")
              }
            }
          }
        }
        val out = if (warnings.isEmpty()) "Performance Audit Passed: No main-thread blocking disk/network operations detected."
        else "Performance Warnings (${warnings.size}):\n" + warnings.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      else -> null
    }
  }

  private fun diagnoseError(error: String): String {
    return buildString {
      appendLine("Error Diagnostic Analysis:")
      appendLine("Input: $error")
      when {
        error.contains("Unresolved reference") -> appendLine("Class: CompilationError. Missing import or undefined symbol.")
        error.contains("Type mismatch") -> appendLine("Class: TypeError. Mismatched argument or variable type.")
        error.contains("NullPointerException") -> appendLine("Class: RuntimeError. Null dereference detected.")
        else -> appendLine("Class: General Software Diagnostic.")
      }
    }
  }
}
