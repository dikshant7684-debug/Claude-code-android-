package com.example.context;

import android.content.Context;
import java.util.List;

/**
 * Cross-Session Memory Manager ensuring persistent project awareness across
 * independent conversations and application launches without starting from zero.
 */
public class CrossSessionMemoryManager {

  private static volatile CrossSessionMemoryManager INSTANCE;
  private final ProjectMemoryManager memoryManager;

  public static CrossSessionMemoryManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (CrossSessionMemoryManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new CrossSessionMemoryManager(ProjectMemoryManager.getInstance(context));
        }
      }
    }
    return INSTANCE;
  }

  public CrossSessionMemoryManager(ProjectMemoryManager memoryManager) {
    this.memoryManager = memoryManager;
  }

  /**
   * Retrieves cross-session briefing for the AI agent when opening a project or starting a new conversation.
   * Summarizes known architecture, conventions, and past technical decisions.
   */
  public String buildCrossSessionBriefing(String projectId) {
    if (projectId == null || projectId.isEmpty()) return "";

    List<ProjectMemoryManager.MemoryRecord> allMemories = memoryManager.getAllProjectMemories(projectId);
    if (allMemories.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("### CROSS-SESSION PROJECT KNOWLEDGE (PERSISTENT MEMORY)\n");
    sb.append("You have previously worked on this project. Here are the verified architectural facts and decisions:\n\n");

    boolean hasArch = false;
    for (ProjectMemoryManager.MemoryRecord m : allMemories) {
      if (m.category.equals(ProjectMemoryManager.CAT_ARCHITECTURE)) {
        if (!hasArch) {
          sb.append("• Architecture & Components:\n");
          hasArch = true;
        }
        sb.append("  - ").append(m.key).append(": ").append(m.content).append("\n");
      }
    }

    boolean hasDecisions = false;
    for (ProjectMemoryManager.MemoryRecord m : allMemories) {
      if (m.category.equals(ProjectMemoryManager.CAT_DECISIONS)) {
        if (!hasDecisions) {
          sb.append("• Past Decisions & Features Implemented:\n");
          hasDecisions = true;
        }
        sb.append("  - ").append(m.key).append(": ").append(m.content).append("\n");
      }
    }

    boolean hasConventions = false;
    for (ProjectMemoryManager.MemoryRecord m : allMemories) {
      if (m.category.equals(ProjectMemoryManager.CAT_CONVENTIONS)) {
        if (!hasConventions) {
          sb.append("• Coding Conventions & Patterns:\n");
          hasConventions = true;
        }
        sb.append("  - ").append(m.key).append(": ").append(m.content).append("\n");
      }
    }

    return sb.toString();
  }
}
