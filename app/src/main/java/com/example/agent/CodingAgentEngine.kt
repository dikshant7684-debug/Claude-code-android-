package com.example.agent

import com.example.agent.core.AutonomousTaskManager
import com.example.agent.core.ExecutionEvent
import com.example.agent.core.ExecutionLoop
import com.example.agent.core.TaskPlanner
import com.example.agent.tools.UniversalToolRegistry
import com.example.agent.workspace.WorkspaceManager
import com.example.data.ai.AiProviderClient
import com.example.data.database.ProjectMemoryRepository
import com.example.data.files.ProjectFileManager
import com.example.data.model.AgentTask
import com.example.data.model.FileChange
import com.example.data.model.Project
import com.example.data.model.ProviderConfig
import com.example.marketplace.service.SkillAndPluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

import com.example.hooks.engine.HookEngine
import com.example.mcp.client.McpServerManager
import com.example.mcp.registry.McpToolRegistry

/**
 * Autonomous Coding Agent Engine integrating the TaskPlanner, ExecutionLoop,
 * WorkspaceManager, UniversalToolRegistry, and AutonomousTaskManager.
 *
 * Implements the full autonomous cycle:
 * UNDERSTAND -> INSPECT -> PLAN -> EXECUTE -> VERIFY -> DEBUG -> RETRY -> COMPLETE
 */
class CodingAgentEngine(
  private val scope: CoroutineScope,
  val toolRegistry: AgentToolRegistry,
  val aiClient: AiProviderClient,
  val fileManager: ProjectFileManager,
  val memoryRepository: ProjectMemoryRepository? = null,
  val skillManager: SkillAndPluginManager? = null,
  val mcpServerManager: McpServerManager? = null,
  val mcpToolRegistry: McpToolRegistry? = null,
  val hookEngine: HookEngine? = null
) {

  val workspaceManager: WorkspaceManager = WorkspaceManager(
    context = fileManager.context,
    fileManager = fileManager
  )

  val universalToolRegistry: UniversalToolRegistry = UniversalToolRegistry(
    workspaceManager = workspaceManager,
    buildManager = toolRegistry.buildManager,
    skillManager = skillManager,
    mcpServerManager = mcpServerManager,
    mcpToolRegistry = mcpToolRegistry,
    hookEngine = hookEngine
  )

  val taskPlanner: TaskPlanner = TaskPlanner()

  val executionLoop: ExecutionLoop = ExecutionLoop(
    toolRegistry = universalToolRegistry,
    workspaceManager = workspaceManager,
    aiClient = aiClient,
    taskPlanner = taskPlanner
  )

  val taskManager: AutonomousTaskManager = AutonomousTaskManager(
    context = fileManager.context,
    scope = scope,
    executionLoop = executionLoop,
    memoryRepository = memoryRepository
  )

  /**
   * Observable reactive state stream of the currently executing or latest autonomous task.
   */
  val currentTask: StateFlow<AgentTask?> = taskManager.activeTask

  /**
   * Observable stream of live execution events for real-time UI tracking.
   */
  val executionEvents: SharedFlow<ExecutionEvent> = taskManager.executionEvents

  /**
   * Submits and begins autonomous execution of a user coding request.
   */
  fun startTask(userPrompt: String, project: Project, config: ProviderConfig) {
    taskManager.submitTask(userPrompt, project, config)
  }

  /**
   * Safely halts the running autonomous task and terminates ongoing tool execution.
   */
  fun cancelCurrentTask() {
    taskManager.cancelActiveTask()
  }

  /**
   * Approves a sensitive operation waiting for explicit user permission.
   */
  fun approvePendingAction() {
    taskManager.approvePendingAction()
  }

  /**
   * Denies a sensitive operation waiting for explicit user permission.
   */
  fun rejectPendingAction() {
    taskManager.rejectPendingAction()
  }

  /**
   * Manually applies an individual file change to the workspace.
   */
  suspend fun applyFileChange(project: Project, change: FileChange) = withContext(Dispatchers.IO) {
    workspaceManager.writeCompleteFile(project, change.relativePath, change.proposedContent, autoApprove = true)
    val current = currentTask.value ?: return@withContext
    val updated = current.pendingChanges.map {
      if (it.relativePath == change.relativePath) it.copy(isApplied = true, isApproved = true) else it
    }
    // Updated in task manager
  }

  /**
   * Manually reverts an individual file change to its original content.
   */
  suspend fun revertFileChange(project: Project, change: FileChange) = withContext(Dispatchers.IO) {
    workspaceManager.writeCompleteFile(project, change.relativePath, change.originalContent, autoApprove = true)
    val current = currentTask.value ?: return@withContext
    val updated = current.pendingChanges.map {
      if (it.relativePath == change.relativePath) it.copy(isApplied = false, isApproved = false) else it
    }
  }
}
