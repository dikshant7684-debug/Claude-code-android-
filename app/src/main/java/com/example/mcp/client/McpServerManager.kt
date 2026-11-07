package com.example.mcp.client

import android.content.Context
import com.example.mcp.logging.McpExecutionLogger
import com.example.mcp.model.McpAuthType
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpServerStatus
import com.example.mcp.model.McpTransportType
import com.example.mcp.persistence.McpDao
import com.example.mcp.persistence.McpServerEntity
import com.example.mcp.persistence.McpToolEntity
import com.example.mcp.registry.McpToolRegistry
import com.example.mcp.security.McpAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class McpServerManager(
  private val context: Context,
  private val mcpDao: McpDao,
  val authManager: McpAuthManager,
  val connectionManager: McpConnectionManager,
  val toolRegistry: McpToolRegistry,
  val logger: McpExecutionLogger
) {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val _serversFlow = MutableStateFlow<List<McpServerConfig>>(emptyList())
  val serversFlow: StateFlow<List<McpServerConfig>> = _serversFlow.asStateFlow()

  init {
    loadServersFromDatabase()
    connectionManager.scheduleHealthCheck()
  }

  private fun loadServersFromDatabase() {
    scope.launch {
      val entities = mcpDao.getAllServers()
      val configs = entities.map { it.toModel() }
      _serversFlow.value = configs

      // Auto-connect enabled and approved servers in background
      configs.filter { it.isEnabled && it.isApprovedByUser }.forEach { server ->
        try {
          connectServer(server)
        } catch (ignored: Exception) {}
      }
    }
  }

  suspend fun addOrUpdateServer(config: McpServerConfig) {
    mcpDao.insertOrUpdateServer(config.toEntity())
    refreshServersList()
    if (config.isEnabled && config.isApprovedByUser) {
      try {
        connectServer(config)
      } catch (ignored: Exception) {}
    }
  }

  suspend fun deleteServer(serverId: String) {
    disconnectServer(serverId)
    mcpDao.deleteToolsForServer(serverId)
    mcpDao.deleteServer(serverId)
    refreshServersList()
  }

  suspend fun connectServer(config: McpServerConfig): Boolean {
    return try {
      val client = connectionManager.connect(config)
      val tools = client.listTools()
      toolRegistry.registerToolsForServer(config, tools)

      // Persist discovered tools
      val toolEntities = tools.map {
        McpToolEntity(
          id = it.id,
          serverId = it.serverId,
          serverName = it.serverName,
          name = it.name,
          description = it.description,
          inputSchemaJson = it.inputSchemaJson,
          requiredPermissionsJson = JSONArray(it.requiredPermissions).toString(),
          isEnabled = it.isEnabled
        )
      }
      mcpDao.deleteToolsForServer(config.id)
      mcpDao.insertTools(toolEntities)

      logger.logEvent(
        serverId = config.id,
        serverName = config.name,
        eventType = "CONNECTED",
        rawMessage = "Successfully connected to ${config.name} over ${config.transportType.displayName}. Discovered ${tools.size} tool(s)."
      )

      val updated = config.copy(
        lastConnectedAt = System.currentTimeMillis(),
        status = McpServerStatus.CONNECTED,
        errorMessage = null
      )
      mcpDao.insertOrUpdateServer(updated.toEntity())
      refreshServersList()
      true
    } catch (e: Exception) {
      logger.logEvent(
        serverId = config.id,
        serverName = config.name,
        eventType = "CONNECTION_FAILED",
        rawMessage = "Failed to connect to ${config.name}: ${e.message}",
        isError = true
      )
      val updated = config.copy(
        status = McpServerStatus.ERROR,
        errorMessage = e.message
      )
      mcpDao.insertOrUpdateServer(updated.toEntity())
      refreshServersList()
      false
    }
  }

  fun disconnectServer(serverId: String) {
    connectionManager.disconnect(serverId)
    toolRegistry.unregisterToolsForServer(serverId)
    scope.launch {
      refreshServersList()
    }
  }

  suspend fun setServerApproved(serverId: String, approved: Boolean) {
    val server = mcpDao.getServerById(serverId) ?: return
    val updated = server.copy(isApprovedByUser = approved)
    mcpDao.insertOrUpdateServer(updated)
    refreshServersList()
    if (approved && updated.isEnabled) {
      connectServer(updated.toModel())
    } else if (!approved) {
      disconnectServer(serverId)
    }
  }

  private suspend fun refreshServersList() {
    val entities = mcpDao.getAllServers()
    val statuses = connectionManager.serverStatuses.value
    _serversFlow.value = entities.map {
      val model = it.toModel()
      val activeStatus = statuses[model.id] ?: if (connectionManager.isConnected(model.id)) McpServerStatus.CONNECTED else McpServerStatus.DISCONNECTED
      model.copy(status = activeStatus)
    }
  }

  private fun McpServerEntity.toModel(): McpServerConfig {
    val headers = mutableMapOf<String, String>()
    try {
      val obj = JSONObject(customHeadersJson)
      obj.keys().forEach { headers[it] = obj.getString(it) }
    } catch (ignored: Exception) {}

    val env = mutableMapOf<String, String>()
    try {
      val obj = JSONObject(envJson)
      obj.keys().forEach { env[it] = obj.getString(it) }
    } catch (ignored: Exception) {}

    val approvedPerms = mutableSetOf<String>()
    try {
      val arr = JSONArray(approvedPermissionsJson)
      for (i in 0 until arr.length()) approvedPerms.add(arr.getString(i))
    } catch (ignored: Exception) {}

    return McpServerConfig(
      id = id,
      name = name,
      url = url,
      transportType = runCatching { McpTransportType.valueOf(transportType) }.getOrDefault(McpTransportType.STREAMABLE_HTTP),
      authType = runCatching { McpAuthType.valueOf(authType) }.getOrDefault(McpAuthType.NONE),
      encryptedAuthToken = encryptedAuthToken,
      customHeaders = headers,
      environmentVariables = env,
      timeoutSeconds = timeoutSeconds,
      description = description,
      isEnabled = isEnabled,
      isApprovedByUser = isApprovedByUser,
      approvedPermissions = approvedPerms,
      lastConnectedAt = lastConnectedAt
    )
  }

  private fun McpServerConfig.toEntity(): McpServerEntity {
    val hObj = JSONObject()
    customHeaders.forEach { (k, v) -> hObj.put(k, v) }

    val eObj = JSONObject()
    environmentVariables.forEach { (k, v) -> eObj.put(k, v) }

    val pArr = JSONArray()
    approvedPermissions.forEach { pArr.put(it) }

    return McpServerEntity(
      id = id,
      name = name,
      url = url,
      transportType = transportType.name,
      authType = authType.name,
      encryptedAuthToken = encryptedAuthToken,
      customHeadersJson = hObj.toString(),
      envJson = eObj.toString(),
      timeoutSeconds = timeoutSeconds,
      description = description,
      isEnabled = isEnabled,
      isApprovedByUser = isApprovedByUser,
      approvedPermissionsJson = pArr.toString(),
      lastConnectedAt = lastConnectedAt
    )
  }
}
