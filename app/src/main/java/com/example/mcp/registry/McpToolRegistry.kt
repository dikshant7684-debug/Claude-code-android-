package com.example.mcp.registry

import com.example.mcp.client.McpConnectionManager
import com.example.mcp.logging.McpExecutionLogger
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpToolDefinition
import com.example.mcp.model.McpToolExecutionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class McpToolRegistry(
  private val connectionManager: McpConnectionManager,
  private val permissionManager: McpPermissionManager,
  private val logger: McpExecutionLogger
) {

  // toolId -> McpToolDefinition
  private val registeredTools = ConcurrentHashMap<String, McpToolDefinition>()

  private val _toolsFlow = MutableStateFlow<List<McpToolDefinition>>(emptyList())
  val toolsFlow: StateFlow<List<McpToolDefinition>> = _toolsFlow.asStateFlow()

  fun registerToolsForServer(serverConfig: McpServerConfig, tools: List<McpToolDefinition>) {
    // Remove old tools from this server
    registeredTools.entries.removeIf { it.value.serverId == serverConfig.id }
    tools.forEach { tool ->
      registeredTools[tool.id] = tool
    }
    _toolsFlow.value = registeredTools.values.toList()
    logger.logEvent(
      serverId = serverConfig.id,
      serverName = serverConfig.name,
      eventType = "TOOLS_DISCOVERED",
      rawMessage = "Discovered and registered ${tools.size} tool(s): ${tools.joinToString { it.name }}"
    )
  }

  fun unregisterToolsForServer(serverId: String) {
    registeredTools.entries.removeIf { it.value.serverId == serverId }
    _toolsFlow.value = registeredTools.values.toList()
  }

  fun getTool(toolId: String): McpToolDefinition? {
    return registeredTools[toolId]
  }

  fun findToolByName(toolName: String): McpToolDefinition? {
    return registeredTools.values.firstOrNull { it.name.equals(toolName, ignoreCase = true) }
  }

  fun getAllTools(): List<McpToolDefinition> {
    return registeredTools.values.toList()
  }

  suspend fun executeTool(
    toolNameOrId: String,
    arguments: JSONObject,
    agentId: String = "main_agent",
    taskId: String? = null
  ): McpToolExecutionResult {
    val tool = getTool(toolNameOrId) ?: findToolByName(toolNameOrId)
      ?: return McpToolExecutionResult(
        isSuccess = false,
        output = "MCP Tool '$toolNameOrId' not found in active registry. Ensure the server is connected.",
        isError = true
      )

    val client = connectionManager.getClient(tool.serverId)
      ?: return McpToolExecutionResult(
        isSuccess = false,
        output = "MCP Server '${tool.serverName}' is not connected. Connect the server before calling tool '${tool.name}'.",
        isError = true
      )

    // Permission Check
    val permCheck = permissionManager.validateExecutionPermission(agentId, client.serverConfig, tool)
    if (permCheck is McpPermissionManager.PermissionCheckResult.Denied) {
      logger.logEvent(
        serverId = tool.serverId,
        serverName = tool.serverName,
        toolName = tool.name,
        agentId = agentId,
        eventType = "TOOL_BLOCKED",
        rawMessage = "Permission denied: ${permCheck.reason}",
        isError = true
      )
      return McpToolExecutionResult(
        isSuccess = false,
        output = "Permission Denied: ${permCheck.reason}",
        isError = true
      )
    }

    val startTime = System.currentTimeMillis()
    logger.logEvent(
      serverId = tool.serverId,
      serverName = tool.serverName,
      toolName = tool.name,
      agentId = agentId,
      eventType = "TOOL_CALL",
      rawMessage = "Executing tool '${tool.name}' with arguments: $arguments"
    )

    return try {
      val result = client.callTool(tool.name, arguments)
      val duration = System.currentTimeMillis() - startTime

      logger.logEvent(
        serverId = tool.serverId,
        serverName = tool.serverName,
        toolName = tool.name,
        agentId = agentId,
        eventType = if (result.isSuccess) "TOOL_SUCCESS" else "TOOL_FAILED",
        rawMessage = "Tool execution completed (${duration}ms). Result: ${result.output.take(500)}",
        durationMs = duration,
        isError = !result.isSuccess
      )
      result
    } catch (e: Exception) {
      val duration = System.currentTimeMillis() - startTime
      logger.logEvent(
        serverId = tool.serverId,
        serverName = tool.serverName,
        toolName = tool.name,
        agentId = agentId,
        eventType = "TOOL_ERROR",
        rawMessage = "Tool execution failed: ${e.message}",
        durationMs = duration,
        isError = true
      )
      McpToolExecutionResult(
        isSuccess = false,
        output = "Execution failed for '${tool.name}': ${e.message}",
        isError = true,
        durationMs = duration
      )
    }
  }
}
