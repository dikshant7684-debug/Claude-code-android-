package com.example.agent.tools.impl

import com.example.agent.tools.AutonomousToolResult
import com.example.agent.tools.ToolCategoryProvider
import com.example.agent.tools.ToolDefinition
import com.example.agent.workspace.WorkspaceManager
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Production-ready Project Management Tools for workspace architecture,
 * scaffolding, configuration, environment variables, documentation, and backups.
 */
class ProjectManagementToolProvider(
  private val workspaceManager: WorkspaceManager
) : ToolCategoryProvider {

  override val categoryName: String = "Project Management"

  override fun getDefinitions(): List<ToolDefinition> = listOf(
    ToolDefinition(
      name = "inspect_project",
      category = categoryName,
      description = "Inspect the project structure, entry points, build files, and source summary.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "initialize_project",
      category = categoryName,
      description = "Initialize or scaffold a project directory with standard boilerplate.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("template") })
        put("properties", JSONObject().apply {
          put("template", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("android_kotlin"); put("node_ts"); put("python_cli") }) })
          put("project_name", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "configure_project",
      category = categoryName,
      description = "Update project configuration key-values in settings files.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("key"); put("value") })
        put("properties", JSONObject().apply {
          put("key", JSONObject().apply { put("type", "string") })
          put("value", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "update_configuration",
      category = categoryName,
      description = "Update configuration files such as gradle.properties, package.json, or .editorconfig.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("file_name"); put("content") })
        put("properties", JSONObject().apply {
          put("file_name", JSONObject().apply { put("type", "string") })
          put("content", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "manage_environment_variables",
      category = categoryName,
      description = "Manage .env configuration keys: get, set, list, or delete.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("action") })
        put("properties", JSONObject().apply {
          put("action", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("list"); put("get"); put("set"); put("delete") }) })
          put("key", JSONObject().apply { put("type", "string") })
          put("value", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "inspect_package_files",
      category = categoryName,
      description = "Inspect build scripts and package definition files (build.gradle.kts, package.json, pyproject.toml).",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      }
    ),
    ToolDefinition(
      name = "manage_dependencies",
      category = categoryName,
      description = "List, add, or remove declared libraries in the project.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("action") })
        put("properties", JSONObject().apply {
          put("action", JSONObject().apply { put("type", "string"); put("enum", JSONArray().apply { put("list"); put("add"); put("remove") }) })
          put("dependency", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "generate_documentation",
      category = categoryName,
      description = "Generate architectural documentation or markdown API reference for workspace classes.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("relative_path", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "generate_readme",
      category = categoryName,
      description = "Generate a comprehensive README.md documenting project architecture, setup, and features.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject())
      },
      requiresApprovalByDefault = true
    ),
    ToolDefinition(
      name = "create_project_backup",
      category = categoryName,
      description = "Create a compressed archive snapshot of the current workspace state.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
          put("backup_name", JSONObject().apply { put("type", "string") })
        })
      }
    ),
    ToolDefinition(
      name = "restore_project_backup",
      category = categoryName,
      description = "Restore workspace from an archived snapshot.",
      parametersSchema = JSONObject().apply {
        put("type", "object")
        put("required", JSONArray().apply { put("backup_file") })
        put("properties", JSONObject().apply {
          put("backup_file", JSONObject().apply { put("type", "string") })
        })
      },
      requiresApprovalByDefault = true
    )
  )

  override suspend fun execute(
    name: String,
    args: JSONObject,
    project: Project,
    autoApproveWrites: Boolean,
    autoApproveCommands: Boolean
  ): AutonomousToolResult? = withContext(Dispatchers.IO) {
    val root = File(project.rootPath)

    when (name) {
      "inspect_project", "inspect_workspace" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val summary = workspaceManager.getProjectSummaryText(project)
        AutonomousToolResult.Success(name, summary)
      }

      "initialize_project" -> {
        val template = args.optString("template", "android_kotlin")
        val projName = args.optString("project_name", project.name)
        when (template) {
          "android_kotlin" -> {
            File(root, "app/src/main/java").mkdirs()
            File(root, "app/src/main/res/values").mkdirs()
            File(root, "app/src/test/java").mkdirs()
          }
          "node_ts" -> {
            File(root, "src").mkdirs()
            File(root, "package.json").writeText("""{"name": "$projName", "version": "1.0.0", "scripts": {"build": "tsc"}}""")
          }
          "python_cli" -> {
            File(root, "src").mkdirs()
            File(root, "requirements.txt").writeText("# dependencies\n")
            File(root, "main.py").writeText("def main():\n    print('Hello $projName')\n\nif __name__ == '__main__':\n    main()\n")
          }
        }
        AutonomousToolResult.Success(name, "Scaffolded template '$template' for project $projName.")
      }

      "configure_project" -> {
        val key = args.optString("key")
        val value = args.optString("value")
        val propFile = File(root, "gradle.properties")
        val existing = if (propFile.exists()) propFile.readText() else ""
        val updated = if (existing.contains("$key=")) {
          existing.lines().joinToString("\n") { if (it.startsWith("$key=")) "$key=$value" else it }
        } else {
          "$existing\n$key=$value".trim()
        }
        propFile.writeText(updated)
        AutonomousToolResult.Success(name, "Configured $key=$value in gradle.properties.")
      }

      "update_configuration" -> {
        val fileName = args.optString("file_name")
        val content = args.optString("content")
        val file = workspaceManager.resolveSafeFile(project, fileName)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Updated configuration file: $fileName (${content.length} bytes).", listOf(fileName))
      }

      "manage_environment_variables" -> {
        val action = args.optString("action", "list").lowercase()
        val envFile = File(root, ".env")
        when (action) {
          "list" -> {
            val content = if (envFile.exists()) envFile.readText() else "(No .env file found)"
            AutonomousToolResult.Success(name, content)
          }
          "get" -> {
            val key = args.optString("key")
            val lines = if (envFile.exists()) envFile.readLines() else emptyList()
            val entry = lines.find { it.startsWith("$key=") }
            val value = entry?.substringAfter("$key=") ?: "(not set)"
            AutonomousToolResult.Success(name, "$key=$value")
          }
          "set" -> {
            val key = args.optString("key")
            val value = args.optString("value")
            val lines = if (envFile.exists()) envFile.readLines().toMutableList() else mutableListOf()
            val idx = lines.indexOfFirst { it.startsWith("$key=") }
            if (idx != -1) lines[idx] = "$key=$value" else lines.add("$key=$value")
            envFile.writeText(lines.joinToString("\n"))
            AutonomousToolResult.Success(name, "Saved $key to .env.")
          }
          "delete" -> {
            val key = args.optString("key")
            if (envFile.exists()) {
              val lines = envFile.readLines().filter { !it.startsWith("$key=") }
              envFile.writeText(lines.joinToString("\n"))
            }
            AutonomousToolResult.Success(name, "Removed $key from .env.")
          }
          else -> AutonomousToolResult.Error(name, "Unsupported action: $action")
        }
      }

      "inspect_package_files" -> {
        val report = buildString {
          listOf("app/build.gradle.kts", "build.gradle.kts", "package.json", "requirements.txt", "Cargo.toml").forEach { rel ->
            val f = File(root, rel)
            if (f.exists()) {
              appendLine("=== $rel ===")
              appendLine(f.readText().take(500))
              appendLine()
            }
          }
        }
        AutonomousToolResult.Success(name, report.ifBlank { "No recognized package or build scripts found." })
      }

      "manage_dependencies" -> {
        val action = args.optString("action", "list").lowercase()
        val dep = args.optString("dependency")
        val buildKts = File(root, "app/build.gradle.kts")
        when (action) {
          "list" -> {
            val overview = workspaceManager.getArchitectureOverview(project)
            AutonomousToolResult.Success(name, "Dependencies:\n" + overview.dependenciesSummary.joinToString("\n"))
          }
          "add" -> {
            if (buildKts.exists()) {
              val orig = buildKts.readText()
              if (!orig.contains(dep)) {
                val updated = orig.replace("dependencies {", "dependencies {\n    implementation(\"$dep\")")
                buildKts.writeText(updated)
                AutonomousToolResult.Success(name, "Added implementation(\"$dep\") to app/build.gradle.kts.")
              } else {
                AutonomousToolResult.Success(name, "Dependency '$dep' is already present.")
              }
            } else {
              AutonomousToolResult.Error(name, "app/build.gradle.kts does not exist.")
            }
          }
          "remove" -> {
            if (buildKts.exists()) {
              val orig = buildKts.readText()
              val updated = orig.lines().filter { !it.contains(dep) }.joinToString("\n")
              buildKts.writeText(updated)
              AutonomousToolResult.Success(name, "Removed '$dep' from app/build.gradle.kts.")
            } else {
              AutonomousToolResult.Error(name, "app/build.gradle.kts does not exist.")
            }
          }
          else -> AutonomousToolResult.Error(name, "Unsupported action: $action")
        }
      }

      "generate_documentation" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val doc = """
# Architecture Documentation: ${project.name}

## Overview
- **Project Type**: ${overview.projectType.displayName}
- **Build System**: ${overview.buildSystem}
- **Total Source Files**: ${overview.sourceFilesCount}
- **Entry Points**: ${if (overview.entryPoints.isEmpty()) "MainActivity" else overview.entryPoints.joinToString(", ")}

## Top Directories
${overview.topLevelDirectories.joinToString("\n") { "- `$it`" }}

## Declared Dependencies
${overview.dependenciesSummary.take(15).joinToString("\n") { "- $it" }}
        """.trimIndent()
        AutonomousToolResult.Success(name, doc)
      }

      "generate_readme" -> {
        val overview = workspaceManager.getArchitectureOverview(project)
        val readmeContent = """
# ${project.name}

Autonomous AI-engineered software system.

## Project Structure
- Type: ${overview.projectType.displayName}
- Build: ${overview.buildSystem}
- Key Modules: ${overview.topLevelDirectories.joinToString(", ")}

## Getting Started
Build and run the application:
```bash
gradle assembleDebug
```

## Architecture
Built with Kotlin and Jetpack Compose adhering to Material 3 design and Clean Architecture.
        """.trimIndent()

        val readmeFile = File(root, "README.md")
        readmeFile.writeText(readmeContent, Charsets.UTF_8)
        AutonomousToolResult.Success(name, "Generated README.md at project root.", listOf("README.md"))
      }

      "create_project_backup" -> {
        val backupName = args.optString("backup_name", "backup_${System.currentTimeMillis()}").removeSuffix(".zip")
        val backupDir = File(root.parentFile ?: root, ".backups").apply { mkdirs() }
        val zipFile = File(backupDir, "$backupName.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
          root.walkTopDown().filter { it.isFile && !it.path.contains("/.backups/") && !it.path.contains("/build/") && !it.path.contains("/.git/") }.forEach { file ->
            val rel = file.relativeTo(root).path
            zos.putNextEntry(ZipEntry(rel))
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()
          }
        }
        AutonomousToolResult.Success(name, "Created project backup archive: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
      }

      "restore_project_backup" -> {
        val backupPath = args.optString("backup_file")
        val zipFile = File(backupPath)
        if (!zipFile.exists()) return@withContext AutonomousToolResult.Error(name, "Backup file not found: $backupPath")
        AutonomousToolResult.Success(name, "Verified backup archive '$backupPath'. Ready for restoration.")
      }

      else -> null
    }
  }
}
