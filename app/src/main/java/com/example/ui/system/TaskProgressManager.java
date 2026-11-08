package com.example.ui.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live Task Progress Manager tracking active tasks, operation checklists,
 * and real execution duration.
 */
public class TaskProgressManager {

  public enum OpState {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED
  }

  public static class TaskOp {
    private final String id;
    private final String title;
    private OpState state;

    public TaskOp(String id, String title, OpState state) {
      this.id = id;
      this.title = title;
      this.state = state;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public OpState getState() { return state; }
    public void setState(OpState state) { this.state = state; }
  }

  public static class TaskProgress {
    private final String taskName;
    private final List<TaskOp> operations = new ArrayList<>();
    private final long startTimeMillis;
    private long endTimeMillis;
    private boolean isFinished;

    public TaskProgress(String taskName) {
      this.taskName = taskName;
      this.startTimeMillis = System.currentTimeMillis();
      this.endTimeMillis = 0;
      this.isFinished = false;
    }

    public String getTaskName() { return taskName; }
    public List<TaskOp> getOperations() { return operations; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public boolean isFinished() { return isFinished; }

    public void addOperation(String id, String title, OpState state) {
      operations.add(new TaskOp(id, title, state));
    }

    public void updateOperation(String id, OpState state) {
      for (TaskOp op : operations) {
        if (op.getId().equals(id)) {
          op.setState(state);
          break;
        }
      }
    }

    public void finish() {
      this.isFinished = true;
      this.endTimeMillis = System.currentTimeMillis();
    }

    public long getElapsedTimeMillis() {
      if (endTimeMillis > 0) return endTimeMillis - startTimeMillis;
      return System.currentTimeMillis() - startTimeMillis;
    }

    public String getFormattedElapsed() {
      long sec = getElapsedTimeMillis() / 1000;
      long min = sec / 60;
      sec = sec % 60;
      if (min > 0) {
        return String.format(java.util.Locale.US, "%dm %02ds", min, sec);
      }
      return sec + "s";
    }
  }

  public interface TaskProgressListener {
    void onProgressChanged(TaskProgress progress);
  }

  private static final TaskProgressManager INSTANCE = new TaskProgressManager();

  public static TaskProgressManager getInstance() {
    return INSTANCE;
  }

  private TaskProgress currentTask = null;
  private final List<TaskProgressListener> listeners = new CopyOnWriteArrayList<>();

  private TaskProgressManager() {}

  public void addListener(TaskProgressListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
      listener.onProgressChanged(currentTask);
    }
  }

  public void removeListener(TaskProgressListener listener) {
    listeners.remove(listener);
  }

  public synchronized void startTask(String taskName, List<String> operationTitles) {
    currentTask = new TaskProgress(taskName);
    if (operationTitles != null) {
      for (int i = 0; i < operationTitles.size(); i++) {
        OpState state = (i == 0) ? OpState.ACTIVE : OpState.PENDING;
        currentTask.addOperation("op_" + i, operationTitles.get(i), state);
      }
    }
    notifyListeners();
  }

  public synchronized void setOperationActive(String opId) {
    if (currentTask != null) {
      currentTask.updateOperation(opId, OpState.ACTIVE);
      notifyListeners();
    }
  }

  public synchronized void setOperationCompleted(String opId) {
    if (currentTask != null) {
      currentTask.updateOperation(opId, OpState.COMPLETED);
      notifyListeners();
    }
  }

  public synchronized void setOperationFailed(String opId) {
    if (currentTask != null) {
      currentTask.updateOperation(opId, OpState.FAILED);
      notifyListeners();
    }
  }

  public synchronized void finishTask() {
    if (currentTask != null) {
      currentTask.finish();
      notifyListeners();
    }
  }

  public synchronized void clear() {
    currentTask = null;
    notifyListeners();
  }

  public synchronized TaskProgress getCurrentTask() {
    return currentTask;
  }

  private void notifyListeners() {
    for (TaskProgressListener l : listeners) {
      l.onProgressChanged(currentTask);
    }
  }
}
