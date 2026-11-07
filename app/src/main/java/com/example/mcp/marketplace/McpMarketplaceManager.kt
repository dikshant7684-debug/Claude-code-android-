package com.example.mcp.marketplace

import com.example.mcp.model.McpAuthType
import com.example.mcp.model.McpPermission
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpTransportType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MarketplaceServerItem(
  val id: String,
  val name: String,
  val category: String,
  val description: String,
  val defaultUrl: String,
  val transportType: McpTransportType,
  val authType: McpAuthType,
  val requiredPermissions: List<String>,
  val popularTools: List<String>,
  val vendor: String = "Community Verified"
)

class McpMarketplaceManager {

  val curatedMarketplaceServers: List<MarketplaceServerItem> = listOf(
    MarketplaceServerItem(
      id = "mcp-github",
      name = "GitHub Context Provider",
      category = "Version Control & CI/CD",
      description = "Inspect repository PRs, issues, commits, branches, and pull request reviews directly over official MCP protocol.",
      defaultUrl = "https://api.github.com/mcp",
      transportType = McpTransportType.STREAMABLE_HTTP,
      authType = McpAuthType.BEARER_TOKEN,
      requiredPermissions = listOf(McpPermission.EXTERNAL_API.identifier, McpPermission.EXECUTE_TOOLS.identifier),
      popularTools = listOf("get_issue", "list_pull_requests", "search_code", "create_issue_comment"),
      vendor = "Official / GitHub Community"
    ),
    MarketplaceServerItem(
      id = "mcp-postgres",
      name = "PostgreSQL Database Inspector",
      category = "Databases & Storage",
      description = "Execute schema inspection, run read-only queries, and analyze query execution plans against PostgreSQL databases.",
      defaultUrl = "http://localhost:8080/mcp/postgres",
      transportType = McpTransportType.STREAMABLE_HTTP,
      authType = McpAuthType.API_KEY,
      requiredPermissions = listOf(McpPermission.EXTERNAL_API.identifier, McpPermission.EXECUTE_TOOLS.identifier),
      popularTools = listOf("query_table", "inspect_schema", "explain_query", "list_tables"),
      vendor = "Database Working Group"
    ),
    MarketplaceServerItem(
      id = "mcp-fetch",
      name = "Web Content Fetcher & Scraper",
      category = "Web & Documentation",
      description = "Fetches web pages, converts HTML to readable markdown, and extracts documentation for LLM consumption.",
      defaultUrl = "https://mcp-fetch.modelcontextprotocol.io/mcp",
      transportType = McpTransportType.STREAMABLE_HTTP,
      authType = McpAuthType.NONE,
      requiredPermissions = listOf(McpPermission.EXTERNAL_API.identifier, McpPermission.EXECUTE_TOOLS.identifier),
      popularTools = listOf("fetch_url", "extract_markdown", "search_docs"),
      vendor = "Model Context Protocol Official"
    ),
    MarketplaceServerItem(
      id = "mcp-memory",
      name = "Knowledge Graph & Semantic Memory",
      category = "Memory & Context",
      description = "Maintains persistent knowledge graph relations, entities, and observations across coding sessions.",
      defaultUrl = "http://localhost:8085/sse",
      transportType = McpTransportType.SERVER_SENT_EVENTS,
      authType = McpAuthType.NONE,
      requiredPermissions = listOf(McpPermission.READ_RESOURCES.identifier, McpPermission.EXECUTE_TOOLS.identifier),
      popularTools = listOf("create_entity", "read_graph", "search_nodes", "add_observation"),
      vendor = "Anthropic Community"
    )
  )

  fun parseMcpConfigJson(jsonString: String): List<McpServerConfig> {
    val results = mutableListOf<McpServerConfig>()
    try {
      val root = JSONObject(jsonString)
      val serversObj = root.optJSONObject("mcpServers") ?: root
      val keys = serversObj.keys()
      while (keys.hasNext()) {
        val serverKey = keys.next()
        val entry = serversObj.optJSONObject(serverKey) ?: continue

        val url = entry.optString("url", entry.optString("endpoint", ""))
        val command = entry.optString("command", "")
        val transport = when {
          url.contains("/sse") -> McpTransportType.SERVER_SENT_EVENTS
          command.isNotBlank() -> McpTransportType.STDIO
          else -> McpTransportType.STREAMABLE_HTTP
        }

        val targetUrl = if (transport == McpTransportType.STDIO) {
          val args = entry.optJSONArray("args")
          val argsStr = if (args != null) (0 until args.length()).joinToString(" ") { args.getString(it) } else ""
          "$command $argsStr".trim()
        } else {
          url
        }

        val headers = mutableMapOf<String, String>()
        val headersObj = entry.optJSONObject("headers")
        headersObj?.keys()?.forEach { hKey ->
          headers[hKey] = headersObj.getString(hKey)
        }

        val env = mutableMapOf<String, String>()
        val envObj = entry.optJSONObject("env")
        envObj?.keys()?.forEach { eKey ->
          env[eKey] = envObj.getString(eKey)
        }

        results.add(
          McpServerConfig(
            id = UUID.randomUUID().toString(),
            name = serverKey,
            url = targetUrl,
            transportType = transport,
            authType = if (headers.containsKey("Authorization")) McpAuthType.BEARER_TOKEN else McpAuthType.NONE,
            customHeaders = headers,
            environmentVariables = env,
            description = entry.optString("description", "Imported from MCP configuration JSON"),
            isApprovedByUser = false // Safety requirement: imported servers start unapproved
          )
        )
      }
    } catch (ignored: Exception) {}
    return results
  }

  fun exportMcpConfigJson(servers: List<McpServerConfig>): String {
    val root = JSONObject()
    val mcpServersObj = JSONObject()

    servers.forEach { s ->
      val sObj = JSONObject()
      if (s.transportType == McpTransportType.STDIO) {
        val tokens = s.url.split(" ")
        sObj.put("command", tokens.firstOrNull() ?: "")
        val argsArray = JSONArray()
        tokens.drop(1).forEach { argsArray.put(it) }
        sObj.put("args", argsArray)
      } else {
        sObj.put("url", s.url)
      }

      if (s.customHeaders.isNotEmpty()) {
        val hObj = JSONObject()
        s.customHeaders.forEach { (k, v) -> hObj.put(k, v) }
        sObj.put("headers", hObj)
      }
      if (s.environmentVariables.isNotEmpty()) {
        val envObj = JSONObject()
        s.environmentVariables.forEach { (k, v) -> envObj.put(k, v) }
        sObj.put("env", envObj)
      }
      sObj.put("description", s.description)
      mcpServersObj.put(s.name, sObj)
    }

    root.put("mcpServers", mcpServersObj)
    return root.toString(2)
  }
}
