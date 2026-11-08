package com.example.agent.workspace

import android.content.Context
import com.example.data.files.DiffUtil
import com.example.data.files.ProjectFileManager
import com.example.data.model.FileChange
import com.example.data.model.Project
import com.example.data.model.ProjectFile
import com.example.data.model.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Type of file operation recorded in the manifest.
 */
enum class FileOperationType {
  CREATE,
  WRITE,
  PATCH,
  DELETE,
  RENAME,
  CREATE_DIRECTORY
}

/**
 * Manifest record of an actual file operation, supporting audits and safe rollback.
 */
data class FileOperationRecord(
  val id: String = java.util.UUID.randomUUID().toString(),
  val operation: FileOperationType,
  val relativePath: String,
  val previousPath: String? = null,
  val backupFilePath: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val description: String = ""
)

/**
 * Detailed project analysis for agent planning.
 */
data class ProjectArchitectureOverview(
  val rootName: String,
  val projectType: ProjectType,
  val buildSystem: String,
  val entryPoints: List<String>,
  val configFiles: List<String>,
  val sourceFilesCount: Int,
  val topLevelDirectories: List<String>,
  val dependenciesSummary: List<String>,
  val testCommands: List<String>
)

/**
 * WorkspaceManager and FileOperationService providing safe, validated workspace file operations,
 * path traversal protection, atomic writes with backups, and architectural project inspection.
 */
