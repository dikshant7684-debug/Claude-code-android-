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
 * Memory Storage Manager managing versioning, portable JSON export/import,
 * and corruption recovery for project memories.
 */
public class MemoryStorageManager {

  private static final int CURRENT_SCHEMA_VERSION = 1;
  private final Context context;

  public MemoryStorageManager(Context context) {
    this.context = context.getApplicationContext();
  }

  public String exportMemoriesToJson(String projectId, List<ProjectMemoryManager.MemoryRecord> records) {
    try {
      JSONObject root = new JSONObject();
      root.put("version", CURRENT_SCHEMA_VERSION);
      root.put("projectId", projectId);
      root.put("exportTimestamp", System.currentTimeMillis());

      JSONArray items = new JSONArray();
      for (ProjectMemoryManager.MemoryRecord r : records) {
        JSONObject obj = new JSONObject();
        obj.put("id", r.id);
        obj.put("category", r.category);
        obj.put("key", r.key);
        obj.put("content", r.content);
        obj.put("importance", r.importance);
        obj.put("tags", r.tags);
        items.put(obj);
      }
      root.put("memories", items);
      return root.toString(2);
    } catch (Exception e) {
      return "{}";
    }
  }

  public List<ProjectMemoryManager.MemoryRecord> importMemoriesFromJson(String projectId, String jsonString) {
    List<ProjectMemoryManager.MemoryRecord> imported = new ArrayList<>();
    if (jsonString == null || jsonString.trim().isEmpty()) return imported;

    try {
      JSONObject root = new JSONObject(jsonString);
      JSONArray items = root.optJSONArray("memories");
      if (items == null) return imported;

      for (int i = 0; i < items.length(); i++) {
        JSONObject obj = items.getJSONObject(i);
        imported.add(new ProjectMemoryManager.MemoryRecord(
            obj.optString("id", java.util.UUID.randomUUID().toString()),
            projectId,
            obj.optString("category", ProjectMemoryManager.CAT_ARCHITECTURE),
            obj.optString("key", "imported_item"),
            obj.optString("content", ""),
            System.currentTimeMillis(),
            (float) obj.optDouble("importance", 0.5),
            obj.optString("tags", "imported")
        ));
      }
    } catch (Exception ignored) {}
    return imported;
  }
}
