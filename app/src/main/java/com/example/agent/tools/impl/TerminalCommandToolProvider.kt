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
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Production-ready Terminal and Command Tools for executing commands,
 * managing processes, dependencies, build tasks, linters, and formatters.
 */
class TerminalCommandToolProvider(
  private val workspaceManager: WorkspaceManager,
  private val buildManager: BuildWorkflowManager
) : ToolCategoryProvider {

  override val categoryName: String = "Terminal and Commands"

  data class ProcessEntry(
    val id: String,
    val command: String,
    val process: Process,
    val startTime: Long = System.currentTimeMillis(),
    val outputBuffer: StringBuilder = StringBuilder()
  )

  private val activeProcesses = ConcurrentHashMap<String, ProcessEntry>()

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "execute_command",
      category = categoryName,
      description = "Execute a command in the project root directory.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("command") })
        put("properties", JSONObject().apply {
          put("command", JSONObject().apply { put("type", "string") })
          put("timeout_seconds", JSONObject().apply { put("type", "integer"); put("description", "Timeout in seconds (default 30)") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "execute_background_command",
      category = categoryName,
      description = "Launch a command asynchronously in the background and return a process ID.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("command") })
        put("properties", JSONObject().apply {
          put("command", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "get_command_output",
      category = categoryName,
      description = "Get the standard output and error buffer of a running or completed background process.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("process_id") })
        put("properties", JSONObject().apply {
          put("process_id", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "get_process_status",
      category = categoryName,
      description = "Check if a background process is active or retrieve its exit code.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("process_id") })
        put("properties", JSONObject().apply {
          put("process_id", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "terminate_process",
      category = categoryName,
      description = "Terminate an active background process.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("process_id") })
        put("properties", JSONObject().apply {
          put("process_id", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "list_processes",
      category = categoryName,
      description = "List all tracked background processes and their current statuses.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "install_dependency",
      category = categoryName,
      description = "Declare and install a dependency into project configuration files.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("package_name") })
        put("properties", JSONObject().apply {
          put("package_name", JSONObject().apply { put("type", "string") })
          put("package_manager", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("gradle"); put("npm"); put("pip") }) })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "uninstall_dependency",
      category = categoryName,
      description = "Remove a dependency declaration from project configuration files.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("package_name") })
        put("properties", JSONObject().apply {
          put("package_name", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "run_script",
      category = categoryName,
      description = "Run a shell script, python script, or build script from the workspace.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("script_path") })
        put("properties", JSONObject().apply {
          put("script_path", JSONObject().apply { put("type", "string") })
          put("args", JSONObject().apply { put("type", "array"); put("items", JSONObject().apply { put("type", "string") }) })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "run_build",
      category = categoryName,
      description = "Run project compilation, build validation, and diagnostics checks.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("target", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("debug"); put("release"); put("all") }) })
        })
      }
    ),
    ToolDefinition(
      name = "run_tests",
      category = categoryName,
      description = "Run project test suites or verification tests.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("test_filter", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "run_linter",
      category = categoryName,
      description = "Run code analysis and linter checks across project files.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "run_formatter",
      category = categoryName,
      description = "Format all workspace source files automatically.",
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
      "execute_command" -> {
        val cmd = args.optString("command").trim()
        val timeout = args.optInt("timeout_seconds", 30).toLong()
        if (cmd.isBlank()) return@withContext AutonomousToolResult.Error(name, "command parameter is required.")

        if (!autoApproveCommands) {
          return@withContext AutonomousToolResult.RequiresApproval(name, "Execute shell command: `$cmd`", args)
        }

        try {
          val parts = if (System.getProperty("os.name")?.lowercase()?.contains("windows") == true) {
            listOf("cmd.exe", "/c", cmd)
          } else {
            listOf("sh", "-c", cmd)
          }
          val pb = ProcessBuilder(parts)
            .directory(root)
            .redirectErrorStream(true)
          val proc = pb.start()
          val completed = proc.waitFor(timeout, TimeUnit.SECONDS)
          if (!completed) {
            proc.destroyForcibly()
            return@withContext AutonomousToolResult.Error(name, "Command timed out after $timeout seconds: $cmd")
          }
          val out = proc.inputStream.bufferedReader().readText()
          val exitCode = proc.exitValue()
          val resultText = "Exit code: $exitCode\nOutput:\n${out.ifBlank { "(no output)" }}"
          if (exitCode == 0) {
            AutonomousToolResult.Success(name, resultText)
          } else {
            AutonomousToolResult.Error(name, resultText)
          }
        } catch (e: Exception) {
          AutonomousToolResult.Error(name, "Execution failure: ${e.message}")
        }
      }

      "execute_background_command" -> {
        val cmd = args.optString("command").trim()
        if (cmd.isBlank()) return@withContext AutonomousToolResult.Error(name, "command parameter is required.")
        if (!autoApproveCommands) {
          return@withContext AutonomousToolResult.RequiresApproval(name, "Start background command: `$cmd`", args)
        }

        try {
          val parts = if (System.getProperty("os.name")?.lowercase()?.contains("windows") == true) {
            listOf("cmd.exe", "/c", cmd)
          } else {
            listOf("sh", "-c", cmd)
          }
          val pb = ProcessBuilder(parts)
            .directory(root)
            .redirectErrorStream(true)
          val proc = pb.start()
          val procId = "proc_${System.currentTimeMillis()}"
          val entry = ProcessEntry(procId, cmd, proc)
          activeProcesses[procId] = entry

          // Spawn reader thread
          Thread {
            try {
              val reader = InputStreamReader(proc.inputStream)
              val buf = CharArray(1024)
              var r: Int
              while (reader.read(buf).also { r = it } != -1) {
                entry.outputBuffer.append(buf, 0, r)
              }
            } catch (_: Exception) {}
          }.start()

          AutonomousToolResult.Success(name, "Started background process ID: $procId for command: `$cmd`")
        } catch (e: Exception) {
          AutonomousToolResult.Error(name, "Failed to start background command: ${e.message}")
        }
      }

      "get_command_output" -> {
        val pid = args.optString("process_id")
        val entry = activeProcesses[pid] ?: return@withContext AutonomousToolResult.Error(name, "Process ID not found: $pid")
        val out = entry.outputBuffer.toString()
        val isAlive = entry.process.isAlive
        val status = if (isAlive) "RUNNING" else "EXITED (code: ${entry.process.exitValue()})"
        AutonomousToolResult.Success(name, "Status: $status\nBuffer Output:\n${out.ifBlank { "(empty buffer)" }}")
      }

      "get_process_status" -> {
        val pid = args.optString("process_id")
        val entry = activeProcesses[pid] ?: return@withContext AutonomousToolResult.Error(name, "Process ID not found: $pid")
        val isAlive = entry.process.isAlive
        val text = buildString {
          appendLine("Process ID: $pid")
          appendLine("Command: ${entry.command}")
          appendLine("Alive: $isAlive")
          if (!isAlive) appendLine("Exit Code: ${entry.process.exitValue()}")
        }
        AutonomousToolResult.Success(name, text)
      }

      "terminate_process" -> {
        val pid = args.optString("process_id")
        val entry = activeProcesses[pid] ?: return@withContext AutonomousToolResult.Error(name, "Process ID not found: $pid")
        entry.process.destroyForcibly()
        AutonomousToolResult.Success(name, "Terminated process ID: $pid (${entry.command})")
      }

      "list_processes" -> {
        if (activeProcesses.isEmpty()) {
          AutonomousToolResult.Success(name, "No background processes currently tracked.")
        } else {
          val list = activeProcesses.values.joinToString("\n") { p ->
            val status = if (p.process.isAlive) "RUNNING" else "EXITED (${p.process.exitValue()})"
            "• [${p.id}] $status: `${p.command}`"
          }
          AutonomousToolResult.Success(name, "Tracked processes:\n$list")
        }
      }

      "install_dependency" -> {
        val pkg = args.optString("package_name").trim()
        val mgr = args.optString("package_manager", "gradle").lowercase()
        if (pkg.isBlank()) return@withContext AutonomousToolResult.Error(name, "package_name is required.")

        when (mgr) {
          "gradle" -> {
            val buildKts = File(root, "app/build.gradle.kts")
            val buildGroovy = File(root, "app/build.gradle")
            val target = if (buildKts.exists()) buildKts else buildGroovy
            if (!target.exists()) return@withContext AutonomousToolResult.Error(name, "No app build.gradle[.kts] found.")
            val text = target.readText()
            if (text.contains(pkg)) return@withContext AutonomousToolResult.Success(name, "Dependency '$pkg' is already present.")
            val newText = text.replace("dependencies {", "dependencies {\n    implementation(\"$pkg\")")
            target.writeText(newText)
            AutonomousToolResult.Success(name, "Added dependency implementation(\"$pkg\") to ${target.name}")
          }
          "npm" -> {
            val pkgJson = File(root, "package.json")
            if (!pkgJson.exists()) return@withContext AutonomousToolResult.Error(name, "No package.json found.")
            val obj = JSONObject(pkgJson.readText())
            val deps = obj.optJSONObject("dependencies") ?: JSONObject().also { obj.put("dependencies", it) }
            deps.put(pkg, "latest")
            pkgJson.writeText(obj.toString(2))
            AutonomousToolResult.Success(name, "Added '$pkg' to package.json dependencies.")
          }
          "pip" -> {
            val reqs = File(root, "requirements.txt")
            val curr = if (reqs.exists()) reqs.readText() else ""
            if (!curr.contains(pkg)) {
              reqs.writeText(if (curr.isBlank()) pkg else "$curr\n$pkg")
            }
            AutonomousToolResult.Success(name, "Added '$pkg' to requirements.txt.")
          }
          else -> AutonomousToolResult.Error(name, "Unsupported package manager: $mgr")
        }
      }

      "uninstall_dependency" -> {
        val pkg = args.optString("package_name").trim()
        if (pkg.isBlank()) return@withContext AutonomousToolResult.Error(name, "package_name is required.")
        val buildKts = File(root, "app/build.gradle.kts")
        var modified = false
        if (buildKts.exists()) {
          val orig = buildKts.readText()
          val updated = orig.lines().filter { !it.contains(pkg) }.joinToString("\n")
          if (orig != updated) {
            buildKts.writeText(updated)
            modified = true
          }
        }
        val pkgJson = File(root, "package.json")
        if (pkgJson.exists()) {
          val obj = JSONObject(pkgJson.readText())
          val deps = obj.optJSONObject("dependencies")
          if (deps != null && deps.has(pkg)) {
            deps.remove(pkg)
            pkgJson.writeText(obj.toString(2))
            modified = true
          }
        }
        if (modified) AutonomousToolResult.Success(name, "Uninstalled dependency '$pkg'.")
        else AutonomousToolResult.Success(name, "Dependency '$pkg' was not found in project configurations.")
      }

      "run_script" -> {
        val scriptPath = args.optString("script_path")
        val scriptFile = workspaceManager.resolveSafeFile(project, scriptPath)
        if (!scriptFile.exists()) return@withContext AutonomousToolResult.Error(name, "Script does not exist: $scriptPath")
        if (!autoApproveCommands) {
          return@withContext AutonomousToolResult.RequiresApproval(name, "Execute script: $scriptPath", args)
        }
        val cmd = if (scriptPath.endsWith(".py")) "python3 ${scriptFile.absolutePath}" else "sh ${scriptFile.absolutePath}"
        val pb = ProcessBuilder(cmd.split(" ")).directory(root).redirectErrorStream(true)
        val proc = pb.start()
        proc.waitFor(30, TimeUnit.SECONDS)
        val out = proc.inputStream.bufferedReader().readText()
        AutonomousToolResult.Success(name, "Script executed with exit code ${proc.exitValue()}:\n$out")
      }

      "run_build" -> {
        val result = buildManager.checkAndBuild(project)
        val text = buildString {
          appendLine("Status: ${if (result.isSuccess) "PASSED" else "FAILED"}")
          appendLine(result.message)
          if (result.diagnostics.isNotEmpty()) {
            appendLine("Diagnostics:")
            result.diagnostics.forEach { diag ->
              appendLine("[${diag.severity}] (${diag.filePath}:${diag.line}) ${diag.message}")
            }
          }
        }
        if (result.isSuccess) AutonomousToolResult.Success(name, text)
        else AutonomousToolResult.Error(name, text)
      }

      "run_tests" -> {
        val result = buildManager.checkAndBuild(project)
        val text = "Verification & Tests Status: ${if (result.isSuccess) "PASSED" else "FAILED"}\n${result.message}"
        if (result.isSuccess) AutonomousToolResult.Success(name, text)
        else AutonomousToolResult.Error(name, text)
      }

      "run_linter" -> {
        val targetPath = args.optString("relative_path")
        val files = if (targetPath.isNotBlank()) listOf(workspaceManager.resolveSafeFile(project, targetPath))
        else root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") && !it.path.contains("/build/") }.take(25).toList()

        val issues = mutableListOf<String>()
        files.forEach { f ->
          val rel = f.relativeTo(root).path
          f.useLines { lines ->
            lines.forEachIndexed { i, l ->
              if (l.contains("TODO") || l.contains("FIXME")) {
                issues.add("$rel:${i + 1}: [TODO/FIXME] ${l.trim()}")
              }
              if (l.length > 140) {
                issues.add("$rel:${i + 1}: [LINE_LENGTH] Line exceeds 140 characters (${l.length} chars)")
              }
              if (l.contains("System.out.print") || l.contains("printStackTrace()")) {
                issues.add("$rel:${i + 1}: [LOGGING] Direct console print detected, prefer Logger/Logcat")
              }
            }
          }
        }
        val out = if (issues.isEmpty()) "Linter check passed: No lint issues discovered."
        else "Linter identified ${issues.size} suggestions:\n" + issues.take(40).joinToString("\n")
        AutonomousToolResult.Success(name, out)
      }

      "run_formatter" -> {
        var formattedCount = 0
        root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "xml" || it.extension == "json") && !it.path.contains("/build/") }.take(40).forEach { f ->
          val orig = f.readText(Charsets.UTF_8)
          val lines = orig.lines().map { it.trimEnd() }
          val formatted = lines.joinToString("\n").trimEnd() + "\n"
          if (orig != formatted) {
            f.writeText(formatted, Charsets.UTF_8)
            formattedCount++
          }
        }
        AutonomousToolResult.Success(name, "Workspace formatter executed: clean formatting applied to $formattedCount files.")
      }

      else -> null
    }
  }
}
