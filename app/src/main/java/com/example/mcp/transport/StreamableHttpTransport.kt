package com.example.mcp.transport

import com.example.mcp.protocol.McpJsonRpc
import com.example.mcp.protocol.McpProtocolConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StreamableHttpTransport(
  val url: String,
  val headers: Map<String, String> = emptyMap(),
  val timeoutSeconds: Int = 30
) : McpTransport {

  private val isConnectedFlag = AtomicBoolean(false)
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  override fun connect() {
    isConnectedFlag.set(true)
  }

  override fun disconnect() {
    isConnectedFlag.set(false)
    client.dispatcher.cancelAll()
  }

  override fun isConnected(): Boolean {
    return isConnectedFlag.get()
  }

  override fun getTransportName(): String = "Streamable HTTP"

  override fun sendRequest(request: McpJsonRpc, timeoutMs: Long): McpJsonRpc {
    if (!isConnected()) {
      throw IOException("Transport is disconnected from $url")
    }

    val requestBody = request.serialize().toRequestBody(jsonMediaType)
    val requestBuilder = Request.Builder()
      .url(url)
      .post(requestBody)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json, text/event-stream")

    headers.forEach { (k, v) ->
      if (k.isNotBlank() && v.isNotBlank()) {
        requestBuilder.header(k, v)
      }
    }

    val call = client.newCall(requestBuilder.build())
    val response = call.execute()

    if (!response.isSuccessful) {
      val code = response.code
      val bodyStr = response.body?.string() ?: ""
      response.close()
      throw IOException("MCP HTTP Server error HTTP $code: $bodyStr")
    }

    val responseBody = response.body?.string() ?: throw IOException("Empty response from MCP server")
    return McpJsonRpc.parse(responseBody)
  }

  override fun sendNotification(notification: McpJsonRpc) {
    if (!isConnected()) return
    try {
      val requestBody = notification.serialize().toRequestBody(jsonMediaType)
      val requestBuilder = Request.Builder()
        .url(url)
        .post(requestBody)
        .header("Content-Type", "application/json")

      headers.forEach { (k, v) ->
        if (k.isNotBlank() && v.isNotBlank()) {
          requestBuilder.header(k, v)
        }
      }
      client.newCall(requestBuilder.build()).execute().close()
    } catch (e: Exception) {
      // Notifications are fire-and-forget in JSON-RPC
    }
  }
}