class WorkspaceManager(
  val context: Context,
  val fileManager: ProjectFileManager
) {

  // Per-file write mutexes to prevent concurrent operations from corrupting identical paths
  private val fileLocks = ConcurrentHashMap<String, Mutex>()

  // In-memory operation manifest for the active workspace session
  private val operationManifest = mutableListOf<FileOperationRecord>()

  private fun getLockForPath(canonicalPath: String): Mutex {
    return fileLocks.computeIfAbsent(canonicalPath) { Mutex() }
  }

  /**
   * Verifies that the path is strictly within the project root.
   * Throws SecurityException if path traversal is attempted.
   */
  fun resolveSafeFile(project: Project, relativePath: String): File {
    return fileManager.resolveSafeFile(project, relativePath)
  }

  suspend fun listFilesAndDirs(project: Project, relativeDir: String = ""): List<ProjectFile> =
    withContext(Dispatchers.IO) {
      fileManager.listFiles(project, relativeDir)
    }

  suspend fun readFileContent(
    project: Project,
    relativePath: String,
    maxLines: Int = 1200,
    offset: Int = 0
  ): String = withContext(Dispatchers.IO) {
    fileManager.readFile(project, relativePath, maxLines, offset)
  }

  suspend fun searchFiles(
    project: Project,
    query: String,
    isRegex: Boolean = false,
    maxResults: Int = 40
  ): List<com.example.data.files.SearchResult> = withContext(Dispatchers.IO) {
    fileManager.searchFiles(project, query, isRegex, maxResults)
  }

  suspend fun createDirectory(project: Project, relativeDir: String): File =
    withContext(Dispatchers.IO) {
      val target = resolveSafeFile(project, relativeDir)
      val lock = getLockForPath(target.canonicalPath)
      lock.withLock {
        target.mkdirs()
        recordOperation(
          FileOperationRecord(
            operation = FileOperationType.CREATE_DIRECTORY,
            relativePath = relativeDir,
            description = "Created directory $relativeDir"
          )
        )
        target
      }
    }

  suspend fun createNewFile(
    project: Project,
    relativePath: String,
    content: String = ""
  ): Pair<File, FileChange> = withContext(Dispatchers.IO) {
    val file = resolveSafeFile(project, relativePath)
    val lock = getLockForPath(file.canonicalPath)
    lock.withLock {
      if (file.exists()) {
        throw IOException("File already exists: $relativePath")
      }
      file.parentFile?.mkdirs()
      file.writeText(content, Charsets.UTF_8)

      val hunks = DiffUtil.computeDiff("", content)
      val change = FileChange(
        filePath = file.absolutePath,
        relativePath = relativePath,
        originalContent = "",
        proposedContent = content,
        diffHunks = hunks,
        isApproved = true,
        isApplied = true
      )

      recordOperation(
        FileOperationRecord(
          operation = FileOperationType.CREATE,
          relativePath = relativePath,
          description = "Created new file $relativePath"
        )
      )
      Pair(file, change)
    }
  }

  suspend fun writeCompleteFile(
    project: Project,
    relativePath: String,
    newContent: String,
    autoApprove: Boolean = true
  ): Pair<FileChange, Boolean> = withContext(Dispatchers.IO) {
    val file = resolveSafeFile(project, relativePath)
    val lock = getLockForPath(file.canonicalPath)
    lock.withLock {
      val origContent = if (file.exists()) file.readText(Charsets.UTF_8) else ""
      val hunks = DiffUtil.computeDiff(origContent, newContent)

      val change = FileChange(
        filePath = file.absolutePath,
        relativePath = relativePath,
        originalContent = origContent,
        proposedContent = newContent,
        diffHunks = hunks,
        isApproved = autoApprove,
        isApplied = autoApprove
      )

      if (autoApprove) {
        // Create backup for recovery
        val backupPath = createBackupFile(project, file)
        fileManager.writeFile(project, relativePath, newContent)

        recordOperation(
          FileOperationRecord(
            operation = FileOperationType.WRITE,
            relativePath = relativePath,
            backupFilePath = backupPath,
            description = "Overwrote file $relativePath (${hunks.size} hunks)"
          )
        )
        Pair(change, true)
      } else {
        Pair(change, false)
      }
    }
  }

  suspend fun applyTargetedPatch(
    project: Project,
    relativePath: String,
    targetContent: String,
    replacementContent: String,
    autoApprove: Boolean = true
  ): Pair<FileChange, Boolean> = withContext(Dispatchers.IO) {
    val file = resolveSafeFile(project, relativePath)
    val lock = getLockForPath(file.canonicalPath)
    lock.withLock {
      if (!file.exists()) {
        throw IOException("Cannot patch non-existent file: $relativePath")
      }
      val origContent = file.readText(Charsets.UTF_8)
      val patchResult = DiffUtil.applyBlockReplacement(origContent, targetContent, replacementContent)
      if (patchResult.isFailure) {
        throw IOException(
          "Patch failed on $relativePath: ${patchResult.exceptionOrNull()?.message ?: "Target content not found. Re-read file before patching."}"
        )
      }

      val newContent = patchResult.getOrThrow()
      val hunks = DiffUtil.computeDiff(origContent, newContent)
      val change = FileChange(
        filePath = file.absolutePath,
        relativePath = relativePath,
        originalContent = origContent,
        proposedContent = newContent,
        diffHunks = hunks,
        isApproved = autoApprove,
        isApplied = autoApprove
      )

      if (autoApprove) {
        val backupPath = createBackupFile(project, file)
        fileManager.writeFile(project, relativePath, newContent)

        recordOperation(
          FileOperationRecord(
            operation = FileOperationType.PATCH,
            relativePath = relativePath,
            backupFilePath = backupPath,
            description = "Applied patch to $relativePath"
          )
        )
        Pair(change, true)
      } else {
        Pair(change, false)
      }
    }
  }

  suspend fun renameOrMoveFile(
    project: Project,
    oldRelativePath: String,
    newRelativePath: String
  ): Boolean = withContext(Dispatchers.IO) {
    val oldFile = resolveSafeFile(project, oldRelativePath)
    val newFile = resolveSafeFile(project, newRelativePath)

    val lockOld = getLockForPath(oldFile.canonicalPath)
    val lockNew = getLockForPath(newFile.canonicalPath)

    lockOld.withLock {
      lockNew.withLock {
        val success = fileManager.renameFile(project, oldRelativePath, newRelativePath)
        if (success) {
          recordOperation(
            FileOperationRecord(
              operation = FileOperationType.RENAME,
              relativePath = newRelativePath,
              previousPath = oldRelativePath,
              description = "Moved $oldRelativePath -> $newRelativePath"
            )
          )
        }
        success
      }
    }
  }

  suspend fun deleteFileOrDir(project: Project, relativePath: String): Boolean =
    withContext(Dispatchers.IO) {
      val file = resolveSafeFile(project, relativePath)
      val lock = getLockForPath(file.canonicalPath)
      lock.withLock {
        if (!file.exists()) return@withContext false
        val backupPath = if (file.isFile) createBackupFile(project, file) else null
        val success = fileManager.deleteFile(project, relativePath)
        if (success) {
          recordOperation(
            FileOperationRecord(
              operation = FileOperationType.DELETE,
              relativePath = relativePath,
              backupFilePath = backupPath,
              description = "Deleted $relativePath"
            )
          )
        }
        success
      }
    }

  /**
   * Inspects and returns a deep architectural overview of the current workspace project.
   */
  suspend fun inspectProjectArchitecture(project: Project): ProjectArchitectureOverview =
    withContext(Dispatchers.IO) {
      val root = File(project.rootPath)
      val allFiles = root.walkTopDown()
        .filter { !it.name.startsWith(".") && !it.path.contains("/build/") && it.name != "build" }
        .toList()

      val entryPoints = mutableListOf<String>()
      val configFiles = mutableListOf<String>()
      val dependencies = mutableListOf<String>()
      val testCommands = mutableListOf<String>()

      var buildSystem = when (project.type) {
        ProjectType.KOTLIN_ANDROID -> "Gradle (Kotlin DSL)"
        ProjectType.PYTHON -> "Python setuptools/pip"
        ProjectType.NODE_JS -> "npm / package.json"
        ProjectType.RUST -> "Cargo"
        ProjectType.GO -> "Go Modules"
        ProjectType.SHELL -> "Bash / Shell"
        else -> "Custom / Generic"
      }

      val topLevelDirs = root.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }
        ?.map { it.name } ?: emptyList()

      allFiles.forEach { file ->
        val name = file.name
        val rel = file.relativeTo(root).path
        when {
          name == "build.gradle.kts" || name == "build.gradle" -> {
            configFiles.add(rel)
            buildSystem = "Gradle"
            testCommands.add("gradle :app:testDebugUnitTest")
          }
          name == "settings.gradle.kts" || name == "settings.gradle" -> configFiles.add(rel)
          name == "package.json" -> {
            configFiles.add(rel)
            buildSystem = "Node.js (npm)"
            testCommands.add("npm test")
          }
          name == "requirements.txt" || name == "pyproject.toml" -> {
            configFiles.add(rel)
            testCommands.add("pytest")
          }
          name == "AndroidManifest.xml" -> entryPoints.add(rel)
          name == "MainActivity.kt" || name == "MainActivity.java" -> entryPoints.add(rel)
          name == "main.py" || name == "app.py" -> entryPoints.add(rel)
          name == "index.ts" || name == "index.js" -> entryPoints.add(rel)
        }
      }

      ProjectArchitectureOverview(
        rootName = project.name,
        projectType = project.type,
        buildSystem = buildSystem,
        entryPoints = entryPoints.distinct(),
        configFiles = configFiles.distinct(),
        sourceFilesCount = allFiles.count { it.isFile },
        topLevelDirectories = topLevelDirs,
        dependenciesSummary = dependencies,
        testCommands = testCommands.distinct()
      )
    }

  /**
   * Retrieves an overview string suitable for system prompts and initial inspection.
   */
  suspend fun getProjectSummaryText(project: Project): String = withContext(Dispatchers.IO) {
    val arch = inspectProjectArchitecture(project)
    buildString {
      appendLine("PROJECT ARCHITECTURE OVERVIEW:")
      appendLine("- Project Name: ${arch.rootName} (${arch.projectType.displayName})")
      appendLine("- Build System: ${arch.buildSystem}")
      appendLine("- Source Files Count: ${arch.sourceFilesCount}")
      if (arch.entryPoints.isNotEmpty()) {
        appendLine("- Entry Points: ${arch.entryPoints.joinToString(", ")}")
      }
      if (arch.configFiles.isNotEmpty()) {
        appendLine("- Configuration Files: ${arch.configFiles.joinToString(", ")}")
      }
      if (arch.topLevelDirectories.isNotEmpty()) {
        appendLine("- Top Directories: ${arch.topLevelDirectories.joinToString(", ")}")
      }
      if (arch.testCommands.isNotEmpty()) {
        appendLine("- Verification / Test Commands: ${arch.testCommands.joinToString("; ")}")
      }
    }
  }

  fun getOperationManifest(): List<FileOperationRecord> = synchronized(operationManifest) {
    operationManifest.toList()
  }

  fun getOperationHistory(): List<FileOperationRecord> = getOperationManifest()

  suspend fun getArchitectureOverview(project: Project): ProjectArchitectureOverview =
    inspectProjectArchitecture(project)

  private fun recordOperation(record: FileOperationRecord) {
    synchronized(operationManifest) {
      operationManifest.add(record)
    }
  }

  private fun createBackupFile(project: Project, sourceFile: File): String? {
    return try {
      if (!sourceFile.exists()) return null
      val backupDir = File(File(project.rootPath), ".droidcode_backup").apply { mkdirs() }
      val backup = File(backupDir, "${System.currentTimeMillis()}_${sourceFile.name}.bak")
      sourceFile.copyTo(backup, overwrite = true)
      backup.absolutePath
    } catch (_: Exception) {
      null
    }
  }
}
