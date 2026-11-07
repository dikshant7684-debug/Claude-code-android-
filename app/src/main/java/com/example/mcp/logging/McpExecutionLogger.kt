package com.example.mcp.logging

import com.example.mcp.security.McpAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

data class McpLogEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val serverId: String,
  val serverName: String,
  val toolName: String? = null,
  val agentId: String? = null,
  val eventType: String,
  val message: String,
  val durationMs: Long = 0L,
  val isError: Boolean = false
)

class McpExecutionLogger(private val authManager: McpAuthManager) {

  private val buffer = ConcurrentLinkedQueue<McpLogEntry>()
  private val _logsFlow = MutableStateFlow<List<McpLogEntry>>(emptyList())
  val logsFlow: StateFlow<List<McpLogEntry>> = _logsFlow.asStateFlow()

  fun logEvent(
    serverId: String,
    serverName: String,
    eventType: String,
    rawMessage: String,
    toolName: String? = null,
    agentId: String? = null,
    durationMs: Long = 0L,
    isError: Boolean = false
  ) {
    val sanitized = authManager.redactSecrets(rawMessage)
    val entry = McpLogEntry(
      serverId = serverId,
      serverName = serverName,
      toolName = toolName,
      agentId = agentId,
      eventType = eventType,
      message = sanitized,
      durationMs = durationMs,
      isError = isError
    )

    buffer.add(entry)
    while (buffer.size > 200) {
      buffer.poll()
    }
    _logsFlow.value = buffer.toList()
  }

  fun getLogsForServer(serverId: String): List<McpLogEntry> {
    return buffer.filter { it.serverId == serverId }
  }

  fun clearLogs() {
    buffer.clear()
    _logsFlow.value = emptyList()
  }
}
