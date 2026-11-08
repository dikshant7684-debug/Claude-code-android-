package com.example.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent Instruction Manager supporting CLAUDE.md and AGENTS.md instruction files
 * at global, project root, and nested subdirectory levels.
 */
public class PersistentInstructionManager {

  public static class InstructionFile {
    public final String relativePath; // e.g. "CLAUDE.md" or "app/src/main/CLAUDE.md"
    public final String directoryScope; // e.g. "" or "app/src/main"
    public final String content;
    public final long lastModified;

    public InstructionFile(String relativePath, String directoryScope, String content, long lastModified) {
      this.relativePath = relativePath;
      this.directoryScope = directoryScope;
      this.content = content;
      this.lastModified = lastModified;
    }
  }

  private static final PersistentInstructionManager INSTANCE = new PersistentInstructionManager();

  public static PersistentInstructionManager getInstance() {
    return INSTANCE;
  }

  // Cache: projectId -> (relativePath -> InstructionFile)
  private final Map<String, Map<String, InstructionFile>> instructionCache = new LinkedHashMap<>();

  private PersistentInstructionManager() {}

  /**
   * Scans a project root directory recursively for CLAUDE.md and AGENTS.md files,
   * skipping build/, .gradle/, node_modules/, .git/.
   */
  public synchronized List<InstructionFile> discoverInstructions(String projectId, File projectRoot) {
    if (projectRoot == null || !projectRoot.exists() || !projectRoot.isDirectory()) {
      return Collections.emptyList();
    }

    Map<String, InstructionFile> projectMap = new LinkedHashMap<>();
    scanDirectory(projectRoot, projectRoot, projectMap);
    instructionCache.put(projectId, projectMap);

    return new ArrayList<>(projectMap.values());
  }

  private void scanDirectory(File root, File currentDir, Map<String, InstructionFile> projectMap) {
    File[] files = currentDir.listFiles();
    if (files == null) return;

    for (File f : files) {
      String name = f.getName();
      // Skip ignored directories
      if (f.isDirectory()) {
        if (name.equals(".git") || name.equals(".gradle") || name.equals("build") ||
            name.equals("node_modules") || name.equals(".idea") || name.equals(".aistudio")) {
          continue;
        }
        scanDirectory(root, f, projectMap);
      } else {
        if (name.equalsIgnoreCase("CLAUDE.md") || name.equalsIgnoreCase("AGENTS.md")) {
          String relPath = getRelativePath(root, f);
          String dirScope = getRelativePath(root, currentDir);
          String content = readFileContent(f);
          InstructionFile inst = new InstructionFile(relPath, dirScope, content, f.lastModified());
          projectMap.put(relPath, inst);
        }
      }
    }
  }

  /**
   * Resolves all applicable instruction files for a given target file or directory.
   * Priority order:
   * 1. Global / Project Root instructions ("CLAUDE.md")
   * 2. Parent directory instructions (e.g. "app/CLAUDE.md")
   * 3. Specific directory instructions (e.g. "app/src/main/CLAUDE.md")
   */
  public synchronized List<InstructionFile> resolveInstructionsForPath(String projectId, String targetRelativePath) {
    Map<String, InstructionFile> projectMap = instructionCache.get(projectId);
    if (projectMap == null || projectMap.isEmpty()) {
      return Collections.emptyList();
    }

    String normalizedTarget = targetRelativePath != null ? targetRelativePath.replace('\\', '/') : "";
    List<InstructionFile> matched = new ArrayList<>();

    // 1. Root instructions always apply
    for (InstructionFile inst : projectMap.values()) {
      if (inst.directoryScope.isEmpty()) {
        matched.add(inst);
      }
    }

    // 2. Subdirectory instructions apply if target is within their directoryScope
    if (!normalizedTarget.isEmpty()) {
      for (InstructionFile inst : projectMap.values()) {
        if (!inst.directoryScope.isEmpty() && normalizedTarget.startsWith(inst.directoryScope)) {
          if (!matched.contains(inst)) {
            matched.add(inst);
          }
        }
      }
    }

    return matched;
  }

  /**
   * Combines all applicable instruction contents into a single markdown string without duplication.
   */
  public String formatApplicableInstructions(String projectId, String targetRelativePath) {
    List<InstructionFile> files = resolveInstructionsForPath(projectId, targetRelativePath);
    if (files.isEmpty()) return "";

    StringBuilder sb = new StringBuilder();
    sb.append("### REPOSITORY INSTRUCTIONS (CLAUDE.md / AGENTS.md)\n");

    for (InstructionFile f : files) {
      sb.append("\n<!-- Source: ").append(f.relativePath).append(" (Scope: ")
          .append(f.directoryScope.isEmpty() ? "Project Root" : f.directoryScope).append(") -->\n");
      sb.append(f.content.trim()).append("\n");
    }

    return sb.toString();
  }

  /**
   * Writes or updates an instruction file (e.g. creating/editing CLAUDE.md).
   */
  public synchronized boolean saveInstructionFile(File projectRoot, String relativePath, String content) {
    if (projectRoot == null || relativePath == null || content == null) return false;
    try {
      File target = new File(projectRoot, relativePath);
      File parent = target.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      try (FileOutputStream fos = new FileOutputStream(target)) {
        fos.write(content.getBytes(StandardCharsets.UTF_8));
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String getRelativePath(File base, File child) {
    String basePath = base.getAbsolutePath();
    String childPath = child.getAbsolutePath();
    if (childPath.equals(basePath)) return "";
    if (childPath.startsWith(basePath)) {
      String rel = childPath.substring(basePath.length());
      if (rel.startsWith(File.separator) || rel.startsWith("/")) {
        rel = rel.substring(1);
      }
      return rel.replace('\\', '/');
    }
    return child.getName();
  }

  private String readFileContent(File f) {
    try (FileInputStream fis = new FileInputStream(f)) {
      byte[] data = new byte[(int) f.length()];
      int read = fis.read(data);
      if (read > 0) {
        return new String(data, 0, read, StandardCharsets.UTF_8);
      }
    } catch (Exception ignored) {}
    return "";
  }
}
