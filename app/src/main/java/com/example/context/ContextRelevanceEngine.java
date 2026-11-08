package com.example.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Context Relevance Engine scoring and ranking project memories and context items
 * against current user directives, active files, and recency.
 */
public class ContextRelevanceEngine {

  public static class ScoredMemory {
    public final ProjectMemoryManager.MemoryRecord record;
    public final float score;

    public ScoredMemory(ProjectMemoryManager.MemoryRecord record, float score) {
      this.record = record;
      this.score = score;
    }
  }

  public static List<ProjectMemoryManager.MemoryRecord> rankMemories(
      List<ProjectMemoryManager.MemoryRecord> allMemories,
      String query,
      String activeFilePath,
      int maxResults
  ) {
    if (allMemories == null || allMemories.isEmpty()) {
      return Collections.emptyList();
    }

    Set<String> queryTokens = extractTokens(query);
    List<ScoredMemory> scoredList = new ArrayList<>();

    for (ProjectMemoryManager.MemoryRecord m : allMemories) {
      float score = m.importance; // Base score

      // Match key
      String keyLower = m.key.toLowerCase();
      for (String t : queryTokens) {
        if (keyLower.contains(t)) score += 2.5f;
      }

      // Match tags
      String tagsLower = m.tags.toLowerCase();
      for (String t : queryTokens) {
        if (tagsLower.contains(t)) score += 1.8f;
      }

      // Match content
      String contentLower = m.content.toLowerCase();
      for (String t : queryTokens) {
        if (contentLower.contains(t)) score += 1.0f;
      }

      // Boost if memory relates to currently active file
      if (activeFilePath != null && !activeFilePath.isEmpty()) {
        String activeFileLower = activeFilePath.toLowerCase();
        if (contentLower.contains(activeFileLower) || keyLower.contains(activeFileLower)) {
          score += 3.0f;
        }
      }

      // Recency boost (within 24 hours)
      long ageMillis = System.currentTimeMillis() - m.timestamp;
      if (ageMillis < 86_400_000L) {
        score += 0.5f;
      }

      scoredList.add(new ScoredMemory(m, score));
    }

    Collections.sort(scoredList, (a, b) -> Float.compare(b.score, a.score));

    List<ProjectMemoryManager.MemoryRecord> result = new ArrayList<>();
    for (int i = 0; i < Math.min(maxResults, scoredList.size()); i++) {
      result.add(scoredList.get(i).record);
    }

    return result;
  }

  private static Set<String> extractTokens(String input) {
    if (input == null || input.trim().isEmpty()) {
      return Collections.emptySet();
    }
    String[] parts = input.toLowerCase().split("[^a-zA-Z0-9_]+");
    Set<String> set = new HashSet<>();
    for (String p : parts) {
      if (p.length() > 2) {
        set.add(p);
      }
    }
    return set;
  }
}
