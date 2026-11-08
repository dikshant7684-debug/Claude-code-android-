package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.AgentToolRegistry
import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.UniversalToolRegistry
import com.example.agent.workspace.WorkspaceManager
import com.example.build.BuildWorkflowManager
import com.example.data.files.ProjectFileManager
import com.example.data.model.Project
import com.example.data.model.ProjectType
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AiCodingToolSystemTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var fileManager: ProjectFileManager
  private lateinit var workspaceManager: WorkspaceManager
  private lateinit var buildManager: BuildWorkflowManager
  private lateinit var toolRegistry: UniversalToolRegistry
  private lateinit var agentToolRegistry: AgentToolRegistry
  private lateinit var project: Project
  private lateinit var projectRoot: File

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    fileManager = ProjectFileManager(context)
    projectRoot = tempFolder.newFolder("test_workspace")
    project = Project(
      id = "test_proj_1",
      name = "NEU\\CODE Test Workspace",
      rootPath = projectRoot.absolutePath,
      type = ProjectType.KOTLIN_ANDROID
    )
    workspaceManager = WorkspaceManager(context, fileManager)
    buildManager = BuildWorkflowManager(fileManager)
    toolRegistry = UniversalToolRegistry(workspaceManager, buildManager)
    agentToolRegistry = AgentToolRegistry(fileManager, buildManager)

    // Seed dummy Android project files
    val appDir = File(projectRoot, "app/src/main/java/com/example").apply { mkdirs() }
    val resDir = File(projectRoot, "app/src/main/res/values").apply { mkdirs() }
    File(projectRoot, "app/build.gradle.kts").writeText("""
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:1.6.0")
}
    """.trimIndent())
    File(projectRoot, "app/src/main/AndroidManifest.xml").writeText("""
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application android:label="Test App">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
    """.trimIndent())
    File(resDir, "strings.xml").writeText("""
<resources>
    <string name="app_name">Test App</string>
</resources>
    """.trimIndent())
    File(appDir, "MainActivity.kt").writeText("""
package com.example

class MainActivity {
    val title: String = "Test"

    fun computeAnswer(): Int {
        return 42
    }
}
    """.trimIndent())
  }

  @Test
  fun testRegistryExposesAll12CategoriesAndToolSchemas() {
    val tools = toolRegistry.getToolsList()
    assertTrue("Should register over 70 tools", tools.size >= 70)

    val prompt = toolRegistry.generateToolsPrompt(project)
    assertTrue(prompt.contains("Filesystem"))
    assertTrue(prompt.contains("Code Understanding"))
    assertTrue(prompt.contains("Code Editing"))
    assertTrue(prompt.contains("Terminal and Commands"))
    assertTrue(prompt.contains("Debugging"))
    assertTrue(prompt.contains("Git"))
    assertTrue(prompt.contains("Project Management"))
    assertTrue(prompt.contains("Web and Documentation"))
    assertTrue(prompt.contains("Android Development"))
    assertTrue(prompt.contains("Multi-Agent"))
    assertTrue(prompt.contains("MCP and Extensions"))
    assertTrue(prompt.contains("Quality and Security"))
  }

  @Test
  fun testCategory1_FilesystemTools() = runBlocking {
    // 1. create_file
    val createRes = toolRegistry.executeTool(
      "create_file",
      JSONObject().apply {
        put("relative_path", "test_file.txt")
        put("content", "Hello Filesystem Tools!")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(createRes is AutonomousToolResult.Success)

    // 2. read_file
    val readRes = toolRegistry.executeTool(
      "read_file",
      JSONObject().apply { put("relative_path", "test_file.txt") },
      project,
      autoApproveWrites = true
    )
    assertEquals("Hello Filesystem Tools!", readRes.output.trim())

    // 3. edit_file / apply_patch
    val editRes = toolRegistry.executeTool(
      "edit_file",
      JSONObject().apply {
        put("relative_path", "test_file.txt")
        put("target_content", "Hello")
        put("replacement_content", "Greetings")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(editRes is AutonomousToolResult.Success)

    // 4. get_file_info
    val infoRes = toolRegistry.executeTool(
      "get_file_info",
      JSONObject().apply { put("relative_path", "test_file.txt") },
      project,
      autoApproveWrites = true
    )
    assertTrue(infoRes.output.contains("Size:"))

    // 5. get_file_tree
    val treeRes = toolRegistry.executeTool(
      "get_file_tree",
      JSONObject(),
      project,
      autoApproveWrites = true
    )
    assertTrue(treeRes.output.contains("test_file.txt"))

    // 6. compare_files
    val compareRes = toolRegistry.executeTool(
      "compare_files",
      JSONObject().apply {
        put("path_a", "test_file.txt")
        put("path_b", "test_file.txt")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(compareRes.output.contains("identical"))

    // 7. watch_file_changes
    val watchRes = toolRegistry.executeTool(
      "watch_file_changes",
      JSONObject(),
      project,
      autoApproveWrites = true
    )
    assertTrue(watchRes is AutonomousToolResult.Success)
  }

  @Test
  fun testCategory2_CodeUnderstandingTools() = runBlocking {
    // 1. analyze_code
    val anaRes = toolRegistry.executeTool(
      "analyze_code",
      JSONObject().apply { put("relative_path", "app/src/main/java/com/example/MainActivity.kt") },
      project,
      autoApproveWrites = true
    )
    assertTrue(anaRes.output.contains("Code Analysis"))
    assertTrue(anaRes.output.contains("Total Lines"))

    // 2. detect_programming_language
    val langRes = toolRegistry.executeTool(
      "detect_programming_language",
      JSONObject().apply { put("relative_path", "app/src/main/java/com/example/MainActivity.kt") },
      project,
      autoApproveWrites = true
    )
    assertTrue(langRes.output.contains("Kotlin"))

    // 3. find_function
    val fnRes = toolRegistry.executeTool(
      "find_function",
      JSONObject().apply { put("function_name", "computeAnswer") },
      project,
      autoApproveWrites = true
    )
    assertTrue(fnRes.output.contains("computeAnswer"))

    // 4. find_class
    val clsRes = toolRegistry.executeTool(
      "find_class",
      JSONObject().apply { put("class_name", "MainActivity") },
      project,
      autoApproveWrites = true
    )
    assertTrue(clsRes.output.contains("MainActivity"))

    // 5. find_variable
    val varRes = toolRegistry.executeTool(
      "find_variable",
      JSONObject().apply { put("variable_name", "title") },
      project,
      autoApproveWrites = true
    )
    assertTrue(varRes.output.contains("title"))

    // 6. inspect_imports
    val impRes = toolRegistry.executeTool(
      "inspect_imports",
      JSONObject().apply { put("relative_path", "app/src/main/java/com/example/MainActivity.kt") },
      project,
      autoApproveWrites = true
    )
    assertTrue(impRes.output.contains("Package: package com.example"))
  }

  @Test
  fun testCategory3_CodeEditingTools() = runBlocking {
    val path = "app/src/main/java/com/example/MainActivity.kt"
    // 1. insert_code
    val insRes = toolRegistry.executeTool(
      "insert_code",
      JSONObject().apply {
        put("relative_path", path)
        put("line_number", 4)
        put("code", "    val subtitle: String = \"Sub\"")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(insRes is AutonomousToolResult.Success)

    // 2. format_code
    val fmtRes = toolRegistry.executeTool(
      "format_code",
      JSONObject().apply { put("relative_path", path) },
      project,
      autoApproveWrites = true
    )
    assertTrue(fmtRes is AutonomousToolResult.Success)

    // 3. generate_code
    val genRes = toolRegistry.executeTool(
      "generate_code",
      JSONObject().apply {
        put("code_type", "data_class")
        put("name", "UserProfile")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(genRes is AutonomousToolResult.Success)
    assertTrue(File(projectRoot, "app/src/main/java/com/example/model/UserProfile.kt").exists())
  }

  @Test
  fun testCategory4_TerminalAndCommandTools() = runBlocking {
    // 1. run_build
    val buildRes = toolRegistry.executeTool("run_build", JSONObject(), project, autoApproveWrites = true, autoApproveCommands = true)
    assertTrue(buildRes.output.contains("PASSED") || buildRes.output.contains("Status"))

    // 2. run_linter
    val lintRes = toolRegistry.executeTool("run_linter", JSONObject(), project, autoApproveWrites = true, autoApproveCommands = true)
    assertTrue(lintRes is AutonomousToolResult.Success)

    // 3. run_formatter
    val fmtAllRes = toolRegistry.executeTool("run_formatter", JSONObject(), project, autoApproveWrites = true, autoApproveCommands = true)
    assertTrue(fmtAllRes.output.contains("formatter executed"))
  }

  @Test
  fun testCategory5_DebuggingTools() = runBlocking {
    // 1. analyze_stack_trace
    val trace = """
java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference
    at com.example.MainActivity.computeAnswer(MainActivity.kt:15)
    at com.example.MainActivity.onCreate(MainActivity.kt:8)
    """.trimIndent()
    val traceRes = toolRegistry.executeTool(
      "analyze_stack_trace",
      JSONObject().apply { put("stack_trace", trace) },
      project,
      autoApproveWrites = true
    )
    assertTrue(traceRes.output.contains("MainActivity.kt at line 15"))

    // 2. suggest_fix
    val fixRes = toolRegistry.executeTool(
      "suggest_fix",
      JSONObject().apply { put("error_message", "Unresolved reference 'Modifier'") },
      project,
      autoApproveWrites = true
    )
    assertTrue(fixRes.output.contains("Missing import or undefined symbol"))

    // 3. detect_memory_issues
    val memRes = toolRegistry.executeTool("detect_memory_issues", JSONObject(), project, autoApproveWrites = true)
    assertTrue(memRes is AutonomousToolResult.Success)
  }

  @Test
  fun testCategory6_GitTools() = runBlocking {
    // 1. git_status
    val statusRes = toolRegistry.executeTool("git_status", JSONObject(), project, autoApproveWrites = true)
    assertTrue(statusRes.output.contains("On branch main"))

    // 2. git_create_branch
    val branchRes = toolRegistry.executeTool(
      "git_create_branch",
      JSONObject().apply { put("branch_name", "feature/ai-tools") },
      project,
      autoApproveWrites = true
    )
    assertTrue(branchRes.output.contains("Created branch 'feature/ai-tools'"))

    // 3. git_add and git_commit
    toolRegistry.executeTool("git_add", JSONObject().apply { put("path", ".") }, project, autoApproveWrites = true)
    val commitRes = toolRegistry.executeTool(
      "git_commit",
      JSONObject().apply { put("message", "Add full autonomous coding toolset") },
      project,
      autoApproveWrites = true
    )
    assertTrue(commitRes.output.contains("Add full autonomous coding toolset"))

    // 4. git_log
    val logRes = toolRegistry.executeTool("git_log", JSONObject(), project, autoApproveWrites = true)
    assertTrue(logRes.output.contains("commit"))
  }

  @Test
  fun testCategory7_ProjectManagementTools() = runBlocking {
    // 1. manage_environment_variables
    val setEnvRes = toolRegistry.executeTool(
      "manage_environment_variables",
      JSONObject().apply {
        put("action", "set")
        put("key", "API_ENDPOINT")
        put("value", "https://api.neucode.dev")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(setEnvRes.output.contains("Saved API_ENDPOINT"))

    val getEnvRes = toolRegistry.executeTool(
      "manage_environment_variables",
      JSONObject().apply {
        put("action", "get")
        put("key", "API_ENDPOINT")
      },
      project,
      autoApproveWrites = true
    )
    assertEquals("API_ENDPOINT=https://api.neucode.dev", getEnvRes.output)

    // 2. generate_readme
    val readmeRes = toolRegistry.executeTool("generate_readme", JSONObject(), project, autoApproveWrites = true)
    assertTrue(readmeRes.output.contains("Generated README.md"))
    assertTrue(File(projectRoot, "README.md").exists())
  }

  @Test
  fun testCategory8_WebDocsTools() = runBlocking {
    val docRes = toolRegistry.executeTool(
      "search_documentation",
      JSONObject().apply { put("query", "jetpack compose scaffold") },
      project,
      autoApproveWrites = true
    )
    assertTrue(docRes.output.contains("Scaffold"))
  }

  @Test
  fun testCategory9_AndroidDevTools() = runBlocking {
    // 1. inspect_android_manifest
    val manifestRes = toolRegistry.executeTool("inspect_android_manifest", JSONObject(), project, autoApproveWrites = true)
    assertTrue(manifestRes.output.contains("android.permission.INTERNET"))
    assertTrue(manifestRes.output.contains("MainActivity"))

    // 2. analyze_android_permissions
    val permRes = toolRegistry.executeTool("analyze_android_permissions", JSONObject(), project, autoApproveWrites = true)
    assertTrue(permRes.output.contains("Google Play Permissions Audit"))

    // 3. validate_android_resources
    val resCheck = toolRegistry.executeTool("validate_android_resources", JSONObject(), project, autoApproveWrites = true)
    assertTrue(resCheck.output.contains("strings.xml: Present"))
  }

  @Test
  fun testCategory10_MultiAgentTools() = runBlocking {
    // 1. create_subagent
    val createAgentRes = toolRegistry.executeTool(
      "create_subagent",
      JSONObject().apply {
        put("role", "researcher")
        put("name", "ResearchBot")
      },
      project,
      autoApproveWrites = true
    )
    assertTrue(createAgentRes.output.contains("Created SubAgent"))

    // 2. coordinate_agents
    val coordRes = toolRegistry.executeTool(
      "coordinate_agents",
      JSONObject().apply { put("workflow", "Refactor UI Architecture") },
      project,
      autoApproveWrites = true
    )
    assertTrue(coordRes.output.contains("Multi-Agent Orchestration Plan"))
  }

  @Test
  fun testCategory11_McpAndExtensionTools() = runBlocking {
    // 1. discover_skills
    val skillRes = toolRegistry.executeTool("discover_skills", JSONObject(), project, autoApproveWrites = true)
    assertTrue(skillRes.output.contains("android-cli"))

    // 2. discover_plugins
    val plugRes = toolRegistry.executeTool("discover_plugins", JSONObject(), project, autoApproveWrites = true)
    assertTrue(plugRes.output.contains("droid-git-pro"))
  }

  @Test
  fun testCategory12_SecurityQualityTools() = runBlocking {
    // 1. run_security_scan
    val secRes = toolRegistry.executeTool("run_security_scan", JSONObject(), project, autoApproveWrites = true)
    assertTrue(secRes is AutonomousToolResult.Success)

    // 2. detect_secrets
    val secretRes = toolRegistry.executeTool("detect_secrets", JSONObject(), project, autoApproveWrites = true)
    assertTrue(secretRes is AutonomousToolResult.Success)

    // 3. check_accessibility
    val a11yRes = toolRegistry.executeTool("check_accessibility", JSONObject(), project, autoApproveWrites = true)
    assertTrue(a11yRes is AutonomousToolResult.Success)
  }

  @Test
  fun testAgentToolRegistryDelegationToUniversalRegistry() = runBlocking {
    val res = agentToolRegistry.executeTool(
      name = "read_file",
      args = JSONObject().apply { put("relative_path", "app/src/main/AndroidManifest.xml") },
      project = project,
      autoApproveWrites = true
    )
    assertTrue(res is com.example.agent.ToolResult.Success)
    val success = res as com.example.agent.ToolResult.Success
    assertTrue(success.output.contains("android.permission.INTERNET"))
  }
}
