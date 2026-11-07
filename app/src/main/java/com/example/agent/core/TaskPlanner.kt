package com.example.agent.core

import com.example.agent.workspace.ProjectArchitectureOverview
import com.example.data.model.AgentStep
import com.example.data.model.Project
import com.example.data.model.StepStatus
import com.example.ui.formatting.ResponseFormatDetector

/**
 * Distinguishes conversational questions from multi-step autonomous coding workflows.
 */
enum class TaskCategory {
  INFORMATIONAL_QUESTION,
  CODE_EXPLANATION,
  PROMPT_OPTIMIZATION,
  AUTONOMOUS_CODING_TASK
}

/**
 * Concrete, actionable plan for an autonomous coding task with explicit acceptance criteria.
 */
data class PlannedTask(
  val category: TaskCategory,
  val planOverview: String,
  val executionStages: List<ExecutionStage>,
  val actionableSteps: List<AgentStep>,
  val acceptanceCriteria: List<String>,
  val targetFiles: List<String> = emptyList(),
  val requiresVerification: Boolean = true
)

/**
 * TaskPlanner converts the user's request into ordered, actionable steps with clear completion criteria,
 * and classifies whether a request requires the full autonomous cycle or direct conversational handling.
 */
class TaskPlanner {

  /**
   * Classifies user intent to determine whether to run full autonomous execution
   * or direct conversational handling.
   */
  fun classifyIntent(prompt: String): TaskCategory {
    val clean = prompt.trim().lowercase()

    // 1. Direct prompt creation or optimization requests
    if (ResponseFormatDetector.isPromptRequest(prompt) || (clean.contains("prompt") && (clean.contains("write") || clean.contains("create") || clean.contains("optimize")))) {
      return TaskCategory.PROMPT_OPTIMIZATION
    }

    // 2. Greetings and general questions
    if (clean.startsWith("hi") || clean.startsWith("hello") || clean.startsWith("hey") || clean.contains("who are you") || clean == "help" || clean.contains("what can you do")) {
      return TaskCategory.INFORMATIONAL_QUESTION
    }

    // 3. Simple conceptual questions without modification intent
    val isQuestion = clean.startsWith("how to ") || clean.startsWith("what is ") || clean.startsWith("explain ") || clean.startsWith("why ")
    val hasEditIntent = clean.contains("fix") || clean.contains("add") || clean.contains("create") || clean.contains("implement") ||
      clean.contains("refactor") || clean.contains("build") || clean.contains("test") || clean.contains("debug") || clean.contains("update") || clean.contains("modify")

    if (isQuestion && !hasEditIntent) {
      return TaskCategory.CODE_EXPLANATION
    }

    return TaskCategory.AUTONOMOUS_CODING_TASK
  }

