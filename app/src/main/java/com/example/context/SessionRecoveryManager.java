package com.example.context;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Session Recovery Manager saving active conversation state, modified files,
 * checkpoints, and pending tasks to survive app process death, crashes, and restarts.
 */
public class SessionRecoveryManager {

  public static class RecoveredSession {
    public final String conversationId;
    public final String projectId;
    public final String modelId;
    public final String lastSummaryJson;
    public final List<String> modifiedFiles;
    public final List<String> pendingTasks;
    public final String lastExecutionState;
    public final long savedTimestamp;

    public RecoveredSession(
        String conversationId,
        String projectId,
        String modelId,
        String lastSummaryJson,
        List<String> modifiedFiles,
        List<String> pendingTasks,
        String lastExecutionState,
        long savedTimestamp
    ) {
      this.conversationId = conversationId;
      this.projectId = projectId;
      this.modelId = modelId;
      this.lastSummaryJson = lastSummaryJson;
      this.modifiedFiles = modifiedFiles != null ? modifiedFiles : new ArrayList<>();
      this.pendingTasks = pendingTasks != null ? pendingTasks : new ArrayList<>();
      this.lastExecutionState = lastExecutionState != null ? lastExecutionState : "IDLE";
      this.savedTimestamp = savedTimestamp;
    }
  }

  private static volatile SessionRecoveryManager INSTANCE;
  private final Context context;

  public static SessionRecoveryManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (SessionRecoveryManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new SessionRecoveryManager(context.getApplicationContext());
        }
      }
    }
    return INSTANCE;
  }

  private SessionRecoveryManager(Context context) {
    this.context = context;
  }

  private File getSessionFile(String projectId) {
    File dir = new File(context.getFilesDir(), "neucode_sessions");
    if (!dir.exists()) dir.mkdirs();
    return new File(dir, "session_" + (projectId != null ? projectId.replace('/', '_') : "default") + ".json");
  }

  public synchronized void saveSession(
      String conversationId,
      String projectId,
      String modelId,
      String summaryJson,
      List<String> modifiedFiles,
      List<String> pendingTasks,
      String executionState
  ) {
    try {
      JSONObject json = new JSONObject();
      json.put("conversationId", conversationId);
      json.put("projectId", projectId);
      json.put("modelId", modelId);
      json.put("summary", summaryJson != null ? summaryJson : "");
      json.put("modifiedFiles", new JSONArray(modifiedFiles != null ? modifiedFiles : new ArrayList<>()));
      json.put("pendingTasks", new JSONArray(pendingTasks != null ? pendingTasks : new ArrayList<>()));
      json.put("executionState", executionState != null ? executionState : "IDLE");
      json.put("timestamp", System.currentTimeMillis());

      File file = getSessionFile(projectId);
      try (FileOutputStream fos = new FileOutputStream(file)) {
        fos.write(json.toString().getBytes(StandardCharsets.UTF_8));
      }
    } catch (Exception ignored) {}
  }

  public synchronized RecoveredSession loadSession(String projectId) {
    File file = getSessionFile(projectId);
    if (!file.exists()) return null;

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] data = new byte[(int) file.length()];
      int read = fis.read(data);
      if (read <= 0) return null;

      JSONObject json = new JSONObject(new String(data, 0, read, StandardCharsets.UTF_8));
      String convId = json.optString("conversationId", "");
      String projId = json.optString("projectId", "");
      String modelId = json.optString("modelId", "");
      String summary = json.optString("summary", "");
      String state = json.optString("executionState", "IDLE");
      long ts = json.optLong("timestamp", 0);

      List<String> mod = new ArrayList<>();
      JSONArray modArr = json.optJSONArray("modifiedFiles");
      if (modArr != null) {
        for (int i = 0; i < modArr.length(); i++) mod.add(modArr.optString(i));
      }

      List<String> pend = new ArrayList<>();
      JSONArray pendArr = json.optJSONArray("pendingTasks");
      if (pendArr != null) {
        for (int i = 0; i < pendArr.length(); i++) pend.add(pendArr.optString(i));
      }

      return new RecoveredSession(convId, projId, modelId, summary, mod, pend, state, ts);
    } catch (Exception e) {
      return null;
    }
  }

  public synchronized void clearSession(String projectId) {
    File file = getSessionFile(projectId);
    if (file.exists()) {
      file.delete();
    }
  }
}
