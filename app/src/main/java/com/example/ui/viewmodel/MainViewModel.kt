package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentToolRegistry
import com.example.agent.CodingAgentEngine
import com.example.build.BuildResult
import com.example.build.BuildWorkflowManager
import com.example.data.ai.AiProviderClient
import com.example.data.database.AppDatabase
import com.example.data.database.ChatRepository
import com.example.data.database.ProjectMemoryRepository
import com.example.data.files.ProjectFileManager
import com.example.data.model.AgentTask
import com.example.data.model.ConversationSession
import com.example.data.model.FileChange
import com.example.data.model.MessageRole
import com.example.data.model.MessageStatus
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.data.model.ProjectType
import com.example.data.model.ProviderConfig
import com.example.data.model.TaskStatus
import com.example.data.model.UiChatMessage
import com.example.data.preferences.EditorPreferences
import com.example.data.preferences.GeneralPreferences
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.ToolPermissionPreferences
import com.example.data.preferences.UserAccountInfo
import com.example.data.preferences.AppCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.motion.data.MotionProjectRepository
import com.example.motion.engine.MotionAiGenerator
import com.example.motion.engine.MotionExporter
import com.example.motion.model.ExportConfig
import com.example.motion.model.ExportFormat
import com.example.motion.model.MotionAspectRatio
import com.example.motion.model.MotionOperationResult
import com.example.motion.model.MotionProject
import com.example.motion.model.MotionStyle

import com.example.mcp.client.McpConnectionManager
import com.example.mcp.client.McpServerManager
import com.example.mcp.logging.McpExecutionLogger
import com.example.mcp.logging.McpLogEntry
import com.example.mcp.marketplace.McpMarketplaceManager
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpToolDefinition
import com.example.mcp.registry.McpPermissionManager
import com.example.mcp.registry.McpToolRegistry
import com.example.mcp.security.McpAuthManager
import com.example.hooks.engine.HookEngine
import com.example.hooks.executors.AutomatedTestingService
import com.example.hooks.executors.CodeFormatterService
import com.example.hooks.model.HookRule

class MainViewModel(application: Application) : AndroidViewModel(application) {

  val fileManager = ProjectFileManager(application)
  val preferencesManager = PreferencesManager(application)
  val aiClient = AiProviderClient()
  val chatDatabase = AppDatabase.getInstance(application)
  val chatRepository = ChatRepository(chatDatabase.chatMessageDao())
  val memoryRepository = ProjectMemoryRepository(chatDatabase.projectMemoryDao())
  val marketplaceManager = com.example.marketplace.service.MarketplaceManager(chatDatabase.marketplaceDao(), scope = viewModelScope)
  val skillPluginManager = com.example.marketplace.service.SkillAndPluginManager(
    context = application,
    installedDao = chatDatabase.installedExtensionDao(),
    executionDao = chatDatabase.extensionExecutionDao(),
    marketplaceDao = chatDatabase.marketplaceDao(),
    adapterRegistry = marketplaceManager.getAdapterRegistry(),
    fileManager = fileManager
  )
  val buildManager = BuildWorkflowManager(fileManager)

  // MCP & Hooks Integration
  val mcpDao = chatDatabase.mcpDao()
  val hookDao = chatDatabase.hookDao()
  val mcpAuthManager = McpAuthManager(application)
  val mcpConnectionManager = McpConnectionManager(application, mcpAuthManager)
  val mcpPermissionManager = McpPermissionManager()
  val mcpExecutionLogger = McpExecutionLogger(mcpAuthManager)
  val mcpToolRegistry = McpToolRegistry(mcpConnectionManager, mcpPermissionManager, mcpExecutionLogger)
  val mcpServerManager = McpServerManager(
    context = application,
    mcpDao = mcpDao,
    authManager = mcpAuthManager,
    connectionManager = mcpConnectionManager,
    toolRegistry = mcpToolRegistry,
    logger = mcpExecutionLogger
  )
  val mcpMarketplaceManager = McpMarketplaceManager()

  val codeFormatterService = CodeFormatterService(fileManager)
  val automatedTestingService = AutomatedTestingService(buildManager)
  val hookEngine = HookEngine(
    context = application,
    hookDao = hookDao,
    codeFormatter = codeFormatterService,
    testingService = automatedTestingService
  )

  val toolRegistry = AgentToolRegistry(
    fileManager = fileManager,
    buildManager = buildManager,
    skillManager = skillPluginManager,
    mcpServerManager = mcpServerManager,
    mcpToolRegistry = mcpToolRegistry,
    hookEngine = hookEngine
  )
  val agentEngine = CodingAgentEngine(
    scope = viewModelScope,
    toolRegistry = toolRegistry,
    aiClient = aiClient,
    fileManager = fileManager,
    memoryRepository = memoryRepository,
    skillManager = skillPluginManager,
    mcpServerManager = mcpServerManager,
    mcpToolRegistry = mcpToolRegistry,
    hookEngine = hookEngine
  )

