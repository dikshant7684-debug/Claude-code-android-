package com.example.ui.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic Working-File Indicator Manager.
 * Tracks files accessed or modified by the AI agent during tasks.
 */
public class WorkingFileManager {

  public enum FileStatus {
    UNCHANGED,
    READING,
    MODIFIED,
    CREATED,
    DELETED
  }

  public static class ActiveFile {
    private final String relativePath;
    private final String fileName;
    private final String extension;
    private String currentOperation;
    private FileStatus status;
    private long lastAccessedTime;

    public ActiveFile(String relativePath, String operation, FileStatus status) {
      this.relativePath = relativePath;
      this.fileName = extractFileName(relativePath);
      this.extension = extractExtension(this.fileName);
      this.currentOperation = operation;
      this.status = status;
      this.lastAccessedTime = System.currentTimeMillis();
    }

    private static String extractFileName(String path) {
      if (path == null || path.isEmpty()) return "unknown";
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < path.length() - 1) {
        return path.substring(lastSlash + 1);
      }
      return path;
    }

    private static String extractExtension(String name) {
      if (name == null) return "";
      int lastDot = name.lastIndexOf('.');
      if (lastDot >= 0 && lastDot < name.length() - 1) {
        return name.substring(lastDot + 1).toLowerCase();
      }
      return "";
    }

    public String getRelativePath() { return relativePath; }
    public String getFileName() { return fileName; }
    public String getExtension() { return extension; }
    public String getCurrentOperation() { return currentOperation; }
    public FileStatus getStatus() { return status; }
    public long getLastAccessedTime() { return lastAccessedTime; }

    public void update(String operation, FileStatus status) {
      this.currentOperation = operation;
      this.status = status;
      this.lastAccessedTime = System.currentTimeMillis();
    }
  }

  public interface WorkingFileListener {
    void onFilesChanged(List<ActiveFile> files);
  }

  public interface FileSelectionCallback {
    void onFileSelected(ActiveFile file);
  }

  private static final WorkingFileManager INSTANCE = new WorkingFileManager();

  public static WorkingFileManager getInstance() {
    return INSTANCE;
  }

  private final Map<String, ActiveFile> fileMap = Collections.synchronizedMap(new LinkedHashMap<>());
  private final List<WorkingFileListener> listeners = new CopyOnWriteArrayList<>();
  private FileSelectionCallback selectionCallback;

  private WorkingFileManager() {}

  public void setFileSelectionCallback(FileSelectionCallback callback) {
    this.selectionCallback = callback;
  }

  public void selectFile(ActiveFile file) {
    if (selectionCallback != null && file != null) {
      selectionCallback.onFileSelected(file);
    }
  }

  public void addListener(WorkingFileListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
      listener.onFilesChanged(getActiveFiles());
    }
  }

  public void removeListener(WorkingFileListener listener) {
    listeners.remove(listener);
  }

  public void reportFileAccess(String relativePath, String operation, FileStatus status) {
    if (relativePath == null || relativePath.trim().isEmpty()) return;
    ActiveFile file = fileMap.get(relativePath);
    if (file == null) {
      file = new ActiveFile(relativePath, operation, status);
      fileMap.put(relativePath, file);
    } else {
      file.update(operation, status);
    }
    notifyListeners();
  }

  public void clear() {
    fileMap.clear();
    notifyListeners();
  }

  public List<ActiveFile> getActiveFiles() {
    synchronized (fileMap) {
      List<ActiveFile> list = new ArrayList<>(fileMap.values());
      // Sort by latest accessed
      Collections.sort(list, (a, b) -> Long.compare(b.getLastAccessedTime(), a.getLastAccessedTime()));
      return list;
    }
  }

  private void notifyListeners() {
    List<ActiveFile> files = getActiveFiles();
    for (WorkingFileListener l : listeners) {
      l.onFilesChanged(files);
    }
  }
}
