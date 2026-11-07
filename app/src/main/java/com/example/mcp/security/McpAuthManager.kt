package com.example.mcp.security

import android.content.Context
import com.example.mcp.model.McpAuthType
import com.example.mcp.model.McpServerConfig

class McpAuthManager(private val context: Context) {

  private val keystoreManager = SecureKeystoreManager.getInstance(context)

  /**
   * Encrypts plain-text token or secret for safe Room database persistence.
   */
  fun encryptCredential(plainToken: String): String {
    return keystoreManager.encrypt(plainToken)
  }

  /**
   * Decrypts encrypted token from database for runtime network authorization.
   */
  fun decryptCredential(encryptedToken: String): String {
    return keystoreManager.decrypt(encryptedToken)
  }

  /**
   * Builds headers map including decrypted authorization headers based on server config.
   */
  fun buildResolvedHeaders(config: McpServerConfig): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    headers.putAll(config.customHeaders)

    val decryptedToken = decryptCredential(config.encryptedAuthToken).trim()
    if (decryptedToken.isNotEmpty()) {
      when (config.authType) {
        McpAuthType.API_KEY -> {
          headers["X-API-Key"] = decryptedToken
          if (!headers.containsKey("Authorization")) {
            headers["Authorization"] = "ApiKey $decryptedToken"
          }
        }
        McpAuthType.BEARER_TOKEN, McpAuthType.OAUTH2 -> {
          headers["Authorization"] = "Bearer $decryptedToken"
        }
        McpAuthType.CUSTOM_HEADERS -> {
          // If custom headers stored in token as "Key: Value\nKey2: Value2"
          decryptedToken.lines().forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
              headers[parts[0].trim()] = parts[1].trim()
            }
          }
        }
        McpAuthType.NONE -> {}
      }
    }
    return headers
  }

  /**
   * Redacts sensitive tokens and keys from logs, outputs, and AI prompts.
   */
  fun redactSecrets(input: String, tokenToRedact: String? = null): String {
    if (input.isBlank()) return input
    var redacted = input

    // Redact standard bearer headers
    redacted = redacted.replace(Regex("Bearer\\s+([A-Za-z0-9_\\-\\.]+)", RegexOption.IGNORE_CASE), "Bearer [REDACTED_TOKEN]")
    redacted = redacted.replace(Regex("X-API-Key:\\s*([^\\s,]+)", RegexOption.IGNORE_CASE), "X-API-Key: [REDACTED_KEY]")
    redacted = redacted.replace(Regex("\"(apiKey|token|password|secret|authorization)\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE), "\"$1\": \"[REDACTED]\"")

    if (!tokenToRedact.isNullOrBlank() && tokenToRedact.length > 4) {
      redacted = redacted.replace(tokenToRedact, "[REDACTED_SECRET]")
    }

    return redacted
  }
}
