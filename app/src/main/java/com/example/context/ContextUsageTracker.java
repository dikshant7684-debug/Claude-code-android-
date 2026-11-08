package com.example.context;

import java.util.Locale;

/**
 * Context Usage Tracker providing structured token breakdowns by category,
 * percentage allocation, and estimation indicators.
 */
public class ContextUsageTracker {

  public static class CategoryUsage {
    public final String categoryName;
    public final int tokenCount;
    public final float percentage;

    public CategoryUsage(String categoryName, int tokenCount, float percentage) {
      this.categoryName = categoryName;
      this.tokenCount = tokenCount;
      this.percentage = percentage;
    }
  }

  public static class ContextReport {
    public final String modelId;
    public final int totalCapacity;
    public final int totalTokensUsed;
    public final int remainingCapacity;
    public final float percentUsed;
    public final boolean isEstimated;

    public final int systemTokens;
    public final int instructionTokens;
    public final int memoryTokens;
    public final int historyTokens;
    public final int fileTokens;
    public final int toolTokens;
    public final int currentRequestTokens;
    public final int reservedOutputTokens;

    public ContextReport(
        String modelId,
        int totalCapacity,
        int systemTokens,
        int instructionTokens,
        int memoryTokens,
        int historyTokens,
        int fileTokens,
        int toolTokens,
        int currentRequestTokens,
        int reservedOutputTokens,
        boolean isEstimated
    ) {
      this.modelId = modelId;
      this.totalCapacity = totalCapacity;
      this.systemTokens = systemTokens;
      this.instructionTokens = instructionTokens;
      this.memoryTokens = memoryTokens;
      this.historyTokens = historyTokens;
      this.fileTokens = fileTokens;
      this.toolTokens = toolTokens;
      this.currentRequestTokens = currentRequestTokens;
      this.reservedOutputTokens = reservedOutputTokens;
      this.isEstimated = isEstimated;

      this.totalTokensUsed = systemTokens + instructionTokens + memoryTokens +
          historyTokens + fileTokens + toolTokens + currentRequestTokens;
      this.remainingCapacity = Math.max(0, totalCapacity - totalTokensUsed - reservedOutputTokens);
      this.percentUsed = totalCapacity > 0 ? ((float) totalTokensUsed / totalCapacity) * 100f : 0f;
    }

    public String formatSummary() {
      StringBuilder sb = new StringBuilder();
      sb.append("Context Usage (").append(isEstimated ? "Estimated" : "Measured").append("):\n");
      sb.append(String.format(Locale.US, "  Total: %d / %d tokens (%.1f%%)\n", totalTokensUsed, totalCapacity, percentUsed));
      if (totalTokensUsed > 0) {
        sb.append(String.format(Locale.US, "  • Project Instructions: %d (%.1f%%)\n", instructionTokens, (float) instructionTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • Project Memory:       %d (%.1f%%)\n", memoryTokens, (float) memoryTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • Conversation History: %d (%.1f%%)\n", historyTokens, (float) historyTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • Retrieved Files:      %d (%.1f%%)\n", fileTokens, (float) fileTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • Tool Results:         %d (%.1f%%)\n", toolTokens, (float) toolTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • Current Request:      %d (%.1f%%)\n", currentRequestTokens, (float) currentRequestTokens / totalTokensUsed * 100));
        sb.append(String.format(Locale.US, "  • System Prompt:        %d (%.1f%%)\n", systemTokens, (float) systemTokens / totalTokensUsed * 100));
      }
      sb.append(String.format(Locale.US, "  • Reserved Output:      %d tokens\n", reservedOutputTokens));
      sb.append(String.format(Locale.US, "  • Remaining Capacity:   %d tokens", remainingCapacity));
      return sb.toString();
    }
  }

  private static final ContextUsageTracker INSTANCE = new ContextUsageTracker();

  public static ContextUsageTracker getInstance() {
    return INSTANCE;
  }

  private ContextReport latestReport = null;

  private ContextUsageTracker() {}

  public synchronized void updateReport(ContextReport report) {
    this.latestReport = report;
  }

  public synchronized ContextReport getLatestReport() {
    return latestReport;
  }
}
