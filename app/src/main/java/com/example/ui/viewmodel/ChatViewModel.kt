package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgentTask
import com.example.data.model.MessageRole
import com.example.data.model.TaskStatus
import com.example.data.model.UiChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AttachedFile(
  val name: String,
  val path: String = "",
  val content: String = "",
  val sizeBytes: Long = 0
)

data class ChatUiState(
  val messages: List<UiChatMessage> = emptyList(),
  val inputText: String = "",
  val selectedModel: String = "Nemotron 30B (Low Latency)",
  val reasoningEffort: String = "Medium",
  val isLoading: Boolean = false,
  val currentTask: AgentTask? = null,
  val attachedFiles: List<AttachedFile> = emptyList(),
  val availableModels: List<String> = listOf(
    "Nemotron 30B (Low Latency)",
    "GLM-5.3 Flash",
    "Claude 3.7 Sonnet",
    "Claude 3.5 Sonnet",
    "GPT-4o",
    "Gemini 2.0 Flash",
    "Llama 3.2"
  ),
  val isMotionMode: Boolean = false
)

class ChatViewModel(
  private val mainViewModel: MainViewModel? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  init {
    mainViewModel?.let { vm ->
      // Sync messages
      viewModelScope.launch {
        vm.chatMessages.collect { list ->
          _uiState.update { it.copy(messages = list.distinctBy { msg -> msg.id }) }
        }
      }
      // Sync running state
      viewModelScope.launch {
        vm.currentTask.collect { task ->
          val running = task != null && (
            task.status == TaskStatus.PLANNING ||
            task.status == TaskStatus.INSPECTING ||
            task.status == TaskStatus.EDITING ||
            task.status == TaskStatus.VERIFYING
          )
          _uiState.update { it.copy(isLoading = running, currentTask = task) }
        }
      }
      // Sync model configuration and reasoning effort
      viewModelScope.launch {
        vm.providerConfig.collect { config ->
          if (config.modelId.isNotBlank()) {
            val displayName = when {
              config.modelId.contains("nemotron", ignoreCase = true) -> "Nemotron 30B (Low Latency)"
              config.modelId.contains("glm", ignoreCase = true) -> "GLM-5.3 Flash"
              config.modelId.contains("gpt-4o", ignoreCase = true) -> "GPT-4o"
              config.modelId.contains("3-7-sonnet", ignoreCase = true) -> "Claude 3.7 Sonnet"
              config.modelId.contains("3-5-sonnet", ignoreCase = true) -> "Claude 3.5 Sonnet"
              config.modelId.contains("gemini", ignoreCase = true) -> "Gemini 2.0 Flash"
              config.modelId.contains("llama", ignoreCase = true) -> "Llama 3.2"
              else -> config.modelId
            }
            val effort = when {
              config.temperature <= 0.35f -> "Low"
              config.temperature >= 0.65f -> "High"
              else -> "Medium"
            }
            _uiState.update { it.copy(selectedModel = displayName, reasoningEffort = effort) }
          }
        }
      }
      // Sync fetched live models from endpoint
      viewModelScope.launch {
        vm.fetchedModels.collect { fetched ->
          if (fetched.isNotEmpty()) {
            val combined = (fetched + listOf(
              "Nemotron 30B (Low Latency)",
              "GLM-5.3 Flash",
              "Claude 3.7 Sonnet",
              "Claude 3.5 Sonnet",
              "GPT-4o",
              "Gemini 2.0 Flash",
              "Llama 3.2"
            )).distinct()
            _uiState.update { it.copy(availableModels = combined) }
          }
        }
      }
      // Sync Claude Motion Mode
      viewModelScope.launch {
        vm.isMotionMode.collect { motion ->
          _uiState.update { it.copy(isMotionMode = motion) }
        }
      }
    }
  }

  fun setMotionMode(isMotion: Boolean) {
    mainViewModel?.setMotionMode(isMotion)
  }

  fun saveMotionVideo(project: com.example.motion.model.MotionProject, onSaved: (String) -> Unit = {}) {
    mainViewModel?.saveMotionProject(project, onSaved)
  }

  fun exportMotionVideo(project: com.example.motion.model.MotionProject, format: com.example.motion.model.ExportFormat, onExported: (String) -> Unit = {}) {
    mainViewModel?.exportMotionProject(project, format, onExported)
  }

  fun onInputTextChanged(newText: String) {
    _uiState.update { it.copy(inputText = newText) }
  }

  fun onAttachFile(file: AttachedFile) {
    if (_uiState.value.attachedFiles.none { it.name == file.name && it.path == file.path }) {
      _uiState.update { it.copy(attachedFiles = it.attachedFiles + file) }
    }
  }

  fun onRemoveAttachedFile(file: AttachedFile) {
    _uiState.update { it.copy(attachedFiles = it.attachedFiles.filter { f -> f != file }) }
  }

  fun onModelSelected(model: String) {
    _uiState.update { it.copy(selectedModel = model) }
    mainViewModel?.let { vm ->
      val modelId = when (model) {
        "Nemotron 30B (Low Latency)" -> "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning"
        "GLM-5.3 Flash" -> "z-ai/glm-5.3-flash"
        "GPT-4o" -> "gpt-4o"
        "Claude 3.7 Sonnet" -> "claude-3-7-sonnet-20250219"
        "Claude 3.5 Sonnet" -> "claude-3-5-sonnet-20241022"
        "Gemini 2.0 Flash" -> "gemini-2.0-flash"
        "Llama 3.2" -> "llama3.2"
        else -> model
      }
      val current = vm.providerConfig.value
      vm.saveProviderConfig(current.copy(modelId = modelId))
    }
  }

  fun onReasoningEffortSelected(effort: String) {
    _uiState.update { it.copy(reasoningEffort = effort) }
    mainViewModel?.let { vm ->
      val temp = when (effort) {
        "Low" -> 0.2f
        "High" -> 0.8f
        else -> 0.5f
      }
      val steps = when (effort) {
        "Low" -> 4
        "High" -> 16
        else -> 8
      }
      val current = vm.providerConfig.value
      vm.saveProviderConfig(current.copy(temperature = temp, maxAgentSteps = steps))
    }
  }

  fun onSuggestionClick(suggestion: String) {
    _uiState.update { it.copy(inputText = suggestion) }
  }

  fun onSendMessage() {
    val rawPrompt = _uiState.value.inputText.trim()
    val attachments = _uiState.value.attachedFiles
    if (rawPrompt.isBlank() && attachments.isEmpty()) return

    val fullPrompt = buildString {
      if (attachments.isNotEmpty()) {
        append("Context from attached files:\n")
        attachments.forEach { file ->
          append("--- [File: ${file.name}] ---\n")
          if (file.content.isNotBlank()) {
            append("```\n")
            append(file.content.take(4000))
            if (file.content.length > 4000) append("\n... (truncated)")
            append("\n```\n\n")
          }
        }
      }
      if (rawPrompt.isNotBlank()) {
        append(rawPrompt)
      } else {
        append("Please review and analyze the attached files.")
      }
    }

    _uiState.update { it.copy(inputText = "", attachedFiles = emptyList()) }

    if (mainViewModel != null) {
      mainViewModel.sendChatMessage(fullPrompt)
    } else {
      // Standalone / Preview fallback
      val userMsg = UiChatMessage(
        id = UUID.randomUUID().toString(),
        role = MessageRole.USER,
        content = fullPrompt,
        timestamp = System.currentTimeMillis()
      )
      val assistantMsg = UiChatMessage(
        id = UUID.randomUUID().toString(),
        role = MessageRole.ASSISTANT,
        content = "I have analyzed your request and attached context: \"${rawPrompt.ifBlank { "Attached files" }}\".\n\n```kotlin\n// Starter code\nfun handleRequest() {\n    println(\"Processing files\")\n}\n```",
        timestamp = System.currentTimeMillis()
      )
      _uiState.update { it.copy(messages = it.messages + userMsg + assistantMsg) }
    }
  }

  fun onRegeneratePrompt(prompt: String) {
    if (prompt.isBlank()) return
    if (mainViewModel != null) {
      mainViewModel.sendChatMessage(prompt)
    }
  }

  fun onCancelTask() {
    mainViewModel?.cancelAgentTask()
    _uiState.update { it.copy(isLoading = false) }
  }

  fun onClearChat() {
    mainViewModel?.clearChat()
    _uiState.update { it.copy(messages = emptyList(), inputText = "") }
  }
}