  /**
   * Generates a structured multi-step plan with acceptance criteria based on user request and project architecture.
   */
  fun createPlan(
    userPrompt: String,
    project: Project,
    architecture: ProjectArchitectureOverview
  ): PlannedTask {
    val category = classifyIntent(userPrompt)
    val lower = userPrompt.lowercase()

    if (category != TaskCategory.AUTONOMOUS_CODING_TASK) {
      return PlannedTask(
        category = category,
        planOverview = "Direct response for conversational inquiry: \"${userPrompt.take(60)}\"",
        executionStages = listOf(ExecutionStage.UNDERSTAND, ExecutionStage.COMPLETE),
        actionableSteps = listOf(
          AgentStep(
            id = "step_respond",
            title = "Respond to Inquiry",
            description = "Formulate expert, production-grade response",
            status = StepStatus.PENDING
          )
        ),
        acceptanceCriteria = listOf("Provide complete, accurate response with code examples"),
        requiresVerification = false
      )
    }

    // Autonomous coding task: build multi-step plan
    val steps = mutableListOf<AgentStep>()
    val criteria = mutableListOf<String>()
    val targetFiles = mutableListOf<String>()

    // Determine target files from prompt or project entry points
    val filePattern = Regex("""[\w\-\./]+\.(?:kt|java|py|js|ts|json|xml|gradle|md)""")
    val matches = filePattern.findAll(userPrompt).map { it.value }.toList()
    targetFiles.addAll(matches)

    // Step 1: UNDERSTAND & INSPECT
    steps.add(
      AgentStep(
        id = "step_1_inspect",
        title = "Inspect Workspace Context",
        description = "Analyze project architecture and existing files related to request.",
        toolName = "inspect_workspace",
        status = StepStatus.PENDING
      )
    )
    criteria.add("Workspace context and dependencies inspected")

    // Step 2: TARGETED READ OR SEARCH
    if (targetFiles.isNotEmpty()) {
      steps.add(
        AgentStep(
          id = "step_2_read",
          title = "Read Existing Target Files",
          description = "Read existing content of ${targetFiles.joinToString(", ")} before modifying.",
          toolName = "read_file",
          status = StepStatus.PENDING
        )
      )
      criteria.add("Existing file content examined to preserve unrelated logic")
    } else {
      steps.add(
        AgentStep(
          id = "step_2_search",
          title = "Locate Target Code Modules",
          description = "Search codebase for components relevant to: \"${userPrompt.take(40)}\"",
          toolName = "search_files",
          status = StepStatus.PENDING
        )
      )
      criteria.add("Relevant files identified in project structure")
    }

    // Step 3: EXECUTE MODIFICATIONS
    val editDesc = when {
      lower.contains("fix") || lower.contains("debug") -> "Apply targeted patch to resolve identified issue."
      lower.contains("add") || lower.contains("create") -> "Implement new module and wire required dependencies."
      else -> "Apply code changes according to specification."
    }
    steps.add(
      AgentStep(
        id = "step_3_execute",
        title = "Apply Code Changes",
        description = editDesc,
        toolName = if (lower.contains("patch")) "apply_patch" else "write_file",
        status = StepStatus.PENDING
      )
    )
    criteria.add("Code changes successfully applied with zero syntax or regression errors")

    // Step 4: VERIFY
    steps.add(
      AgentStep(
        id = "step_4_verify",
        title = "Verify Code Diagnostics",
        description = "Run syntax and compilation checks across workspace.",
        toolName = "verify_code",
        status = StepStatus.PENDING
      )
    )
    criteria.add("Project syntax and build checks pass without breaking errors")

    // Step 5: COMPLETE
    steps.add(
      AgentStep(
        id = "step_5_summary",
        title = "Summarize Implementation",
        description = "Provide clear summary of applied changes, files modified, and verification results.",
        status = StepStatus.PENDING
      )
    )
    criteria.add("User presented with complete summary and instructions to view changes in Workspace")

    val stages = listOf(
      ExecutionStage.UNDERSTAND,
      ExecutionStage.INSPECT,
      ExecutionStage.PLAN,
      ExecutionStage.EXECUTE,
      ExecutionStage.VERIFY,
      ExecutionStage.COMPLETE
    )

    return PlannedTask(
      category = category,
      planOverview = "Autonomous execution for: \"${userPrompt.take(80)}\"",
      executionStages = stages,
      actionableSteps = steps,
      acceptanceCriteria = criteria,
      targetFiles = targetFiles.distinct(),
      requiresVerification = true
    )
  }

  /**
   * Dynamically adapts the plan when unexpected errors or verification failures occur (DEBUG / RETRY stage).
   */
  fun adaptPlanOnFailure(
    currentPlan: PlannedTask,
    failedToolName: String,
    errorMessage: String
  ): PlannedTask {
    val updatedSteps = currentPlan.actionableSteps.toMutableList()
    val debugStep = AgentStep(
      id = "step_debug_${System.currentTimeMillis()}",
      title = "Debug & Resolve: $failedToolName",
      description = "Diagnose issue: ${errorMessage.take(100)}. Reread file or refine patch parameters.",
      toolName = "read_file",
      status = StepStatus.PENDING
    )
    updatedSteps.add(updatedSteps.size - 2, debugStep)

    val updatedCriteria = currentPlan.acceptanceCriteria + "Fix failed operation ($failedToolName): ${errorMessage.take(60)}"

    return currentPlan.copy(
      executionStages = currentPlan.executionStages + listOf(ExecutionStage.DEBUG, ExecutionStage.RETRY),
      actionableSteps = updatedSteps,
      acceptanceCriteria = updatedCriteria
    )
  }
}
