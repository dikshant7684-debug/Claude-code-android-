package com.example.extensions.validator;

import com.example.extensions.models.AgentDefinition;
import com.example.extensions.models.SkillDefinition;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtensionValidator {

    public static class ValidationResult {
        public boolean isValid;
        public String detectedType; // "SKILL" or "AGENT"
        public String name;
        public String description;
        public String version = "1.0.0";
        public String author = "Community";
        public List<String> issues = new ArrayList<>();
        public SkillDefinition skillDefinition;
        public AgentDefinition agentDefinition;
    }

    /**
     * Inspects a directory to discover whether it contains a valid Skill (SKILL.md)
     * or Agent definition (agent.json / agent.yaml).
     */
    public ValidationResult validateDirectory(File directory) {
        ValidationResult result = new ValidationResult();
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            result.isValid = false;
            result.issues.add("Target is not a valid directory.");
            return result;
        }

        // 1. Check for SKILL.md
        File skillMdFile = findFileRecursive(directory, "SKILL.md", 3);
        if (skillMdFile != null && skillMdFile.isFile()) {
            return validateSkillFile(skillMdFile, directory);
        }

        // 2. Check for agent configuration (agent.json or agent_config.json)
        File agentJsonFile = findFileRecursive(directory, "agent.json", 3);
        if (agentJsonFile == null) {
            agentJsonFile = findFileRecursive(directory, "agent_config.json", 3);
        }
        if (agentJsonFile != null && agentJsonFile.isFile()) {
            return validateAgentFile(agentJsonFile, directory);
        }

        result.isValid = false;
        result.issues.add("No valid SKILL.md or agent.json found in repository or archive.");
        return result;
    }

    /**
     * Validates a SKILL.md file and extracts YAML frontmatter / instructions.
     */
    public ValidationResult validateSkillFile(File skillMdFile, File baseDir) {
        ValidationResult result = new ValidationResult();
        result.detectedType = "SKILL";

        try {
            String content = readFileToString(skillMdFile);
            if (content.trim().isEmpty()) {
                result.isValid = false;
                result.issues.add("SKILL.md is empty.");
                return result;
            }

            // Extract YAML frontmatter if present (between --- and ---)
            String name = null;
            String description = null;
            String category = null;
            String instructions = content;

            if (content.startsWith("---")) {
                int endIdx = content.indexOf("---", 3);
                if (endIdx != -1) {
                    String frontmatter = content.substring(3, endIdx);
                    instructions = content.substring(endIdx + 3).trim();
                    for (String line : frontmatter.split("\n")) {
                        String t = line.trim();
                        if (t.startsWith("name:")) {
                            name = t.substring(5).trim().replace("\"", "").replace("'", "");
                        } else if (t.startsWith("description:")) {
                            description = t.substring(12).trim().replace("\"", "").replace("'", "");
                        } else if (t.startsWith("category:")) {
                            category = t.substring(9).trim().replace("\"", "").replace("'", "");
                        } else if (t.startsWith("version:")) {
                            result.version = t.substring(8).trim().replace("\"", "").replace("'", "");
                        } else if (t.startsWith("author:")) {
                            result.author = t.substring(7).trim().replace("\"", "").replace("'", "");
                        }
                    }
                }
            }

            // If frontmatter missing, extract from markdown heading # Title
            if (name == null || name.isEmpty()) {
                for (String line : content.split("\n")) {
                    String t = line.trim();
                    if (t.startsWith("# ")) {
                        name = t.substring(2).trim();
                        break;
                    }
                }
            }

            if (name == null || name.isEmpty()) {
                name = skillMdFile.getParentFile() != null ? skillMdFile.getParentFile().getName() : "Custom Skill";
            }
            if (description == null || description.isEmpty()) {
                description = "Specialized AI skill module for " + name;
            }

            result.name = name;
            result.description = description;

            SkillDefinition def = new SkillDefinition(
                    name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", ""),
                    name,
                    description,
                    content
            );
            if (category != null) def.setCategory(category);
            def.setInstructions(instructions);
            def.setDirectoryPath(baseDir != null ? baseDir.getAbsolutePath() : skillMdFile.getParent());

            // Discover supporting directories: scripts/, references/, assets/
            File dir = skillMdFile.getParentFile();
            if (dir != null) {
                File scriptsDir = new File(dir, "scripts");
                if (scriptsDir.exists() && scriptsDir.isDirectory()) {
                    File[] sFiles = scriptsDir.listFiles();
                    if (sFiles != null) {
                        for (File f : sFiles) def.getScriptPaths().add(f.getAbsolutePath());
                    }
                }

                File refsDir = new File(dir, "references");
                if (refsDir.exists() && refsDir.isDirectory()) {
                    File[] rFiles = refsDir.listFiles();
                    if (rFiles != null) {
                        for (File f : rFiles) def.getReferencePaths().add(f.getAbsolutePath());
                    }
                }

                File assetsDir = new File(dir, "assets");
                if (assetsDir.exists() && assetsDir.isDirectory()) {
                    File[] aFiles = assetsDir.listFiles();
                    if (aFiles != null) {
                        for (File f : aFiles) def.getAssetPaths().add(f.getAbsolutePath());
                    }
                }
            }

            result.skillDefinition = def;
            result.isValid = true;
            return result;
        } catch (Exception e) {
            result.isValid = false;
            result.issues.add("Failed reading SKILL.md: " + e.getMessage());
            return result;
        }
    }

    /**
     * Validates an agent.json file.
     */
    public ValidationResult validateAgentFile(File agentJsonFile, File baseDir) {
        ValidationResult result = new ValidationResult();
        result.detectedType = "AGENT";

        try {
            String jsonText = readFileToString(agentJsonFile);
            JSONObject json = new JSONObject(jsonText);

            String name = json.optString("name", "");
            if (name.isEmpty()) {
                result.isValid = false;
                result.issues.add("agent.json missing required 'name' field.");
                return result;
            }

            String description = json.optString("description", "Autonomous specialized agent.");
            String instructions = json.optString("system_instructions", json.optString("instructions", ""));
            result.name = name;
            result.description = description;
            result.version = json.optString("version", "1.0.0");
            result.author = json.optString("author", "Community");

            AgentDefinition def = new AgentDefinition(
                    name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", ""),
                    name,
                    description,
                    instructions
            );
            def.setConfigurationJson(jsonText);
            def.setDirectoryPath(baseDir != null ? baseDir.getAbsolutePath() : agentJsonFile.getParent());
            def.setPreferredModelId(json.optString("preferred_model", ""));

            JSONArray caps = json.optJSONArray("capabilities");
            if (caps != null) {
                for (int i = 0; i < caps.length(); i++) def.getCapabilities().add(caps.getString(i));
            }

            JSONArray tools = json.optJSONArray("tools");
            if (tools != null) {
                for (int i = 0; i < tools.length(); i++) def.getAvailableTools().add(tools.getString(i));
            }

            JSONArray perms = json.optJSONArray("permissions");
            if (perms != null) {
                for (int i = 0; i < perms.length(); i++) def.getPermissionRequirements().add(perms.getString(i));
            }

            result.agentDefinition = def;
            result.isValid = true;
            return result;
        } catch (Exception e) {
            result.isValid = false;
            result.issues.add("Failed parsing agent.json: " + e.getMessage());
            return result;
        }
    }

    private File findFileRecursive(File dir, String filename, int maxDepth) {
        if (dir == null || !dir.exists() || maxDepth <= 0) return null;
        File exact = new File(dir, filename);
        if (exact.exists()) return exact;

        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    File found = findFileRecursive(f, filename, maxDepth - 1);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
