package com.example.hooks.executors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class HttpWebhookExecutor {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun executeWebhook(
    url: String,
    method: String = "POST",
    headers: Map<String, String> = emptyMap(),
    payload: JSONObject? = null
  ): WebhookResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    try {
      validateUrl(url)

      val requestBuilder = Request.Builder().url(url)
      headers.forEach { (k, v) -> requestBuilder.header(k, v) }

      if (method.equals("GET", ignoreCase = true)) {
        requestBuilder.get()
      } else {
        val body = (payload?.toString() ?: "{}").toRequestBody(jsonMediaType)
        requestBuilder.post(body)
      }

      val response = client.newCall(requestBuilder.build()).execute()
      val duration = System.currentTimeMillis() - startTime
      val code = response.code
      val body = response.body?.string() ?: ""

      WebhookResult(
        isSuccess = response.isSuccessful,
        statusCode = code,
        responseBody = body,
        durationMs = duration
      )
    } catch (e: Exception) {
      WebhookResult(
        isSuccess = false,
        statusCode = 0,
        responseBody = "Webhook dispatch failed: ${e.message}",
        durationMs = System.currentTimeMillis() - startTime
      )
    }
  }

  private fun validateUrl(urlString: String) {
    val uri = URI(urlString)
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
      throw SecurityException("Invalid webhook URL scheme: only http and https are permitted.")
    }

    val host = uri.host?.lowercase() ?: throw SecurityException("Webhook URL missing valid host.")
    // SSRF checks: block cloud metadata services
    if (host == "169.254.169.254" || host.startsWith("metadata.google.internal")) {
      throw SecurityException("Access to cloud metadata endpoints is strictly prohibited.")
    }
  }

  data class WebhookResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val responseBody: String,
    val durationMs: Long
  )
}
