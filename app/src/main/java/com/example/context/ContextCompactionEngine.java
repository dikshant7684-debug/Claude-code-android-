package com.example.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Context Compaction Engine implementing intelligent, non-destructive compaction.
 * Preserves completed work, pending tasks, file paths, and instructions while
 * replacing repetitive intermediate dialogue with structured summaries.
 */
public class ContextCompactionEngine {

  public static class CompactionResult {
    public final boolean wasCompacted;
    public final String summaryContext;
    public final List<ContextMessage> compactedMessages;
    public final int originalTokens;
    public final int compactedTokens;
    public final float savingsPercent;

    public CompactionResult(
        boolean wasCompacted,
        String summaryContext,
        List<ContextMessage> compactedMessages,
        int originalTokens,
        int compactedTokens
    ) {
      this.wasCompacted = wasCompacted;
      this.summaryContext = summaryContext;
      this.compactedMessages = compactedMessages;
      this.originalTokens = originalTokens;
      this.compactedTokens = compactedTokens;
      this.savingsPercent = originalTokens > 0
          ? ((float) (originalTokens - compactedTokens) / originalTokens) * 100f
          : 0f;
    }
  }

  public static class ContextMessage {
    public final String role;
    public final String content;
    public final long timestamp;

    public ContextMessage(String role, String content, long timestamp) {
      this.role = role;
      this.content = content;
      this.timestamp = timestamp;
    }
  }

  /**
   * Compacts conversation messages when token usage exceeds configured thresholds.
   * Keeps the latest 4-6 messages intact while summarizing older dialogue.
   */
  public static CompactionResult compactHistory(
      String projectId,
      List<ContextMessage> history,
      int maxAllowedHistoryTokens
  ) {
    if (history == null || history.size() <= 4) {
      int tokens = calculateTokens(history);
      return new CompactionResult(false, "", history != null ? history : Collections.emptyList(), tokens, tokens);
    }

    int originalTokens = calculateTokens(history);
    if (originalTokens <= maxAllowedHistoryTokens) {
      return new CompactionResult(false, "", history, originalTokens, originalTokens);
    }

    // Keep the most recent 4 messages intact
    int splitIndex = Math.max(1, history.size() - 4);
    List<ContextMessage> olderMessages = history.subList(0, splitIndex);
    List<ContextMessage> recentMessages = new ArrayList<>(history.subList(splitIndex, history.size()));

    // Extract key facts from older messages
    List<String> completedTasks = new ArrayList<>();
    List<String> modifiedFiles = new ArrayList<>();
    List<String> pendingTasks = new ArrayList<>();
    List<String> knownIssues = new ArrayList<>();
    String latestObjective = "";

    for (ContextMessage msg : olderMessages) {
      String text = msg.content;
      if (msg.role.equalsIgnoreCase("user")) {
        latestObjective = text.length() > 120 ? text.substring(0, 120) + "..." : text;
      } else {
        // Extract file paths
        extractFilePaths(text, modifiedFiles);

        // Extract errors / issues
        if (text.contains("error") || text.contains("Exception") || text.contains("FAILED")) {
          String snippet = extractSnippet(text, "error");
          if (snippet.length() > 10 && !knownIssues.contains(snippet)) {
            knownIssues.add(snippet);
          }
        }

        // Extract completed tasks
        if (text.contains("completed") || text.contains("Created") || text.contains("Implemented")) {
          String snippet = extractSnippet(text, "completed");
          if (snippet.length() > 10 && !completedTasks.contains(snippet)) {
            completedTasks.add(snippet);
          }
        }
      }
    }

    ConversationSummarizer.ConversationSummary summary = ConversationSummarizer.buildSummary(
        projectId,
        latestObjective.isEmpty() ? "Development in workspace" : latestObjective,
        completedTasks,
        modifiedFiles,
        pendingTasks,
        knownIssues,
        "Continue current technical directive"
    );

    String summaryMarkdown = summary.toCompactMarkdown();
    ContextMessage summaryMessage = new ContextMessage("system", summaryMarkdown, System.currentTimeMillis());

    List<ContextMessage> finalCompacted = new ArrayList<>();
    finalCompacted.add(summaryMessage);
    finalCompacted.addAll(recentMessages);

    int compactedTokens = calculateTokens(finalCompacted);

    return new CompactionResult(true, summaryMarkdown, finalCompacted, originalTokens, compactedTokens);
  }

  private static void extractFilePaths(String text, List<String> list) {
    String[] words = text.split("[\\s,;()'\"]+");
    for (String w : words) {
      if ((w.contains("/") || w.endsWith(".kt") || w.endsWith(".java") || w.endsWith(".xml") || w.endsWith(".gradle") || w.endsWith(".json"))
          && !w.startsWith("http") && w.length() > 4) {
        if (!list.contains(w) && list.size() < 12) {
          list.add(w);
        }
      }
    }
  }

  private static String extractSnippet(String text, String keyword) {
    int idx = text.toLowerCase().indexOf(keyword.toLowerCase());
    if (idx < 0) return "";
    int start = Math.max(0, idx - 20);
    int end = Math.min(text.length(), idx + 100);
    String snippet = text.substring(start, end).replace('\n', ' ').trim();
    return snippet.length() > 80 ? snippet.substring(0, 80) + "..." : snippet;
  }

  private static int calculateTokens(List<ContextMessage> messages) {
    if (messages == null) return 0;
    int count = 0;
    for (ContextMessage m : messages) {
      count += ContextWindowMonitor.estimateTokens(m.content) + 4;
    }
    return count;
  }
}
