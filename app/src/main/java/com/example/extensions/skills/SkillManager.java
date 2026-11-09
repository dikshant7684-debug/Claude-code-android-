package com.example.extensions.skills;

import android.content.Context;

import com.example.extensions.database.ExtensionDatabaseHelper;
import com.example.extensions.installer.ExtensionInstaller;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationHistoryEntry;
import com.example.extensions.models.SkillDefinition;
import com.example.extensions.registry.ExtensionRegistry;
import com.example.extensions.validator.ExtensionValidator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SkillManager {
    private final Context context;
    private final ExtensionDatabaseHelper dbHelper;
    private final ExtensionRegistry registry;
    private final ExtensionInstaller installer;
    private final ExtensionValidator validator;
    private final File skillsDir;

    public SkillManager(Context context, ExtensionDatabaseHelper dbHelper,
                        ExtensionRegistry registry, ExtensionInstaller installer) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.registry = registry;
        this.installer = installer;
        this.validator = new ExtensionValidator();
        this.skillsDir = new File(this.context.getFilesDir(), "extensions/skills");
        if (!skillsDir.exists()) skillsDir.mkdirs();
    }

    public List<SkillDefinition> getAllInstalledSkills() {
        return new ArrayList<>(registry.getActiveSkills());
    }

    public SkillDefinition getSkillById(String id) {
        return registry.getSkill(id);
    }

    /**
     * Creates a custom skill locally with a valid SKILL.md file and supporting directories.
     */
    public SkillDefinition createCustomSkill(String name, String description, String category,
                                             String instructions, String author) throws IOException {
        String cleanName = name.trim();
        String id = cleanName.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
        File skillFolder = new File(skillsDir, id);
        if (skillFolder.exists()) {
            throw new IOException("A skill with ID '" + id + "' already exists.");
        }
        skillFolder.mkdirs();

        // Create supporting folders
        new File(skillFolder, "scripts").mkdirs();
        new File(skillFolder, "references").mkdirs();
        new File(skillFolder, "assets").mkdirs();

        // Construct SKILL.md with standard YAML frontmatter
        StringBuilder skillMd = new StringBuilder();
        skillMd.append("---\n");
        skillMd.append("name: \"").append(cleanName).append("\"\n");
        skillMd.append("description: \"").append(description).append("\"\n");
        skillMd.append("category: \"").append(category != null ? category : "Custom").append("\"\n");
        skillMd.append("author: \"").append(author != null ? author : "User").append("\"\n");
        skillMd.append("version: \"1.0.0\"\n");
        skillMd.append("---\n\n");
        skillMd.append("# ").append(cleanName).append("\n\n");
        skillMd.append(instructions != null ? instructions : "Custom specialized instructions.");

        File skillMdFile = new File(skillFolder, "SKILL.md");
        try (FileWriter writer = new FileWriter(skillMdFile)) {
            writer.write(skillMd.toString());
        }

        // Validate and register
        ExtensionValidator.ValidationResult val = validator.validateDirectory(skillFolder);
        if (!val.isValid || val.skillDefinition == null) {
            throw new IOException("Created skill failed validation: " + String.join(", ", val.issues));
        }

        SkillDefinition skill = val.skillDefinition;
        skill.setId(id);
        skill.setCategory(category != null ? category : "Custom");

        ExtensionItem item = new ExtensionItem(
                id, cleanName, description, ExtensionType.SKILL, "1.0.0", author != null ? author : "User",
                "Custom Created", skillFolder.getAbsolutePath()
        );

        dbHelper.insertOrUpdateExtension(item);
        dbHelper.recordHistory(new InstallationHistoryEntry(
                id, cleanName, "CREATE", "SUCCESS", "1.0.0", "Created custom skill"
        ));

        registry.registerExtension(item);
        registry.registerSkill(skill);

        return skill;
    }

    /**
     * Updates an existing skill's instructions.
     */
    public boolean updateSkillInstructions(String id, String newInstructions) throws IOException {
        SkillDefinition skill = registry.getSkill(id);
        if (skill == null || skill.getDirectoryPath() == null) return false;

        File skillMd = new File(skill.getDirectoryPath(), "SKILL.md");
        if (!skillMd.exists()) return false;

        try (FileWriter writer = new FileWriter(skillMd)) {
            writer.write(newInstructions);
        }

        skill.setInstructions(newInstructions);
        skill.setSkillMdContent(newInstructions);

        ExtensionItem item = dbHelper.getExtensionById(id);
        if (item != null) {
            item.setLastUpdatedTimestamp(System.currentTimeMillis());
            dbHelper.insertOrUpdateExtension(item);
        }

        return true;
    }

    public boolean setSkillEnabled(String id, boolean enabled) {
        boolean ok = dbHelper.setExtensionEnabled(id, enabled);
        if (ok) {
            ExtensionItem item = registry.getExtension(id);
            if (item != null) item.setEnabled(enabled);
            dbHelper.recordHistory(new InstallationHistoryEntry(
                    id, item != null ? item.getName() : id, enabled ? "ENABLE" : "DISABLE", "SUCCESS",
                    item != null ? item.getVersion() : "1.0.0", "Toggled by user"
            ));
        }
        return ok;
    }

    public boolean deleteSkill(String id) {
        return installer.uninstallExtension(id);
    }

    /**
     * Exports an installed skill directory into a portable ZIP file.
     */
    public File exportSkillToZip(String id, File targetZipFile) throws IOException {
        SkillDefinition skill = registry.getSkill(id);
        if (skill == null || skill.getDirectoryPath() == null) {
            throw new IOException("Skill not found: " + id);
        }

        File srcDir = new File(skill.getDirectoryPath());
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetZipFile)))) {
            zipDirectoryRecursive(srcDir, srcDir, zos);
        }
        return targetZipFile;
    }

    private void zipDirectoryRecursive(File root, File current, ZipOutputStream zos) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[8192];
        for (File f : files) {
            if (f.isDirectory()) {
                zipDirectoryRecursive(root, f, zos);
            } else {
                String relPath = root.toURI().relativize(f.toURI()).getPath();
                ZipEntry entry = new ZipEntry(relPath);
                zos.putNextEntry(entry);
                try (FileInputStream in = new FileInputStream(f)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
