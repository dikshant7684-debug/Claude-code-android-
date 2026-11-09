package com.example.extensions.models;

import java.io.Serializable;

public class InstallationEvent implements Serializable {
    private String extensionId;
    private String extensionName;
    private InstallationStatus status;
    private int progressPercentage;
    private String currentOperation;
    private int filesProcessed;
    private int totalFiles;
    private String message;
    private String errorMessage;
    private long timestamp;

    public InstallationEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public InstallationEvent(String extensionId, String extensionName, InstallationStatus status, String currentOperation) {
        this();
        this.extensionId = extensionId;
        this.extensionName = extensionName;
        this.status = status;
        this.currentOperation = currentOperation;
        this.message = currentOperation;
    }

    public String getExtensionId() { return extensionId; }
    public void setExtensionId(String extensionId) { this.extensionId = extensionId; }

    public String getExtensionName() { return extensionName; }
    public void setExtensionName(String extensionName) { this.extensionName = extensionName; }

    public InstallationStatus getStatus() { return status; }
    public void setStatus(InstallationStatus status) { this.status = status; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getCurrentOperation() { return currentOperation; }
    public void setCurrentOperation(String currentOperation) { this.currentOperation = currentOperation; }

    public int getFilesProcessed() { return filesProcessed; }
    public void setFilesProcessed(int filesProcessed) { this.filesProcessed = filesProcessed; }

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
