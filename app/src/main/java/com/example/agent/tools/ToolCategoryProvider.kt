package com.example.agent.tools

import com.example.data.model.Project
import org.json.JSONObject

/**
 * Common interface for modular, category-specific tool implementations.
 * Enables the autonomous coding agent to invoke native development capabilities
 * across Filesystem, Code Understanding, Code Editing, Terminal, Debugging,
 * Git, Project Management, Web/Docs, Android Dev, Multi-Agent, MCP, and Security.
 */
interface ToolCategoryProvider {
  val categoryName: String

  fun getDefinitions(): List<ToolDefinition>

  suspend fun execute(
    name: String,
    args: JSONObject,
    project: Project,
    autoApproveWrites: Boolean,
    autoApproveCommands: Boolean
  ): AutonomousToolResult?
}
