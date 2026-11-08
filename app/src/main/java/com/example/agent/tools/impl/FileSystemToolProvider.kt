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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production-ready Filesystem Tools for autonomous project manipulation.
 */
class FileSystemToolProvider(
  private val workspaceManager: WorkspaceManager
) : ToolCategoryProvider {

  override val categoryName: String = "Filesystem"

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "read_file",
      category = categoryName,
      description = "Read the exact content of an existing workspace file (UTF-8).",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string"); put("description", "Relative path to file") })
          put("max_lines", JSONObject().apply { put("type", "integer"); put("description", "Max lines to return (default 1200)") })
          put("offset", JSONObject().apply { put("type", "integer"); put("description", "Line offset to start from") })
        })
      }
    ),
    ToolDefinition(
      name = "read_multiple_files",
      category = categoryName,
      description = "Read multiple workspace files simultaneously.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("paths") })
        put("properties", JSONObject().apply {
          put("paths", JSONObject().apply {
            put("type", "array")
            put("items", JSONObject().apply { put("type", "string") })
            put("description", "List of relative file paths")
          })
          put("max_lines_per_file", JSONObject().apply { put("type", "integer"); put("description", "Max lines per file (default 500)") })
        })
      }
    ),
    ToolDefinition(
      name = "write_file",
      category = categoryName,
      description = "Write or overwrite a file with full contents. Automatically computes diffs and safety backups.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("content") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("content", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "edit_file",
      category = categoryName,
      description = "Apply a targeted edit by replacing a unique exact target block with replacement content.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path"); put("target_content"); put("replacement_content") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("target_content", JSONObject().apply { put("type", "string") })
          put("replacement_content", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "create_file",
      category = categoryName,
      description = "Create a new file in the workspace with initial content.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("content", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "delete_file",
      category = categoryName,
      description = "Safely delete a file or directory from the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "rename_file",
      category = categoryName,
      description = "Rename a file or directory within the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("old_path"); put("new_path") })
        put("properties", JSONObject().apply {
          put("old_path", JSONObject().apply { put("type", "string") })
          put("new_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "copy_file",
      category = categoryName,
      description = "Copy a file to another location in the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("source_path"); put("destination_path") })
        put("properties", JSONObject().apply {
          put("source_path", JSONObject().apply { put("type", "string") })
          put("destination_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "move_file",
      category = categoryName,
      description = "Move a file or directory to another path in the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("old_path"); put("new_path") })
        put("properties", JSONObject().apply {
          put("old_path", JSONObject().apply { put("type", "string") })
          put("new_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "create_directory",
      category = categoryName,
      description = "Create a directory or nested directories in the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_dir") })
        put("properties", JSONObject().apply {
          put("relative_dir", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "list_directory",
      category = categoryName,
      description = "List files and subdirectories in a relative directory of the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "search_files",
      category = categoryName,
      description = "Search across workspace files by keyword substring or regular expression.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("query") })
        put("properties", JSONObject().apply {
          put("query", JSONObject().apply { put("type", "string") })
          put("is_regex", JSONObject().apply { put("type", "boolean") })
          put("max_results", JSONObject().apply { put("type", "integer") })
        })
      }
    ),
    ToolDefinition(
      name = "find_files",
      category = categoryName,
      description = "Find files by filename, pattern, or extension (e.g. *.kt, Main*, build.gradle).",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("pattern") })
        put("properties", JSONObject().apply {
          put("pattern", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "find_in_files",
      category = categoryName,
      description = "Search text specifically inside files matching an extension or path prefix.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("query") })
        put("properties", JSONObject().apply {
          put("query", JSONObject().apply { put("type", "string") })
          put("extension", JSONObject().apply { put("type", "string"); put("description", "e.g. kt, java, xml, py") })
          put("path_prefix", JSONObject().apply { put("type", "string"); put("description", "e.g. app/src/main") })
        })
      }
    ),
    ToolDefinition(
      name = "get_file_info",
      category = categoryName,
      description = "Get detailed metadata, size, line count, permissions, and timestamps for a file.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("relative_path") })
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "get_file_tree",
      category = categoryName,
      description = "Generate an ASCII hierarchical file tree of the workspace or subdirectory.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
          put("max_depth", JSONObject().apply { put("type", "integer"); put("description", "Maximum depth (default 4)") })
        })
      }
    ),
    ToolDefinition(
      name = "compare_files",
      category = categoryName,
      description = "Compare two workspace files and produce a unified diff report.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("path_a"); put("path_b") })
        put("properties", JSONObject().apply {
          put("path_a", JSONObject().apply { put("type", "string") })
          put("path_b", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "watch_file_changes",
      category = categoryName,
      description = "Inspect recent file changes, audit trail, or files modified after a timestamp.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("since_timestamp", JSONObject().apply { put("type", "integer") })
          put("limit", JSONObject().apply { put("type", "integer") })
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
  ): AutonomousToolResult? {
    return when (name) {
      "read_file" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return AutonomousToolResult.Error(name, "relative_path parameter is required.")
        val maxLines = args.optInt("max_lines", 1200)
        val offset = args.optInt("offset", 0)
        val content = workspaceManager.readFileContent(project, relPath, maxLines, offset)
        AutonomousToolResult.Success(name, content)
      }

      "read_multiple_files" -> {
        val pathsArr = args.optJSONArray("paths") ?: JSONArray()
        val maxLines = args.optInt("max_lines_per_file", 500)
        if (pathsArr.length() == 0) return AutonomousToolResult.Error(name, "paths array is required.")
        val output = buildString {
          for (i in 0 until pathsArr.length()) {
            val path = pathsArr.getString(i)
            appendLine("=== File: $path ===")
            try {
              val content = workspaceManager.readFileContent(project, path, maxLines, 0)
              appendLine(content)
            } catch (e: Exception) {
              appendLine("Error reading file: ${e.message}")
            }
            appendLine()
          }
        }
        AutonomousToolResult.Success(name, output)
      }

      "write_file" -> {
        val relPath = args.optString("relative_path")
        val content = args.optString("content")
        if (relPath.isBlank()) return AutonomousToolResult.Error(name, "relative_path is required.")
        if (!autoApproveWrites) {
          val file = workspaceManager.resolveSafeFile(project, relPath)
          val orig = if (file.exists()) file.readText() else ""
          val hunks = DiffUtil.computeDiff(orig, content)
          val change = FileChange(
            filePath = file.absolutePath,
            relativePath = relPath,
            originalContent = orig,
            proposedContent = content,
            diffHunks = hunks,
            isApproved = false
          )
          AutonomousToolResult.RequiresApproval(name, "Proposed write to '$relPath' (${hunks.size} diff hunks).", args, change)
        } else {
          val (change, _) = workspaceManager.writeCompleteFile(project, relPath, content, true)
          AutonomousToolResult.Success(name, "File written successfully: $relPath (${content.length} chars).", listOf(relPath), change)
        }
      }

      "edit_file", "apply_patch" -> {
        val relPath = args.optString("relative_path")
        val target = args.optString("target_content")
        val replacement = args.optString("replacement_content")
        if (relPath.isBlank() || target.isEmpty()) {
          return AutonomousToolResult.Error(name, "relative_path and target_content are required.")
        }
        if (!autoApproveWrites) {
          val file = workspaceManager.resolveSafeFile(project, relPath)
          val orig = if (file.exists()) file.readText() else ""
          val patchResult = DiffUtil.applyBlockReplacement(orig, target, replacement)
          if (patchResult.isFailure) {
            return AutonomousToolResult.Error(name, "Patch failed: ${patchResult.exceptionOrNull()?.message}")
          }
          val newContent = patchResult.getOrThrow()
          val hunks = DiffUtil.computeDiff(orig, newContent)
          val change = FileChange(
            filePath = file.absolutePath,
            relativePath = relPath,
            originalContent = orig,
            proposedContent = newContent,
            diffHunks = hunks,
            isApproved = false
          )
          AutonomousToolResult.RequiresApproval(name, "Proposed patch on '$relPath' (${hunks.size} diff hunks).", args, change)
        } else {
          val (change, _) = workspaceManager.applyTargetedPatch(project, relPath, target, replacement, true)
          AutonomousToolResult.Success(name, "Patch applied successfully to $relPath.", listOf(relPath), change)
        }
      }

      "create_file" -> {
        val relPath = args.optString("relative_path")
        val content = args.optString("content", "")
        if (relPath.isBlank()) return AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (file.exists()) return AutonomousToolResult.Error(name, "File already exists: $relPath. Use edit_file or write_file.")
        if (!autoApproveWrites) {
          val hunks = DiffUtil.computeDiff("", content)
          val change = FileChange(
            filePath = file.absolutePath,
            relativePath = relPath,
            originalContent = "",
            proposedContent = content,
            diffHunks = hunks,
            isApproved = false
          )
          AutonomousToolResult.RequiresApproval(name, "Create new file '$relPath'", args, change)
        } else {
          val (_, change) = workspaceManager.createNewFile(project, relPath, content)
          AutonomousToolResult.Success(name, "Created file: $relPath", listOf(relPath), change)
        }
      }

      "delete_file" -> {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return AutonomousToolResult.Error(name, "relative_path parameter is required.")
        if (!autoApproveWrites) {
          AutonomousToolResult.RequiresApproval(name, "Delete '$relPath' from project.", args)
        } else {
          val ok = workspaceManager.deleteFileOrDir(project, relPath)
          if (ok) AutonomousToolResult.Success(name, "Deleted: $relPath", listOf(relPath))
          else AutonomousToolResult.Error(name, "Failed to delete: $relPath")
        }
      }

      "rename_file" -> {
        val oldPath = args.optString("old_path")
        val newPath = args.optString("new_path")
        if (oldPath.isBlank() || newPath.isBlank()) return AutonomousToolResult.Error(name, "old_path and new_path are required.")
        val ok = workspaceManager.renameOrMoveFile(project, oldPath, newPath)
        if (ok) AutonomousToolResult.Success(name, "Renamed $oldPath to $newPath", listOf(newPath))
        else AutonomousToolResult.Error(name, "Failed to rename $oldPath")
      }

      "copy_file" -> withContext(Dispatchers.IO) {
        val src = args.optString("source_path")
        val dst = args.optString("destination_path")
        if (src.isBlank() || dst.isBlank()) return@withContext AutonomousToolResult.Error(name, "source_path and destination_path are required.")
        val srcFile = workspaceManager.resolveSafeFile(project, src)
        val dstFile = workspaceManager.resolveSafeFile(project, dst)
        if (!srcFile.exists()) return@withContext AutonomousToolResult.Error(name, "Source file does not exist: $src")
        dstFile.parentFile?.mkdirs()
        srcFile.copyTo(dstFile, overwrite = true)
        AutonomousToolResult.Success(name, "Copied $src to $dst", listOf(dst))
      }

      "move_file" -> {
        val oldPath = args.optString("old_path")
        val newPath = args.optString("new_path")
        if (oldPath.isBlank() || newPath.isBlank()) return AutonomousToolResult.Error(name, "old_path and new_path are required.")
        val ok = workspaceManager.renameOrMoveFile(project, oldPath, newPath)
        if (ok) AutonomousToolResult.Success(name, "Moved $oldPath to $newPath", listOf(newPath))
        else AutonomousToolResult.Error(name, "Failed to move $oldPath")
      }

      "create_directory" -> {
        val relDir = args.optString("relative_dir")
        if (relDir.isBlank()) return AutonomousToolResult.Error(name, "relative_dir parameter is required.")
        val dir = workspaceManager.createDirectory(project, relDir)
        AutonomousToolResult.Success(name, "Directory created: ${dir.name}")
      }

      "list_directory" -> {
        val relPath = args.optString("relative_path", "")
        val files = workspaceManager.listFilesAndDirs(project, relPath)
        val output = if (files.isEmpty()) "Directory '$relPath' is empty."
        else files.joinToString("\n") { f ->
          val icon = if (f.isDirectory) "📁" else "📄"
          val size = if (f.isDirectory) "" else " (${f.sizeBytes} bytes)"
          "$icon ${f.name}$size"
        }
        AutonomousToolResult.Success(name, output)
      }

      "search_files" -> {
        val query = args.optString("query")
        val isRegex = args.optBoolean("is_regex", false)
        val maxResults = args.optInt("max_results", 40)
        if (query.isBlank()) return AutonomousToolResult.Error(name, "query parameter is required.")
        val matches = workspaceManager.searchFiles(project, query, isRegex, maxResults)
        val out = if (matches.isEmpty()) "No matches found for '$query'."
        else matches.joinToString("\n") { "${it.relativePath}:${it.lineNumber}: ${it.lineContent}" }
        AutonomousToolResult.Success(name, "Found ${matches.size} matches:\n$out")
      }

      "find_files" -> withContext(Dispatchers.IO) {
        val pattern = args.optString("pattern").trim()
        if (pattern.isBlank()) return@withContext AutonomousToolResult.Error(name, "pattern is required.")
        val regex = pattern
          .replace(".", "\\.")
          .replace("*", ".*")
          .replace("?", ".")
          .let { Regex("^.*$it.*$", RegexOption.IGNORE_CASE) }
        val root = File(project.rootPath)
        val matches = root.walkTopDown()
          .filter { it.isFile && regex.matches(it.name) }
          .take(100)
          .map { it.relativeTo(root).path }
          .toList()
        val text = if (matches.isEmpty()) "No files found matching pattern '$pattern'."
        else "Found ${matches.size} files:\n" + matches.joinToString("\n") { "• $it" }
        AutonomousToolResult.Success(name, text)
      }

      "find_in_files" -> withContext(Dispatchers.IO) {
        val query = args.optString("query")
        val ext = args.optString("extension").removePrefix(".")
        val prefix = args.optString("path_prefix", "")
        if (query.isBlank()) return@withContext AutonomousToolResult.Error(name, "query is required.")
        val root = File(project.rootPath)
        val results = mutableListOf<String>()
        root.walkTopDown().filter { file ->
          file.isFile && (ext.isBlank() || file.extension.equals(ext, ignoreCase = true)) &&
            (prefix.isBlank() || file.relativeTo(root).path.startsWith(prefix))
        }.take(300).forEach { file ->
          try {
            val rel = file.relativeTo(root).path
            file.useLines { lines ->
              lines.forEachIndexed { idx, line ->
                if (line.contains(query, ignoreCase = true)) {
                  results.add("$rel:${idx + 1}: ${line.trim()}")
                }
              }
            }
          } catch (_: Exception) {}
        }
        val out = if (results.isEmpty()) "No occurrences of '$query' found."
        else "Found ${results.size} occurrences:\n" + results.take(60).joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "get_file_info" -> withContext(Dispatchers.IO) {
        val relPath = args.optString("relative_path")
        if (relPath.isBlank()) return@withContext AutonomousToolResult.Error(name, "relative_path is required.")
        val file = workspaceManager.resolveSafeFile(project, relPath)
        if (!file.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $relPath")
        val lineCount = if (file.isFile) try { file.readLines().size } catch (_: Exception) { -1 } else 0
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val info = buildString {
          appendLine("Path: ${file.relativeTo(File(project.rootPath)).path}")
          appendLine("Type: ${if (file.isDirectory) "Directory" else "File"}")
          appendLine("Size: ${file.length()} bytes")
          if (file.isFile && lineCount >= 0) appendLine("Lines: $lineCount")
          appendLine("Last Modified: ${sdf.format(Date(file.lastModified()))}")
          appendLine("Readable: ${file.canRead()}, Writable: ${file.canWrite()}")
          appendLine("Extension: ${file.extension.ifBlank { "(none)" }}")
        }
        AutonomousToolResult.Success(name, info)
      }

      "get_file_tree" -> withContext(Dispatchers.IO) {
        val relPath = args.optString("relative_path", "")
        val maxDepth = args.optInt("max_depth", 4)
        val target = if (relPath.isBlank()) File(project.rootPath) else workspaceManager.resolveSafeFile(project, relPath)
        if (!target.exists()) return@withContext AutonomousToolResult.Error(name, "Directory does not exist: $relPath")

        fun renderTree(dir: File, prefix: String, depth: Int): String {
          if (depth > maxDepth) return ""
          val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: return ""
          return buildString {
            files.filter { !it.name.startsWith(".") && it.name != "build" && it.name != "node_modules" }.forEachIndexed { idx, f ->
              val isLast = idx == files.size - 1
              val branch = if (isLast) "└── " else "├── "
              val nextPrefix = prefix + if (isLast) "    " else "│   "
              appendLine("$prefix$branch${f.name}${if (f.isDirectory) "/" else ""}")
              if (f.isDirectory) {
                append(renderTree(f, nextPrefix, depth + 1))
              }
            }
          }
        }

        val tree = "${target.name}/\n" + renderTree(target, "", 1)
        AutonomousToolResult.Success(name, tree)
      }

      "compare_files" -> withContext(Dispatchers.IO) {
        val pathA = args.optString("path_a")
        val pathB = args.optString("path_b")
        if (pathA.isBlank() || pathB.isBlank()) return@withContext AutonomousToolResult.Error(name, "path_a and path_b are required.")
        val fileA = workspaceManager.resolveSafeFile(project, pathA)
        val fileB = workspaceManager.resolveSafeFile(project, pathB)
        if (!fileA.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $pathA")
        if (!fileB.exists()) return@withContext AutonomousToolResult.Error(name, "File does not exist: $pathB")
        val contentA = fileA.readText(Charsets.UTF_8)
        val contentB = fileB.readText(Charsets.UTF_8)
        val hunks = DiffUtil.computeDiff(contentA, contentB)
        val diffReport = buildString {
          appendLine("--- a/$pathA")
          appendLine("+++ b/$pathB")
          if (hunks.isEmpty()) {
            appendLine("(Files are identical)")
          } else {
            hunks.forEach { hunk ->
              appendLine("@@ -${hunk.oldStart},${hunk.oldLinesCount} +${hunk.newStart},${hunk.newLinesCount} @@")
              hunk.lines.forEach { line ->
                val sign = when (line.type) {
                  com.example.data.model.DiffLineType.ADDITION -> "+"
                  com.example.data.model.DiffLineType.DELETION -> "-"
                  else -> " "
                }
                appendLine("$sign ${line.text}")
              }
            }
          }
        }
        AutonomousToolResult.Success(name, diffReport)
      }

      "watch_file_changes" -> withContext(Dispatchers.IO) {
        val since = args.optLong("since_timestamp", 0L)
        val limit = args.optInt("limit", 20)
        val manifest = workspaceManager.getOperationHistory().filter { it.timestamp >= since }.takeLast(limit)
        val text = if (manifest.isEmpty()) "No recent file operations recorded."
        else manifest.joinToString("\n") {
          val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it.timestamp))
          "[$time] ${it.operation.name}: ${it.relativePath} - ${it.description}"
        }
        AutonomousToolResult.Success(name, text)
      }

      else -> null
    }
  }
}
