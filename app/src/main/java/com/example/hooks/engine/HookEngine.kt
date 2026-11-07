package com.example.hooks.engine

import android.content.Context
import com.example.hooks.executors.AutomatedTestingService
import com.example.hooks.executors.CodeFormatterService
import com.example.hooks.executors.CustomScriptExecutor
import com.example.hooks.executors.HttpWebhookExecutor
import com.example.hooks.executors.NotificationHookService
import com.example.hooks.model.HookActionType
import com.example.hooks.model.HookEvent
import com.example.hooks.model.HookEventType
import com.example.hooks.model.HookExecutionResult
import com.example.hooks.model.HookRule
import com.example.hooks.persistence.HookDao
import com.example.hooks.persistence.HookLogEntity
import com.example.hooks.persistence.HookRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HookEngine(
  private val context: Context,
  private val hookDao: HookDao,
  val codeFormatter: CodeFormatterService,
  val testingService: AutomatedTestingService,
  val webhookExecutor: HttpWebhookExecutor = HttpWebhookExecutor(),
  val notificationService: NotificationHookService = NotificationHookService(context),
  val scriptExecutor: CustomScriptExecutor = CustomScriptExecutor()
) {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val registeredRules = ConcurrentHashMap<String, HookRule>()

  private val _eventFlow = MutableSharedFlow<HookEvent>(extraBufferCapacity = 64)
  val eventFlow: SharedFlow<HookEvent> = _eventFlow.asSharedFlow()

  private val _rulesFlow = MutableStateFlow<List<HookRule>>(emptyList())
  val rulesFlow: StateFlow<List<HookRule>> = _rulesFlow.asStateFlow()

  init {
    loadRulesFromDatabase()
  }

  private fun loadRulesFromDatabase() {
    scope.launch {
      val entities = hookDao.getAllRules()
      if (entities.isEmpty()) {
        // Bootstrap standard initial hooks
        val defaultRules = listOf(
          HookRule(
            id = "default_format_json",
            name = "Auto-Format JSON Files",
            description = "Automatically formats JSON files on modification",
            eventType = HookEventType.FILE_MODIFIED,
            filePattern = "*.json",
            actionType = HookActionType.FORMAT_CODE
          ),
          HookRule(
            id = "default_test_kotlin",
            name = "Kotlin Verification Check",
            description = "Runs syntax and build verification check when Kotlin source files change",
            eventType = HookEventType.FILE_MODIFIED,
            filePattern = "*.kt",
            actionType = HookActionType.RUN_TESTS
          )
        )
        defaultRules.forEach { hookDao.insertOrUpdateRule(it.toEntity()) }
        defaultRules.forEach { registeredRules[it.id] = it }
        _rulesFlow.value = defaultRules
      } else {
        val models = entities.map { it.toModel() }
        models.forEach { registeredRules[it.id] = it }
        _rulesFlow.value = models
      }
    }
  }

  suspend fun registerRule(rule: HookRule) {
    registeredRules[rule.id] = rule
    _rulesFlow.value = registeredRules.values.toList()
    hookDao.insertOrUpdateRule(rule.toEntity())
  }

  suspend fun deleteRule(ruleId: String) {
    registeredRules.remove(ruleId)
    _rulesFlow.value = registeredRules.values.toList()
    hookDao.deleteRule(ruleId)
  }

  suspend fun toggleRule(ruleId: String, enabled: Boolean) {
    val rule = registeredRules[ruleId] ?: return
    val updated = rule.copy(isEnabled = enabled)
    registerRule(updated)
  }

  /**
   * Main dispatch entrypoint for all lifecycle events in the app.
   */
  suspend fun dispatchEvent(event: HookEvent): HookExecutionResult {
    // 1. Check anti-recursion depth
    if (event.executionDepth > 3) {
      return HookExecutionResult(
        isAllowed = true,
        outputMessage = "Anti-recursion guard: aborted hook dispatch at depth ${event.executionDepth}."
      )
    }

    _eventFlow.tryEmit(event)

    val matchingRules = registeredRules.values.filter { it.matches(event) }
    if (matchingRules.isEmpty()) {
      return HookExecutionResult(isAllowed = true)
    }

    var isAllowedCombined = true
    var modifiedArgs = event.toolArguments
    val outputMessages = mutableListOf<String>()

    for (rule in matchingRules) {
      val startTime = System.currentTimeMillis()
      val result = executeActionForRule(rule, event)
      val duration = System.currentTimeMillis() - startTime

      if (!result.isAllowed) {
        isAllowedCombined = false
      }
      if (result.modifiedArguments != null) {
        modifiedArgs = result.modifiedArguments
      }
      if (result.outputMessage.isNotBlank()) {
        outputMessages.add("[${rule.name}] ${result.outputMessage}")
      }

      // Record log in Room
      hookDao.insertLog(
        HookLogEntity(
          id = UUID.randomUUID().toString(),
          timestamp = System.currentTimeMillis(),
          ruleId = rule.id,
          ruleName = rule.name,
          eventType = event.type.name,
          outputMessage = result.outputMessage,
          durationMs = duration,
          isSuccess = result.isAllowed
        )
      )

      if (result.shouldAbort) {
        break
      }
    }

    return HookExecutionResult(
      isAllowed = isAllowedCombined,
      modifiedArguments = modifiedArgs,
      outputMessage = outputMessages.joinToString("\n")
    )
  }

  private suspend fun executeActionForRule(rule: HookRule, event: HookEvent): HookExecutionResult {
    return when (rule.actionType) {
      HookActionType.VALIDATE_TOOL -> {
        // Validate pre-tool execution arguments or block dangerous patterns
        val toolName = event.toolName ?: ""
        val args = event.toolArguments ?: JSONObject()
        val command = args.optString("command", "")

        if (command.contains("rm -rf /") || command.contains(":(){ :|:& };:")) {
          HookExecutionResult(
            isAllowed = false,
            shouldAbort = true,
            outputMessage = "Blocked destructive system command: '$command'"
          )
        } else {
          HookExecutionResult(isAllowed = true, outputMessage = "Tool arguments validated successfully.")
        }
      }

      HookActionType.FORMAT_CODE -> {
        val proj = event.project
        val filePath = event.filePath
        if (proj != null && filePath != null) {
          val res = codeFormatter.formatFile(proj, filePath)
          HookExecutionResult(isAllowed = true, outputMessage = res.message)
        } else {
          HookExecutionResult(isAllowed = true)
        }
      }

      HookActionType.RUN_TESTS -> {
        val proj = event.project
        val filePath = event.filePath
        if (proj != null && filePath != null) {
          val res = testingService.runRelevantTests(proj, filePath)
          HookExecutionResult(
            isAllowed = res.isSuccess,
            outputMessage = "Automated tests: ${if (res.isSuccess) "PASSED" else "FAILED"} (${res.failures} failures, ${res.durationMs}ms)"
          )
        } else {
          HookExecutionResult(isAllowed = true)
        }
      }

      HookActionType.HTTP_WEBHOOK -> {
        val config = rule.parseConfig()
        val url = config.optString("url")
        if (url.isNotBlank()) {
          val payload = JSONObject().apply {
            put("event", event.type.name)
            put("agentId", event.agentId)
            put("toolName", event.toolName)
            put("filePath", event.filePath)
            put("timestamp", event.timestamp)
          }
          val res = webhookExecutor.executeWebhook(url = url, payload = payload)
          HookExecutionResult(isAllowed = true, outputMessage = "Webhook response: HTTP ${res.statusCode}")
        } else {
          HookExecutionResult(isAllowed = true, outputMessage = "Webhook skipped: URL is blank.")
        }
      }

      HookActionType.SEND_NOTIFICATION -> {
        val title = "Claude Code: ${rule.name}"
        val body = event.toolOutput?.take(150) ?: "Event ${event.type.displayName} completed."
        val sent = notificationService.sendAutomationNotification(title, body)
        HookExecutionResult(isAllowed = true, outputMessage = if (sent) "Notification dispatched" else "Notification skipped")
      }

      HookActionType.CUSTOM_SCRIPT -> {
        val config = rule.parseConfig()
        val cmd = config.optString("command")
        if (cmd.isNotBlank()) {
          val res = scriptExecutor.executeScript(event.project, cmd)
          HookExecutionResult(isAllowed = res.isSuccess, outputMessage = res.output.take(500))
        } else {
          HookExecutionResult(isAllowed = true)
        }
      }
    }
  }

  private fun HookRuleEntity.toModel(): HookRule {
    return HookRule(
      id = id,
      name = name,
      description = description,
      eventType = runCatching { HookEventType.valueOf(eventType) }.getOrDefault(HookEventType.PRE_TOOL_EXECUTION),
      filePattern = filePattern,
      actionType = runCatching { HookActionType.valueOf(actionType) }.getOrDefault(HookActionType.FORMAT_CODE),
      actionConfigJson = actionConfigJson,
      isEnabled = isEnabled,
      isApprovedByUser = isApprovedByUser,
      executionCount = executionCount,
      lastExecutedAt = lastExecutedAt
    )
  }

  private fun HookRule.toEntity(): HookRuleEntity {
    return HookRuleEntity(
      id = id,
      name = name,
      description = description,
      eventType = eventType.name,
      filePattern = filePattern,
      actionType = actionType.name,
      actionConfigJson = actionConfigJson,
      isEnabled = isEnabled,
      isApprovedByUser = isApprovedByUser,
      executionCount = executionCount,
      lastExecutedAt = lastExecutedAt
    )
  }
}
