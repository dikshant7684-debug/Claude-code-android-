package com.example.context;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent Conversation Summarizer generating structured checkpoints:
 * objective, completed work, modified files, pending tasks, known issues, and next steps.
 */
public class ConversationSummarizer {

  public static class ConversationSummary {
    public final String projectId;
    public final String objective;
    public final List<String> completedTasks;
    public final List<String> modifiedFiles;
    public final List<String> pendingTasks;
    public final List<String> knownIssues;
    public final String nextAction;
    public final long timestamp;

    public ConversationSummary(
        String projectId,
        String objective,
        List<String> completedTasks,
        List<String> modifiedFiles,
        List<String> pendingTasks,
        List<String> knownIssues,
        String nextAction,
        long timestamp
    ) {
      this.projectId = projectId;
      this.objective = objective;
      this.completedTasks = completedTasks != null ? completedTasks : new ArrayList<>();
      this.modifiedFiles = modifiedFiles != null ? modifiedFiles : new ArrayList<>();
      this.pendingTasks = pendingTasks != null ? pendingTasks : new ArrayList<>();
      this.knownIssues = knownIssues != null ? knownIssues : new ArrayList<>();
      this.nextAction = nextAction != null ? nextAction : "";
      this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public JSONObject toJson() {
      JSONObject json = new JSONObject();
      try {
        json.put("project", projectId);
        json.put("objective", objective);
        json.put("completed", new JSONArray(completedTasks));
        json.put("modified_files", new JSONArray(modifiedFiles));
        json.put("pending", new JSONArray(pendingTasks));
        json.put("known_issues", new JSONArray(knownIssues));
        json.put("next_action", nextAction);
        json.put("timestamp", timestamp);
      } catch (Exception ignored) {}
      return json;
    }

    public static ConversationSummary fromJson(JSONObject json) {
      if (json == null) return null;
      String proj = json.optString("project", "");
      String obj = json.optString("objective", "");
      String next = json.optString("next_action", "");
      long ts = json.optLong("timestamp", System.currentTimeMillis());

      List<String> comp = jsonArrayToList(json.optJSONArray("completed"));
      List<String> mod = jsonArrayToList(json.optJSONArray("modified_files"));
      List<String> pend = jsonArrayToList(json.optJSONArray("pending"));
      List<String> issues = jsonArrayToList(json.optJSONArray("known_issues"));

      return new ConversationSummary(proj, obj, comp, mod, pend, issues, next, ts);
    }

    private static List<String> jsonArrayToList(JSONArray arr) {
      List<String> list = new ArrayList<>();
      if (arr != null) {
        for (int i = 0; i < arr.length(); i++) {
          list.add(arr.optString(i));
        }
      }
      return list;
    }

    public String toCompactMarkdown() {
      StringBuilder sb = new StringBuilder();
      sb.append("### CONVERSATION CHECKPOINT SUMMARY\n");
      if (!objective.isEmpty()) {
        sb.append("• Objective: ").append(objective).append("\n");
      }
      if (!completedTasks.isEmpty()) {
        sb.append("• Completed:\n");
        for (String c : completedTasks) {
          sb.append("  - ").append(c).append("\n");
        }
      }
      if (!modifiedFiles.isEmpty()) {
        sb.append("• Modified Files: ").append(String.join(", ", modifiedFiles)).append("\n");
      }
      if (!pendingTasks.isEmpty()) {
        sb.append("• Pending / In-Progress:\n");
        for (String p : pendingTasks) {
          sb.append("  - ").append(p).append("\n");
        }
      }
      if (!knownIssues.isEmpty()) {
        sb.append("• Known Issues:\n");
        for (String issue : knownIssues) {
          sb.append("  - ").append(issue).append("\n");
        }
      }
      if (!nextAction.isEmpty()) {
        sb.append("• Next Recommended Action: ").append(nextAction).append("\n");
      }
      return sb.toString();
    }
  }

  public static ConversationSummary buildSummary(
      String projectId,
      String userObjective,
      List<String> completedTasks,
      List<String> modifiedFiles,
      List<String> pendingTasks,
      List<String> knownIssues,
      String nextAction
  ) {
    return new ConversationSummary(
        projectId, userObjective, completedTasks, modifiedFiles,
        pendingTasks, knownIssues, nextAction, System.currentTimeMillis()
    );
  }
}
