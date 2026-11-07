package com.example.hooks.executors

import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CustomScriptExecutor {

  suspend fun executeScript(
    project: Project?,
    command: String,
    timeoutSeconds: Int = 20
  ): ScriptResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val workingDir = if (project != null) File(project.rootPath) else null

    return@withContext try {
      val tokens = command.split("\\s+".toRegex()).filter { it.isNotBlank() }
      if (tokens.isEmpty()) {
        return@withContext ScriptResult(false, -1, "Empty command", 0)
      }

      val pb = ProcessBuilder(tokens)
      if (workingDir != null && workingDir.exists()) {
        pb.directory(workingDir)
      }
      pb.redirectErrorStream(true)

      val process = pb.start()
      val output = StringBuilder()

      val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        output.appendLine(line)
        if (output.length > 50000) {
          output.appendLine("\n[Output truncated at 50,000 characters]")
          break
        }
      }

      val finished = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
      val duration = System.currentTimeMillis() - startTime

      if (!finished) {
        process.destroyForcibly()
        return@withContext ScriptResult(false, -1, "Command timed out after ${timeoutSeconds}s:\n$output", duration)
      }

      val exitCode = process.exitValue()
      ScriptResult(
        isSuccess = exitCode == 0,
        exitCode = exitCode,
        output = output.toString().trim(),
        durationMs = duration
      )
    } catch (e: Exception) {
      ScriptResult(false, -1, "Execution failed: ${e.message}", System.currentTimeMillis() - startTime)
    }
  }

  data class ScriptResult(
    val isSuccess: Boolean,
    val exitCode: Int,
    val output: String,
    val durationMs: Long
  )
}
