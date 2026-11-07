package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.core.AutonomousTaskManager
import com.example.agent.core.ExecutionEvent
import com.example.agent.core.ExecutionLoop
import com.example.agent.core.ExecutionStage
import com.example.agent.core.TaskCategory
import com.example.agent.core.TaskPlanner
import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.UniversalToolRegistry
import com.example.agent.workspace.WorkspaceManager
import com.example.build.BuildWorkflowManager
import com.example.data.ai.AiProviderClient
import com.example.data.files.ProjectFileManager
import com.example.data.model.AgentTask
import com.example.data.model.Project
import com.example.data.model.ProjectType
import com.example.data.model.ProviderConfig
import com.example.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutonomousAgentSystemTest {

  private lateinit var context: Context
  private lateinit var fileManager: ProjectFileManager
  private lateinit var workspaceManager: WorkspaceManager
  private lateinit var buildManager: BuildWorkflowManager
  private lateinit var toolRegistry: UniversalToolRegistry
  private lateinit var taskPlanner: TaskPlanner
  private lateinit var aiClient: AiProviderClient
  private lateinit var project: Project

  @Before
  fun setUp() = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    fileManager = ProjectFileManager(context)
    workspaceManager = WorkspaceManager(context, fileManager)
    buildManager = BuildWorkflowManager(fileManager)
    toolRegistry = UniversalToolRegistry(workspaceManager, buildManager)
    taskPlanner = TaskPlanner()
    aiClient = AiProviderClient()

    project = fileManager.createProject(
      name = "AgentTestProject_${System.currentTimeMillis()}",
      type = ProjectType.KOTLIN_ANDROID,
      description = "Test workspace for autonomous agent"
    )
  }

  @Test
  fun testWorkspaceManager_nestedFileCreationAndAtomicWrite() = runBlocking {
    // 1. Create nested directory
    val dir = workspaceManager.createDirectory(project, "src/main/kotlin/com/example")
    assertTrue(dir.exists())

    // 2. Create new file with content
    val (newFile, fileChange) = workspaceManager.createNewFile(
      project = project,
      relativePath = "src/main/kotlin/com/example/MyService.kt",
      content = "package com.example\n\nclass MyService {\n    fun compute(): Int = 42\n}\n"
    )
    assertTrue(newFile.exists())
    assertEquals("src/main/kotlin/com/example/MyService.kt", fileChange.relativePath)

    // 3. Read content back
    val readBack = workspaceManager.readFileContent(project, "src/main/kotlin/com/example/MyService.kt")
    assertTrue(readBack.contains("fun compute(): Int = 42"))

    // 4. Apply targeted patch
    val (patchChange, patchApplied) = workspaceManager.applyTargetedPatch(
      project = project,
      relativePath = "src/main/kotlin/com/example/MyService.kt",
      targetContent = "fun compute(): Int = 42",
      replacementContent = "fun compute(): Int = 100",
      autoApprove = true
    )
    assertTrue(patchApplied)
    val patchedContent = workspaceManager.readFileContent(project, "src/main/kotlin/com/example/MyService.kt")
    assertTrue(patchedContent.contains("fun compute(): Int = 100"))
    assertFalse(patchedContent.contains("42"))

    // 5. Verify operation manifest recorded operations
    val manifest = workspaceManager.getOperationManifest()
    assertTrue(manifest.isNotEmpty())
    assertTrue(manifest.any { it.relativePath == "src/main/kotlin/com/example/MyService.kt" })
  }

  @Test
  fun testWorkspaceManager_pathTraversalBlocked() {
    assertThrows(SecurityException::class.java) {
      workspaceManager.resolveSafeFile(project, "../../../system/secret.txt")
    }

    assertThrows(SecurityException::class.java) {
      workspaceManager.resolveSafeFile(project, "../../etc/hosts")
    }
  }

  @Test
  fun testWorkspaceManager_projectArchitectureInspection() = runBlocking {
    val overview = workspaceManager.inspectProjectArchitecture(project)
    assertNotNull(overview)
    assertEquals(project.name, overview.rootName)
    assertEquals(ProjectType.KOTLIN_ANDROID, overview.projectType)
    assertTrue(overview.testCommands.isNotEmpty())
  }

  @Test
  fun testTaskPlanner_intentClassification() {
    // 1. Informational queries
    assertEquals(TaskCategory.INFORMATIONAL_QUESTION, taskPlanner.classifyIntent("Hello! Who are you?"))
    assertEquals(TaskCategory.INFORMATIONAL_QUESTION, taskPlanner.classifyIntent("what can you do"))

    // 2. Code explanation (no edit intent)
    assertEquals(TaskCategory.CODE_EXPLANATION, taskPlanner.classifyIntent("How to use mutableStateOf in Jetpack Compose?"))

    // 3. Prompt optimization
    assertEquals(TaskCategory.PROMPT_OPTIMIZATION, taskPlanner.classifyIntent("Write an optimized system prompt for an Android architect"))

    // 4. Autonomous coding tasks
    assertEquals(TaskCategory.AUTONOMOUS_CODING_TASK, taskPlanner.classifyIntent("Fix the compile error in AppScreen.kt"))
    assertEquals(TaskCategory.AUTONOMOUS_CODING_TASK, taskPlanner.classifyIntent("Build a complete user profile screen with Jetpack Compose"))
  }

  @Test
  fun testTaskPlanner_planGenerationWithAcceptanceCriteria() = runBlocking {
    val overview = workspaceManager.inspectProjectArchitecture(project)
    val plan = taskPlanner.createPlan(
      userPrompt = "Add a Room database and UserRepository in UserRepository.kt",
      project = project,
      architecture = overview
    )

    assertEquals(TaskCategory.AUTONOMOUS_CODING_TASK, plan.category)
    assertTrue(plan.actionableSteps.size >= 4)
    assertTrue(plan.acceptanceCriteria.isNotEmpty())
    assertTrue(plan.executionStages.contains(ExecutionStage.VERIFY))
  }

  @Test
  fun testTaskPlanner_dynamicPlanAdaptationOnFailure() = runBlocking {
    val overview = workspaceManager.inspectProjectArchitecture(project)
    val initialPlan = taskPlanner.createPlan("Create User.kt", project, overview)
    val initialStepsCount = initialPlan.actionableSteps.size

    val adapted = taskPlanner.adaptPlanOnFailure(
      currentPlan = initialPlan,
      failedToolName = "apply_patch",
      errorMessage = "Target content not found in User.kt"
    )

    assertTrue(adapted.actionableSteps.size > initialStepsCount)
    assertTrue(adapted.executionStages.contains(ExecutionStage.DEBUG))
    assertTrue(adapted.executionStages.contains(ExecutionStage.RETRY))
  }

  @Test
  fun testUniversalToolRegistry_toolDiscoveryAndExecution() = runBlocking {
    val tools = toolRegistry.getToolsList()
    assertTrue(tools.any { it.name == "inspect_workspace" })
    assertTrue(tools.any { it.name == "read_file" })
    assertTrue(tools.any { it.name == "write_file" })
    assertTrue(tools.any { it.name == "apply_patch" })
    assertTrue(tools.any { it.name == "verify_code" })

    // Execute inspect_workspace tool
    val result = toolRegistry.executeTool(
      name = "inspect_workspace",
      args = JSONObject(),
      project = project,
      autoApproveWrites = true
    )
    assertTrue(result is AutonomousToolResult.Success)
    assertTrue(result.output.contains(project.name))
  }

  @Test
  fun testUniversalToolRegistry_multiFormatToolCallParsing() {
    // Format 1: XML tag <tool_call>
    val tagResponse = """
      I will inspect the workspace files first.
      <tool_call>
      {
        "name": "read_file",
        "arguments": {
          "relative_path": "README.md"
        }
      }
      </tool_call>
    """.trimIndent()
    val parsed1 = toolRegistry.parseToolCall(tagResponse)
    assertNotNull(parsed1)
    assertEquals("read_file", parsed1?.first)
    assertEquals("README.md", parsed1?.second?.getString("relative_path"))

    // Format 2: Markdown fenced code block
    val fenceResponse = """
      ```json
      {
        "name": "write_file",
        "arguments": {
          "relative_path": "Test.kt",
          "content": "class Test"
        }
      }
      ```
    """.trimIndent()
    val parsed2 = toolRegistry.parseToolCall(fenceResponse)
    assertNotNull(parsed2)
    assertEquals("write_file", parsed2?.first)
    assertEquals("Test.kt", parsed2?.second?.getString("relative_path"))

    // Format 3: Raw JSON object
    val rawJson = """{"name": "verify_code", "arguments": {}}"""
    val parsed3 = toolRegistry.parseToolCall(rawJson)
    assertNotNull(parsed3)
    assertEquals("verify_code", parsed3?.first)
  }

  @Test
  fun testAutonomousTaskManager_lifecycleAndCancellation() = runBlocking {
    val testScope = CoroutineScope(Dispatchers.IO + Job())
    val executionLoop = ExecutionLoop(toolRegistry, workspaceManager, aiClient, taskPlanner)
    val taskManager = AutonomousTaskManager(context, testScope, executionLoop)

    // Submit task
    val task = taskManager.submitTask(
      userPrompt = "Inspect project and build greeting module",
      project = project,
      config = ProviderConfig(apiKey = "")
    )
    assertNotNull(task)
    assertEquals(TaskStatus.PLANNING, taskManager.activeTask.value?.status)

    // Cancel task
    taskManager.cancelActiveTask()
    assertEquals(TaskStatus.CANCELLED, taskManager.activeTask.value?.status)
    assertTrue(taskManager.activeTask.value?.summary?.contains("cancelled", ignoreCase = true) == true)
  }

  @Test
  fun testAutonomousTaskManager_saveCheckpoint() = runBlocking {
    val testScope = CoroutineScope(Dispatchers.IO + Job())
    val executionLoop = ExecutionLoop(toolRegistry, workspaceManager, aiClient, taskPlanner)
    val taskManager = AutonomousTaskManager(context, testScope, executionLoop)

    val overview = workspaceManager.inspectProjectArchitecture(project)
    val plan = taskPlanner.createPlan("Create Sample.kt", project, overview)
    val testTask = AgentTask(
      id = "test_task_checkpoint_123",
      projectId = project.id,
      userPrompt = "Create Sample.kt",
      status = TaskStatus.EDITING,
      currentPlan = plan.planOverview
    )

    taskManager.saveTaskCheckpoint(testTask, ExecutionStage.EXECUTE, plan)

    val checkpointFile = File(context.filesDir, "Claude code/tasks/checkpoint_test_task_checkpoint_123.json")
    assertTrue(checkpointFile.exists())
    val content = checkpointFile.readText()
    assertTrue(content.contains("test_task_checkpoint_123"))
    assertTrue(content.contains(project.id))
  }
}
