package com.example.ui.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-time Agent Activity Manager tracking tool operations, statuses,
 * execution duration, and resource metadata.
 */
public class AgentActivityManager {

  public enum Status {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
  }

  public static class ActivityItem {
    private final String id;
    private final String toolName;
    private final String operationName;
    private final String resourcePath;
    private Status status;
    private final long startTimeMillis;
    private long endTimeMillis;
    private String details;
    private boolean isExpanded;

    public ActivityItem(String id, String toolName, String operationName, String resourcePath) {
      this.id = id;
      this.toolName = toolName;
      this.operationName = operationName;
      this.resourcePath = resourcePath;
      this.status = Status.RUNNING;
      this.startTimeMillis = System.currentTimeMillis();
      this.endTimeMillis = 0;
      this.details = "";
      this.isExpanded = false;
    }

    public String getId() { return id; }
    public String getToolName() { return toolName; }
    public String getOperationName() { return operationName; }
    public String getResourcePath() { return resourcePath; }
    public Status getStatus() { return status; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public String getDetails() { return details; }
    public boolean isExpanded() { return isExpanded; }

    public void setStatus(Status status) {
      this.status = status;
      if (status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED) {
        this.endTimeMillis = System.currentTimeMillis();
      }
    }

    public void setDetails(String details) { this.details = details; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public long getDurationMillis() {
      if (endTimeMillis > 0) {
        return endTimeMillis - startTimeMillis;
      }
      return System.currentTimeMillis() - startTimeMillis;
    }

    public String getFormattedDuration() {
      long dur = getDurationMillis();
      if (dur < 1000) {
        return dur + "ms";
      }
      return String.format(java.util.Locale.US, "%.1fs", dur / 1000.0);
    }
  }

  public interface ActivityListener {
    void onActivityUpdated(List<ActivityItem> activities);
  }

  private static final AgentActivityManager INSTANCE = new AgentActivityManager();

  public static AgentActivityManager getInstance() {
    return INSTANCE;
  }

  private final List<ActivityItem> activities = new CopyOnWriteArrayList<>();
  private final List<ActivityListener> listeners = new CopyOnWriteArrayList<>();

  private AgentActivityManager() {}

  public void addListener(ActivityListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
      listener.onActivityUpdated(getActivities());
    }
  }

  public void removeListener(ActivityListener listener) {
    listeners.remove(listener);
  }

  public ActivityItem recordStart(String toolName, String operationName, String resourcePath) {
    String id = "act_" + System.currentTimeMillis() + "_" + activities.size();
    ActivityItem item = new ActivityItem(id, toolName, operationName, resourcePath);
    activities.add(0, item); // latest first
    notifyListeners();
    return item;
  }

  public void recordFinish(String id, boolean success, String details) {
    for (ActivityItem item : activities) {
      if (item.getId().equals(id)) {
        item.setStatus(success ? Status.COMPLETED : Status.FAILED);
        item.setDetails(details);
        break;
      }
    }
    notifyListeners();
  }

  public void toggleExpansion(String id) {
    for (ActivityItem item : activities) {
      if (item.getId().equals(id)) {
        item.setExpanded(!item.isExpanded());
        break;
      }
    }
    notifyListeners();
  }

  public void clear() {
    activities.clear();
    notifyListeners();
  }

  public List<ActivityItem> getActivities() {
    return Collections.unmodifiableList(new ArrayList<>(activities));
  }

  private void notifyListeners() {
    List<ActivityItem> copy = getActivities();
    for (ActivityListener l : listeners) {
      l.onActivityUpdated(copy);
    }
  }
}
