package com.example.agent.tools.impl

import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.ToolCategoryProvider
import com.example.agent.tools.ToolDefinition
import com.example.agent.workspace.WorkspaceManager
import com.example.data.files.DiffUtil
import com.example.data.model.FileChange
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Production-ready Code Editing Tools for modifying, refactoring,
 * patching, formatting, generating, and reverting source code.
 */
class CodeEditingToolProvider(
  private val workspaceManager: WorkspaceManager
) : ToolCategoryProvider {

  override val categoryName: String = "Code Editing"

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "insert_code",
      category = categoryName,
      description = "Insert code into a file at a specific 1-indexed line number.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("line_number"); put("code") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("line_number", JSONObject().apply { put("type", "integer") })
          put("code", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "replace_code",
      category = categoryName,
      description = "Replace a 1-indexed line range [start_line, end_line] with replacement code.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("start_line"); put("end_line"); put("code") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("start_line", JSONObject().apply { put("type", "integer") })
          put("end_line", JSONObject().apply { put("type", "integer") })
          put("code", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "append_code",
      category = categoryName,
      description = "Append code to the end of a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("code") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("code", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "remove_code",
      category = categoryName,
      description = "Remove a 1-indexed line range [start_line, end_line] from a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("start_line"); put("end_line") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("start_line", JSONObject().apply { put("type", "integer") })
          put("end_line", JSONObject().apply { put("type", "integer") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "refactor_code",
      category = categoryName,
      description = "Perform a structured refactoring (e.g. rename, extract, or replace pattern) on target file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("action"); put("target"); put("replacement") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("action", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("replace"); put("rename"); put("inline") }) })
          put("target", JSONObject().apply { put("type", "string") })
          put("replacement", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "rename_symbol",
      category = categoryName,
      description = "Safely rename a symbol across the workspace or within a target file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("old_name"); put("new_name") })
        put("properties", JSONObject().apply {
          put("old_name", JSONObject().apply { put("type", "string") })
          put("new_name", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string"); put("description", "Optional target file (otherwise project-wide)") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "organize_imports",
      category = categoryName,
      description = "Organize, sort alphabetically, and remove duplicate imports in a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "format_code",
      category = categoryName,
      description = "Format a file: trim trailing whitespace, normalize newlines, and clean formatting.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "generate_code",
      category = categoryName,
      description = "Generate code components (data class, interface, function, composable).",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("code_type"); put("name") })
        put("properties", JSONObject().apply {
          put("code_type", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("data_class"); put("interface"); put("composable"); put("function") }) })
          put("name", JSONObject().apply { put("type", "string") })
          put("package_name", JSONObject().apply { put("type", "string") })
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "generate_tests",
      category = categoryName,
      description = "Generate a unit test suite template for a given source file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "apply_diff",
      category = categoryName,
      description = "Parse and apply a standard unified diff string to a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("diff") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("diff", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "preview_changes",
      category = categoryName,
      description = "Compute and preview diff hunks between existing file and proposed content without writing.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("new_content") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("new_content", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "revert_changes",
      category = categoryName,
      description = "Revert a file to its latest backup state.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
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
      "insert_code" -> {
        val relPath = args.optString("relative_path")
        val lineNum = args.optInt("line_number", 1)
        val code = args.optString("code")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")

        val orig = file.readText(Charsets.UTF_8)
        val lines = orig.lines().toMutableList()
        val insertIdx = (lineNum - 1).coerceIn(0, lines.size)
        lines.add(insertIdx, code)
        val newContent = lines.joinToString("\n")
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "replace_code" -> {
        val relPath = args.optString("relative_path")
        val startLine = args.optInt("start_line", 1)
        val endLine = args.optInt("end_line", 1)
        val code = args.optString("code")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")

        val orig = file.readText(Charsets.UTF_8)
        val lines = orig.lines().toMutableList()
        val startIdx = (startLine - 1).coerceIn(0, lines.size)
        val endIdx = endLine.coerceIn(startIdx, lines.size)
        val prefix = lines.take(startIdx)
        val suffix = lines.drop(endIdx)
        val newContent = (prefix + listOf(code) + suffix).joinToString("\n")
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "append_code" -> {
        val relPath = args.optString("relative_path")
        val code = args.optString("code")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        val orig = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        val newContent = if (orig.isEmpty()) code else "$orig\n$code"
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "remove_code" -> {
        val relPath = args.optString("relative_path")
        val startLine = args.optInt("start_line", 1)
        val endLine = args.optInt("end_line", 1)
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")

        val orig = file.readText(Charsets.UTF_8)
        val lines = orig.lines()
        val startIdx = (startLine - 1).coerceIn(0, lines.size)
        val endIdx = endLine.coerceIn(startIdx, lines.size)
        val newContent = (lines.take(startIdx) + lines.drop(endIdx)).joinToString("\n")
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "refactor_code" -> {
        val relPath = args.optString("relative_path")
        val target = args.optString("target")
        val replacement = args.optString("replacement")
        if (relPath.isBlank() || target.isEmpty()) {
          return@withContext AutonomousToolResult.Error(name, "relative_path and target are required.")
        }
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")
        val orig = file.readText(Charsets.UTF_8)
        if (!orig.contains(target)) return@withContext AutonomousToolResult.Error(name, "Target pattern '$target' not found in $relPath")
        val newContent = orig.replace(target, replacement)
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "rename_symbol" -> {
        val oldName = args.optString("old_name").trim()
        val newName = args.optString("new_name").trim()
        val relPath = args.optString("relative_path").trim()
        if (oldName.isBlank() || newName.isBlank()) return@withContext AutonomousToolResult.Error(name, "old_name and new_name are required.")

        val filesToEdit = if (relPath.isNotBlank()) {
          listOf(workspaceManager.resolveSafeFile(project, relPath))
        } else {
          root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml" || it.extension == "py") && !it.path.contains("/build/") }.toList()
        }

        val modified = mutableListOf<String>()
        val regex = Regex("""\b$oldName\b""")

        filesToEdit.forEach { f ->
          val orig = f.readText(Charsets.UTF_8)
          if (regex.containsMatchIn(orig)) {
            val updated = orig.replace(regex, newName)
            if (autoApproveWrites) {
              f.writeText(updated, Charsets.UTF_8)
              modified.add(f.relativeTo(root).path)
            }
          }
        }

        if (!autoApproveWrites) {
          AutonomousToolResult.RequiresApproval(name, "Rename symbol '$oldName' -> '$newName' across matching files.", args)
        } else {
          AutonomousToolResult.Success(name, "Renamed '$oldName' to '$newName' in ${modified.size} files:\n" + modified.joinToString("\n"), modified)
        }
      }

      "organize_imports" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")

        val orig = file.readText(Charsets.UTF_8)
        val lines = orig.lines()
        val pkgLines = lines.filter { it.trim().startsWith("package ") }
        val importLines = lines.filter { it.trim().startsWith("import ") }.distinct().sorted()
        val nonImportLines = lines.filter { !it.trim().startsWith("package ") && !it.trim().startsWith("import ") }

        val newContent = buildString {
          if (pkgLines.isNotEmpty()) {
            pkgLines.forEach { appendLine(it) }
            appendLine()
          }
          if (importLines.isNotEmpty()) {
            importLines.forEach { appendLine(it) }
            appendLine()
          }
          append(nonImportLines.dropWhile { it.isBlank() }.joinToString("\n"))
        }

        file.writeText(newContent, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Organized and sorted ${importLines.size} imports in $relPath.", listOf(relPath))
      }

      "format_code" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File not found: $relPath")

        val lines = file.readLines().map { it.trimEnd() }
        val cleaned = lines.joinToString("\n").trimEnd() + "\n"
        file.writeText(cleaned, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Formatted $relPath: trimmed trailing whitespace and ensured single terminal newline.", listOf(relPath))
      }

      "generate_code" -> {
        val codeType = args.optString("code_type", "data_class")
        val componentName = args.optString("name")
        val pkg = args.optString("package_name", "com.example.model")
        val relPath = args.optString("relative_path").ifBlank { "app/src/main/java/${pkg.replace('.', '/')}/$componentName.kt" }

        val snippet = when (codeType) {
          "data_class" -> """
package $pkg

data class $componentName(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String = "",
  val timestamp: Long = System.currentTimeMillis()
)
          """.trimIndent()

          "interface" -> """
package $pkg

interface $componentName {
  suspend fun execute(): Result<Unit>
}
          """.trimIndent()

          "composable" -> """
package $pkg

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

@Composable
fun $componentName(modifier: Modifier = Modifier) {
  Text(text = "$componentName", modifier = modifier)
}
          """.trimIndent()

          else -> """
package $pkg

class $componentName {
  fun execute(): String = "$componentName initialized"
}
          """.trimIndent()
        }

        val file = workspaceManager.resolveSafeFile(project, relPath)
        file.parentFile?.mkdirs()
        file.writeText(snippet, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Generated $codeType '$componentName' at $relPath:\n$snippet", listOf(relPath))
      }

      "generate_tests" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")

        val className = file.nameWithoutExtension
        val testClassName = "${className}Test"
        val testRelPath = if (relPath.contains("src/main/java/")) {
          relPath.replace("src/main/java/", "src/test/java/").replace("$className.kt", "$testClassName.kt")
        } else {
          "app/src/test/java/com/example/${testClassName}.kt"
        }

        val testContent = """
package com.example

import org.junit.Assert.*
import org.junit.Test

class $testClassName {

  @Test
  fun testInitialState() {
    assertTrue("Test scaffold initialized successfully", true)
  }
}
        """.trimIndent()

        val testFile = workspaceManager.resolveSafeFile(project, testRelPath)
        testFile.parentFile?.mkdirs()
        testFile.writeText(testContent, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Generated unit test scaffold at $testRelPath", listOf(testRelPath))
      }

      "apply_diff" -> {
        val relPath = args.optString("relative_path")
        val diff = args.optString("diff")
        if (relPath.isBlank() || diff.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path and diff are required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        val orig = if (file.exists()) file.readText(Charsets.UTF_8) else ""

        val lines = orig.lines().toMutableList()
        var currentIdx = 0
        diff.lines().forEach { line ->
          when {
            line.startsWith("+") && !line.startsWith("+++") -> {
              val addition = line.removePrefix("+").removePrefix(" ")
              lines.add(currentIdx.coerceIn(0, lines.size), addition)
              currentIdx++
            }
            line.startsWith("-") && !line.startsWith("---") -> {
              val deletion = line.removePrefix("-").removePrefix(" ")
              val found = lines.indexOfFirst { it.trim() == deletion.trim() }
              if (found != -1) {
                lines.removeAt(found)
                currentIdx = found
              }
            }
          }
        }
        val newContent = lines.joinToString("\n")
        applyContentChange(name, project, relPath, orig, newContent, args, autoApproveWrites)
      }

      "preview_changes" -> {
        val relPath = args.optString("relative_path")
        val newContent = args.optString("new_content")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        val orig = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        val hunks = DiffUtil.computeDiff(orig, newContent)

        val preview = buildString {
          appendLine("=== Preview Changes: $relPath (${hunks.size} hunks) ===")
          hunks.forEach { hunk ->
            appendLine("@@ -${hunk.oldStart},${hunk.oldLinesCount} +${hunk.newStart},${hunk.newLinesCount} @@")
            hunk.lines.forEach { l ->
              val sign = when (l.type) {
                com.example.data.model.DiffLineType.ADDITION -> "+"
                com.example.data.model.DiffLineType.DELETION -> "-"
                else -> " "
              }
              appendLine("$sign ${l.text}")
            }
          }
        }
        AutonomousToolResult.Success(name, preview)
      }

      "revert_changes" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val lastOp = workspaceManager.getOperationHistory().lastOrNull { it.relativePath == relPath && it.backupFilePath != null }
        if (lastOp?.backupFilePath == null) {
          return@withContext AutonomousToolResult.Error(name, "No backup found to revert $relPath.")
        }
        val backupFile = File(lastOp.backupFilePath)
        if (!backupFile.exists()) return@withContext AutonomousToolResult.Error(name, "Backup file no longer exists.")

        val targetFile = workspaceManager.resolveSafeFile(project, relPath)
        backupFile.copyTo(targetFile, overwrite = true)
        AutonomousToolResult.Success(name, "Successfully reverted $relPath to backup.", listOf(relPath))
      }

      else -> null
    }
  }

  private suspend fun applyContentChange(
    name: String,
    project: Project,
    relPath: String,
    orig: String,
    newContent: String,
    args: JSONObject,
    autoApproveWrites: Boolean
  ): AutonomousToolResult {
    val hunks = DiffUtil.computeDiff(orig, newContent)
    val file = workspaceManager.resolveSafeFile(project, relPath)
    val change = FileChange(
      filePath = file.absolutePath,
      relativePath = relPath,
      originalContent = orig,
      proposedContent = newContent,
      diffHunks = hunks,
      isApproved = autoApproveWrites
    )

    return if (!autoApproveWrites) {
      AutonomousToolResult.RequiresApproval(name, "Proposed modification to '$relPath' (${hunks.size} diff hunks).", args, change)
    } else {
      workspaceManager.writeCompleteFile(project, relPath, newContent, true)
      AutonomousToolResult.Success(name, "Successfully modified $relPath (${hunks.size} diff hunks).", listOf(relPath), change)
    }
  }
}
