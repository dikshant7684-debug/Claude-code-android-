package com.example.agent.core

import android.content.Context
import com.example.data.database.ProjectMemoryRepository
import com.example.data.model.AgentTask
import com.example.data.model.FileChange
import com.example.data.model.Project
import com.example.data.model.ProviderConfig
import com.example.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * AutonomousTaskManager owns the lifecycle of each autonomous coding task:
 * planning, execution loop supervision, checkpoints saving, disk persistence, and restoration.
 */
class AutonomousTaskManager(
  private val context: Context,
  private val scope: CoroutineScope,
  val executionLoop: ExecutionLoop,
  private val memoryRepository: ProjectMemoryRepository? = null
) {

  private val _activeTask = MutableStateFlow<AgentTask?>(null)
  val activeTask: StateFlow<AgentTask?> = _activeTask.asStateFlow()

  val executionEvents: SharedFlow<ExecutionEvent> = executionLoop.events

  private var executionJob: Job? = null

  private val tasksDir by lazy {
    File(context.filesDir, "Claude code/tasks").apply { mkdirs() }
  }

  init {
    restoreLatestCheckpointIfInterrupted()
  }

  /**
   * Starts a new autonomous coding task, creating initial task state and launching the execution loop.
   */
  fun submitTask(
    userPrompt: String,
    project: Project,
    config: ProviderConfig
  ): AgentTask {
    cancelActiveTask()

    val taskId = UUID.randomUUID().toString()
    val initialTask = AgentTask(
      id = taskId,
      projectId = project.id,
      userPrompt = userPrompt,
      status = TaskStatus.PLANNING,
      currentPlan = "Analyzing request and inspecting project...",
      currentActivity = "Starting autonomous execution...",
      steps = emptyList(),
      pendingChanges = emptyList()
    )

    _activeTask.value = initialTask

    executionJob = scope.launch(Dispatchers.IO) {
      executionLoop.executeTask(
        initialTask = initialTask,
        project = project,
        config = config,
        taskStateFlow = _activeTask,
        onCheckpoint = { currentTask, stage, plan ->
          saveTaskCheckpoint(currentTask, stage, plan)
        }
      )
    }

    return initialTask
  }

  /**
   * Cancels the currently running autonomous task and terminates tool execution.
   */
  fun cancelActiveTask() {
    executionJob?.cancel()
    executionJob = null
    executionLoop.rejectPending()

    _activeTask.value?.let { current ->
      if (current.status != TaskStatus.COMPLETED && current.status != TaskStatus.FAILED) {
        _activeTask.value = current.copy(
          status = TaskStatus.CANCELLED,
          currentActivity = "Task cancelled by user",
          summary = "Task cancelled by user."
        )
      }
    }
  }

  fun approvePendingAction() {
    executionLoop.approvePending()
  }

  fun rejectPendingAction() {
    executionLoop.rejectPending()
  }

  /**
   * Saves task state checkpoint to disk to survive application restarts and allow resumption.
   */
  suspend fun saveTaskCheckpoint(
    task: AgentTask,
    stage: ExecutionStage,
    plannedTask: PlannedTask
  ) = withContext(Dispatchers.IO) {
    try {
      val checkpointFile = File(tasksDir, "checkpoint_${task.id}.json")
      val json = JSONObject().apply {
        put("taskId", task.id)
        put("projectId", task.projectId)
        put("userPrompt", task.userPrompt)
        put("status", task.status.name)
        put("stage", stage.name)
        put("currentPlan", task.currentPlan)
        put("currentActivity", task.currentActivity)
        put("summary", task.summary ?: "")
        put("createdTimestamp", task.createdTimestamp)
        put("timestamp", System.currentTimeMillis())

        val stepsArr = JSONArray()
        task.steps.forEach { step ->
          stepsArr.put(JSONObject().apply {
            put("id", step.id)
            put("title", step.title)
            put("description", step.description)
            put("toolName", step.toolName ?: "")
            put("toolInput", step.toolInput ?: "")
            put("toolOutput", step.toolOutput ?: "")
            put("status", step.status.name)
          })
        }
        put("steps", stepsArr)

        val filesArr = JSONArray()
        task.pendingChanges.forEach { f ->
          filesArr.put(f.relativePath)
        }
        put("modifiedFiles", filesArr)

        val criteriaArr = JSONArray()
        plannedTask.acceptanceCriteria.forEach { criteriaArr.put(it) }
        put("acceptanceCriteria", criteriaArr)
      }
      checkpointFile.writeText(json.toString(), Charsets.UTF_8)

      // Also persist task summary to Project Memory if finished
      if (task.status == TaskStatus.COMPLETED && !task.summary.isNullOrBlank()) {
        try {
          memoryRepository?.recordMemory(
            projectId = task.projectId,
            category = "AUTONOMOUS_TASK",
            key = "Task_${task.id.take(8)}",
            content = "Prompt: ${task.userPrompt.take(100)} | Result: ${task.summary!!.take(200)}",
            importance = 0.7f
          )
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {
      // Graceful fallback for checkpointing
    }
  }

  /**
   * Inspects disk checkpoints to restore any unfinished task after an unexpected kill/restart.
   */
  private fun restoreLatestCheckpointIfInterrupted() {
    scope.launch(Dispatchers.IO) {
      try {
        val files = tasksDir.listFiles { f -> f.name.startsWith("checkpoint_") && f.name.endsWith(".json") }
          ?: return@launch
        val latest = files.maxByOrNull { it.lastModified() } ?: return@launch
        val json = JSONObject(latest.readText(Charsets.UTF_8))
        val statusStr = json.optString("status", TaskStatus.IDLE.name)
        val status = try { TaskStatus.valueOf(statusStr) } catch (_: Exception) { TaskStatus.IDLE }

        if (status == TaskStatus.PLANNING || status == TaskStatus.INSPECTING || status == TaskStatus.EDITING || status == TaskStatus.VERIFYING) {
          val restoredTask = AgentTask(
            id = json.getString("taskId"),
            projectId = json.getString("projectId"),
            userPrompt = json.getString("userPrompt"),
            status = TaskStatus.CANCELLED,
            currentPlan = json.optString("currentPlan", "Interrupted during previous session"),
            currentActivity = "Task was interrupted by app termination. Ready to resume.",
            summary = "Interrupted by system event. Checkpoints preserved."
          )
          _activeTask.value = restoredTask
        }
      } catch (_: Exception) {}
    }
  }
}
