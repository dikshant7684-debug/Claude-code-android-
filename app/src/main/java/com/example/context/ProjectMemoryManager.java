package com.example.context;

import android.content.Context;
import com.example.data.database.AppDatabase;
import com.example.data.database.ProjectMemoryEntity;
import com.example.data.database.ProjectMemoryRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

/**
 * Project Memory Manager organizing project knowledge into structured, searchable categories:
 * - PROJECT_STRUCTURE
 * - ARCHITECTURE
 * - CODING_CONVENTIONS
 * - PROJECT_DECISIONS
 * - KNOWN_ISSUES
 * - PENDING_TASKS
 */
public class ProjectMemoryManager {

  public static final String CAT_STRUCTURE = "PROJECT_STRUCTURE";
  public static final String CAT_ARCHITECTURE = "ARCHITECTURE";
  public static final String CAT_CONVENTIONS = "CODING_CONVENTIONS";
  public static final String CAT_DECISIONS = "PROJECT_DECISIONS";
  public static final String CAT_ISSUES = "KNOWN_ISSUES";
  public static final String CAT_PENDING = "PENDING_TASKS";

  public static class MemoryRecord {
    public final String id;
    public final String projectId;
    public final String category;
    public final String key;
    public final String content;
    public final long timestamp;
    public final float importance;
    public final String tags;

    public MemoryRecord(String id, String projectId, String category, String key, String content, long timestamp, float importance, String tags) {
      this.id = id;
      this.projectId = projectId;
      this.category = category;
      this.key = key;
      this.content = content;
      this.timestamp = timestamp;
      this.importance = importance;
      this.tags = tags;
    }
  }

  private static volatile ProjectMemoryManager INSTANCE;
  private final ProjectMemoryRepository repository;

  public static ProjectMemoryManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (ProjectMemoryManager.class) {
        if (INSTANCE == null) {
          AppDatabase db = AppDatabase.Companion.getInstance(context.getApplicationContext());
          ProjectMemoryRepository repo = new ProjectMemoryRepository(db.projectMemoryDao());
          INSTANCE = new ProjectMemoryManager(repo);
        }
      }
    }
    return INSTANCE;
  }

  public ProjectMemoryManager(ProjectMemoryRepository repository) {
    this.repository = repository;
  }

  public static ProjectMemoryManager getInstanceWithRepo(ProjectMemoryRepository repo) {
    if (INSTANCE == null) {
      synchronized (ProjectMemoryManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new ProjectMemoryManager(repo);
        }
      }
    }
    return INSTANCE;
  }

  public void recordDecision(String projectId, String key, String decision, String tags) {
    recordMemory(projectId, CAT_DECISIONS, key, decision, 0.8f, tags);
  }

  public void recordArchitecture(String projectId, String component, String description) {
    recordMemory(projectId, CAT_ARCHITECTURE, component, description, 0.9f, "arch,system");
  }

  public void recordConvention(String projectId, String topic, String conventionRule) {
    recordMemory(projectId, CAT_CONVENTIONS, topic, conventionRule, 0.7f, "conventions,style");
  }

  public void recordStructure(String projectId, String moduleName, String layoutSummary) {
    recordMemory(projectId, CAT_STRUCTURE, moduleName, layoutSummary, 0.6f, "structure,modules");
  }

  public void recordPendingTask(String projectId, String taskKey, String details) {
    recordMemory(projectId, CAT_PENDING, taskKey, details, 0.85f, "todo,pending");
  }

  public void recordMemory(String projectId, String category, String key, String content, float importance, String tags) {
    if (projectId == null || key == null || content == null) return;
    String cleanContent = redactSecrets(content);

    try {
      BuildersKt.runBlocking(Dispatchers.getIO(), (scope, continuation) -> {
        return repository.recordMemory(projectId, category, key, cleanContent, importance, tags, continuation);
      });
    } catch (Exception ignored) {}
  }

  public List<MemoryRecord> getRelevantMemories(String projectId, String query, int limit) {
    if (projectId == null) return Collections.emptyList();
    try {
      List<ProjectMemoryEntity> entities = BuildersKt.runBlocking(Dispatchers.getIO(), (scope, continuation) -> {
        return repository.getRelevantMemories(projectId, query != null ? query : "", limit, continuation);
      });

      List<MemoryRecord> records = new ArrayList<>();
      for (ProjectMemoryEntity e : entities) {
        records.add(new MemoryRecord(
            e.getId(), e.getProjectId(), e.getCategory(), e.getKey(),
            e.getContent(), e.getTimestamp(), e.getImportance(), e.getTags()
        ));
      }
      return records;
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  public List<MemoryRecord> getAllProjectMemories(String projectId) {
    if (projectId == null) return Collections.emptyList();
    try {
      List<ProjectMemoryEntity> entities = BuildersKt.runBlocking(Dispatchers.getIO(), (scope, continuation) -> {
        return repository.getMemories(projectId, continuation);
      });

      List<MemoryRecord> records = new ArrayList<>();
      for (ProjectMemoryEntity e : entities) {
        records.add(new MemoryRecord(
            e.getId(), e.getProjectId(), e.getCategory(), e.getKey(),
            e.getContent(), e.getTimestamp(), e.getImportance(), e.getTags()
        ));
      }
      return records;
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  public void clearProjectMemories(String projectId) {
    if (projectId == null) return;
    try {
      BuildersKt.runBlocking(Dispatchers.getIO(), (scope, continuation) -> {
        return repository.clearMemoriesForProject(projectId, continuation);
      });
    } catch (Exception ignored) {}
  }

  public String formatMemoriesForContext(String projectId, String query, int maxTokens) {
    List<MemoryRecord> memories = getRelevantMemories(projectId, query, 8);
    if (memories.isEmpty()) return "";

    StringBuilder sb = new StringBuilder();
    sb.append("### RELEVANT PROJECT MEMORY & DECISIONS\n");
    int runningTokens = 15;

    for (MemoryRecord m : memories) {
      String block = String.format("[%s] %s: %s\n", m.category, m.key, m.content);
      int cost = ContextWindowMonitor.estimateTokens(block);
      if (runningTokens + cost > maxTokens) break;
      sb.append(block);
      runningTokens += cost;
    }

    return sb.toString();
  }

  private static final Pattern SECRET_PATTERN_1 = Pattern.compile("(?i)(?:api[_-]?key|secret|token|password|bearer)\\s*[:=]\\s*['\"]?([a-zA-Z0-9_\\-]{16,})['\"]?");
  private static final Pattern SECRET_PATTERN_2 = Pattern.compile("sk-[a-zA-Z0-9]{20,}");
  private static final Pattern SECRET_PATTERN_3 = Pattern.compile("AIza[0-9A-Za-z-_]{35}");

  public static String redactSecrets(String input) {
    if (input == null) return "";
    String result = SECRET_PATTERN_1.matcher(input).replaceAll("[REDACTED_SECRET]");
    result = SECRET_PATTERN_2.matcher(result).replaceAll("[REDACTED_KEY]");
    result = SECRET_PATTERN_3.matcher(result).replaceAll("[REDACTED_API_KEY]");
    return result;
  }
}