  val mcpServers: StateFlow<List<McpServerConfig>> = mcpServerManager.serversFlow
  val mcpTools: StateFlow<List<McpToolDefinition>> = mcpToolRegistry.toolsFlow
  val mcpLogs: StateFlow<List<McpLogEntry>> = mcpExecutionLogger.logsFlow
  val hookRules: StateFlow<List<HookRule>> = hookEngine.rulesFlow

  val motionRepository = MotionProjectRepository(application, fileManager)
  val motionGenerator = MotionAiGenerator(aiClient)
  val motionExporter = MotionExporter(application, fileManager)

  val marketplaces: StateFlow<List<com.example.marketplace.model.MarketplaceSource>> = marketplaceManager.marketplacesFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val marketplaceCatalog: StateFlow<List<com.example.marketplace.model.UniversalExtension>> = marketplaceManager.catalog

  val isRefreshingMarketplaces: StateFlow<Boolean> = marketplaceManager.isRefreshing

  val installedExtensions: StateFlow<List<com.example.marketplace.model.InstalledExtension>> = skillPluginManager.installedExtensionsFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val providerConfig: StateFlow<ProviderConfig> = preferencesManager.providerConfigFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, ProviderConfig())

  val fetchedModels: StateFlow<List<String>> = preferencesManager.fetchedModelsFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val storageInitialized: StateFlow<Boolean> = preferencesManager.storageInitializedFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private val _isFetchingModels = MutableStateFlow(false)
  val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

  private val _modelsFetchStatus = MutableStateFlow<String?>(null)
  val modelsFetchStatus: StateFlow<String?> = _modelsFetchStatus.asStateFlow()

  private val _storageSetupMessage = MutableStateFlow<String?>(null)
  val storageSetupMessage: StateFlow<String?> = _storageSetupMessage.asStateFlow()

  val mcpServersJson: StateFlow<String> = preferencesManager.mcpServersJsonFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, "{\n  \"mcpServers\": {}\n}")

  val editorPreferences: StateFlow<EditorPreferences> = preferencesManager.editorPreferencesFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, EditorPreferences())

  val generalPreferences: StateFlow<GeneralPreferences> = preferencesManager.generalPreferencesFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, GeneralPreferences())

  val toolPermissions: StateFlow<ToolPermissionPreferences> = preferencesManager.toolPermissionsFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, ToolPermissionPreferences())

  val userAccount: StateFlow<UserAccountInfo> = preferencesManager.userAccountFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, UserAccountInfo())

  val appCapabilities: StateFlow<AppCapabilities> = preferencesManager.appCapabilitiesFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, AppCapabilities())

  val notificationsEnabled: StateFlow<Boolean> = preferencesManager.notificationsEnabledFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)

  val hapticEnabled: StateFlow<Boolean> = preferencesManager.hapticEnabledFlow
    .stateIn(viewModelScope, SharingStarted.Eagerly, true)

  private val _selectedProject = MutableStateFlow<Project?>(null)
  val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

  private val _openFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
  val openFiles: StateFlow<List<ProjectFile>> = _openFiles.asStateFlow()

  private val _activeFile = MutableStateFlow<ProjectFile?>(null)
  val activeFile: StateFlow<ProjectFile?> = _activeFile.asStateFlow()

  private val _activeFileContent = MutableStateFlow("")
  val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

  private val _isContentDirty = MutableStateFlow(false)
  val isContentDirty: StateFlow<Boolean> = _isContentDirty.asStateFlow()

  private val _buildResult = MutableStateFlow<BuildResult?>(null)
  val buildResult: StateFlow<BuildResult?> = _buildResult.asStateFlow()

  private val _connectionStatus = MutableStateFlow<String?>(null)
  val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

  val currentTask: StateFlow<AgentTask?> = agentEngine.currentTask

  // Mode selection: false = Chat mode, true = Code mode
  private val _isCodeMode = MutableStateFlow(false)
  val isCodeMode: StateFlow<Boolean> = _isCodeMode.asStateFlow()

  fun setCodeMode(isCode: Boolean) {
    _isCodeMode.value = isCode
  }

  // Claude Motion Mode: true when user is generating/editing animations in chat
  private val _isMotionMode = MutableStateFlow(false)
  val isMotionMode: StateFlow<Boolean> = _isMotionMode.asStateFlow()

  fun setMotionMode(isMotion: Boolean) {
    _isMotionMode.value = isMotion
    if (isMotion) {
      _isCodeMode.value = false
    }
  }

  // Currently active motion animation project in conversation
  private val _activeMotionProject = MutableStateFlow<MotionProject?>(null)
  val activeMotionProject: StateFlow<MotionProject?> = _activeMotionProject.asStateFlow()

  fun setActiveMotionProject(project: MotionProject?) {
    _activeMotionProject.value = project
  }

  // Conversation sessions management
  private val _conversations = MutableStateFlow<List<ConversationSession>>(emptyList())
  val conversations: StateFlow<List<ConversationSession>> = _conversations.asStateFlow()

  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  // Conversational AI Chat messages
  private val _chatMessages = MutableStateFlow<List<UiChatMessage>>(emptyList())
  val chatMessages: StateFlow<List<UiChatMessage>> = _chatMessages.asStateFlow()

  init {
    initDefaultWorkspace()
    loadConversationsMeta()
    loadPersistedChatMessages()
  }

  fun startNewConversation() {
    val newId = UUID.randomUUID().toString()
    val newSession = ConversationSession(
      id = newId,
      title = "New Chat",
      lastUpdated = System.currentTimeMillis()
    )
    _conversations.value = listOf(newSession) + _conversations.value
    _activeConversationId.value = newId
    _chatMessages.value = emptyList()
    _isCodeMode.value = false
    agentEngine.cancelCurrentTask()
    saveConversationsMeta()
  }

  fun selectConversation(session: ConversationSession) {
    _activeConversationId.value = session.id
    _isCodeMode.value = false
  }

  fun toggleStarConversation(id: String) {
    _conversations.value = _conversations.value.map {
      if (it.id == id) it.copy(isStarred = !it.isStarred) else it
    }
    saveConversationsMeta()
  }

  fun deleteConversation(id: String) {
    _conversations.value = _conversations.value.filter { it.id != id }
    if (_activeConversationId.value == id) {
      if (_conversations.value.isNotEmpty()) {
        selectConversation(_conversations.value.first())
      } else {
        startNewConversation()
      }
    }
    saveConversationsMeta()
  }

  private fun loadConversationsMeta() {
    try {
      val file = java.io.File(getApplication<Application>().filesDir, "conversations_meta.json")
      if (file.exists()) {
        val text = file.readText()
        val jsonArr = org.json.JSONArray(text)
        val list = mutableListOf<ConversationSession>()
        for (i in 0 until jsonArr.length()) {
          val obj = jsonArr.getJSONObject(i)
          list.add(
            ConversationSession(
              id = obj.getString("id"),
              title = obj.getString("title"),
              lastUpdated = obj.getLong("lastUpdated"),
              isStarred = obj.optBoolean("isStarred", false),
              previewText = obj.optString("previewText", "")
            )
          )
        }
        if (list.isNotEmpty()) {
          _conversations.value = list
          _activeConversationId.value = list.first().id
          return
        }
      }
    } catch (_: Exception) {}

    // Default initial conversation
    val initialSession = ConversationSession(
      title = "NEU\\CODE Workspace",
      lastUpdated = System.currentTimeMillis(),
      previewText = "Welcome to NEU\\CODE on Android"
    )
    _conversations.value = listOf(initialSession)
    _activeConversationId.value = initialSession.id
    saveConversationsMeta()
  }

  private fun saveConversationsMeta() {
    try {
      val file = java.io.File(getApplication<Application>().filesDir, "conversations_meta.json")
      val jsonArr = org.json.JSONArray()
      _conversations.value.forEach { sess ->
        val obj = org.json.JSONObject().apply {
          put("id", sess.id)
          put("title", sess.title)
          put("lastUpdated", sess.lastUpdated)
          put("isStarred", sess.isStarred)
          put("previewText", sess.previewText)
        }
        jsonArr.put(obj)
      }
      file.writeText(jsonArr.toString())
    } catch (_: Exception) {}
  }

  private fun loadPersistedChatMessages() {
    viewModelScope.launch {
      chatRepository.purgeEmptyMessages()
      chatRepository.messagesFlow.collect { savedMessages ->
        val currentRunning = _chatMessages.value.filter { it.isRunning && it.content.isNotBlank() }
        _chatMessages.update { prev ->
          val savedIds = savedMessages.map { it.id }.toSet()
          val unpersisted = currentRunning.filter { it.id !in savedIds }
          (savedMessages + unpersisted)
            .filter { it.content.isNotBlank() || it.role == MessageRole.USER }
            .distinctBy { it.id }
        }
      }
    }
  }

  private fun initDefaultWorkspace() {
    viewModelScope.launch {
      var list = fileManager.listProjects()
      if (list.isEmpty()) {
        val workspace = fileManager.createProject(
          name = "Workspace",
          type = ProjectType.KOTLIN_ANDROID,
          description = "Active developer workspace."
        )
        list = listOf(workspace)
      }
      val target = list.first()
      _selectedProject.value = target

      // Auto open first file or README
      val files = fileManager.listFiles(target)
      val mainFile = files.firstOrNull { it.name.equals("README.md", ignoreCase = true) }
        ?: files.firstOrNull { !it.isDirectory }
      if (mainFile != null) {
        openFile(mainFile)
      }
    }
  }

  fun openFile(file: ProjectFile) {
    if (file.isDirectory) return
    val currentOpen = _openFiles.value.toMutableList()
    if (currentOpen.none { it.path == file.path }) {
      currentOpen.add(file)
      _openFiles.value = currentOpen
    }
    _activeFile.value = file

    viewModelScope.launch {
      val project = _selectedProject.value ?: return@launch
      try {
        val content = fileManager.readFile(project, file.relativePath)
        _activeFileContent.value = content
        _isContentDirty.value = false
      } catch (e: Exception) {
        _activeFileContent.value = "// Error reading file: ${e.message}"
      }
    }
  }

  fun openFileByRelativePath(relativePath: String) {
    viewModelScope.launch {
      val project = _selectedProject.value ?: return@launch
      val files = fileManager.listFiles(project)
      val target = files.firstOrNull { it.relativePath == relativePath || it.name == relativePath }
      if (target != null) {
        openFile(target)
      }
    }
  }

  fun closeFile(file: ProjectFile) {
    val currentOpen = _openFiles.value.toMutableList()
    currentOpen.removeAll { it.path == file.path }
    _openFiles.value = currentOpen

    if (_activeFile.value?.path == file.path) {
      val next = currentOpen.lastOrNull()
      if (next != null) {
        openFile(next)
      } else {
        _activeFile.value = null
        _activeFileContent.value = ""
        _isContentDirty.value = false
      }
    }
  }

  suspend fun getWorkspaceProjectFiles(): List<ProjectFile> {
    val project = _selectedProject.value ?: return emptyList()
    return fileManager.listFiles(project).filter { !it.isDirectory }
  }

  suspend fun readProjectFile(relativePath: String): String {
    val project = _selectedProject.value ?: return ""
    return try {
      fileManager.readFile(project, relativePath)
    } catch (_: Exception) {
      ""
    }
  }

  fun updateActiveContent(content: String) {
    _activeFileContent.value = content
    _isContentDirty.value = true
  }

  fun saveActiveFile() {
    val file = _activeFile.value ?: return
    val project = _selectedProject.value ?: return
    val content = _activeFileContent.value

    viewModelScope.launch {
      try {
        fileManager.writeFile(project, file.relativePath, content)
        _isContentDirty.value = false
      } catch (_: Exception) {
      }
    }
  }

  fun startAgentTask(prompt: String) {
    val project = _selectedProject.value ?: return
    agentEngine.startTask(prompt, project, providerConfig.value)
  }

  fun sendChatMessage(prompt: String) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank()) return

    viewModelScope.launch {
      val project = _selectedProject.value ?: run {
        val existing = fileManager.listProjects()
        val p = if (existing.isNotEmpty()) existing.first() else {
          fileManager.createProject("Workspace", ProjectType.KOTLIN_ANDROID, "Active developer workspace.")
        }
        _selectedProject.value = p
        p
      }

      // Ensure active conversation session
      val currentId = _activeConversationId.value ?: run {
        val newId = UUID.randomUUID().toString()
        val newSess = ConversationSession(id = newId, title = cleanPrompt.take(32), previewText = cleanPrompt.take(50))
        _conversations.value = listOf(newSess) + _conversations.value
        _activeConversationId.value = newId
        newId
      }

      // Update conversation title and preview
      _conversations.value = _conversations.value.map { sess ->
        if (sess.id == currentId) {
          val newTitle = if (sess.title == "New Chat" || sess.title.isBlank()) cleanPrompt.take(32) else sess.title
          sess.copy(title = newTitle, previewText = cleanPrompt.take(50), lastUpdated = System.currentTimeMillis())
        } else sess
      }
      saveConversationsMeta()

      val userMsg = UiChatMessage(
        role = MessageRole.USER,
        content = cleanPrompt,
        timestamp = System.currentTimeMillis()
      )

      val assistantMsgId = UUID.randomUUID().toString()

      // Functional append of user message only - NEVER append empty assistant thinking message to database
      _chatMessages.update { prev -> (prev + userMsg).distinctBy { it.id } }
      chatRepository.saveMessage(userMsg)

      // Extension Command Check: Install or create skills/agents directly from chat
      if (com.example.extensions.chat.ChatExtensionCommandDetector.isExtensionCommand(cleanPrompt)) {
        val initialMsg = UiChatMessage(
          id = assistantMsgId,
          role = MessageRole.ASSISTANT,
          content = "⏳ Processing extension request...",
          timestamp = System.currentTimeMillis()
        )
        _chatMessages.update { prev -> (prev + initialMsg).distinctBy { it.id } }

        com.example.extensions.chat.ChatExtensionCommandDetector.executeCommand(
          getApplication(),
          cleanPrompt,
          object : com.example.extensions.chat.ChatExtensionCommandDetector.ChatCommandCallback {
            override fun onOperationStarted(extensionName: String, operation: String) {
              _chatMessages.update { prev ->
                prev.map { if (it.id == assistantMsgId) it.copy(content = "⏳ Starting: $operation...") else it }
              }
            }

            override fun onProgress(event: com.example.extensions.models.InstallationEvent) {
              _chatMessages.update { prev ->
                prev.map { if (it.id == assistantMsgId) it.copy(content = "📦 [${event.status}] ${event.currentOperation}") else it }
              }
            }

            override fun onCompleted(summaryResponse: String) {
              val completedMsg = UiChatMessage(
                id = assistantMsgId,
                role = MessageRole.ASSISTANT,
                content = summaryResponse,
                timestamp = System.currentTimeMillis()
              )
              _chatMessages.update { prev ->
                prev.map { if (it.id == assistantMsgId) completedMsg else it }
              }
              viewModelScope.launch {
                chatRepository.saveMessage(completedMsg)
              }
            }

            override fun onError(errorMessage: String) {
              val errorMsg = UiChatMessage(
                id = assistantMsgId,
                role = MessageRole.ASSISTANT,
                content = "❌ $errorMessage",
                timestamp = System.currentTimeMillis()
              )
              _chatMessages.update { prev ->
                prev.map { if (it.id == assistantMsgId) errorMsg else it }
              }
              viewModelScope.launch {
                chatRepository.saveMessage(errorMsg)
              }
            }
          }
        )
        return@launch
      }

      // Claude Motion Check: If in Motion Mode or user asked for motion graphics/video/animation
      if (_isMotionMode.value || isAnimationPrompt(cleanPrompt)) {
        handleMotionGenerationOrEdit(cleanPrompt, assistantMsgId)
        return@launch
      }

      // Start task in agent
      val startedTask = agentEngine.startTask(cleanPrompt, project, providerConfig.value)
      val targetTaskId = startedTask.id

      // Collect updates and synchronize assistant message
      var collectJob: kotlinx.coroutines.Job? = null
      collectJob = launch {
        agentEngine.currentTask.collect { task ->
          if (task == null) return@collect
          // CRITICAL: Only process events for THIS task so previous messages are never overwritten!
          if (task.id != targetTaskId) return@collect

          val isStillRunning = task.status == TaskStatus.PLANNING ||
            task.status == TaskStatus.INSPECTING ||
            task.status == TaskStatus.EDITING ||
            task.status == TaskStatus.VERIFYING

          val msgStatus = when {
            task.status == TaskStatus.COMPLETED -> MessageStatus.COMPLETED
            task.status == TaskStatus.FAILED -> MessageStatus.ERROR
            task.streamingDelta.isNotEmpty() || task.summary != null -> MessageStatus.STREAMING
            else -> MessageStatus.SUBMITTING
          }

          val statusText = when {
            isStillRunning && task.currentActivity.isNotBlank() -> task.currentActivity
            isStillRunning && task.currentPlan.isNotBlank() -> task.currentPlan
            isStillRunning -> "Generating response..."
            else -> null
          }

          // Content ONLY contains real answer or streaming delta - NEVER internal agent plans
          val content = when {
            !task.summary.isNullOrBlank() -> task.summary!!
            task.streamingDelta.isNotBlank() -> task.streamingDelta
            task.status == TaskStatus.FAILED -> "Task encountered an error."
            else -> ""
          }

          val modified = task.pendingChanges.map { it.relativePath }

          // Only add/update assistant message once content begins arriving or task completes
          if (content.isNotBlank() || !isStillRunning) {
            val responseContent = content.ifBlank { "Task completed." }
            _chatMessages.update { prevList ->
              val existingIdx = prevList.indexOfFirst { it.id == assistantMsgId }
              if (existingIdx >= 0) {
                prevList.mapIndexed { idx, msg ->
                  if (idx == existingIdx) {
                    msg.copy(
                      content = responseContent,
                      status = msgStatus,
                      currentStatusText = statusText,
                      steps = task.steps,
                      modifiedFiles = modified,
                      isRunning = isStillRunning,
                      isError = task.status == TaskStatus.FAILED,
                      summary = task.summary
                    )
                  } else msg
                }
              } else {
                val newAssistantMsg = UiChatMessage(
                  id = assistantMsgId,
                  role = MessageRole.ASSISTANT,
                  content = responseContent,
                  status = msgStatus,
                  currentStatusText = statusText,
                  modelName = providerConfig.value.modelId,
                  steps = task.steps,
                  modifiedFiles = modified,
                  isRunning = isStillRunning,
                  isError = task.status == TaskStatus.FAILED,
                  summary = task.summary,
                  timestamp = System.currentTimeMillis()
                )
                prevList + newAssistantMsg
              }.distinctBy { it.id }
            }
          }

          // When task completes, save to Room ONCE, open modified files, and terminate collector
          if (!isStillRunning) {
            val finalMsg = _chatMessages.value.firstOrNull { it.id == assistantMsgId }
            if (finalMsg != null && finalMsg.content.isNotBlank()) {
              chatRepository.saveMessage(finalMsg)
            }
            if (modified.isNotEmpty()) {
              val files = fileManager.listFiles(project)
              val touched = files.firstOrNull { it.relativePath == modified.last() }
              if (touched != null) {
                openFile(touched)
              }
            }
            // Terminate collector so subsequent tasks never mutate this assistant message
            collectJob?.cancel()
          }
        }
      }
    }
  }

  private fun isAnimationPrompt(prompt: String): Boolean {
    val lower = prompt.lowercase()
    return lower.contains("animation") || lower.contains("animate") ||
      lower.contains("video") || lower.contains("motion") ||
      lower.contains("kinetic") || lower.contains("motion graphics") ||
      lower.contains("cinematic") || lower.contains("explainer") ||
      lower.contains("logo animation")
  }

  private fun handleMotionGenerationOrEdit(prompt: String, assistantMsgId: String) {
    viewModelScope.launch {
      // Find latest motion project in current conversation or active
      val existingProject = _chatMessages.value
        .lastOrNull { it.motionProject != null }?.motionProject
        ?: _activeMotionProject.value

      // Create initial assistant message showing the real generation stage
      val initialAssistantMsg = UiChatMessage(
        id = assistantMsgId,
        role = MessageRole.ASSISTANT,
        content = "",
        status = MessageStatus.STREAMING,
        currentStatusText = "Planning animation...",
        modelName = providerConfig.value.modelId,
        isMotionGenerating = true,
        motionStageText = "Planning animation...",
        isRunning = false,
        timestamp = System.currentTimeMillis()
      )
      _chatMessages.update { (it + initialAssistantMsg).distinctBy { msg -> msg.id } }

      val lower = prompt.lowercase()
      val isEdit = existingProject != null && (
        lower.contains("change") || lower.contains("make") || lower.contains("slow") ||
        lower.contains("fast") || lower.contains("quicker") || lower.contains("longer") ||
        lower.contains("shorter") || lower.contains("background") || lower.contains("title") ||
        lower.contains("rename") || lower.contains("color") || lower.contains("scene") ||
        lower.contains("replace") || lower.contains("add") || lower.contains("remove") ||
        lower.contains("edit") || lower.contains("smoother") || lower.contains("glow") ||
        lower.contains("vertical") || lower.contains("format") || lower.contains("9:16") ||
        lower.contains("16:9") || lower.contains("square") || lower.contains("text")
      )

      val result: Result<MotionProject> = if (isEdit && existingProject != null) {
        motionGenerator.applyTargetedEdit(
          currentProject = existingProject,
          instruction = prompt,
          config = providerConfig.value,
          onStageChanged = { stage, message ->
            _chatMessages.update { list ->
              list.map { msg ->
                if (msg.id == assistantMsgId) {
                  msg.copy(
                    motionStageText = message,
                    currentStatusText = message
                  )
                } else msg
              }
            }
          }
        )
      } else {
        // Detect style and aspect ratio hints from prompt
        val detectedRatio = when {
          lower.contains("vertical") || lower.contains("9:16") || lower.contains("story") || lower.contains("tiktok") || lower.contains("reel") -> MotionAspectRatio.RATIO_9_16
          lower.contains("1:1") || lower.contains("square") || lower.contains("instagram") -> MotionAspectRatio.RATIO_1_1
          lower.contains("4:5") || lower.contains("portrait") -> MotionAspectRatio.RATIO_4_5
          else -> MotionAspectRatio.RATIO_16_9
        }
        val detectedStyle = when {
          lower.contains("cinematic") || lower.contains("movie") || lower.contains("film") -> MotionStyle.CINEMATIC
          lower.contains("neon") || lower.contains("cyberpunk") || lower.contains("glow") -> MotionStyle.NEON_CYBER
          lower.contains("chart") || lower.contains("data") || lower.contains("graph") || lower.contains("infographic") -> MotionStyle.INFOGRAPHIC
          lower.contains("playful") || lower.contains("bounce") || lower.contains("fun") -> MotionStyle.PLAYFUL
          lower.contains("kinetic") || lower.contains("typography") || lower.contains("text") -> MotionStyle.KINETIC_TYPO
          else -> MotionStyle.MODERN_MINIMAL
        }
        val detectedDuration = when {
          lower.contains("20") || lower.contains("twenty") -> 20.0
          lower.contains("15") || lower.contains("fifteen") -> 15.0
          lower.contains("10") || lower.contains("ten") -> 10.0
          lower.contains("5") || lower.contains("five") -> 5.0
          else -> 8.0
        }

        motionGenerator.generateAnimation(
          prompt = prompt,
          style = detectedStyle,
          aspectRatio = detectedRatio,
          targetDurationSeconds = detectedDuration,
          config = providerConfig.value,
          onStageChanged = { stage, message ->
            _chatMessages.update { list ->
              list.map { msg ->
                if (msg.id == assistantMsgId) {
                  msg.copy(
                    motionStageText = message,
                    currentStatusText = message
                  )
                } else msg
              }
            }
          }
        )
      }

      if (result.isSuccess) {
        val updatedProject = result.getOrThrow()
        motionRepository.saveProject(updatedProject)
        _activeMotionProject.value = updatedProject

        val summaryText = buildString {
          if (isEdit) {
            append("I have updated your animation **\"${updatedProject.name}\"** according to your instructions:\n\n")
          } else {
            append("Here is your generated animation for **\"${updatedProject.name}\"**:\n\n")
          }
          append("* **Style:** ${updatedProject.style.displayName}\n")
          append("* **Duration:** ${updatedProject.durationSeconds.toInt()}s total runtime\n")
          append("* **Scenes:** ${updatedProject.scenes.size} structured scenes\n")
          append("* **Original Format:** ${updatedProject.aspectRatio.label}\n\n")
          append("You can play, scrub, and replay the square 1:1 preview directly below, or continue typing instructions to adjust colors, timing, transitions, or text.")
        }

        _chatMessages.update { list ->
          list.map { msg ->
            if (msg.id == assistantMsgId) {
              msg.copy(
                content = summaryText,
                status = MessageStatus.COMPLETED,
                currentStatusText = null,
                isMotionGenerating = false,
                motionStageText = null,
                motionProject = updatedProject,
                motionProjectId = updatedProject.id,
                isRunning = false,
                isError = false
              )
            } else msg
          }
        }

        val finalMsg = _chatMessages.value.firstOrNull { it.id == assistantMsgId }
        if (finalMsg != null) {
          chatRepository.saveMessage(finalMsg)
        }
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Animation generation encountered an issue."
        _chatMessages.update { list ->
          list.map { msg ->
            if (msg.id == assistantMsgId) {
              msg.copy(
                content = "Could not generate animation: $errorMsg\n\nYou can try again or describe the animation differently.",
                status = MessageStatus.ERROR,
                currentStatusText = null,
                isMotionGenerating = false,
                motionStageText = null,
                isRunning = false,
                isError = true
              )
            } else msg
          }
        }
      }
    }
  }

  fun saveMotionProject(project: MotionProject, onSaved: (String) -> Unit = {}) {
    viewModelScope.launch {
      motionRepository.saveProject(project)
      val config = ExportConfig(format = ExportFormat.HTML_BUNDLE)
      val result = motionExporter.exportProject(project, config)
      onSaved(result.filePath ?: "Saved project to device storage")
    }
  }

  fun exportMotionProject(project: MotionProject, format: ExportFormat, onExported: (String) -> Unit = {}) {
    viewModelScope.launch {
      val config = ExportConfig(format = format)
      val result = motionExporter.exportProject(project, config)
      if (result.isSuccess) {
        onExported(result.message)
      } else {
        onExported("Export error: ${result.message}")
      }
    }
  }

  fun clearChat() {
    viewModelScope.launch {
      chatRepository.clearAllMessages()
    }
    _chatMessages.value = emptyList()
    agentEngine.cancelCurrentTask()
  }

  fun cancelAgentTask() {
    agentEngine.cancelCurrentTask()
    _chatMessages.update { list ->
      list.map { msg ->
        if (msg.isRunning) msg.copy(isRunning = false, currentStatusText = null) else msg
      }
    }
  }

  fun approveAgentAction() {
    agentEngine.approvePendingAction()
  }

  fun rejectAgentAction() {
    agentEngine.rejectPendingAction()
  }

  fun applyChange(change: FileChange) {
    val project = _selectedProject.value ?: return
    viewModelScope.launch {
      agentEngine.applyFileChange(project, change)
      if (_activeFile.value?.relativePath == change.relativePath) {
        _activeFileContent.value = change.proposedContent
        _isContentDirty.value = false
      }
    }
  }

  fun revertChange(change: FileChange) {
    val project = _selectedProject.value ?: return
    viewModelScope.launch {
      agentEngine.revertFileChange(project, change)
      if (_activeFile.value?.relativePath == change.relativePath) {
        _activeFileContent.value = change.originalContent
        _isContentDirty.value = false
      }
    }
  }

  fun runBuildCheck() {
    val project = _selectedProject.value ?: return
    viewModelScope.launch {
      val result = buildManager.checkAndBuild(project)
      _buildResult.value = result
    }
  }

  fun saveProviderConfig(config: ProviderConfig) {
    viewModelScope.launch {
      preferencesManager.saveProviderConfig(config)
    }
  }

  fun saveMcpServersJson(json: String) {
    viewModelScope.launch {
      preferencesManager.saveMcpServersJson(json)
    }
  }

  fun saveEditorPreferences(prefs: EditorPreferences) {
    viewModelScope.launch {
      preferencesManager.saveEditorPreferences(prefs)
    }
  }

  fun saveGeneralPreferences(prefs: GeneralPreferences) {
    viewModelScope.launch {
      preferencesManager.saveGeneralPreferences(prefs)
    }
  }

  fun saveToolPermissions(prefs: ToolPermissionPreferences) {
    viewModelScope.launch {
      preferencesManager.saveToolPermissions(prefs)
    }
  }

  fun saveUserAccount(account: UserAccountInfo) {
    viewModelScope.launch {
      preferencesManager.saveUserAccount(account)
    }
  }

  fun saveAppCapabilities(capabilities: AppCapabilities) {
    viewModelScope.launch {
      preferencesManager.saveAppCapabilities(capabilities)
    }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesManager.saveNotificationsEnabled(enabled)
    }
  }

  fun setHapticEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesManager.saveHapticEnabled(enabled)
    }
  }

  fun resetAllSettings() {
    viewModelScope.launch {
      preferencesManager.resetAllToDefaults()
    }
  }

  fun clearCache(onComplete: (Long) -> Unit) {
    viewModelScope.launch {
      preferencesManager.clearTemporaryCache()
      val freeMb = getApplication<Application>().filesDir.freeSpace / (1024 * 1024)
      onComplete(freeMb)
    }
  }

  fun testConnection() {
    viewModelScope.launch {
      _connectionStatus.value = "Testing connection..."
      val result = aiClient.testConnection(providerConfig.value)
      _connectionStatus.value = if (result.isSuccess) {
        result.getOrNull()
      } else {
        "Failed: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun initializeClaudeStorage() {
    viewModelScope.launch(Dispatchers.IO) {
      val result = fileManager.initializeClaudeStorageDirectories()
      preferencesManager.setStorageInitialized(true)
      _storageSetupMessage.value = result
    }
  }

  fun fetchModelsList() {
    viewModelScope.launch {
      _isFetchingModels.value = true
      _modelsFetchStatus.value = "Fetching models from endpoint..."
      val result = aiClient.fetchModels(providerConfig.value)
      _isFetchingModels.value = false
      if (result.isSuccess) {
        val models = result.getOrNull() ?: emptyList()
        preferencesManager.saveFetchedModels(models)
        _modelsFetchStatus.value = "✓ Successfully fetched ${models.size} models from endpoint!"
      } else {
        _modelsFetchStatus.value = "Failed: ${result.exceptionOrNull()?.message}"
      }
    }
  }

  // =========================================================================
  // UNIVERSAL PLUGIN & MARKETPLACE ACTIONS
  // =========================================================================

  fun installExtension(
    extension: com.example.marketplace.model.UniversalExtension,
    grantedPermissions: Set<com.example.marketplace.model.DeclaredPermission> = extension.declaredPermissions.toSet(),
    onComplete: (Boolean, String) -> Unit = { _, _ -> }
  ) {
    viewModelScope.launch {
      val res = skillPluginManager.installExtension(extension, grantedPermissions)
      if (res.isSuccess) {
        onComplete(true, "Successfully installed '${extension.name}'.")
      } else {
        onComplete(false, "Installation failed: ${res.exceptionOrNull()?.message}")
      }
    }
  }

  fun uninstallExtension(extensionId: String, onComplete: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val res = skillPluginManager.uninstallExtension(extensionId)
      onComplete(res.isSuccess)
    }
  }

  fun toggleExtension(extensionId: String, isEnabled: Boolean) {
    viewModelScope.launch {
      skillPluginManager.setEnabled(extensionId, isEnabled)
    }
  }

  fun updateExtensionPermissions(extensionId: String, permissions: Set<com.example.marketplace.model.DeclaredPermission>) {
    viewModelScope.launch {
      skillPluginManager.updateGrantedPermissions(extensionId, permissions)
    }
  }

  fun addCustomMarketplace(
    name: String,
    location: String,
    sourceType: com.example.marketplace.model.MarketplaceSourceType,
    ecosystem: com.example.marketplace.model.EcosystemType,
    onComplete: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch {
      val res = marketplaceManager.addMarketplace(name, location, sourceType, ecosystem)
      if (res.isSuccess) {
        onComplete(true, "Marketplace '${res.getOrNull()?.name}' added successfully.")
      } else {
        onComplete(false, "Failed to add marketplace: ${res.exceptionOrNull()?.message}")
      }
    }
  }

  fun refreshAllMarketplaces() {
    viewModelScope.launch {
      marketplaceManager.refreshAllMarketplaces()
    }
  }

  fun toggleMarketplace(marketplaceId: String, isEnabled: Boolean) {
    viewModelScope.launch {
      marketplaceManager.toggleMarketplace(marketplaceId, isEnabled)
    }
  }

  fun deleteMarketplace(marketplaceId: String) {
    viewModelScope.launch {
      marketplaceManager.deleteMarketplace(marketplaceId)
    }
  }

  fun checkForExtensionUpdates() {
    viewModelScope.launch {
      skillPluginManager.checkForUpdates()
    }
  }

  // --- MCP (Model Context Protocol) Actions ---

  fun connectMcpServer(config: McpServerConfig, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val ok = mcpServerManager.connectServer(config)
      if (ok) {
        onResult(true, "Connected to ${config.name}")
      } else {
        onResult(false, "Failed to connect to ${config.name}")
      }
    }
  }

  fun disconnectMcpServer(serverId: String) {
    mcpServerManager.disconnectServer(serverId)
  }

  fun addOrUpdateMcpServer(config: McpServerConfig) {
    viewModelScope.launch {
      mcpServerManager.addOrUpdateServer(config)
    }
  }

  fun deleteMcpServer(serverId: String) {
    viewModelScope.launch {
      mcpServerManager.deleteServer(serverId)
    }
  }

  fun approveMcpServer(serverId: String, approved: Boolean) {
    viewModelScope.launch {
      mcpServerManager.setServerApproved(serverId, approved)
    }
  }

  fun executeMcpTool(toolName: String, args: org.json.JSONObject, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = mcpToolRegistry.executeTool(toolName, args)
      onResult(res.isSuccess, res.output)
    }
  }

  fun importMcpServersJson(json: String): Int {
    val servers = mcpMarketplaceManager.parseMcpConfigJson(json)
    viewModelScope.launch {
      servers.forEach { s -> mcpServerManager.addOrUpdateServer(s) }
    }
    return servers.size
  }

  fun exportMcpServersJson(): String {
    return mcpMarketplaceManager.exportMcpConfigJson(mcpServers.value)
  }

  // --- Hooks & Automation Actions ---

  fun toggleHookRule(ruleId: String, isEnabled: Boolean) {
    viewModelScope.launch {
      hookEngine.toggleRule(ruleId, isEnabled)
    }
  }

  fun deleteHookRule(ruleId: String) {
    viewModelScope.launch {
      hookEngine.deleteRule(ruleId)
    }
  }

  fun formatFileWithHook(project: Project, relativePath: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
    viewModelScope.launch {
      val res = hookEngine.codeFormatter.formatFile(project, relativePath)
      onResult(res.isSuccess, res.message)
    }
  }
}
