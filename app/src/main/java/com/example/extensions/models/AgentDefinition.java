package com.example.extensions.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentDefinition implements Serializable {
    private String id;
    private String name;
    private String description;
    private String systemInstructions;
    private List<String> capabilities;
    private List<String> availableTools;
    private String preferredModelId;
    private Map<String, Object> executionSettings;
    private List<String> permissionRequirements;
    private String configurationJson;
    private String directoryPath;

    public AgentDefinition() {
        this.capabilities = new ArrayList<>();
        this.availableTools = new ArrayList<>();
        this.permissionRequirements = new ArrayList<>();
        this.executionSettings = new HashMap<>();
    }

    public AgentDefinition(String id, String name, String description, String systemInstructions) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.systemInstructions = systemInstructions;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSystemInstructions() { return systemInstructions; }
    public void setSystemInstructions(String systemInstructions) { this.systemInstructions = systemInstructions; }

    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    public List<String> getAvailableTools() { return availableTools; }
    public void setAvailableTools(List<String> availableTools) { this.availableTools = availableTools; }

    public String getPreferredModelId() { return preferredModelId; }
    public void setPreferredModelId(String preferredModelId) { this.preferredModelId = preferredModelId; }

    public Map<String, Object> getExecutionSettings() { return executionSettings; }
    public void setExecutionSettings(Map<String, Object> executionSettings) { this.executionSettings = executionSettings; }

    public List<String> getPermissionRequirements() { return permissionRequirements; }
    public void setPermissionRequirements(List<String> permissionRequirements) { this.permissionRequirements = permissionRequirements; }

    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }

    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }
}
