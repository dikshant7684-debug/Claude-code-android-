package com.example.extensions.installer;

import android.content.Context;
import android.util.Log;

import com.example.extensions.database.ExtensionDatabaseHelper;
import com.example.extensions.github.GitHubRepositoryClient;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationEvent;
import com.example.extensions.models.InstallationHistoryEntry;
import com.example.extensions.models.InstallationStatus;
import com.example.extensions.registry.ExtensionRegistry;
import com.example.extensions.security.ExtensionSecurityManager;
import com.example.extensions.validator.ExtensionValidator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtensionInstaller {
    private static final String TAG = "ExtensionInstaller";

    public interface InstallationListener {
        void onProgress(InstallationEvent event);
        void onCompleted(ExtensionItem item);
        void onError(String extensionId, String errorMessage);
    }

    private final Context context;
    private final ExtensionDatabaseHelper dbHelper;
    private final ExtensionRegistry registry;
    private final ExtensionSecurityManager securityManager;
    private final ExtensionValidator validator;
    private final GitHubRepositoryClient gitHubClient;
    private final ExecutorService executorService;
    private final File extensionsBaseDir;

    public ExtensionInstaller(Context context, ExtensionDatabaseHelper dbHelper,
                              ExtensionRegistry registry, ExtensionSecurityManager securityManager) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.registry = registry;
        this.securityManager = securityManager;
        this.validator = new ExtensionValidator();
        this.gitHubClient = new GitHubRepositoryClient();
        this.executorService = Executors.newFixedThreadPool(2);

        this.extensionsBaseDir = new File(this.context.getFilesDir(), "extensions");
        if (!extensionsBaseDir.exists()) extensionsBaseDir.mkdirs();
    }

    /**
     * Installs an extension from a GitHub repository asynchronously.
     */
    public void installFromGitHub(String gitHubUrl, String token, InstallationListener listener) {
        executorService.execute(() -> {
            String tempId = "ext_" + UUID.randomUUID().toString().substring(0, 8);
            File stagingDir = null;
            File targetInstallDir = null;

            try {
                // 1. FETCHING
                notifyStatus(listener, tempId, "GitHub Extension", InstallationStatus.FETCHING,
                        "Connecting to GitHub repository...", 10, 0, 0);

                GitHubRepositoryClient.RepoTarget target = GitHubRepositoryClient.parseGitHubUrl(gitHubUrl);
                if (target == null) {
                    throw new IOException("Invalid GitHub repository URL: " + gitHubUrl);
                }

                File tempZip = new File(context.getCacheDir(), "repo_" + System.currentTimeMillis() + ".zip");
                notifyStatus(listener, tempId, target.repo, InstallationStatus.FETCHING,
                        "Downloading repository archive from GitHub (" + target.owner + "/" + target.repo + ")...", 25, 0, 0);

                gitHubClient.downloadRepositoryZip(target, tempZip, token);

                // 2. INSPECTING & EXTRACTING
                notifyStatus(listener, tempId, target.repo, InstallationStatus.INSPECTING,
                        "Extracting and inspecting archive contents...", 40, 0, 0);

                stagingDir = new File(context.getCacheDir(), "staging_" + System.currentTimeMillis());
                stagingDir.mkdirs();

                int filesExtracted = extractZipSafely(tempZip, stagingDir);
                tempZip.delete();

                // 3. VALIDATING
                notifyStatus(listener, tempId, target.repo, InstallationStatus.VALIDATING,
                        "Validating extension metadata and structural security...", 60, filesExtracted, filesExtracted);

                ExtensionValidator.ValidationResult valResult = validator.validateDirectory(stagingDir);
                if (!valResult.isValid) {
                    throw new IOException("Validation failed: " + String.join(", ", valResult.issues));
                }

                String extId = valResult.name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                String extName = valResult.name;
                ExtensionType type = "AGENT".equalsIgnoreCase(valResult.detectedType) ? ExtensionType.AGENT : ExtensionType.SKILL;

                // 4. INSTALLING
                notifyStatus(listener, extId, extName, InstallationStatus.INSTALLING,
                        "Installing files into isolated application directory...", 75, filesExtracted, filesExtracted);

                targetInstallDir = new File(extensionsBaseDir, (type == ExtensionType.SKILL ? "skills/" : "agents/") + extId);
                if (targetInstallDir.exists()) {
                    // Update / overwrite cleanly
                    deleteDirectoryRecursively(targetInstallDir);
                }
                targetInstallDir.mkdirs();

                copyDirectory(stagingDir, targetInstallDir);
                deleteDirectoryRecursively(stagingDir);

                // Re-validate against permanent path
                ExtensionValidator.ValidationResult permVal = validator.validateDirectory(targetInstallDir);

                // 5. REGISTERING & PERSISTING
                notifyStatus(listener, extId, extName, InstallationStatus.REGISTERING,
                        "Registering with runtime environment and updating persistent database...", 90, filesExtracted, filesExtracted);

                String checksum = securityManager.computeChecksum(targetInstallDir);

                ExtensionItem item = new ExtensionItem(
                        extId,
                        extName,
                        valResult.description,
                        type,
                        valResult.version,
                        valResult.author,
                        gitHubUrl,
                        targetInstallDir.getAbsolutePath()
                );
                item.setChecksumSha256(checksum);
                item.setValidationStatus("PASSED");

                dbHelper.insertOrUpdateExtension(item);
                dbHelper.recordHistory(new InstallationHistoryEntry(
                        extId, extName, "INSTALL", "SUCCESS", valResult.version,
                        "Installed from " + gitHubUrl + " (" + filesExtracted + " files)"
                ));

                registry.registerExtension(item);
                if (type == ExtensionType.SKILL && permVal.skillDefinition != null) {
                    permVal.skillDefinition.setId(extId);
                    registry.registerSkill(permVal.skillDefinition);
                } else if (type == ExtensionType.AGENT && permVal.agentDefinition != null) {
                    permVal.agentDefinition.setId(extId);
                    registry.registerAgent(permVal.agentDefinition);
                }

                // 6. COMPLETED
                notifyStatus(listener, extId, extName, InstallationStatus.COMPLETED,
                        "Extension installed successfully and active.", 100, filesExtracted, filesExtracted);

                if (listener != null) listener.onCompleted(item);

            } catch (Exception e) {
                Log.e(TAG, "Installation failed", e);
                // 7. ROLLING_BACK
                notifyStatus(listener, tempId, "Extension", InstallationStatus.ROLLING_BACK,
                        "Error encountered: " + e.getMessage() + ". Rolling back...", 0, 0, 0);

                if (stagingDir != null && stagingDir.exists()) deleteDirectoryRecursively(stagingDir);
                if (targetInstallDir != null && targetInstallDir.exists()) deleteDirectoryRecursively(targetInstallDir);

                dbHelper.recordHistory(new InstallationHistoryEntry(
                        tempId, "Extension", "INSTALL", "FAILED", "unknown", e.getMessage()
                ));

                notifyStatus(listener, tempId, "Extension", InstallationStatus.FAILED,
                        "Installation failed: " + e.getMessage(), 0, 0, 0);

                if (listener != null) listener.onError(tempId, e.getMessage());
            }
        });
    }

    /**
     * Installs a local ZIP file imported via Storage Access Framework.
     */
    public void installFromLocalZip(File zipFile, InstallationListener listener) {
        executorService.execute(() -> {
            String tempId = "local_" + UUID.randomUUID().toString().substring(0, 8);
            File stagingDir = null;
            File targetInstallDir = null;

            try {
                notifyStatus(listener, tempId, "Local Extension", InstallationStatus.INSPECTING,
                        "Extracting local archive...", 30, 0, 0);

                stagingDir = new File(context.getCacheDir(), "local_staging_" + System.currentTimeMillis());
                stagingDir.mkdirs();

                int filesExtracted = extractZipSafely(zipFile, stagingDir);

                notifyStatus(listener, tempId, "Local Extension", InstallationStatus.VALIDATING,
                        "Validating extension...", 60, filesExtracted, filesExtracted);

                ExtensionValidator.ValidationResult valResult = validator.validateDirectory(stagingDir);
                if (!valResult.isValid) {
                    throw new IOException("Validation failed: " + String.join(", ", valResult.issues));
                }

                String extId = valResult.name.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
                String extName = valResult.name;
                ExtensionType type = "AGENT".equalsIgnoreCase(valResult.detectedType) ? ExtensionType.AGENT : ExtensionType.SKILL;

                targetInstallDir = new File(extensionsBaseDir, (type == ExtensionType.SKILL ? "skills/" : "agents/") + extId);
                if (targetInstallDir.exists()) deleteDirectoryRecursively(targetInstallDir);
                targetInstallDir.mkdirs();

                copyDirectory(stagingDir, targetInstallDir);
                deleteDirectoryRecursively(stagingDir);

                ExtensionValidator.ValidationResult permVal = validator.validateDirectory(targetInstallDir);
                String checksum = securityManager.computeChecksum(targetInstallDir);

                ExtensionItem item = new ExtensionItem(
                        extId, extName, valResult.description, type, valResult.version, valResult.author,
                        "Local Import", targetInstallDir.getAbsolutePath()
                );
                item.setChecksumSha256(checksum);

                dbHelper.insertOrUpdateExtension(item);
                dbHelper.recordHistory(new InstallationHistoryEntry(
                        extId, extName, "INSTALL", "SUCCESS", valResult.version, "Imported from local archive"
                ));

                registry.registerExtension(item);
                if (type == ExtensionType.SKILL && permVal.skillDefinition != null) {
                    permVal.skillDefinition.setId(extId);
                    registry.registerSkill(permVal.skillDefinition);
                } else if (type == ExtensionType.AGENT && permVal.agentDefinition != null) {
                    permVal.agentDefinition.setId(extId);
                    registry.registerAgent(permVal.agentDefinition);
                }

                notifyStatus(listener, extId, extName, InstallationStatus.COMPLETED,
                        "Imported successfully.", 100, filesExtracted, filesExtracted);
                if (listener != null) listener.onCompleted(item);

            } catch (Exception e) {
                if (stagingDir != null && stagingDir.exists()) deleteDirectoryRecursively(stagingDir);
                if (targetInstallDir != null && targetInstallDir.exists()) deleteDirectoryRecursively(targetInstallDir);
                if (listener != null) listener.onError(tempId, e.getMessage());
            }
        });
    }

    /**
     * Uninstalls an extension, deleting files and clearing database / registry.
     */
    public boolean uninstallExtension(String extensionId) {
        ExtensionItem item = dbHelper.getExtensionById(extensionId);
        if (item == null) return false;

        if (item.getInstallDirectory() != null) {
            File dir = new File(item.getInstallDirectory());
            if (dir.exists()) deleteDirectoryRecursively(dir);
        }

        dbHelper.deleteExtension(extensionId);
        registry.unregisterExtension(extensionId);
        dbHelper.recordHistory(new InstallationHistoryEntry(
                extensionId, item.getName(), "UNINSTALL", "SUCCESS", item.getVersion(), "Uninstalled by user"
        ));
        return true;
    }

    private int extractZipSafely(File zipFile, File destinationDir) throws IOException {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // Strip top-level github folder if present (e.g. repo-main/...)
                int slash = name.indexOf('/');
                if (slash != -1 && name.length() > slash + 1) {
                    name = name.substring(slash + 1);
                }

                if (name.isEmpty() || !securityManager.isSafeZipPath(destinationDir, name)) {
                    zis.closeEntry();
                    continue;
                }

                File targetFile = new File(destinationDir, name);
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    count++;
                }
                zis.closeEntry();
            }
        }
        return count;
    }

    private void copyDirectory(File source, File target) throws IOException {
        if (!source.exists()) return;
        if (source.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    copyDirectory(file, new File(target, file.getName()));
                }
            }
        } else {
            try (FileInputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }
    }

    private void deleteDirectoryRecursively(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) return;
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) deleteDirectoryRecursively(child);
            }
        }
        fileOrDir.delete();
    }

    private void notifyStatus(InstallationListener listener, String extId, String name,
                              InstallationStatus status, String msg, int progress, int files, int total) {
        if (listener == null) return;
        InstallationEvent event = new InstallationEvent(extId, name, status, msg);
        event.setProgressPercentage(progress);
        event.setFilesProcessed(files);
        event.setTotalFiles(total);
        listener.onProgress(event);
    }
}
