package com.example.context;

import java.util.ArrayList;
import java.util.List;

/**
 * Context Assembler constructing model-specific context prompts within capacity limits,
 * calculating token budgets, and logging usage breakdowns to ContextUsageTracker.
 */
public class ContextAssembler {

  public static class AssembledContext {
    public final String systemPromptWithContext;
    public final List<ContextCompactionEngine.ContextMessage> messages;
    public final ContextUsageTracker.ContextReport usageReport;

    public AssembledContext(
        String systemPromptWithContext,
        List<ContextCompactionEngine.ContextMessage> messages,
        ContextUsageTracker.ContextReport usageReport
    ) {
      this.systemPromptWithContext = systemPromptWithContext;
      this.messages = messages;
      this.usageReport = usageReport;
    }
  }

  public static AssembledContext assemble(
      String baseSystemPrompt,
      String projectId,
      String targetRelativePath,
      String userPrompt,
      List<ContextCompactionEngine.ContextMessage> rawHistory,
      String activeFileContent,
      String modelId,
      ProjectMemoryManager memoryManager,
      PersistentInstructionManager instructionManager
  ) {
    ContextWindowMonitor.ModelLimit limit = ContextWindowMonitor.getModelLimit(modelId);
    int totalCapacity = limit.contextWindowTokens;
    int reservedOutput = limit.maxOutputTokens;

    // Available input budget
    int availableInputBudget = Math.max(4_000, totalCapacity - reservedOutput - 1_000);

    // 1. Scoped Instructions (CLAUDE.md)
    String instructions = instructionManager != null
        ? instructionManager.formatApplicableInstructions(projectId, targetRelativePath)
        : "";
    int instructionTokens = ContextWindowMonitor.estimateTokens(instructions);

    // 2. Project Memory & Decisions (budget: up to 15% of available input)
    int maxMemoryTokens = (int) (availableInputBudget * 0.15f);
    String memoryContext = memoryManager != null
        ? memoryManager.formatMemoriesForContext(projectId, userPrompt, maxMemoryTokens)
        : "";
    int memoryTokens = ContextWindowMonitor.estimateTokens(memoryContext);

    // 3. Current Request
    int requestTokens = ContextWindowMonitor.estimateTokens(userPrompt);

    // 4. Active File Content (budget: up to 25% of available input)
    String fileBlock = "";
    int fileTokens = 0;
    if (activeFileContent != null && !activeFileContent.isEmpty()) {
      int maxFileTokens = (int) (availableInputBudget * 0.25f);
      int estimated = ContextWindowMonitor.estimateTokens(activeFileContent);
      if (estimated > maxFileTokens) {
        fileBlock = "\n### CURRENT FILE CONTENT (Truncated to fit budget)\n" +
            activeFileContent.substring(0, Math.min(activeFileContent.length(), maxFileTokens * 4));
      } else {
        fileBlock = "\n### CURRENT FILE CONTENT\n" + activeFileContent;
      }
      fileTokens = ContextWindowMonitor.estimateTokens(fileBlock);
    }

    // 5. System Prompt + Assembled Context Head
    StringBuilder sbSystem = new StringBuilder();
    sbSystem.append(baseSystemPrompt != null ? baseSystemPrompt : "").append("\n\n");
    if (!instructions.isEmpty()) {
      sbSystem.append(instructions).append("\n\n");
    }
    if (!memoryContext.isEmpty()) {
      sbSystem.append(memoryContext).append("\n\n");
    }
    if (!fileBlock.isEmpty()) {
      sbSystem.append(fileBlock).append("\n\n");
    }

    String finalSystemPrompt = sbSystem.toString().trim();
    int systemTokens = ContextWindowMonitor.estimateTokens(baseSystemPrompt);

    // 6. History Compaction: Remaining budget allocated to history
    int overhead = systemTokens + instructionTokens + memoryTokens + fileTokens + requestTokens;
    int maxHistoryBudget = Math.max(2_000, availableInputBudget - overhead);

    ContextCompactionEngine.CompactionResult compaction = ContextCompactionEngine.compactHistory(
        projectId,
        rawHistory,
        maxHistoryBudget
    );

    List<ContextCompactionEngine.ContextMessage> finalMessages = new ArrayList<>(compaction.compactedMessages);
    finalMessages.add(new ContextCompactionEngine.ContextMessage("user", userPrompt, System.currentTimeMillis()));

    int historyTokens = compaction.compactedTokens;

    // 7. Generate & Track Context Report
    ContextUsageTracker.ContextReport report = new ContextUsageTracker.ContextReport(
        modelId,
        totalCapacity,
        systemTokens,
        instructionTokens,
        memoryTokens,
        historyTokens,
        fileTokens,
        0, // Tool tokens
        requestTokens,
        reservedOutput,
        true // Estimated token counts
    );

    ContextUsageTracker.getInstance().updateReport(report);

    return new AssembledContext(finalSystemPrompt, finalMessages, report);
  }
}
