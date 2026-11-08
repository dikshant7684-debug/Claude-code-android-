package com.example.context;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized Context & Memory Management Engine.
 * Provides unified APIs for project awareness, cross-session memory,
 * CLAUDE.md discovery, context compaction, and session recovery.
 */
public class ContextManager {

  private static volatile ContextManager INSTANCE;

  private final Context context;
  private final ProjectMemoryManager memoryManager;
  private final PersistentInstructionManager instructionManager;
  private final CrossSessionMemoryManager crossSessionManager;
  private final SessionRecoveryManager recoveryManager;
  private final ContextUsageTracker usageTracker;

  public static ContextManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (ContextManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new ContextManager(context.getApplicationContext());
        }
      }
    }
    return INSTANCE;
  }

  private ContextManager(Context context) {
    this.context = context;
    this.memoryManager = ProjectMemoryManager.getInstance(context);
    this.instructionManager = PersistentInstructionManager.getInstance();
    this.crossSessionManager = CrossSessionMemoryManager.getInstance(context);
    this.recoveryManager = SessionRecoveryManager.getInstance(context);
    this.usageTracker = ContextUsageTracker.getInstance();
  }

  public ProjectMemoryManager getMemoryManager() { return memoryManager; }
  public PersistentInstructionManager getInstructionManager() { return instructionManager; }
  public CrossSessionMemoryManager getCrossSessionManager() { return crossSessionManager; }
  public SessionRecoveryManager getRecoveryManager() { return recoveryManager; }
  public ContextUsageTracker getUsageTracker() { return usageTracker; }

  /**
   * Initializes or updates project context when the user opens a project.
   */
  public ProjectContextLoader.ProjectProfile initializeProjectContext(String projectId, File projectRoot) {
    // 1. Discover instructions (CLAUDE.md, AGENTS.md)
    instructionManager.discoverInstructions(projectId, projectRoot);

    // 2. Discover architecture profile
    ProjectContextLoader.ProjectProfile profile = ProjectContextLoader.loadProjectProfile(projectRoot);

    // 3. Record structure and architecture in persistent memory if not already present
    memoryManager.recordStructure(projectId, "Root", profile.toContextSummary());
    memoryManager.recordArchitecture(projectId, "BuildSystem", profile.buildSystem);

    return profile;
  }

  /**
   * Assembles the optimized, model-specific context for an upcoming AI agent prompt.
   */
  public ContextAssembler.AssembledContext assembleContext(
      String baseSystemPrompt,
      String projectId,
      String targetRelativePath,
      String userPrompt,
      List<ContextCompactionEngine.ContextMessage> history,
      String activeFileContent,
      String modelId
  ) {
    return ContextAssembler.assemble(
        baseSystemPrompt,
        projectId,
        targetRelativePath,
        userPrompt,
        history,
        activeFileContent,
        modelId,
        memoryManager,
        instructionManager
    );
  }

  /**
   * Records a user or agent technical decision into persistent cross-session memory.
   */
  public void recordDecision(String projectId, String key, String decision, String tags) {
    memoryManager.recordDecision(projectId, key, decision, tags);
  }

  /**
   * Manually compacts conversation history for a given project.
   */
  public ContextCompactionEngine.CompactionResult manualCompact(
      String projectId,
      List<ContextCompactionEngine.ContextMessage> history,
      int maxAllowedTokens
  ) {
    return ContextCompactionEngine.compactHistory(projectId, history, maxAllowedTokens);
  }
}
