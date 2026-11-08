package com.example.agent.tools.impl

import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.ToolCategoryProvider
import com.example.agent.tools.ToolDefinition
import com.example.agent.workspace.WorkspaceManager
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Production-ready Code Understanding Tools for analyzing AST, structure,
 * symbols, dependencies, and code hierarchies across languages.
 */
class CodeUnderstandingToolProvider(
  private val workspaceManager: WorkspaceManager
) : ToolCategoryProvider {

  override val categoryName: String = "Code Understanding"

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "analyze_code",
      category = categoryName,
      description = "Perform comprehensive code analysis on a file: LOC, functions, classes, imports, and complexity.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "explain_code",
      category = categoryName,
      description = "Explain the logic, purpose, inputs/outputs, and architecture of a specific file or line range.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("start_line", JSONObject().apply { put("type", "integer") })
          put("end_line", JSONObject().apply { put("type", "integer") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_programming_language",
      category = categoryName,
      description = "Detect the programming language of a file or code snippet.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("code_snippet", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_framework",
      category = categoryName,
      description = "Identify UI frameworks, backend frameworks, and architectural libraries in the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "detect_project_type",
      category = categoryName,
      description = "Detect the primary project type, build system, and architecture.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "analyze_project_structure",
      category = categoryName,
      description = "Analyze the directory layout, source roots, test suites, assets, and config boundaries.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "find_function",
      category = categoryName,
      description = "Locate function declarations by name across all files or within a target file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("function_name") })
        put("properties", JSONObject().apply {
          put("function_name", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "find_class",
      category = categoryName,
      description = "Locate class, interface, enum, struct, or type declarations by name.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("class_name") })
        put("properties", JSONObject().apply {
          put("class_name", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "find_variable",
      category = categoryName,
      description = "Find variable, constant, or field declarations across the project.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("variable_name") })
        put("properties", JSONObject().apply {
          put("variable_name", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "find_references",
      category = categoryName,
      description = "Find all usages and references to a symbol across the entire codebase.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("symbol_name") })
        put("properties", JSONObject().apply {
          put("symbol_name", JSONObject().apply { put("type", "string") })
          put("limit", JSONObject().apply { put("type", "integer") })
        })
      }
    ),
    ToolDefinition(
      name = "find_dependencies",
      category = categoryName,
      description = "List all internal and external dependencies declared in build files or imports.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "inspect_imports",
      category = categoryName,
      description = "Inspect package declaration and import statements in a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_unused_code",
      category = categoryName,
      description = "Detect unused imports, private functions, or unused variables.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "detect_duplicate_code",
      category = categoryName,
      description = "Detect duplicated blocks of code across workspace source files.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("min_lines", JSONObject().apply { put("type", "integer"); put("description", "Minimum duplicate block lines (default 5)") })
        })
      }
    ),
    ToolDefinition(
      name = "analyze_call_hierarchy",
      category = categoryName,
      description = "Analyze caller hierarchy and call sites for a given function.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("function_name") })
        put("properties", JSONObject().apply {
          put("function_name", JSONObject().apply { put("type", "string") })
        })
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
      "analyze_code" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")

        val lines = file.readLines()
        val totalLines = lines.size
        val blankLines = lines.count { it.isBlank() }
        val commentLines = lines.count { it.trim().startsWith("//") || it.trim().startsWith("/*") || it.trim().startsWith("*") || it.trim().startsWith("#") }
        val codeLines = totalLines - blankLines - commentLines

        val funcMatches = lines.filter { it.contains(Regex("""\b(fun|def|function|fn|void|public|private|protected)\s+\w+\s*\(""")) }
        val classMatches = lines.filter { it.contains(Regex("""\b(class|interface|enum|struct|type|object)\s+\w+""")) }
        val imports = lines.filter { it.trim().startsWith("import ") }

        val analysis = buildString {
          appendLine("=== Code Analysis: $relPath ===")
          appendLine("Total Lines: $totalLines (Code: $codeLines, Comments: $commentLines, Blank: $blankLines)")
          appendLine("Classes/Types: ${classMatches.size}")
          classMatches.forEach { appendLine("  • ${it.trim().take(80)}") }
          appendLine("Functions/Methods: ${funcMatches.size}")
          funcMatches.take(15).forEach { appendLine("  • ${it.trim().take(80)}") }
          appendLine("Imports: ${imports.size}")
        }
        AutonomousToolResult.Success(name, analysis)
      }

      "explain_code" -> {
        val relPath = args.optString("relative_path")
        val startLine = args.optInt("start_line", 1)
        val endLine = args.optInt("end_line", -1)
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")

        val allLines = file.readLines()
        val toRead = if (endLine > 0 && endLine <= allLines.size && startLine <= endLine) {
          allLines.subList(startLine - 1, endLine)
        } else {
          allLines.take(200)
        }

        val explanation = buildString {
          appendLine("File: $relPath (Lines ${startLine}..${if (endLine > 0) endLine else allLines.size})")
          appendLine("Language: ${detectLang(file)}")
          appendLine("Summary of inspected segment:")
          appendLine(toRead.joinToString("\n"))
        }
        AutonomousToolResult.Success(name, explanation)
      }

      "detect_programming_language" -> {
        val relPath = args.optString("relative_path")
        val snippet = args.optString("code_snippet")
        val lang = if (relPath.isNotBlank()) {
          val f = File(relPath)
          detectLang(f)
        } else if (snippet.isNotBlank()) {
          detectLangFromSnippet(snippet)
        } else {
          "Unknown"
        }
        AutonomousToolResult.Success(name, "Detected Language: $lang")
      }

      "detect_framework" -> {
        val frameworks = mutableListOf<String>()
        val allFiles = root.walkTopDown().take(200).toList()
        if (allFiles.any { it.name.endsWith(".gradle.kts") || it.name.endsWith(".gradle") }) {
          frameworks.add("Android Native (Gradle)")
        }
        if (allFiles.any { it.name == "package.json" }) {
          val text = File(root, "package.json").readText()
          if (text.contains("react")) frameworks.add("React")
          if (text.contains("vue")) frameworks.add("Vue")
          if (text.contains("next")) frameworks.add("Next.js")
          if (text.contains("express")) frameworks.add("Express.js")
          frameworks.add("Node.js")
        }
        if (allFiles.any { it.name == "requirements.txt" || it.name == "pyproject.toml" }) {
          frameworks.add("Python")
        }
        if (allFiles.any { it.name.endsWith(".kt") }) {
          allFiles.filter { it.extension == "kt" }.take(10).forEach { kt ->
            val txt = kt.readText()
            if (txt.contains("androidx.compose")) frameworks.add("Jetpack Compose")
            if (txt.contains("androidx.room")) frameworks.add("Room Database")
          }
        }
        val res = if (frameworks.isEmpty()) "Standard / Plain Codebase" else frameworks.distinct().joinToString(", ")
        AutonomousToolResult.Success(name, "Detected Frameworks: $res")
      }

      "detect_project_type" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val text = buildString {
          appendLine("Project Name: ${overview.rootName}")
          appendLine("Type: ${overview.projectType.displayName}")
          appendLine("Build System: ${overview.buildSystem}")
          appendLine("Source Files Count: ${overview.sourceFilesCount}")
          appendLine("Top Directories: ${overview.topLevelDirectories.joinToString(", ")}")
          appendLine("Config Files: ${overview.configFiles.joinToString(", ")}")
        }
        AutonomousToolResult.Success(name, text)
      }

      "analyze_project_structure" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val subdirs = root.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.map { it.name } ?: emptyList()
        val text = buildString {
          appendLine("=== Project Structure Analysis ===")
          appendLine("Root: ${root.absolutePath}")
          appendLine("Modules / Top Directories: ${subdirs.joinToString(", ")}")
          val entryPointsText = if (overview.entryPoints.isEmpty()) "None detected" else overview.entryPoints.joinToString(", ")
          val testCommandsText = if (overview.testCommands.isEmpty()) "N/A" else overview.testCommands.joinToString("; ")
          appendLine("Entry Points: $entryPointsText")
          appendLine("Build Configs: ${overview.configFiles.joinToString(", ")}")
          appendLine("Test Commands: $testCommandsText")
        }
        AutonomousToolResult.Success(name, text)
      }

      "find_function" -> {
        val func = args.optString("function_name").trim()
        val targetPath = args.optString("relative_path")
        if (func.isBlank()) return@withContext AutonomousToolResult.Error(name, "function_name is required.")

        val funcRegex = Regex("""\b(fun|def|function|fn|void|public|private|protected)\s+(?:[\w<>?:]+\s+)*$func\s*\(""", RegexOption.IGNORE_CASE)
        val matches = mutableListOf<String>()

        val files = if (targetPath.isNotBlank()) listOf(workspaceManager.resolveSafeFile(project, targetPath))
        else root.walkTopDown().filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/node_modules/") }.toList()

        files.forEach { f ->
          val rel = f.relativeTo(root).path
          f.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (funcRegex.containsMatchIn(l) || l.contains("fun $func") || l.contains("def $func")) {
                matches.add("$rel:${i + 1}: ${l.trim()}")
              }
            }
          }
        }
        val out = if (matches.isEmpty()) "No functions found matching '$func'."
        else "Found ${matches.size} function declarations:\n" + matches.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "find_class" -> {
        val cls = args.optString("class_name").trim()
        val targetPath = args.optString("relative_path")
        if (cls.isBlank()) return@withContext AutonomousToolResult.Error(name, "class_name is required.")

        val clsRegex = Regex("""\b(class|interface|enum\s+class|enum|struct|type|object)\s+$cls\b""", RegexOption.IGNORE_CASE)
        val matches = mutableListOf<String>()

        val files = if (targetPath.isNotBlank()) listOf(workspaceManager.resolveSafeFile(project, targetPath))
        else root.walkTopDown().filter { it.isFile && !it.path.contains("/build/") }.toList()

        files.forEach { f ->
          val rel = f.relativeTo(root).path
          f.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (clsRegex.containsMatchIn(l)) {
                matches.add("$rel:${i + 1}: ${l.trim()}")
              }
            }
          }
        }
        val out = if (matches.isEmpty()) "No class/interface/type found matching '$cls'."
        else "Found ${matches.size} declarations:\n" + matches.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "find_variable" -> {
        val v = args.optString("variable_name").trim()
        if (v.isBlank()) return@withContext AutonomousToolResult.Error(name, "variable_name is required.")
        val varRegex = Regex("""\b(val|var|const|let|int|String|boolean|float)\s+$v\b""")
        val matches = mutableListOf<String>()

        root.walkTopDown().filter { it.isFile && !it.path.contains("/build/") }.take(250).forEach { f ->
          val rel = f.relativeTo(root).path
          f.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (varRegex.containsMatchIn(l) || l.contains("val $v") || l.contains("var $v") || l.contains("let $v") || l.contains("const $v")) {
                matches.add("$rel:${i + 1}: ${l.trim()}")
              }
            }
          }
        }
        val out = if (matches.isEmpty()) "No variables found matching '$v'."
        else "Found ${matches.size} variable declarations:\n" + matches.take(50).joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "find_references" -> {
        val sym = args.optString("symbol_name").trim()
        val limit = args.optInt("limit", 60)
        if (sym.isBlank()) return@withContext AutonomousToolResult.Error(name, "symbol_name is required.")
        val matches = mutableListOf<String>()

        root.walkTopDown().filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/.git/") }.forEach { f ->
          val rel = f.relativeTo(root).path
          f.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (l.contains(sym)) {
                matches.add("$rel:${i + 1}: ${l.trim()}")
              }
            }
          }
        }
        val out = if (matches.isEmpty()) "No references found for '$sym'."
        else "Found ${matches.size} references (showing top ${matches.take(limit).size}):\n" + matches.take(limit).joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "find_dependencies" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val text = buildString {
          appendLine("=== Project Dependencies ===")
          overview.dependenciesSummary.forEach { appendLine("• $it") }
          if (overview.dependenciesSummary.isEmpty()) {
            appendLine("No specific dependency files discovered or parsed.")
          }
        }
        AutonomousToolResult.Success(name, text)
      }

      "inspect_imports" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")
        val lines = file.readLines()
        val pkg = lines.firstOrNull { it.trim().startsWith("package ") }?.trim() ?: "No package"
        val imports = lines.filter { it.trim().startsWith("import ") }.map { it.trim() }
        val out = buildString {
          appendLine("Package: $pkg")
          appendLine("Total Imports: ${imports.size}")
          imports.forEach { appendLine("  $it") }
        }
        AutonomousToolResult.Success(name, out)
      }

      "detect_unused_code" -> {
        val relPath = args.optString("relative_path")
        val filesToScan = if (relPath.isNotBlank()) listOf(workspaceManager.resolveSafeFile(project, relPath))
        else root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") && !it.path.contains("/build/") }.take(20).toList()

        val warnings = mutableListOf<String>()
        filesToScan.forEach { file ->
          val rel = file.relativeTo(root).path
          val lines = file.readLines()
          val fullText = lines.joinToString("\n")

          // Unused imports check
          lines.filter { it.trim().startsWith("import ") }.forEach { impLine ->
            val importedSymbol = impLine.trim().split(".").lastOrNull()?.removeSuffix(";")?.trim()
            if (importedSymbol != null && importedSymbol != "*" && !importedSymbol.contains(" ")) {
              val usages = lines.count { !it.trim().startsWith("import ") && !it.trim().startsWith("package ") && it.contains(importedSymbol) }
              if (usages == 0) {
                warnings.add("$rel: Unused import '$importedSymbol'")
              }
            }
          }

          // Unused private functions check
          lines.forEachIndexed { idx, line ->
            val match = Regex("""private\s+fun\s+(\w+)\s*\(""").find(line)
            if (match != null) {
              val fnName = match.groupValues[1]
              val count = fullText.split(fnName).size - 1
              if (count <= 1) {
                warnings.add("$rel:${idx + 1}: Unused private function '$fnName'")
              }
            }
          }
        }
        val out = if (warnings.isEmpty()) "No obvious unused imports or private functions detected."
        else "Detected issues:\n" + warnings.joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "detect_duplicate_code" -> {
        val minLines = args.optInt("min_lines", 5)
        val files = root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "py") && !it.path.contains("/build/") }.take(30).toList()
        val blocks = mutableMapOf<String, MutableList<String>>()

        files.forEach { file ->
          val lines = file.readLines()
          for (i in 0..(lines.size - minLines)) {
            val block = lines.subList(i, i + minLines).map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("//") }.joinToString("\n")
            if (block.length > 50) {
              val loc = "${file.relativeTo(root).path}:${i + 1}"
              blocks.computeIfAbsent(block) { mutableListOf() }.add(loc)
            }
          }
        }

        val duplicates = blocks.filter { it.value.size > 1 }
        val out = if (duplicates.isEmpty()) "No duplicated code blocks (>= $minLines lines) found."
        else buildString {
          appendLine("Found ${duplicates.size} potential duplicate code blocks:")
          duplicates.entries.take(5).forEachIndexed { i, entry ->
            appendLine("\n--- Duplicate Block #${i + 1} (${entry.value.size} occurrences) ---")
            entry.value.forEach { appendLine("  Location: $it") }
            appendLine("Snippet:\n" + entry.key.lines().take(3).joinToString("\n") { "    $it" })
          }
        }
        AutonomousToolResult.Success(name, out)
      }

      "analyze_call_hierarchy" -> {
        val fnName = args.optString("function_name").trim()
        if (fnName.isBlank()) return@withContext AutonomousToolResult.Error(name, "function_name is required.")
        val callRegex = Regex("""\b$fnName\s*\(""")
        val callers = mutableListOf<String>()

        root.walkTopDown().filter { it.isFile && !it.path.contains("/build/") }.forEach { file ->
          val rel = file.relativeTo(root).path
          file.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (callRegex.containsMatchIn(l) && !l.contains("fun $fnName") && !l.contains("def $fnName")) {
                callers.add("$rel:${i + 1}: ${l.trim()}")
              }
            }
          }
        }
        val out = if (callers.isEmpty()) "No call sites found invoking '$fnName('."
        else "Found ${callers.size} calls to '$fnName':\n" + callers.take(40).joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      else -> null
    }
  }

  private fun detectLang(file: File): String {
    return when (file.extension.lowercase()) {
      "kt", "kts" -> "Kotlin"
      "java" -> "Java"
      "py" -> "Python"
      "ts", "tsx" -> "TypeScript"
      "js", "jsx" -> "JavaScript"
      "rs" -> "Rust"
      "go" -> "Go"
      "c", "h" -> "C"
      "cpp", "hpp", "cc" -> "C++"
      "xml" -> "XML"
      "json" -> "JSON"
      "yaml", "yml" -> "YAML"
      "sh", "bash" -> "Shell"
      "sql" -> "SQL"
      "md" -> "Markdown"
      else -> "Plain Text"
    }
  }

  private fun detectLangFromSnippet(code: String): String {
    return when {
      code.contains("fun ") || code.contains("val ") || code.contains("var ") -> "Kotlin"
      code.contains("public class") || code.contains("System.out.println") -> "Java"
      code.contains("def ") || code.contains("import ") && code.contains(":\n") -> "Python"
      code.contains("const ") || code.contains("let ") || code.contains("=>") -> "TypeScript/JavaScript"
      code.contains("fn ") || code.contains("let mut ") -> "Rust"
      else -> "General Code"
    }
  }
}
