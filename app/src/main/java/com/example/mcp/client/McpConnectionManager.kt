package com.example.mcp.client

import android.content.Context
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpServerStatus
import com.example.mcp.model.McpTransportType
import com.example.mcp.security.McpAuthManager
import com.example.mcp.transport.McpTransport
import com.example.mcp.transport.ServerSentEventsTransport
import com.example.mcp.transport.StdioProcessTransport
import com.example.mcp.transport.StreamableHttpTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class McpConnectionManager(
  private val context: Context,
  private val authManager: McpAuthManager
) {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val activeClients = ConcurrentHashMap<String, McpClient>()

  private val _serverStatuses = MutableStateFlow<Map<String, McpServerStatus>>(emptyMap())
  val serverStatuses: StateFlow<Map<String, McpServerStatus>> = _serverStatuses.asStateFlow()

  suspend fun connect(config: McpServerConfig): McpClient {
    val existing = activeClients[config.id]
    if (existing != null && existing.isConnected) {
      return existing
    }

    updateStatus(config.id, McpServerStatus.CONNECTING)
    val transport = createTransport(config)
    val client = McpClient(config, transport)

    try {
      client.connectAndInitialize()
      activeClients[config.id] = client
      updateStatus(config.id, McpServerStatus.CONNECTED)
      return client
    } catch (e: Exception) {
      updateStatus(config.id, McpServerStatus.ERROR)
      throw e
    }
  }

  fun disconnect(serverId: String) {
    val client = activeClients.remove(serverId)
    client?.disconnect()
    updateStatus(serverId, McpServerStatus.DISCONNECTED)
  }

  fun disconnectAll() {
    activeClients.forEach { (id, client) ->
      client.disconnect()
      updateStatus(id, McpServerStatus.DISCONNECTED)
    }
    activeClients.clear()
  }

  fun getClient(serverId: String): McpClient? {
    return activeClients[serverId]
  }

  fun isConnected(serverId: String): Boolean {
    return activeClients[serverId]?.isConnected == true
  }

  fun getAllConnectedClients(): List<McpClient> {
    return activeClients.values.filter { it.isConnected }
  }

  private fun createTransport(config: McpServerConfig): McpTransport {
    val resolvedHeaders = authManager.buildResolvedHeaders(config)
    return when (config.transportType) {
      McpTransportType.STREAMABLE_HTTP -> {
        StreamableHttpTransport(
          url = config.url,
          headers = resolvedHeaders,
          timeoutSeconds = config.timeoutSeconds
        )
      }
      McpTransportType.SERVER_SENT_EVENTS -> {
        ServerSentEventsTransport(
          sseUrl = config.url,
          headers = resolvedHeaders,
          timeoutSeconds = config.timeoutSeconds
        )
      }
      McpTransportType.STDIO -> {
        val tokens = config.url.split(" ")
        StdioProcessTransport(
          command = tokens,
          workingDir = File(context.filesDir, "mcp_workspace").apply { mkdirs() },
          env = config.environmentVariables,
          timeoutSeconds = config.timeoutSeconds
        )
      }
    }
  }

  private fun updateStatus(serverId: String, status: McpServerStatus) {
    val current = _serverStatuses.value.toMutableMap()
    current[serverId] = status
    _serverStatuses.value = current
  }

  fun scheduleHealthCheck() {
    scope.launch {
      while (true) {
        delay(30000L) // Check every 30s
        activeClients.forEach { (id, client) ->
          val alive = client.ping()
          if (!alive && _serverStatuses.value[id] == McpServerStatus.CONNECTED) {
            updateStatus(id, McpServerStatus.ERROR)
          } else if (alive && _serverStatuses.value[id] != McpServerStatus.CONNECTED) {
            updateStatus(id, McpServerStatus.CONNECTED)
          }
        }
      }
    }
  }
}
