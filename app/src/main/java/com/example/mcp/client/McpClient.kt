package com.example.mcp.client

import com.example.mcp.model.McpPrompt
import com.example.mcp.model.McpPromptArgument
import com.example.mcp.model.McpResource
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpToolDefinition
import com.example.mcp.model.McpToolExecutionResult
import com.example.mcp.protocol.McpJsonRpc
import com.example.mcp.protocol.McpProtocolConstants
import com.example.mcp.transport.McpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class McpClient(
  val serverConfig: McpServerConfig,
  private val transport: McpTransport
) {

  private val nextRequestId = AtomicLong(1L)
  var serverProtocolVersion: String = McpProtocolConstants.LATEST_PROTOCOL_VERSION
    private set
  var serverCapabilities: JSONObject = JSONObject()
    private set
  var isInitialized: Boolean = false
    private set

  suspend fun connectAndInitialize(): Boolean = withContext(Dispatchers.IO) {
    try {
      transport.connect()

      // 1. Send initialize request
      val initParams = JSONObject().apply {
        put("protocolVersion", McpProtocolConstants.LATEST_PROTOCOL_VERSION)
        put("clientInfo", JSONObject().apply {
          put("name", "Claude Code Android")
          put("version", "1.0.0")
        })
        put("capabilities", JSONObject().apply {
          put("tools", JSONObject().apply { put("listChanged", true) })
          put("resources", JSONObject().apply { put("subscribe", true) })
          put("prompts", JSONObject().apply { put("listChanged", true) })
        })
      }

      val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_INITIALIZE, initParams)
      val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())

      if (resp.isError) {
        throw IOException("MCP initialize failed [${resp.errorCode}]: ${resp.errorMessage}")
      }

      val result = resp.result ?: JSONObject()
      serverProtocolVersion = result.optString("protocolVersion", McpProtocolConstants.LATEST_PROTOCOL_VERSION)
      serverCapabilities = result.optJSONObject("capabilities") ?: JSONObject()

      // 2. Send initialized notification
      val initializedNotification = McpJsonRpc.createNotification(McpProtocolConstants.METHOD_INITIALIZED, JSONObject())
      transport.sendNotification(initializedNotification)

      isInitialized = true
      true
    } catch (e: Exception) {
      transport.disconnect()
      isInitialized = false
      throw e
    }
  }

  suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
    if (!transport.isConnected) return@withContext false
    try {
      val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_PING, JSONObject())
      val resp = transport.sendRequest(req, 5000L)
      !resp.isError
    } catch (e: Exception) {
      false
    }
  }

  suspend fun listTools(): List<McpToolDefinition> = withContext(Dispatchers.IO) {
    ensureConnected()
    val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_TOOLS_LIST, JSONObject())
    val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())

    if (resp.isError) {
      throw IOException("tools/list failed [${resp.errorCode}]: ${resp.errorMessage}")
    }

    val result = resp.result ?: JSONObject()
    val toolsArray = result.optJSONArray("tools") ?: JSONArray()
    val toolsList = mutableListOf<McpToolDefinition>()

    for (i in 0 until toolsArray.length()) {
      val item = toolsArray.optJSONObject(i) ?: continue
      val name = item.optString("name")
      if (name.isBlank()) continue

      val description = item.optString("description", "")
      val inputSchema = item.optJSONObject("inputSchema")?.toString() ?: "{}"
      val toolId = "${serverConfig.id}::$name"

      toolsList.add(
        McpToolDefinition(
          id = toolId,
          serverId = serverConfig.id,
          serverName = serverConfig.name,
          name = name,
          description = description,
          inputSchemaJson = inputSchema
        )
      )
    }

    toolsList
  }

  suspend fun callTool(toolName: String, arguments: JSONObject): McpToolExecutionResult = withContext(Dispatchers.IO) {
    ensureConnected()
    val startTime = System.currentTimeMillis()

    val params = JSONObject().apply {
      put("name", toolName)
      put("arguments", arguments)
    }

    val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_TOOLS_CALL, params)
    val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())
    val duration = System.currentTimeMillis() - startTime

    if (resp.isError) {
      return@withContext McpToolExecutionResult(
        isSuccess = false,
        output = "Error [${resp.errorCode}]: ${resp.errorMessage}",
        isError = true,
        durationMs = duration
      )
    }

    val result = resp.result ?: JSONObject()
    val isErrorFlag = result.optBoolean("isError", false)
    val contentArray = result.optJSONArray("content")

    val outputText = buildString {
      if (contentArray != null && contentArray.length() > 0) {
        for (i in 0 until contentArray.length()) {
          val c = contentArray.optJSONObject(i) ?: continue
          val type = c.optString("type")
          if (type == "text") {
            appendLine(c.optString("text"))
          } else if (type == "resource") {
            val res = c.optJSONObject("resource")
            appendLine("[Resource ${res?.optString("uri")}] ${res?.optString("text")}")
          } else {
            appendLine(c.toString())
          }
        }
      } else {
        // Fallback for tools returning direct text or json
        if (result.has("text")) {
          append(result.optString("text"))
        } else {
          append(result.toString(2))
        }
      }
    }.trim()

    McpToolExecutionResult(
      isSuccess = !isErrorFlag,
      output = outputText,
      isError = isErrorFlag,
      durationMs = duration,
      rawResultJson = result
    )
  }

  suspend fun listResources(): List<McpResource> = withContext(Dispatchers.IO) {
    ensureConnected()
    val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_RESOURCES_LIST, JSONObject())
    val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())
    if (resp.isError) return@withContext emptyList()

    val array = resp.result?.optJSONArray("resources") ?: return@withContext emptyList()
    val list = mutableListOf<McpResource>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      list.add(
        McpResource(
          uri = obj.optString("uri"),
          serverId = serverConfig.id,
          name = obj.optString("name", obj.optString("uri")),
          description = obj.optString("description", ""),
          mimeType = obj.optString("mimeType", null)
        )
      )
    }
    list
  }

  suspend fun readResource(uri: String): String = withContext(Dispatchers.IO) {
    ensureConnected()
    val params = JSONObject().apply { put("uri", uri) }
    val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_RESOURCES_READ, params)
    val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())
    if (resp.isError) throw IOException("Failed to read resource: ${resp.errorMessage}")

    val contents = resp.result?.optJSONArray("contents") ?: JSONArray()
    if (contents.length() > 0) {
      val item = contents.getJSONObject(0)
      item.optString("text", item.optString("blob", ""))
    } else {
      ""
    }
  }

  suspend fun listPrompts(): List<McpPrompt> = withContext(Dispatchers.IO) {
    ensureConnected()
    val req = McpJsonRpc.createRequest(nextRequestId.getAndIncrement(), McpProtocolConstants.METHOD_PROMPTS_LIST, JSONObject())
    val resp = transport.sendRequest(req, (serverConfig.timeoutSeconds * 1000).toLong())
    if (resp.isError) return@withContext emptyList()

    val array = resp.result?.optJSONArray("prompts") ?: return@withContext emptyList()
    val list = mutableListOf<McpPrompt>()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      val name = obj.optString("name")
      val argsArray = obj.optJSONArray("arguments") ?: JSONArray()
      val argsList = mutableListOf<McpPromptArgument>()
      for (j in 0 until argsArray.length()) {
        val argObj = argsArray.optJSONObject(j) ?: continue
        argsList.add(
          McpPromptArgument(
            name = argObj.optString("name"),
            description = argObj.optString("description", ""),
            required = argObj.optBoolean("required", false)
          )
        )
      }
      list.add(
        McpPrompt(
          id = "${serverConfig.id}::$name",
          serverId = serverConfig.id,
          name = name,
          description = obj.optString("description", ""),
          arguments = argsList
        )
      )
    }
    list
  }

  fun disconnect() {
    transport.disconnect()
    isInitialized = false
  }

  val isConnected: Boolean
    get() = transport.isConnected && isInitialized

  private fun ensureConnected() {
    if (!transport.isConnected) {
      throw IOException("MCP server '${serverConfig.name}' is not connected.")
    }
  }
}
