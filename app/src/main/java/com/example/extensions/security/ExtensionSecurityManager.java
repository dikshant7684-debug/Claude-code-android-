package com.example.extensions.security;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtensionSecurityManager {
    private static final String TAG = "ExtensionSecurity";

    // Known privileged tools that require explicit permission authorization
    private static final Set<String> PRIVILEGED_TOOLS = new HashSet<>(Arrays.asList(
            "execute_command", "run_terminal_command", "delete_file", "execute_background_command",
            "git_push", "git_commit", "deploy_app", "write_file", "apply_patch"
    ));

    private static volatile ExtensionSecurityManager instance;
    private final Context context;
    private final List<String> securityAuditLogs;

    public static synchronized ExtensionSecurityManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExtensionSecurityManager(context.getApplicationContext());
        }
        return instance;
    }

    private ExtensionSecurityManager(Context context) {
        this.context = context;
        this.securityAuditLogs = new ArrayList<>();
    }

    /**
     * Validates that target extraction path is securely confined to the destination folder (Zip Slip protection).
     */
    public boolean isSafeZipPath(File destinationDir, String entryName) {
        try {
            File targetFile = new File(destinationDir, entryName);
            String destCanonical = destinationDir.getCanonicalPath();
            String targetCanonical = targetFile.getCanonicalPath();

            if (!targetCanonical.startsWith(destCanonical + File.separator) && !targetCanonical.equals(destCanonical)) {
                logAudit("BLOCKED_ZIP_SLIP", "Path traversal attempt detected in ZIP entry: " + entryName);
                return false;
            }
            return true;
        } catch (IOException e) {
            logAudit("SECURITY_ERROR", "Error validating ZIP path: " + e.getMessage());
            return false;
        }
    }

    /**
     * Protects against relative path traversal ("../") in file operations.
     */
    public boolean isSafeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return false;
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            logAudit("BLOCKED_PATH_TRAVERSAL", "Path traversal blocked: " + relativePath);
            return false;
        }
        return true;
    }

    /**
     * Computes SHA-256 checksum of an installed directory or file for tamper verification.
     */
    public String computeChecksum(File fileOrDir) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, fileOrDir);
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to compute checksum", e);
            return "";
        }
    }

    private void updateDigest(MessageDigest digest, File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, (a, b) -> a.getName().compareTo(b.getName()));
                for (File child : children) {
                    updateDigest(digest, child);
                }
            }
        } else {
            digest.update(file.getName().getBytes("UTF-8"));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
        }
    }

    /**
     * Checks if an agent is authorized to invoke a tool based on its declared permission set.
     */
    public boolean isToolExecutionAllowed(String toolName, List<String> grantedPermissions) {
        if (!PRIVILEGED_TOOLS.contains(toolName)) {
            // Read-only and inspection tools are safe by default
            return true;
        }

        // Privileged operations require corresponding granted permissions
        if (grantedPermissions == null || grantedPermissions.isEmpty()) {
            logAudit("TOOL_DENIED", "Privileged tool '" + toolName + "' denied: No permissions declared.");
            return false;
        }

        for (String perm : grantedPermissions) {
            String p = perm.toUpperCase();
            if (p.contains("ALL") || p.contains("ADMIN") || p.contains("FULL_ACCESS")) return true;
            if (toolName.contains("command") && (p.contains("TERMINAL") || p.contains("COMMAND") || p.contains("EXECUTE"))) return true;
            if ((toolName.contains("write") || toolName.contains("delete") || toolName.contains("patch")) &&
                    (p.contains("WRITE") || p.contains("MODIFY") || p.contains("STORAGE"))) return true;
            if (toolName.contains("file") && !toolName.contains("write") && !toolName.contains("delete") &&
                    (p.contains("FILES") || p.contains("READ"))) return true;
            if (toolName.contains("git") && (p.contains("GIT") || p.contains("VCS"))) return true;
        }

        logAudit("TOOL_DENIED", "Privileged tool '" + toolName + "' denied: Missing matching permission in " + grantedPermissions);
        return false;
    }

    public synchronized void logAudit(String eventType, String details) {
        String entry = "[" + System.currentTimeMillis() + "] " + eventType + ": " + details;
        securityAuditLogs.add(entry);
        Log.w(TAG, entry);
    }

    public synchronized List<String> getAuditLogs() {
        return new ArrayList<>(securityAuditLogs);
    }
}
