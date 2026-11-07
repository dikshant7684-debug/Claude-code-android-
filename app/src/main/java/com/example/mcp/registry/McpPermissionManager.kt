package com.example.mcp.registry

import com.example.mcp.model.McpPermission
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpToolDefinition
import java.util.concurrent.ConcurrentHashMap

class McpPermissionManager {

  // Agent-specific approved permissions: agentId -> Set<permissionId>
  private val agentPermissions = ConcurrentHashMap<String, MutableSet<String>>()

  // Tool-level overrides: toolId -> Boolean (true = approved, false = blocked)
  private val toolOverrides = ConcurrentHashMap<String, Boolean>()

  init {
    // Default system permissions for standard coding agents
    grantAgentPermissions("main_agent", setOf(
      McpPermission.READ_RESOURCES.identifier,
      McpPermission.ACCESS_FILES.identifier,
      McpPermission.EXECUTE_TOOLS.identifier,
      McpPermission.EXTERNAL_API.identifier
    ))

    grantAgentPermissions("testing_agent", setOf(
      McpPermission.READ_RESOURCES.identifier,
      McpPermission.ACCESS_FILES.identifier,
      McpPermission.EXECUTE_TOOLS.identifier
    ))
  }

  fun grantAgentPermissions(agentId: String, permissions: Set<String>) {
    val current = agentPermissions.computeIfAbsent(agentId) { mutableSetOf() }
    current.addAll(permissions)
  }

  fun revokeAgentPermission(agentId: String, permission: String) {
    agentPermissions[agentId]?.remove(permission)
  }

  fun setToolApproval(toolId: String, approved: Boolean) {
    toolOverrides[toolId] = approved
  }

  /**
   * Validates if an agent has permission to execute a specific MCP tool on a server.
   */
  fun validateExecutionPermission(
    agentId: String,
    serverConfig: McpServerConfig,
    tool: McpToolDefinition
  ): PermissionCheckResult {
    // 1. Server enabled & approved
    if (!serverConfig.isEnabled) {
      return PermissionCheckResult.Denied("MCP server '${serverConfig.name}' is currently disabled.")
    }
    if (!serverConfig.isApprovedByUser) {
      return PermissionCheckResult.Denied("MCP server '${serverConfig.name}' requires explicit user approval before tools can be executed.")
    }

    // 2. Specific tool override
    val override = toolOverrides[tool.id]
    if (override == false) {
      return PermissionCheckResult.Denied("Tool '${tool.name}' has been explicitly blocked by user.")
    }

    // 3. Agent permission check
    val agentPerms = agentPermissions[agentId] ?: agentPermissions["main_agent"] ?: emptySet()
    for (req in tool.requiredPermissions) {
      if (!agentPerms.contains(req) && !serverConfig.approvedPermissions.contains(req)) {
        return PermissionCheckResult.Denied("Agent '$agentId' lacks required permission '$req' to execute '${tool.name}'.")
      }
    }

    return PermissionCheckResult.Granted
  }

  sealed class PermissionCheckResult {
    object Granted : PermissionCheckResult()
    data class Denied(val reason: String) : PermissionCheckResult()
  }
}
