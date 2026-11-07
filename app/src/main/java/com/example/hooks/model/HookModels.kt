package com.example.hooks.model

import com.example.data.model.Project
import org.json.JSONObject

enum class HookEventType(val displayName: String) {
  PRE_TOOL_EXECUTION("Pre-Tool Execution"),
  POST_TOOL_EXECUTION("Post-Tool Execution"),
  FILE_CREATED("File Created"),
  FILE_MODIFIED("File Modified"),
  FILE_DELETED("File Deleted"),
  SESSION_START("Session Start"),
  SESSION_END("Session End"),
  BUILD_EVENT("Build / Verification Event"),
  AGENT_TASK_COMPLETED("Agent Task Completed")
}

enum class HookActionType(val displayName: String) {
  VALIDATE_TOOL("Validate / Restrict Tool"),
  FORMAT_CODE("Format Code Automatically"),
  RUN_TESTS("Run Automated Tests"),
  HTTP_WEBHOOK("Dispatch HTTP Webhook"),
  SEND_NOTIFICATION("System Notification"),
  CUSTOM_SCRIPT("Execute Automation Script")
}

data class HookEvent(
  val eventId: String = java.util.UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val type: HookEventType,
  val project: Project? = null,
  val toolName: String? = null,
  val toolArguments: JSONObject? = null,
  val toolOutput: String? = null,
  val toolSuccess: Boolean = true,
  val filePath: String? = null,
  val agentId: String = "main_agent",
  val taskId: String? = null,
  val executionDepth: Int = 0 // Used to prevent infinite recursive triggers
)

data class HookRule(
  val id: String,
  val name: String,
  val description: String = "",
  val eventType: HookEventType,
  val filePattern: String = "*", // e.g. "*.kt", "*.py", "*.json", "*"
  val actionType: HookActionType,
  val actionConfigJson: String = "{}",
  val isEnabled: Boolean = true,
  val isApprovedByUser: Boolean = true,
  val executionCount: Int = 0,
  val lastExecutedAt: Long = 0L
) {
  fun parseConfig(): JSONObject {
    return try {
      JSONObject(actionConfigJson)
    } catch (e: Exception) {
      JSONObject()
    }
  }

  fun matches(event: HookEvent): Boolean {
    if (!isEnabled || !isApprovedByUser) return false
    if (event.type != eventType) return false

    if (filePattern != "*" && filePattern.isNotBlank()) {
      val target = event.filePath ?: ""
      val ext = target.substringAfterLast('.', "")
      if (filePattern.startsWith("*.") && !target.endsWith(filePattern.removePrefix("*"))) {
        return false
      } else if (!filePattern.startsWith("*.") && !target.contains(filePattern)) {
        return false
      }
    }
    return true
  }
}

data class HookExecutionResult(
  val isAllowed: Boolean = true,
  val modifiedArguments: JSONObject? = null,
  val outputMessage: String = "",
  val shouldAbort: Boolean = false,
  val durationMs: Long = 0L
)
