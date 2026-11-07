package com.example.mcp.model

import org.json.JSONObject

enum class McpTransportType(val displayName: String) {
  STREAMABLE_HTTP("Streamable HTTP"),
  SERVER_SENT_EVENTS("Server-Sent Events (SSE)"),
  STDIO("Local Stdio Process")
}

enum class McpAuthType(val displayName: String) {
  NONE("No Authentication"),
  API_KEY("API Key"),
  BEARER_TOKEN("Bearer Token"),
  CUSTOM_HEADERS("Custom HTTP Headers"),
  OAUTH2("OAuth 2.1 Token")
}

enum class McpServerStatus(val displayName: String) {
  DISCONNECTED("Disconnected"),
  CONNECTING("Connecting..."),
  CONNECTED("Connected"),
  ERROR("Connection Error"),
  UNAUTHORIZED("Authentication Required")
}

enum class McpPermission(val identifier: String, val description: String) {
  READ_RESOURCES("read_resources", "Read exposed MCP resources and project data"),
  ACCESS_FILES("access_files", "Access and read project files"),
  EXECUTE_TOOLS("execute_tools", "Invoke remote and local MCP tools"),
  EXTERNAL_API("external_api", "Access third-party APIs and network resources"),
  MODIFY_FILES("modify_files", "Create or edit files on device"),
  EXECUTE_COMMANDS("execute_commands", "Execute terminal or shell commands")
}

data class McpServerConfig(
  val id: String,
  val name: String,
  val url: String,
  val transportType: McpTransportType = McpTransportType.STREAMABLE_HTTP,
  val authType: McpAuthType = McpAuthType.NONE,
  val encryptedAuthToken: String = "",
  val customHeaders: Map<String, String> = emptyMap(),
  val environmentVariables: Map<String, String> = emptyMap(),
  val timeoutSeconds: Int = 30,
  val description: String = "",
  val isEnabled: Boolean = true,
  val isApprovedByUser: Boolean = false,
  val approvedPermissions: Set<String> = setOf(McpPermission.EXECUTE_TOOLS.identifier),
  val lastConnectedAt: Long = 0L,
  val status: McpServerStatus = McpServerStatus.DISCONNECTED,
  val errorMessage: String? = null
)

data class McpToolDefinition(
  val id: String,
  val serverId: String,
  val serverName: String,
  val name: String,
  val description: String,
  val inputSchemaJson: String = "{}",
  val requiredPermissions: List<String> = listOf(McpPermission.EXECUTE_TOOLS.identifier),
  val isEnabled: Boolean = true,
  val executionCount: Int = 0
) {
  fun parseInputSchema(): JSONObject {
    return try {
      JSONObject(inputSchemaJson)
    } catch (e: Exception) {
      JSONObject()
    }
  }
}

data class McpResource(
  val uri: String,
  val serverId: String,
  val name: String,
  val description: String = "",
  val mimeType: String? = null
)

data class McpPrompt(
  val id: String,
  val serverId: String,
  val name: String,
  val description: String = "",
  val arguments: List<McpPromptArgument> = emptyList()
)

data class McpPromptArgument(
  val name: String,
  val description: String = "",
  val required: Boolean = false
)

data class McpToolExecutionResult(
  val isSuccess: Boolean,
  val output: String,
  val isError: Boolean = false,
  val durationMs: Long = 0L,
  val rawResultJson: JSONObject? = null
)
