package com.example

import com.example.context.ContextCompactionEngine
import com.example.context.ContextRelevanceEngine
import com.example.context.ContextUsageTracker
import com.example.context.ContextWindowMonitor
import com.example.context.ConversationSummarizer
import com.example.context.MemoryStorageManager
import com.example.context.PersistentInstructionManager
import com.example.context.ProjectContextLoader
import com.example.context.ProjectMemoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContextMemorySystemTest {

  @Test
  fun testTokenEstimationAndModelLimits() {
    val claudeLimit = ContextWindowMonitor.getModelLimit("claude-3-7-sonnet-20250219")
    assertEquals(200_000, claudeLimit.contextWindowTokens)

    val geminiLimit = ContextWindowMonitor.getModelLimit("gemini-2.0-flash")
    assertEquals(1_048_576, geminiLimit.contextWindowTokens)

    val sampleText = "Hello world, this is a test of context estimation."
    val tokens = ContextWindowMonitor.estimateTokens(sampleText)
    assertTrue("Tokens should be estimated > 0", tokens > 5)

    val normalState = ContextWindowMonitor.evaluateThreshold(50_000, 200_000)
    assertEquals(ContextWindowMonitor.CompactionThresholdState.NORMAL, normalState)

    val compactState = ContextWindowMonitor.evaluateThreshold(175_000, 200_000)
    assertEquals(ContextWindowMonitor.CompactionThresholdState.COMPACT_REQUIRED, compactState)
  }

  @Test
  fun testSecretRedaction() {
    val textWithApiKey = "Please use apiKey: 'AIzaSyA123456789012345678901234567' for Gemini"
    val redacted = ProjectMemoryManager.redactSecrets(textWithApiKey)
    assertFalse("API key should be redacted", redacted.contains("AIzaSyA"))
    assertTrue("Should indicate redacted secret", redacted.contains("[REDACTED_SECRET]") || redacted.contains("[REDACTED_API_KEY]"))
  }

  @Test
  fun testConversationSummarizerJsonAndMarkdown() {
    val summary = ConversationSummarizer.buildSummary(
      "TestProject",
      "Implement background process recovery",
      listOf("Created process manager", "Implemented stdout streaming"),
      listOf("CommandManager.java"),
      listOf("Add session restoration"),
      listOf("Process timeout error"),
      "Test process restart"
    )

    val json = summary.toJson()
    val restored = ConversationSummarizer.ConversationSummary.fromJson(json)
    assertNotNull(restored)
    assertEquals("TestProject", restored.projectId)
    assertEquals(2, restored.completedTasks.size)
    assertEquals(1, restored.modifiedFiles.size)

    val md = summary.toCompactMarkdown()
    assertTrue(md.contains("Objective: Implement background process recovery"))
    assertTrue(md.contains("CommandManager.java"))
  }

  @Test
  fun testContextCompactionEngine() {
    val history = mutableListOf<ContextCompactionEngine.ContextMessage>()
    for (i in 1..10) {
      history.add(
        ContextCompactionEngine.ContextMessage(
          if (i % 2 == 1) "user" else "assistant",
          "This is message $i containing code and decisions about file app/src/main/MainActivity.java and completed step $i",
          System.currentTimeMillis()
        )
      )
    }

    // Force compaction by setting budget smaller than total tokens
    val result = ContextCompactionEngine.compactHistory("TestProj", history, 50)
    assertTrue("Should have performed compaction", result.wasCompacted)
    assertTrue("Compacted tokens should be lower or equal to original", result.compactedTokens < result.originalTokens)
    assertTrue("Should preserve recent messages", result.compactedMessages.size >= 4)
  }

  @Test
  fun testContextRelevanceRanking() {
    val memories = listOf(
      ProjectMemoryManager.MemoryRecord(
        "1", "p1", ProjectMemoryManager.CAT_ARCHITECTURE, "DatabaseSchema",
        "Uses Room database for ChatMessageEntity and ProjectMemoryEntity", System.currentTimeMillis(), 0.9f, "db,room"
      ),
      ProjectMemoryManager.MemoryRecord(
        "2", "p1", ProjectMemoryManager.CAT_CONVENTIONS, "NamingConvention",
        "Use PascalCase for classes and camelCase for functions", System.currentTimeMillis(), 0.7f, "style,conventions"
      ),
      ProjectMemoryManager.MemoryRecord(
        "3", "p1", ProjectMemoryManager.CAT_DECISIONS, "McpTransport",
        "Implemented StreamableHttpTransport for remote MCP servers", System.currentTimeMillis(), 0.8f, "mcp,network"
      )
    )

    val ranked = ContextRelevanceEngine.rankMemories(memories, "Room database queries", null, 2)
    assertEquals(2, ranked.size)
    assertEquals("DatabaseSchema", ranked[0].key)
  }

  @Test
  fun testInstructionResolution() {
    val tempDir = File.createTempFile("test_proj_", "")
    tempDir.delete()
    tempDir.mkdirs()

    val rootClaude = File(tempDir, "CLAUDE.md")
    rootClaude.writeText("Root Instructions: Use Java for Android logic")

    val subDir = File(tempDir, "app/src/main")
    subDir.mkdirs()
    val subClaude = File(subDir, "CLAUDE.md")
    subClaude.writeText("Subdirectory Instructions: Follow repository pattern")

    val manager = PersistentInstructionManager.getInstance()
    val discovered = manager.discoverInstructions("test_id", tempDir)
    assertEquals(2, discovered.size)

    val resolvedRoot = manager.resolveInstructionsForPath("test_id", "README.md")
    assertEquals(1, resolvedRoot.size)
    assertEquals("CLAUDE.md", resolvedRoot[0].relativePath)

    val resolvedSub = manager.resolveInstructionsForPath("test_id", "app/src/main/MainActivity.java")
    assertEquals(2, resolvedSub.size)

    val formatted = manager.formatApplicableInstructions("test_id", "app/src/main/MainActivity.java")
    assertTrue(formatted.contains("Root Instructions"))
    assertTrue(formatted.contains("Subdirectory Instructions"))

    tempDir.deleteRecursively()
  }
}
