package com.example.extensions.models;

import java.io.Serializable;

public class InstallationHistoryEntry implements Serializable {
    private long id;
    private String extensionId;
    private String extensionName;
    private String action; // INSTALL, UPDATE, UNINSTALL, ENABLE, DISABLE
    private String status; // SUCCESS, FAILED, ROLLBACK
    private String version;
    private String details;
    private long timestamp;

    public InstallationHistoryEntry() {
        this.timestamp = System.currentTimeMillis();
    }

    public InstallationHistoryEntry(String extensionId, String extensionName, String action, String status, String version, String details) {
        this();
        this.extensionId = extensionId;
        this.extensionName = extensionName;
        this.action = action;
        this.status = status;
        this.version = version;
        this.details = details;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getExtensionId() { return extensionId; }
    public void setExtensionId(String extensionId) { this.extensionId = extensionId; }

    public String getExtensionName() { return extensionName; }
    public void setExtensionName(String extensionName) { this.extensionName = extensionName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
