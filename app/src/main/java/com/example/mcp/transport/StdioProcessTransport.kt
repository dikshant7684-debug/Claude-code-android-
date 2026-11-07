package com.example.mcp.transport

import com.example.mcp.protocol.McpJsonRpc
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StdioProcessTransport(
  val command: List<String>,
  val workingDir: File? = null,
  val env: Map<String, String> = emptyMap(),
  val timeoutSeconds: Int = 30
) : McpTransport {

  private var process: Process? = null
  private var writer: BufferedWriter? = null
  private var readerThread: Thread? = null
  private val isConnectedFlag = AtomicBoolean(false)

  private val pendingRequests = ConcurrentHashMap<Long, CompletableFutureHolder>()

  private class CompletableFutureHolder {
    val latch = CountDownLatch(1)
    var response: McpJsonRpc? = null
    var error: Exception? = null
  }

  override fun connect() {
    if (isConnectedFlag.get()) return
    if (command.isEmpty()) throw IOException("Empty command for StdioProcessTransport")

    val pb = ProcessBuilder(command)
    if (workingDir != null && workingDir.exists()) {
      pb.directory(workingDir)
    }
    env.forEach { (k, v) -> pb.environment()[k] = v }
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)

    val proc = try {
      pb.start()
    } catch (e: Exception) {
      throw IOException("Failed to start local MCP process '${command.first()}': ${e.message}", e)
    }
    process = proc
    writer = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))
    isConnectedFlag.set(true)

    readerThread = Thread {
      val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
      var line: String? = null
      try {
        while (isConnectedFlag.get() && reader.readLine().also { line = it } != null) {
          val l = line?.trim() ?: continue
          if (l.startsWith("{")) {
            try {
              val msg = McpJsonRpc.parse(l)
              if (msg.isResponse) {
                val holder = pendingRequests[msg.id]
                if (holder != null) {
                  holder.response = msg
                  holder.latch.countDown()
                }
              }
            } catch (ignored: Exception) {}
          }
        }
      } catch (ignored: Exception) {
      } finally {
        isConnectedFlag.set(false)
      }
    }.apply {
      isDaemon = true
      start()
    }
  }

  override fun disconnect() {
    isConnectedFlag.set(false)
    try {
      writer?.close()
    } catch (ignored: Exception) {}
    process?.destroy()
    readerThread?.interrupt()
    process = null
    writer = null
    readerThread = null
  }

  override fun isConnected(): Boolean {
    val proc = process
    return isConnectedFlag.get() && proc != null && proc.isAlive
  }

  override fun getTransportName(): String = "Local Stdio Process"

  override fun sendRequest(request: McpJsonRpc, timeoutMs: Long): McpJsonRpc {
    if (!isConnected()) throw IOException("Local MCP process is not running")

    val id = request.id
    val holder = CompletableFutureHolder()
    pendingRequests[id] = holder

    try {
      val out = writer ?: throw IOException("Process stdin is closed")
      synchronized(out) {
        out.write(request.serialize())
        out.newLine()
        out.flush()
      }

      val ok = holder.latch.await(timeoutMs, TimeUnit.MILLISECONDS)
      if (!ok) {
        throw IOException("Timed out waiting for local MCP stdio response (ID: $id)")
      }

      return holder.response ?: throw IOException("Empty response from local MCP stdio process")
    } finally {
      pendingRequests.remove(id)
    }
  }

  override fun sendNotification(notification: McpJsonRpc) {
    if (!isConnected()) return
    try {
      val out = writer ?: return
      synchronized(out) {
        out.write(notification.serialize())
        out.newLine()
        out.flush()
      }
    } catch (ignored: Exception) {}
  }
}
