package com.example.hooks.executors

import com.example.data.files.ProjectFileManager
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class CodeFormatterService(private val fileManager: ProjectFileManager) {

  suspend fun formatFile(project: Project, relativePath: String): FormatResult = withContext(Dispatchers.IO) {
    val ext = relativePath.substringAfterLast('.', "").lowercase()
    val original = try {
      fileManager.readFile(project, relativePath)
    } catch (e: Exception) {
      return@withContext FormatResult(false, "Could not read $relativePath: ${e.message}")
    }

    val formatted = when (ext) {
      "json" -> formatJson(original)
      "xml" -> formatXml(original)
      "kt", "kts", "java" -> formatJavaOrKotlin(original)
      "py" -> formatPython(original)
      else -> null
    }

    if (formatted == null) {
      return@withContext FormatResult(true, "No formatter needed for .$ext", wasChanged = false)
    }

    if (formatted != original) {
      fileManager.writeFile(project, relativePath, formatted)
      FormatResult(true, "Successfully formatted $relativePath", wasChanged = true)
    } else {
      FormatResult(true, "File $relativePath was already properly formatted", wasChanged = false)
    }
  }

  private fun formatJson(content: String): String? {
    val trimmed = content.trim()
    return try {
      if (trimmed.startsWith("{")) {
        JSONObject(trimmed).toString(2)
      } else if (trimmed.startsWith("[")) {
        JSONArray(trimmed).toString(2)
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun formatXml(content: String): String? {
    return try {
      val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
      }
      val out = StringWriter()
      transformer.transform(StreamSource(StringReader(content)), StreamResult(out))
      out.toString()
    } catch (e: Exception) {
      null
    }
  }

  private fun formatJavaOrKotlin(content: String): String {
    val lines = content.lines()
    val result = StringBuilder()
    var indent = 0

    lines.forEach { line ->
      val trimmed = line.trim()
      if (trimmed.isEmpty()) {
        result.appendLine()
        return@forEach
      }

      // Decrement indent for closing braces
      if (trimmed.startsWith("}") || trimmed.startsWith(")")) {
        indent = (indent - 1).coerceAtLeast(0)
      }

      result.append("  ".repeat(indent))
      result.appendLine(trimmed)

      // Increment indent for opening braces
      val openCount = trimmed.count { it == '{' }
      val closeCount = trimmed.count { it == '}' }
      if (openCount > closeCount && !trimmed.startsWith("}")) {
        indent += (openCount - closeCount)
      }
    }
    return result.toString().trimEnd() + "\n"
  }

  private fun formatPython(content: String): String {
    // Basic Python PEP-8 clean trailing whitespace and ensure single trailing newline
    val cleaned = content.lines().joinToString("\n") { it.trimEnd() }
    return cleaned.trimEnd() + "\n"
  }

  data class FormatResult(
    val isSuccess: Boolean,
    val message: String,
    val wasChanged: Boolean = false
  )
}
