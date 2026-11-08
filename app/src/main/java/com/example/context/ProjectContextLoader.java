package com.example.context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Intelligent Project Context Loader.
 * Discovers project architecture, language breakdown, entry points, build configs,
 * and instruction files without bloating context with generated or vendor files.
 */
public class ProjectContextLoader {

  public static class ProjectProfile {
    public final String projectName;
    public final String rootPath;
    public final String projectType; // Android, Java, Kotlin, Python, Web, Mixed
    public final String buildSystem; // Gradle, Maven, NPM, CMake, Unknown
    public final List<String> primaryLanguages;
    public final List<String> keyDirectories;
    public final List<String> keyFiles;
    public final List<String> instructionFiles;

    public ProjectProfile(
        String projectName,
        String rootPath,
        String projectType,
        String buildSystem,
        List<String> primaryLanguages,
        List<String> keyDirectories,
        List<String> keyFiles,
        List<String> instructionFiles
    ) {
      this.projectName = projectName;
      this.rootPath = rootPath;
      this.projectType = projectType;
      this.buildSystem = buildSystem;
      this.primaryLanguages = primaryLanguages;
      this.keyDirectories = keyDirectories;
      this.keyFiles = keyFiles;
      this.instructionFiles = instructionFiles;
    }

    public String toContextSummary() {
      StringBuilder sb = new StringBuilder();
      sb.append("### PROJECT CONTEXT & ARCHITECTURE\n");
      sb.append("• Project: ").append(projectName).append(" (").append(projectType).append(")\n");
      sb.append("• Build System: ").append(buildSystem).append("\n");
      sb.append("• Languages: ").append(String.join(", ", primaryLanguages)).append("\n");
      if (!keyDirectories.isEmpty()) {
        sb.append("• Key Directories: ").append(String.join(", ", keyDirectories)).append("\n");
      }
      if (!keyFiles.isEmpty()) {
        sb.append("• Key Configuration Files: ").append(String.join(", ", keyFiles)).append("\n");
      }
      if (!instructionFiles.isEmpty()) {
        sb.append("• Discovered Instructions: ").append(String.join(", ", instructionFiles)).append("\n");
      }
      return sb.toString();
    }
  }

  public static ProjectProfile loadProjectProfile(File rootDir) {
    if (rootDir == null || !rootDir.exists()) {
      return new ProjectProfile("Unknown", "", "Unknown", "Unknown",
          Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    String name = rootDir.getName();
    List<String> languages = new ArrayList<>();
    List<String> keyDirs = new ArrayList<>();
    List<String> keyFiles = new ArrayList<>();
    List<String> instructions = new ArrayList<>();

    String buildSystem = "Custom";
    String projectType = "General";

    File[] rootFiles = rootDir.listFiles();
    if (rootFiles != null) {
      for (File f : rootFiles) {
        String fname = f.getName();
        if (f.isDirectory()) {
          if (isImportantDirectory(fname)) {
            keyDirs.add(fname);
          }
        } else {
          if (fname.equals("build.gradle.kts") || fname.equals("build.gradle") || fname.equals("settings.gradle.kts")) {
            buildSystem = "Gradle (Android/Kotlin)";
            if (!keyFiles.contains(fname)) keyFiles.add(fname);
          } else if (fname.equals("pom.xml")) {
            buildSystem = "Maven";
            if (!keyFiles.contains(fname)) keyFiles.add(fname);
          } else if (fname.equals("package.json")) {
            buildSystem = "NPM / Node.js";
            if (!keyFiles.contains(fname)) keyFiles.add(fname);
          } else if (fname.equals("requirements.txt") || fname.equals("Pipfile")) {
            buildSystem = "Pip / Python";
            if (!keyFiles.contains(fname)) keyFiles.add(fname);
          }

          if (fname.equalsIgnoreCase("CLAUDE.md") || fname.equalsIgnoreCase("AGENTS.md")) {
            instructions.add(fname);
          }
        }
      }
    }

    // Classify Project Type and Languages
    boolean hasKotlin = false;
    boolean hasJava = false;
    boolean hasPython = false;
    boolean hasJs = false;

    File appDir = new File(rootDir, "app/src/main");
    if (appDir.exists() || buildSystem.contains("Android")) {
      projectType = "Android Application";
    }

    // Quick sample scan
    scanLanguageSample(rootDir, 0, languages);

    return new ProjectProfile(
        name,
        rootDir.getAbsolutePath(),
        projectType,
        buildSystem,
        languages.isEmpty() ? Collections.singletonList("Kotlin / Java") : languages,
        keyDirs,
        keyFiles,
        instructions
    );
  }

  private static boolean isImportantDirectory(String name) {
    if (name.equals(".git") || name.equals(".gradle") || name.equals("build") ||
        name.equals("node_modules") || name.equals(".idea") || name.equals(".aistudio")) {
      return false;
    }
    return name.equals("app") || name.equals("src") || name.equals("lib") ||
        name.equals("backend") || name.equals("frontend") || name.equals("core");
  }

  private static void scanLanguageSample(File dir, int depth, List<String> languages) {
    if (depth > 2 || dir == null) return;
    File[] files = dir.listFiles();
    if (files == null) return;

    for (File f : files) {
      if (f.isDirectory()) {
        if (!f.getName().startsWith(".") && !f.getName().equals("build") && !f.getName().equals("node_modules")) {
          scanLanguageSample(f, depth + 1, languages);
        }
      } else {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".kt") && !languages.contains("Kotlin")) languages.add("Kotlin");
        if (n.endsWith(".java") && !languages.contains("Java")) languages.add("Java");
        if (n.endsWith(".py") && !languages.contains("Python")) languages.add("Python");
        if ((n.endsWith(".js") || n.endsWith(".ts")) && !languages.contains("TypeScript/JavaScript")) languages.add("TypeScript/JavaScript");
      }
    }
  }
}
