package com.example.extensions.discovery;

import com.example.extensions.models.SkillDefinition;
import com.example.extensions.registry.ExtensionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutonomousSkillDiscoveryEngine {
    private final ExtensionRegistry registry;

    public AutonomousSkillDiscoveryEngine(ExtensionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Matches user prompt against active skills and returns the list of applicable skills.
     */
    public List<SkillDefinition> discoverRelevantSkills(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) return Collections.emptyList();
        String lower = userPrompt.toLowerCase();
        List<SkillDefinition> active = registry.getActiveSkills();
        List<SkillDefinition> matched = new ArrayList<>();

        for (SkillDefinition skill : active) {
            String nameLower = skill.getName().toLowerCase();
            String descLower = skill.getDescription() != null ? skill.getDescription().toLowerCase() : "";
            String categoryLower = skill.getCategory() != null ? skill.getCategory().toLowerCase() : "";

            boolean hit = false;
            // Name match
            if (lower.contains(nameLower) || nameLower.contains(lower)) hit = true;

            // Keyword extraction
            for (String token : nameLower.split("[\\s_\\-]+")) {
                if (token.length() >= 3 && lower.contains(token)) {
                    hit = true;
                    break;
                }
            }

            // Description or category match
            if (!hit && descLower.contains("android") && (lower.contains("android") || lower.contains("compose") || lower.contains("kotlin"))) hit = true;
            if (!hit && descLower.contains("python") && (lower.contains("python") || lower.contains(".py") || lower.contains("flask") || lower.contains("django"))) hit = true;
            if (!hit && descLower.contains("web") && (lower.contains("web") || lower.contains("html") || lower.contains("css") || lower.contains("javascript") || lower.contains("react"))) hit = true;
            if (!hit && descLower.contains("test") && (lower.contains("test") || lower.contains("unit test") || lower.contains("robolectric"))) hit = true;
            if (!hit && descLower.contains("debug") && (lower.contains("debug") || lower.contains("fix bug") || lower.contains("error"))) hit = true;

            if (hit) {
                matched.add(skill);
            }
        }

        return matched;
    }

    /**
     * Formats discovered skills' instructions into an injection block for the AI system prompt.
     */
    public static String buildSkillContextPrompt(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ACTIVATED AI SKILLS (Autonomous Discovery) ===\n");
        for (SkillDefinition skill : skills) {
            sb.append("\n[Skill: ").append(skill.getName()).append("]\n");
            sb.append("Description: ").append(skill.getDescription()).append("\n");
            sb.append("Instructions:\n");
            sb.append(skill.getInstructions() != null ? skill.getInstructions().trim() : skill.getSkillMdContent()).append("\n");
            if (!skill.getScriptPaths().isEmpty()) {
                sb.append("Available helper scripts: ").append(skill.getScriptPaths().size()).append(" scripts\n");
            }
            if (!skill.getReferencePaths().isEmpty()) {
                sb.append("Available reference guides: ").append(skill.getReferencePaths().size()).append(" docs\n");
            }
        }
        sb.append("==================================================\n");
        return sb.toString();
    }
}
