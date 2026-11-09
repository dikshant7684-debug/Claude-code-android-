package com.example.extensions.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExtensionItem implements Serializable {
    private String id;
    private String name;
    private String description;
    private ExtensionType type;
    private String version;
    private String author;
    private String sourceUrl;
    private String installDirectory;
    private boolean enabled;
    private String validationStatus;
    private List<String> dependencies;
    private List<String> permissions;
    private String checksumSha256;
    private long installedTimestamp;
    private long lastUpdatedTimestamp;
    private boolean updateAvailable;
    private String latestVersion;

    public ExtensionItem() {
        this.dependencies = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.enabled = true;
        this.validationStatus = "PASSED";
        this.installedTimestamp = System.currentTimeMillis();
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }

    public ExtensionItem(String id, String name, String description, ExtensionType type,
                         String version, String author, String sourceUrl, String installDirectory) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.version = version;
        this.author = author;
        this.sourceUrl = sourceUrl;
        this.installDirectory = installDirectory;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ExtensionType getType() { return type; }
    public void setType(ExtensionType type) { this.type = type; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getInstallDirectory() { return installDirectory; }
    public void setInstallDirectory(String installDirectory) { this.installDirectory = installDirectory; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies != null ? dependencies : new ArrayList<>(); }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions != null ? permissions : new ArrayList<>(); }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public long getInstalledTimestamp() { return installedTimestamp; }
    public void setInstalledTimestamp(long installedTimestamp) { this.installedTimestamp = installedTimestamp; }

    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public void setUpdateAvailable(boolean updateAvailable) { this.updateAvailable = updateAvailable; }

    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
}
