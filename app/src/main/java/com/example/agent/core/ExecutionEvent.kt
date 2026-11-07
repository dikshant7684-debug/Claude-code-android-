package com.example.agent.core

import com.example.data.model.FileChange

/**
 * The autonomous cycle stages defined in the agent specification:
 * UNDERSTAND -> INSPECT -> PLAN -> EXECUTE -> VERIFY -> DEBUG -> RETRY -> COMPLETE
 */
enum class ExecutionStage(val displayName: String) {
  UNDERSTAND("Understanding Request"),
  INSPECT("Inspecting Workspace"),
  PLAN("Planning Solution"),
  EXECUTE("Executing Operations"),
  VERIFY("Verifying Changes"),
  DEBUG("Diagnosing & Fixing"),
  RETRY("Retrying Operation"),
  COMPLETE("Task Complete")
}

/**
 * Real-time structured events emitted throughout the lifecycle of every autonomous task.
 * Enables the UI, activity panel, and logs to display the real active file, command, operation status,
 * and execution history.
 */
sealed class ExecutionEvent {
  abstract val taskId: String
  abstract val timestamp: Long

  data class TaskStarted(
    override val taskId: String,
    val prompt: String,
    val projectId: String,
    val stage: ExecutionStage = ExecutionStage.UNDERSTAND,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class StageChanged(
    override val taskId: String,
    val stage: ExecutionStage,
    val description: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class PlanGenerated(
    override val taskId: String,
    val planOverview: String,
    val plannedSteps: List<String>,
    val acceptanceCriteria: List<String>,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class ToolInvoked(
    override val taskId: String,
    val toolName: String,
    val argumentsJson: String,
    val category: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class ToolExecutionFinished(
    override val taskId: String,
    val toolName: String,
    val isSuccess: Boolean,
    val outputPreview: String,
    val modifiedFiles: List<String> = emptyList(),
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class ApprovalRequested(
    override val taskId: String,
    val toolName: String,
    val explanation: String,
    val proposedChange: FileChange?,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class VerificationResult(
    override val taskId: String,
    val isPassed: Boolean,
    val summary: String,
    val issuesFound: Int,
    val diagnostics: List<String> = emptyList(),
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class ModelStreaming(
    override val taskId: String,
    val contentDelta: String,
    val reasoningDelta: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class CheckpointSaved(
    override val taskId: String,
    val checkpointId: String,
    val stepIndex: Int,
    val summary: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class TaskCompleted(
    override val taskId: String,
    val summary: String,
    val modifiedFiles: List<String>,
    val totalSteps: Int,
    val durationMs: Long,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class TaskFailed(
    override val taskId: String,
    val reason: String,
    val isRecoverable: Boolean = false,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()

  data class TaskCancelled(
    override val taskId: String,
    val reason: String = "Cancelled by user",
    override val timestamp: Long = System.currentTimeMillis()
  ) : ExecutionEvent()
}

/**
 * Serializable checkpoint representation stored on disk to safely resume interrupted tasks.
 */
data class TaskCheckpoint(
  val taskId: String,
  val projectId: String,
  val userPrompt: String,
  val stage: String,
  val plan: String,
  val plannedSteps: List<String>,
  val completedSteps: List<String>,
  val acceptanceCriteria: List<String>,
  val modifiedFiles: List<String>,
  val toolExecutionHistory: List<String>,
  val iterationCount: Int,
  val timestamp: Long = System.currentTimeMillis()
)
