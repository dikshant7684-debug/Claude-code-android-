package com.example.agent.tools

import com.example.agent.workspace.WorkspaceManager
import com.example.build.BuildWorkflowManager
import com.example.data.files.DiffUtil
import com.example.data.model.FileChange
import com.example.data.model.Project
import com.example.hooks.engine.HookEngine
import com.example.hooks.model.HookEvent
import com.example.hooks.model.HookEventType
import com.example.marketplace.service.SkillAndPluginManager
import com.example.mcp.client.McpServerManager
import com.example.mcp.registry.McpToolRegistry
import com.example.agent.tools.impl.*
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

/**
 * Result of executing an individual autonomous agent tool.
 */
sealed class AutonomousToolResult {
  abstract val toolName: String
  abstract val output: String

  data class Success(
    override val toolName: String,
    override val output: String,
    val modifiedFiles: List<String> = emptyList(),
    val proposedChange: FileChange? = null
  ) : AutonomousToolResult()

  data class Error(
    override val toolName: String,
    override val output: String,
    val exceptionMessage: String? = null
  ) : AutonomousToolResult()

  data class RequiresApproval(
    override val toolName: String,
    val explanation: String,
    val arguments: JSONObject,
    val proposedChange: FileChange? = null
  ) : AutonomousToolResult() {
    override val output: String = "Approval requested: $explanation"
  }
}

/**
 * Metadata definition and schema for discoverable tools.
 */
data class ToolDefinition(
  val name: String,
  val category: String,
  val description: String,
  val parametersSchema: JSONObject,
  val requiresApprovalByDefault: Boolean = false
)

/**
 * Universal Tool Registry & Tool Executor exposing real development capabilities,
 * validated schemas, structured execution, Model Context Protocol (MCP), and Hooks automation.
 */
