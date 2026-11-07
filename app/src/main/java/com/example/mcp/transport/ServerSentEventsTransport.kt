package com.example.mcp.transport

import com.example.mcp.protocol.McpJsonRpc
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ServerSentEventsTransport(
  val sseUrl: String,
  val headers: Map<String, String> = emptyMap(),
  val timeoutSeconds: Int = 30
) : McpTransport {

  private val isConnectedFlag = AtomicBoolean(false)
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private var messagePostUrl: String = sseUrl

  private val pendingRequests = ConcurrentHashMap<Long, CompletableFutureHolder>()
  private var sseThread: Thread? = null

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // SSE needs infinite read timeout
    .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    .build()

  private class CompletableFutureHolder {
    val latch = CountDownLatch(1)
    var response: McpJsonRpc? = null
    var error: Exception? = null
  }

  override fun connect() {
    if (isConnectedFlag.get()) return

    val requestBuilder = Request.Builder()
      .url(sseUrl)
      .header("Accept", "text/event-stream")
      .header("Cache-Control", "no-cache")

    headers.forEach { (k, v) ->
      if (k.isNotBlank() && v.isNotBlank()) {
        requestBuilder.header(k, v)
      }
    }

    val connectLatch = CountDownLatch(1)
    var connectError: Exception? = null

    sseThread = Thread {
      try {
        val call = client.newCall(requestBuilder.build())
        val response = call.execute()

        if (!response.isSuccessful) {
          connectError = IOException("Failed to establish SSE stream: HTTP ${response.code}")
          connectLatch.countDown()
          return@Thread
        }

        isConnectedFlag.set(true)
        connectLatch.countDown()

        val inputStream = response.body?.byteStream() ?: return@Thread
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var line: String? = null
        var currentEvent = "message"
        val dataBuilder = StringBuilder()

        while (isConnectedFlag.get() && reader.readLine().also { line = it } != null) {
          val l = line!!.trim()
          if (l.isEmpty()) {
            if (dataBuilder.isNotEmpty()) {
              handleSseEvent(currentEvent, dataBuilder.toString())
              dataBuilder.setLength(0)
            }
            currentEvent = "message"
          } else if (l.startsWith("event:")) {
            currentEvent = l.substring("event:".length).trim()
          } else if (l.startsWith("data:")) {
            dataBuilder.append(l.substring("data:".length).trim())
          }
        }
      } catch (e: Exception) {
        connectError = e
        connectLatch.countDown()
      } finally {
        isConnectedFlag.set(false)
      }
    }.apply {
      isDaemon = true
      start()
    }

    connectLatch.await(timeoutSeconds.toLong(), TimeUnit.SECONDS)
    if (connectError != null) {
      throw connectError!!
    }
  }

  private fun handleSseEvent(event: String, data: String) {
    if (event == "endpoint") {
      // MCP SSE specifies that the endpoint event delivers the URL to send POST messages to
      messagePostUrl = if (data.startsWith("http://") || data.startsWith("https://")) {
        data
      } else {
        val base = sseUrl.substringBeforeLast('/')
        if (data.startsWith("/")) "$base$data" else "$base/$data"
      }
    } else {
      try {
        val parsed = McpJsonRpc.parse(data)
        if (parsed.isResponse) {
          val id = parsed.id
          val holder = pendingRequests[id]
          if (holder != null) {
            holder.response = parsed
            holder.latch.countDown()
          }
        }
      } catch (ignored: Exception) {}
    }
  }

  override fun disconnect() {
    isConnectedFlag.set(false)
    sseThread?.interrupt()
    sseThread = null
    client.dispatcher.cancelAll()
  }

  override fun isConnected(): Boolean = isConnectedFlag.get()

  override fun getTransportName(): String = "Server-Sent Events (SSE)"

  override fun sendRequest(request: McpJsonRpc, timeoutMs: Long): McpJsonRpc {
    if (!isConnected()) {
      throw IOException("SSE transport is not connected")
    }

    val id = request.id
    val holder = CompletableFutureHolder()
    pendingRequests[id] = holder

    try {
      val requestBody = request.serialize().toRequestBody(jsonMediaType)
      val postReq = Request.Builder()
        .url(messagePostUrl)
        .post(requestBody)
        .header("Content-Type", "application/json")

      headers.forEach { (k, v) ->
        if (k.isNotBlank() && v.isNotBlank()) {
          postReq.header(k, v)
        }
      }

      val postClient = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .build()

      val response = postClient.newCall(postReq.build()).execute()
      if (response.isSuccessful) {
        val bodyStr = response.body?.string()?.trim() ?: ""
        if (bodyStr.startsWith("{")) {
          // If response came back directly in the HTTP body
          val directResp = McpJsonRpc.parse(bodyStr)
          return directResp
        }
      }

      // Otherwise wait for the response via SSE stream
      val ok = holder.latch.await(timeoutMs, TimeUnit.MILLISECONDS)
      if (!ok) {
        throw IOException("Timed out waiting for MCP SSE response (request ID: $id)")
      }

      return holder.response ?: throw IOException("Empty SSE response received for ID: $id")
    } finally {
      pendingRequests.remove(id)
    }
  }

  override fun sendNotification(notification: McpJsonRpc) {
    if (!isConnected()) return
    try {
      val requestBody = notification.serialize().toRequestBody(jsonMediaType)
      val postReq = Request.Builder()
        .url(messagePostUrl)
        .post(requestBody)
        .header("Content-Type", "application/json")

      headers.forEach { (k, v) ->
        if (k.isNotBlank() && v.isNotBlank()) postReq.header(k, v)
      }
      OkHttpClient().newCall(postReq.build()).execute().close()
    } catch (ignored: Exception) {}
  }
}
