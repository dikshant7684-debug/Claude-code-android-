package com.example.extensions.models;

import java.io.Serializable;

public class ExtensionSource implements Serializable {
    private String id;
    private String name;
    private String url;
    private String type; // GITHUB, CUSTOM_INDEX, LOCAL
    private boolean enabled;
    private boolean trusted;
    private long lastSyncTimestamp;

    public ExtensionSource() {
        this.enabled = true;
        this.trusted = false;
        this.lastSyncTimestamp = System.currentTimeMillis();
    }

    public ExtensionSource(String id, String name, String url, String type, boolean trusted) {
        this();
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
        this.trusted = trusted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTrusted() { return trusted; }
    public void setTrusted(boolean trusted) { this.trusted = trusted; }

    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(long lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }
}
