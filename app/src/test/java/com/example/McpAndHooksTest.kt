package com.example

import com.example.hooks.model.HookActionType
import com.example.hooks.model.HookEvent
import com.example.hooks.model.HookEventType
import com.example.hooks.model.HookRule
import com.example.mcp.marketplace.McpMarketplaceManager
import com.example.mcp.model.McpAuthType
import com.example.mcp.model.McpPermission
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpToolDefinition
import com.example.mcp.model.McpTransportType
import com.example.mcp.protocol.McpProtocolConstants
import com.example.mcp.registry.McpPermissionManager
import com.example.mcp.security.SecureKeystoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpAndHooksTest {

  @Test
  fun testMcpProtocolConstants_standardSpecificationValues() {
    assertEquals("2.0", McpProtocolConstants.JSONRPC_VERSION)
    assertEquals("2024-11-05", McpProtocolConstants.LATEST_PROTOCOL_VERSION)
    assertEquals("initialize", McpProtocolConstants.METHOD_INITIALIZE)
    assertEquals("notifications/initialized", McpProtocolConstants.METHOD_INITIALIZED)
    assertEquals("tools/list", McpProtocolConstants.METHOD_TOOLS_LIST)
    assertEquals("tools/call", McpProtocolConstants.METHOD_TOOLS_CALL)
    assertEquals("resources/list", McpProtocolConstants.METHOD_RESOURCES_LIST)
    assertEquals("prompts/list", McpProtocolConstants.METHOD_PROMPTS_LIST)
    assertEquals(-32001, McpProtocolConstants.PERMISSION_DENIED)
  }

  @Test
  fun testSecureKeystoreManager_encryptAndDecrypt() {
    val plainToken = "sk-mcp-production-token-987654321"
    val keystore = SecureKeystoreManager.getInstance(null)

    val encrypted = keystore.encrypt(plainToken)
    assertNotNull(encrypted)
    assertTrue("Encrypted token must not be empty", encrypted.isNotEmpty())
    assertFalse("Encrypted token must not be stored in plain text", encrypted == plainToken)

    val decrypted = keystore.decrypt(encrypted)
    assertEquals(plainToken, decrypted)
  }

  @Test
  fun testMcpMarketplace_curatedCatalog() {
    val marketplace = McpMarketplaceManager()
    val servers = marketplace.curatedMarketplaceServers

    assertTrue("Marketplace must have verified servers", servers.isNotEmpty())

    val github = servers.firstOrNull { it.id == "mcp-github" }
    assertNotNull(github)
    assertEquals("GitHub Context Provider", github!!.name)
    assertEquals(McpTransportType.STREAMABLE_HTTP, github.transportType)
    assertEquals(McpAuthType.BEARER_TOKEN, github.authType)
    assertTrue(github.popularTools.contains("get_issue"))
    assertTrue(github.requiredPermissions.contains(McpPermission.EXTERNAL_API.identifier))

    val postgres = servers.firstOrNull { it.id == "mcp-postgres" }
    assertNotNull(postgres)
    assertTrue(postgres!!.popularTools.contains("inspect_schema"))

    val memory = servers.firstOrNull { it.id == "mcp-memory" }
    assertNotNull(memory)
    assertEquals(McpTransportType.SERVER_SENT_EVENTS, memory!!.transportType)
  }

  @Test
  fun testMcpPermissions_granularEnforcement() {
    val permissionManager = McpPermissionManager()

    val serverConfig = McpServerConfig(
      id = "server-verified",
      name = "Verified MCP Server",
      url = "https://example.com/mcp",
      isEnabled = true,
      isApprovedByUser = true,
      approvedPermissions = setOf(McpPermission.EXECUTE_TOOLS.identifier)
    )

    val tool = McpToolDefinition(
      id = "server-verified::read_metrics",
      serverId = "server-verified",
      serverName = "Verified MCP Server",
      name = "read_metrics",
      description = "Reads performance metrics",
      requiredPermissions = listOf(McpPermission.EXECUTE_TOOLS.identifier)
    )

    // 1. Authorized agent
    val check1 = permissionManager.validateExecutionPermission("main_agent", serverConfig, tool)
    assertTrue(check1 is McpPermissionManager.PermissionCheckResult.Granted)

    // 2. Denied when server disabled
    val disabledServer = serverConfig.copy(isEnabled = false)
    val checkDisabled = permissionManager.validateExecutionPermission("main_agent", disabledServer, tool)
    assertTrue(checkDisabled is McpPermissionManager.PermissionCheckResult.Denied)

    // 3. Denied when server requires user approval
    val unapprovedServer = serverConfig.copy(isApprovedByUser = false)
    val checkUnapproved = permissionManager.validateExecutionPermission("main_agent", unapprovedServer, tool)
    assertTrue(checkUnapproved is McpPermissionManager.PermissionCheckResult.Denied)

    // 4. Denied when tool is explicitly blocked
    permissionManager.setToolApproval(tool.id, false)
    val checkBlocked = permissionManager.validateExecutionPermission("main_agent", serverConfig, tool)
    assertTrue(checkBlocked is McpPermissionManager.PermissionCheckResult.Denied)
  }

  @Test
  fun testHookRules_matchingLogic() {
    val rule1 = HookRule(
      id = "rule-1",
      name = "Kotlin Formatting",
      eventType = HookEventType.FILE_MODIFIED,
      filePattern = "*.kt",
      actionType = HookActionType.FORMAT_CODE,
      isEnabled = true,
      isApprovedByUser = true
    )

    // Matching event
    val matchingEvent = HookEvent(
      type = HookEventType.FILE_MODIFIED,
      filePath = "src/main/java/App.kt"
    )
    assertTrue(rule1.matches(matchingEvent))

    // Non-matching file extension
    val pythonEvent = HookEvent(
      type = HookEventType.FILE_MODIFIED,
      filePath = "script.py"
    )
    assertFalse(rule1.matches(pythonEvent))

    // Non-matching event type
    val createdEvent = HookEvent(
      type = HookEventType.FILE_CREATED,
      filePath = "App.kt"
    )
    assertFalse(rule1.matches(createdEvent))

    // Disabled rule
    val disabledRule = rule1.copy(isEnabled = false)
    assertFalse(disabledRule.matches(matchingEvent))

    // Universal wildcard pattern
    val wildcardRule = rule1.copy(filePattern = "*")
    assertTrue(wildcardRule.matches(matchingEvent))
    assertTrue(wildcardRule.matches(pythonEvent))
  }
}