class UniversalToolRegistry(
  val workspaceManager: WorkspaceManager,
  val buildManager: BuildWorkflowManager,
  val skillManager: SkillAndPluginManager? = null,
  val mcpServerManager: McpServerManager? = null,
  val mcpToolRegistry: McpToolRegistry? = null,
  val hookEngine: HookEngine? = null
) {

  private val registeredTools = mutableMapOf<String, ToolDefinition>()

  val providers: List<ToolCategoryProvider> = listOf(
    FileSystemToolProvider(workspaceManager),
    CodeUnderstandingToolProvider(workspaceManager),
    CodeEditingToolProvider(workspaceManager),
    TerminalCommandToolProvider(workspaceManager, buildManager),
    DebuggingToolProvider(workspaceManager, buildManager),
    GitToolProvider(workspaceManager),
    ProjectManagementToolProvider(workspaceManager),
    WebDocsToolProvider(),
    AndroidDevToolProvider(workspaceManager, buildManager),
    MultiAgentToolProvider(),
    McpExtensionToolProvider(mcpServerManager, mcpToolRegistry, skillManager),
    SecurityQualityToolProvider(workspaceManager)
  )

  init {
    registerDefaultTools()
    providers.forEach { provider ->
      provider.getDefinitions().forEach { def ->
        register(def)
      }
    }
  }

  private fun registerDefaultTools() {
    // 1. inspect_workspace
    register(
      ToolDefinition(
        name = "inspect_workspace",
        category = "Workspace",
        description = "Inspect the project structure, entry points, build files, and source summary.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject())
        }
      )
    )

    // 2. list_directory
    register(
      ToolDefinition(
        name = "list_directory",
        category = "Workspace",
        description = "List files and subdirectories in a relative directory of the workspace.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject().apply {
            put("relative_path", JSONObject().apply {
              put("type", "string")
              put("description", "Relative directory path (e.g. '', 'app/src/main', 'src')")
            })
          })
        }
      )
    )

    // 3. read_file
    register(
      ToolDefinition(
        name = "read_file",
        category = "Workspace",
        description = "Read the exact content of an existing workspace file (UTF-8).",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("relative_path") })
          put("properties", JSONObject().apply {
            put("relative_path", JSONObject().apply { put("type", "string") })
            put("max_lines", JSONObject().apply { put("type", "integer") })
            put("offset", JSONObject().apply { put("type", "integer") })
          })
        }
      )
    )

    // 4. write_file
    register(
      ToolDefinition(
        name = "write_file",
        category = "Workspace",
        description = "Write or overwrite a file with full contents. Automatically generates diffs and safety backups.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("relative_path"); put("content") })
          put("properties", JSONObject().apply {
            put("relative_path", JSONObject().apply { put("type", "string") })
            put("content", JSONObject().apply { put("type", "string") })
          })
        },
        requiresApprovalByDefault = true
      )
    )

    // 5. apply_patch
    register(
      ToolDefinition(
        name = "apply_patch",
        category = "Workspace",
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
      )
    )

    // 6. create_directory
    register(
      ToolDefinition(
        name = "create_directory",
        category = "Workspace",
        description = "Create a directory or nested directories in the workspace.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("relative_dir") })
          put("properties", JSONObject().apply {
            put("relative_dir", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )

    // 7. move_file
    register(
      ToolDefinition(
        name = "move_file",
        category = "Workspace",
        description = "Rename or move a file/directory inside the workspace.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("old_path"); put("new_path") })
          put("properties", JSONObject().apply {
            put("old_path", JSONObject().apply { put("type", "string") })
            put("new_path", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )

    // 8. delete_file
    register(
      ToolDefinition(
        name = "delete_file",
        category = "Workspace",
        description = "Delete a file from the workspace. Backed up before deletion.",
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

    // 9. search_files
    register(
      ToolDefinition(
        name = "search_files",
        category = "Workspace",
        description = "Search across workspace files by keyword substring or regular expression.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("query") })
          put("properties", JSONObject().apply {
            put("query", JSONObject().apply { put("type", "string") })
            put("is_regex", JSONObject().apply { put("type", "boolean") })
          })
        }
      )
    )

    // 10. verify_code
    register(
      ToolDefinition(
        name = "verify_code",
        category = "Execution",
        description = "Run syntax verification, compiler checks, or build diagnostics on all workspace files.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject())
        }
      )
    )

    // 11. inspect_syntax_error
    register(
      ToolDefinition(
        name = "inspect_syntax_error",
        category = "Execution",
        description = "Inspect diagnostics, lint issues, and compile errors across modified workspace files.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject())
        }
      )
    )

    // 12. git_status
    register(
      ToolDefinition(
        name = "git_status",
        category = "Development",
        description = "Inspect changed, staged, and untracked files in the current workspace.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject())
        }
      )
    )

    // 13. lookup_documentation
    register(
      ToolDefinition(
        name = "lookup_documentation",
        category = "Web and Research",
        description = "Look up developer documentation, API guidelines, Android best practices, or library specs.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("topic") })
          put("properties", JSONObject().apply {
            put("topic", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )

    // 14. discover_mcp_tools
    register(
      ToolDefinition(
        name = "discover_mcp_tools",
        category = "MCP and Extensions",
        description = "Discover available Model Context Protocol (MCP) server endpoints and installed extensions.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject())
        }
      )
    )

    // 15. run_plugin_capability
    register(
      ToolDefinition(
        name = "run_plugin_capability",
        category = "MCP and Extensions",
        description = "Execute an installed extension skill or capability.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("extension_id"); put("capability_name") })
          put("properties", JSONObject().apply {
            put("extension_id", JSONObject().apply { put("type", "string") })
            put("capability_name", JSONObject().apply { put("type", "string") })
            put("arguments", JSONObject().apply { put("type", "object") })
          })
        }
      )
    )

    // 16. mcp_call_tool
    register(
      ToolDefinition(
        name = "mcp_call_tool",
        category = "MCP and Extensions",
        description = "Execute a tool exposed by a connected Model Context Protocol (MCP) server.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("tool_name") })
          put("properties", JSONObject().apply {
            put("tool_name", JSONObject().apply { put("type", "string") })
            put("arguments", JSONObject().apply { put("type", "object") })
          })
        }
      )
    )

    // 17. mcp_read_resource
    register(
      ToolDefinition(
        name = "mcp_read_resource",
        category = "MCP and Extensions",
        description = "Read a resource content URI exposed by a connected MCP server.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("required", JSONArray().apply { put("uri") })
          put("properties", JSONObject().apply {
            put("uri", JSONObject().apply { put("type", "string") })
            put("server_id", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )

    // 18. mcp_manage_server
    register(
      ToolDefinition(
        name = "mcp_manage_server",
        category = "MCP and Extensions",
        description = "List, inspect, connect, or disconnect configured MCP servers.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject().apply {
            put("action", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("list"); put("connect"); put("disconnect") }) })
            put("server", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )

    // 19. hooks_manage
    register(
      ToolDefinition(
        name = "hooks_manage",
        category = "Automation and Hooks",
        description = "Inspect active automation rules or trigger automated code formatting on a file.",
        parametersSchema = JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject().apply {
            put("action", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("list"); put("format_file") }) })
            put("relative_path", JSONObject().apply { put("type", "string") })
          })
        }
      )
    )
  }

  fun register(tool: ToolDefinition) {
    registeredTools[tool.name] = tool
  }

  fun getToolsList(): List<ToolDefinition> = registeredTools.values.toList()

  fun generateToolsPrompt(project: Project): String {
    val builder = StringBuilder()
    builder.appendLine("You have real access to the workspace via the following tools:")
    builder.appendLine("To execute a tool, output a JSON block wrapped in <tool_call> tags:")
    builder.appendLine("<tool_call>")
    builder.appendLine("{\"name\": \"tool_name\", \"arguments\": {\"key\": \"value\"}}")
    builder.appendLine("</tool_call>")
    builder.appendLine()
    builder.appendLine("AVAILABLE TOOLS:")

    registeredTools.values.groupBy { it.category }.forEach { (category, tools) ->
      builder.appendLine("\n### Category: $category")
      tools.forEach { tool ->
        builder.appendLine("- **${tool.name}**: ${tool.description}")
        builder.appendLine("  Arguments: ${tool.parametersSchema.toString()}")
      }
    }

    // Append dynamically registered MCP tools
    val mcpTools = mcpToolRegistry?.getAllTools() ?: emptyList()
    if (mcpTools.isNotEmpty()) {
      builder.appendLine("\n### Category: Dynamic Model Context Protocol (MCP) Tools")
      mcpTools.forEach { tool ->
        builder.appendLine("- **${tool.name}** [from MCP server '${tool.serverName}']: ${tool.description}")
        builder.appendLine("  Arguments: ${tool.inputSchemaJson}")
      }
    }

    return builder.toString()
  }

  suspend fun executeTool(
    name: String,
    args: JSONObject,
    project: Project,
    autoApproveWrites: Boolean,
    autoApproveCommands: Boolean = false,
    agentId: String = "main_agent",
    taskId: String? = null
  ): AutonomousToolResult {
    // 1. Pre-execution Hook Validation
    if (hookEngine != null) {
      val preEvent = HookEvent(
        type = HookEventType.PRE_TOOL_EXECUTION,
        project = project,
        toolName = name,
        toolArguments = args,
        agentId = agentId,
        taskId = taskId
      )
      val preCheck = hookEngine.dispatchEvent(preEvent)
      if (!preCheck.isAllowed) {
        return AutonomousToolResult.Error(name, "Execution prevented by pre-tool automation hook: ${preCheck.outputMessage}")
      }
    }

    val result = try {
      var providerResult: AutonomousToolResult? = null
      for (provider in providers) {
        val res = provider.execute(name, args, project, autoApproveWrites, autoApproveCommands)
        if (res != null) {
          providerResult = res
          break
        }
      }

      if (providerResult != null) {
        providerResult
      } else {
        when (name) {
        "inspect_workspace", "inspect_project" -> {
          val overviewText = workspaceManager.getProjectSummaryText(project)
          AutonomousToolResult.Success(name, overviewText)
        }

        "list_directory" -> {
          val relPath = args.optString("relative_path", "")
          val files = workspaceManager.listFilesAndDirs(project, relPath)
          val formatted = if (files.isEmpty()) {
            "Directory '$relPath' is empty."
          } else {
            files.joinToString("\n") { file ->
              val icon = if (file.isDirectory) "📁 [DIR]" else "📄"
              val size = if (file.isDirectory) "" else " (${file.sizeBytes} bytes)"
              "$icon ${file.name}$size"
            }
          }
          AutonomousToolResult.Success(name, formatted)
        }

        "read_file" -> {
          val relPath = args.optString("relative_path")
          if (relPath.isBlank()) {
            return AutonomousToolResult.Error(name, "relative_path parameter is required.")
          }
          val maxLines = args.optInt("max_lines", 1200)
          val offset = args.optInt("offset", 0)
          val content = workspaceManager.readFileContent(project, relPath, maxLines, offset)
          AutonomousToolResult.Success(name, content)
        }

        "write_file" -> {
          val relPath = args.optString("relative_path")
          val content = args.optString("content")
          if (relPath.isBlank()) {
            return AutonomousToolResult.Error(name, "relative_path is required.")
          }

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
            AutonomousToolResult.RequiresApproval(
              toolName = name,
              explanation = "Proposed write to '$relPath' (${hunks.size} diff hunks).",
              arguments = args,
              proposedChange = change
            )
          } else {
            val (change, applied) = workspaceManager.writeCompleteFile(project, relPath, content, true)
            AutonomousToolResult.Success(
              toolName = name,
              output = "File written successfully: $relPath (${content.length} characters).",
              modifiedFiles = listOf(relPath),
              proposedChange = change
            )
          }
        }

        "apply_patch" -> {
          val relPath = args.optString("relative_path")
          val targetContent = args.optString("target_content")
          val replacementContent = args.optString("replacement_content")
          if (relPath.isBlank() || targetContent.isEmpty()) {
            return AutonomousToolResult.Error(name, "relative_path and target_content are required.")
          }

          if (!autoApproveWrites) {
            val file = workspaceManager.resolveSafeFile(project, relPath)
            val orig = if (file.exists()) file.readText() else ""
            val patchResult = DiffUtil.applyBlockReplacement(orig, targetContent, replacementContent)
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
            AutonomousToolResult.RequiresApproval(
              toolName = name,
              explanation = "Proposed patch on '$relPath' (${hunks.size} diff hunks).",
              arguments = args,
              proposedChange = change
            )
          } else {
            val (change, applied) = workspaceManager.applyTargetedPatch(
              project, relPath, targetContent, replacementContent, true
            )
            AutonomousToolResult.Success(
              toolName = name,
              output = "Patch successfully applied to $relPath.",
              modifiedFiles = listOf(relPath),
              proposedChange = change
            )
          }
        }

        "create_directory" -> {
          val relDir = args.optString("relative_dir")
          if (relDir.isBlank()) return AutonomousToolResult.Error(name, "relative_dir parameter is required.")
          val dir = workspaceManager.createDirectory(project, relDir)
          AutonomousToolResult.Success(name, "Directory created: ${dir.name}")
        }

        "move_file" -> {
          val oldPath = args.optString("old_path")
          val newPath = args.optString("new_path")
          if (oldPath.isBlank() || newPath.isBlank()) {
            return AutonomousToolResult.Error(name, "old_path and new_path parameters are required.")
          }
          val success = workspaceManager.renameOrMoveFile(project, oldPath, newPath)
          if (success) {
            AutonomousToolResult.Success(name, "Moved $oldPath to $newPath", listOf(newPath))
          } else {
            AutonomousToolResult.Error(name, "Failed to move $oldPath to $newPath")
          }
        }

        "delete_file" -> {
          val relPath = args.optString("relative_path")
          if (relPath.isBlank()) return AutonomousToolResult.Error(name, "relative_path parameter is required.")

          if (!autoApproveWrites) {
            AutonomousToolResult.RequiresApproval(
              toolName = name,
              explanation = "Delete file '$relPath' from project.",
              arguments = args
            )
          } else {
            val success = workspaceManager.deleteFileOrDir(project, relPath)
            if (success) {
              AutonomousToolResult.Success(name, "File deleted: $relPath", listOf(relPath))
            } else {
              AutonomousToolResult.Error(name, "File not found or failed to delete: $relPath")
            }
          }
        }

        "search_files" -> {
          val query = args.optString("query")
          val isRegex = args.optBoolean("is_regex", false)
          if (query.isBlank()) return AutonomousToolResult.Error(name, "query parameter is required.")
          val results = workspaceManager.searchFiles(project, query, isRegex)
          if (results.isEmpty()) {
            AutonomousToolResult.Success(name, "No matches found for '$query'.")
          } else {
            val formatted = results.joinToString("\n") {
              "${it.relativePath}:${it.lineNumber}: ${it.lineContent}"
            }
            AutonomousToolResult.Success(name, "Found ${results.size} matches:\n$formatted")
          }
        }

        "verify_code", "inspect_syntax_error" -> {
          val result = buildManager.checkAndBuild(project)
          val text = buildString {
            appendLine("Verification Status: ${if (result.isSuccess) "PASSED" else "FAILED"}")
            appendLine(result.message)
            if (result.diagnostics.isNotEmpty()) {
              appendLine("\nDiagnostics:")
              result.diagnostics.forEach { diag ->
                val loc = if (diag.line > 0) " (${diag.filePath}:${diag.line})" else ""
                appendLine("[${diag.severity}]$loc ${diag.message}")
              }
            }
          }
          AutonomousToolResult.Success(name, text)
        }

        "git_status" -> {
          val manifest = workspaceManager.getOperationManifest()
          val text = if (manifest.isEmpty()) {
            "Workspace clean: No file modifications recorded in this session."
          } else {
            buildString {
              appendLine("Session modified files (${manifest.size} operations):")
              manifest.forEach { op ->
                appendLine("- [${op.operation}] ${op.relativePath}")
              }
            }
          }
          AutonomousToolResult.Success(name, text)
        }

        "lookup_documentation" -> {
          val topic = args.optString("topic", "")
          val doc = lookupLocalDocs(topic)
          AutonomousToolResult.Success(name, doc)
        }

        "discover_mcp_tools" -> {
          val list = buildString {
            appendLine("MCP SERVERS & CAPABILITIES:")
            if (mcpServerManager != null) {
              val servers = mcpServerManager.serversFlow.value
              appendLine("- Configured MCP Servers: ${servers.size}")
              servers.forEach { s ->
                val toolCount = mcpToolRegistry?.getAllTools()?.count { it.serverId == s.id } ?: 0
                appendLine("  * [${s.status.name}] ${s.name} (${s.url}) - Transport: ${s.transportType.displayName}, Tools: $toolCount")
              }
            }
            val tools = mcpToolRegistry?.getAllTools() ?: emptyList()
            if (tools.isNotEmpty()) {
              appendLine("\nACTIVE MCP TOOLS (${tools.size}):")
              tools.forEach { t ->
                appendLine("  * [${t.serverName}] ${t.name}: ${t.description.take(80)}")
              }
            } else {
              appendLine("- No external MCP tools currently connected.")
            }
            if (skillManager != null) {
              val installed = skillManager.installedExtensionsFlow.firstOrNull() ?: emptyList()
              appendLine("\nInstalled Skill Extensions: ${installed.size}")
              installed.forEach { ext ->
                appendLine("  * [${ext.id}] ${ext.name} (v${ext.version})")
              }
            }
          }
          AutonomousToolResult.Success(name, list)
        }

        "mcp_call_tool" -> {
          val toolName = args.optString("tool_name").ifBlank { args.optString("name") }
          val toolArgs = args.optJSONObject("arguments") ?: args.optJSONObject("params") ?: JSONObject()
          if (toolName.isBlank()) {
            AutonomousToolResult.Error(name, "tool_name parameter is required.")
          } else if (mcpToolRegistry != null) {
            val res = mcpToolRegistry.executeTool(toolName, toolArgs, agentId, taskId)
            if (res.isSuccess) {
              AutonomousToolResult.Success(name, "MCP Tool '$toolName' output:\n${res.output}")
            } else {
              AutonomousToolResult.Error(name, "MCP Tool '$toolName' execution failed: ${res.output}")
            }
          } else {
            AutonomousToolResult.Error(name, "MCP tool registry is not initialized.")
          }
        }

        "mcp_read_resource" -> {
          val uri = args.optString("uri")
          val serverId = args.optString("server_id")
          if (uri.isBlank()) {
            AutonomousToolResult.Error(name, "uri parameter is required.")
          } else if (mcpServerManager != null) {
            val client = if (serverId.isNotBlank()) {
              mcpServerManager.connectionManager.getClient(serverId)
            } else {
              mcpServerManager.connectionManager.getAllConnectedClients().firstOrNull()
            }
            if (client != null) {
              val content = client.readResource(uri)
              AutonomousToolResult.Success(name, "Resource content ($uri):\n$content")
            } else {
              AutonomousToolResult.Error(name, "No connected MCP server found to read resource '$uri'.")
            }
          } else {
            AutonomousToolResult.Error(name, "MCP connection manager is not available.")
          }
        }

        "mcp_manage_server" -> {
          val action = args.optString("action", "list").lowercase()
          if (mcpServerManager == null) {
            AutonomousToolResult.Error(name, "MCP server manager is not available.")
          } else {
            when (action) {
              "list" -> {
                val servers = mcpServerManager.serversFlow.value
                val text = if (servers.isEmpty()) {
                  "No MCP servers configured."
                } else {
                  servers.joinToString("\n") { s ->
                    "• [${s.status.name}] ${s.name} (${s.url}) - Transport: ${s.transportType.displayName}, Enabled: ${s.isEnabled}, Approved: ${s.isApprovedByUser}"
                  }
                }
                AutonomousToolResult.Success(name, text)
              }
              "connect" -> {
                val serverNameOrId = args.optString("server")
                val server = mcpServerManager.serversFlow.value.firstOrNull { it.id == serverNameOrId || it.name.equals(serverNameOrId, ignoreCase = true) }
                if (server != null) {
                  val ok = mcpServerManager.connectServer(server)
                  AutonomousToolResult.Success(name, if (ok) "Connected to ${server.name} and discovered tools." else "Failed to connect to ${server.name}.")
                } else {
                  AutonomousToolResult.Error(name, "Server '$serverNameOrId' not found.")
                }
              }
              "disconnect" -> {
                val serverNameOrId = args.optString("server")
                val server = mcpServerManager.serversFlow.value.firstOrNull { it.id == serverNameOrId || it.name.equals(serverNameOrId, ignoreCase = true) }
                if (server != null) {
                  mcpServerManager.disconnectServer(server.id)
                  AutonomousToolResult.Success(name, "Disconnected from ${server.name}.")
                } else {
                  AutonomousToolResult.Error(name, "Server '$serverNameOrId' not found.")
                }
              }
              else -> AutonomousToolResult.Error(name, "Unsupported action '$action'. Use list, connect, or disconnect.")
            }
          }
        }

        "hooks_manage" -> {
          val action = args.optString("action", "list").lowercase()
          if (hookEngine == null) {
            AutonomousToolResult.Error(name, "Hook Engine is not initialized.")
          } else {
            when (action) {
              "list" -> {
                val rules = hookEngine.rulesFlow.value
                val text = if (rules.isEmpty()) {
                  "No automation hook rules currently registered."
                } else {
                  rules.joinToString("\n") { r ->
                    "• [${if (r.isEnabled) "ACTIVE" else "DISABLED"}] ${r.name}: Event=${r.eventType.displayName}, Pattern=${r.filePattern}, Action=${r.actionType.displayName}"
                  }
                }
                AutonomousToolResult.Success(name, text)
              }
              "format_file" -> {
                val path = args.optString("relative_path")
                if (path.isBlank()) {
                  AutonomousToolResult.Error(name, "relative_path parameter is required.")
                } else {
                  val res = hookEngine.codeFormatter.formatFile(project, path)
                  AutonomousToolResult.Success(name, res.message)
                }
              }
              else -> AutonomousToolResult.Error(name, "Unsupported hook action '$action'. Use list or format_file.")
            }
          }
        }

        "run_plugin_capability" -> {
          val extId = args.optString("extension_id")
          val capName = args.optString("capability_name")
          val capArgs = args.optJSONObject("arguments") ?: JSONObject()
          if (skillManager != null) {
            val res = skillManager.executePluginCapability(extId, capName, capArgs, project)
            when (res) {
              is com.example.agent.ToolResult.Success -> AutonomousToolResult.Success(name, res.output, proposedChange = res.proposedChange)
              is com.example.agent.ToolResult.Error -> AutonomousToolResult.Error(name, res.message)
              is com.example.agent.ToolResult.RequiresApproval -> AutonomousToolResult.RequiresApproval(name, res.explanation, res.args, res.proposedChange)
            }
          } else {
            AutonomousToolResult.Error(name, "Plugin manager not available.")
          }
        }

        else -> {
          if (mcpToolRegistry != null && mcpToolRegistry.findToolByName(name) != null) {
            val res = mcpToolRegistry.executeTool(name, args, agentId, taskId)
            if (res.isSuccess) {
              AutonomousToolResult.Success(name, "MCP Tool '$name' result:\n${res.output}")
            } else {
              AutonomousToolResult.Error(name, "MCP Tool '$name' failed: ${res.output}")
            }
          } else {
            AutonomousToolResult.Error(name, "Unrecognized tool: $name")
          }
        }
      }
      }
    } catch (e: Exception) {
      AutonomousToolResult.Error(name, "Tool execution exception: ${e.message}", e.message)
    }

    // 2. Post-tool execution and file modification hooks
    if (result is AutonomousToolResult.Success) {
      if (name in listOf("write_file", "replace_string_in_file", "create_file")) {
        val relPath = args.optString("relative_path")
        if (relPath.isNotBlank()) {
          hookEngine?.dispatchEvent(
            HookEvent(
              type = HookEventType.FILE_MODIFIED,
              project = project,
              filePath = relPath,
              toolName = name,
              toolOutput = result.output,
              agentId = agentId,
              taskId = taskId
            )
          )
        }
      }
      hookEngine?.dispatchEvent(
        HookEvent(
          type = HookEventType.POST_TOOL_EXECUTION,
          project = project,
          toolName = name,
          toolArguments = args,
          toolOutput = result.output,
          toolSuccess = true,
          agentId = agentId,
          taskId = taskId
        )
      )
    }

    return result
  }

  /**
   * Robust parser supporting XML <tool_call>, markdown fenced JSON, and raw JSON formats.
   */
  fun parseToolCall(response: String): Pair<String, JSONObject>? {
    // 1. Check for XML tag <tool_call> ... </tool_call>
    val tagRegex = Regex("""<tool_call>\s*([\s\S]*?)\s*</tool_call>""")
    val match = tagRegex.find(response)
    if (match != null) {
      val jsonContent = match.groupValues[1].trim()
      try {
        val obj = JSONObject(jsonContent)
        val name = obj.optString("name").ifBlank { obj.optString("tool") }
        val args = obj.optJSONObject("arguments") ?: obj.optJSONObject("parameters") ?: JSONObject()
        if (name.isNotBlank()) return Pair(name, args)
      } catch (_: Exception) {}
    }

    // 2. Check for markdown code fences ```json ... ```
    val fenceRegex = Regex("""```(?:json)?\s*(\{\s*"(?:name|tool)"[\s\S]*?\})\s*```""")
    val fenceMatch = fenceRegex.find(response)
    if (fenceMatch != null) {
      val jsonContent = fenceMatch.groupValues[1].trim()
      try {
        val obj = JSONObject(jsonContent)
        val name = obj.optString("name").ifBlank { obj.optString("tool") }
        val args = obj.optJSONObject("arguments") ?: obj.optJSONObject("parameters") ?: JSONObject()
        if (name.isNotBlank()) return Pair(name, args)
      } catch (_: Exception) {}
    }

    // 3. Raw JSON object containing "name" and "arguments"
    val trimmed = response.trim()
    if (trimmed.startsWith("{") && (trimmed.contains("\"name\"") || trimmed.contains("\"tool\""))) {
      try {
        val obj = JSONObject(trimmed)
        val name = obj.optString("name").ifBlank { obj.optString("tool") }
        val args = obj.optJSONObject("arguments") ?: obj.optJSONObject("parameters") ?: JSONObject()
        if (name.isNotBlank()) return Pair(name, args)
      } catch (_: Exception) {}
    }

    return null
  }

  private fun lookupLocalDocs(topic: String): String {
    val lower = topic.lowercase()
    return when {
      lower.contains("compose") || lower.contains("button") -> {
        "Jetpack Compose Guidelines: Use Surface or Box with Modifier.clickable. Maintain 48dp minimum touch target. Prefer MaterialTheme.colorScheme tokens."
      }
      lower.contains("gradle") || lower.contains("build") -> {
        "Android Gradle Plugin: Kotlin DSL (build.gradle.kts). Dependencies configured in gradle/libs.versions.toml. Test task: 'gradle :app:testDebugUnitTest'."
      }
      lower.contains("state") || lower.contains("flow") -> {
        "State Management: Use ViewModel and MutableStateFlow. Collect in Composables via collectAsStateWithLifecycle() or collectAsState()."
      }
      else -> {
        "Documentation for '$topic': Ensure modular architecture, explicit nullability checks, and clean error handling."
      }
    }
  }
}
