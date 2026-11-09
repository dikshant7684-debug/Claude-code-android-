package com.example.extensions.models;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillDefinition implements Serializable {
    private String id;
    private String name;
    private String description;
    private String skillMdContent;
    private String instructions;
    private String category;
    private List<String> scriptPaths;
    private List<String> referencePaths;
    private List<String> assetPaths;
    private Map<String, String> configEntries;
    private String directoryPath;

    public SkillDefinition() {
        this.scriptPaths = new ArrayList<>();
        this.referencePaths = new ArrayList<>();
        this.assetPaths = new ArrayList<>();
        this.configEntries = new HashMap<>();
        this.category = "General";
    }

    public SkillDefinition(String id, String name, String description, String skillMdContent) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.skillMdContent = skillMdContent;
        this.instructions = skillMdContent;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSkillMdContent() { return skillMdContent; }
    public void setSkillMdContent(String skillMdContent) { this.skillMdContent = skillMdContent; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getScriptPaths() { return scriptPaths; }
    public void setScriptPaths(List<String> scriptPaths) { this.scriptPaths = scriptPaths; }

    public List<String> getReferencePaths() { return referencePaths; }
    public void setReferencePaths(List<String> referencePaths) { this.referencePaths = referencePaths; }

    public List<String> getAssetPaths() { return assetPaths; }
    public void setAssetPaths(List<String> assetPaths) { this.assetPaths = assetPaths; }

    public Map<String, String> getConfigEntries() { return configEntries; }
    public void setConfigEntries(Map<String, String> configEntries) { this.configEntries = configEntries; }

    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }
}
