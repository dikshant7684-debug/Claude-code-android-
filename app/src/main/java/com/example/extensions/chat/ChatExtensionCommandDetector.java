package com.example.extensions.chat;

import android.content.Context;

import com.example.extensions.core.ExtensionManager;
import com.example.extensions.installer.ExtensionInstaller;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.InstallationEvent;
import com.example.extensions.models.SkillDefinition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatExtensionCommandDetector {

    public interface ChatCommandCallback {
        void onOperationStarted(String extensionName, String operation);
        void onProgress(InstallationEvent event);
        void onCompleted(String summaryResponse);
        void onError(String errorMessage);
    }

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "https?://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"
    );

    /**
     * Checks if user prompt is requesting to install, create, or manage an extension.
     */
    public static boolean isExtensionCommand(String prompt) {
        if (prompt == null) return false;
        String lower = prompt.toLowerCase().trim();

        boolean hasAction = lower.contains("install") || lower.contains("import") ||
                lower.contains("add") || lower.contains("create") ||
                lower.contains("download") || lower.contains("update") ||
                lower.contains("remove") || lower.contains("delete");

        boolean hasTarget = lower.contains("skill") || lower.contains("agent") ||
                lower.contains("extension") || lower.contains("github.com");

        return hasAction && hasTarget;
    }

    /**
     * Executes the detected extension command and returns formatted responses.
     */
    public static void executeCommand(Context context, String prompt, ChatCommandCallback callback) {
        String clean = prompt.trim();
        String lower = clean.toLowerCase();
        ExtensionManager manager = ExtensionManager.getInstance(context);

        // 1. Detect GitHub URL installation
        Matcher matcher = GITHUB_URL_PATTERN.matcher(clean);
        if (matcher.find()) {
            String gitHubUrl = matcher.group();
            String name = gitHubUrl.substring(gitHubUrl.lastIndexOf('/') + 1);

            callback.onOperationStarted(name, "Installing extension from GitHub (" + gitHubUrl + ")");

            manager.getInstaller().installFromGitHub(gitHubUrl, null, new ExtensionInstaller.InstallationListener() {
                @Override
                public void onProgress(InstallationEvent event) {
                    callback.onProgress(event);
                }

                @Override
                public void onCompleted(ExtensionItem item) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ Extension installed successfully.\n\n");
                    sb.append("• **Name**: ").append(item.getName()).append("\n");
                    sb.append("• **Type**: ").append(item.getType()).append("\n");
                    sb.append("• **Version**: ").append(item.getVersion()).append("\n");
                    sb.append("• **Source**: GitHub (").append(item.getSourceUrl()).append(")\n");
                    sb.append("• **Validation**: ").append(item.getValidationStatus()).append("\n");
                    sb.append("• **Registration**: Successful (Active)\n\n");
                    sb.append("The extension is loaded and active immediately. You can now use its capabilities or inspect it in the **Extension Center**.");
                    callback.onCompleted(sb.toString());
                }

                @Override
                public void onError(String extensionId, String errorMessage) {
                    callback.onError("Installation failed: " + errorMessage);
                }
            });
            return;
        }

        // 2. Detect Custom Skill creation via chat
        if (lower.contains("create") && lower.contains("skill")) {
            String skillName = extractSkillName(clean);
            callback.onOperationStarted(skillName, "Creating custom skill");

            try {
                String instructions = "Specialized AI skill instructions generated for " + skillName + ".\n" +
                        "- Execute best-practice development workflows.\n" +
                        "- Inspect project files before applying modifications.\n" +
                        "- Verify syntax and compilation integrity.";

                SkillDefinition skill = manager.getSkillManager().createCustomSkill(
                        skillName,
                        "Custom AI skill for " + skillName,
                        "Development",
                        instructions,
                        "AI Studio User"
                );

                StringBuilder sb = new StringBuilder();
                sb.append("✓ Custom Skill Created Successfully!\n\n");
                sb.append("• **Name**: ").append(skill.getName()).append("\n");
                sb.append("• **Category**: ").append(skill.getCategory()).append("\n");
                sb.append("• **Location**: `").append(skill.getDirectoryPath()).append("`\n");
                sb.append("• **Status**: Active & Registered\n\n");
                sb.append("The skill is now part of your active AI context and will guide upcoming development tasks.");
                callback.onCompleted(sb.toString());
            } catch (Exception e) {
                callback.onError("Failed creating custom skill: " + e.getMessage());
            }
            return;
        }

        // 3. Fallback instructions
        callback.onError("Could not parse extension command. Please provide a GitHub URL (e.g. `https://github.com/owner/repo`) or specify a skill name to create.");
    }

    private static String extractSkillName(String prompt) {
        String p = prompt.replaceAll("(?i)(create|a|custom|skill|for|that|specializes|in|advanced)", "").trim();
        p = p.replaceAll("[^A-Za-z0-9\\s]", "").trim();
        if (p.isEmpty()) return "Custom Developer Skill";
        return capitalizeWords(p);
    }

    private static String capitalizeWords(String str) {
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
