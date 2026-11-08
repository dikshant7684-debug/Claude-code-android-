package com.example.ui.system;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Artifact Viewer helper supporting HTML, Markdown, JSON, SVG, code, and images.
 */
public class ArtifactViewer {

  public enum ArtifactType {
    HTML,
    MARKDOWN,
    JSON,
    SVG,
    CODE,
    IMAGE,
    UNKNOWN
  }

  public static ArtifactType detectType(String fileName) {
    if (fileName == null) return ArtifactType.UNKNOWN;
    String lower = fileName.toLowerCase();
    if (lower.endsWith(".html") || lower.endsWith(".htm")) return ArtifactType.HTML;
    if (lower.endsWith(".md") || lower.endsWith(".markdown")) return ArtifactType.MARKDOWN;
    if (lower.endsWith(".json")) return ArtifactType.JSON;
    if (lower.endsWith(".svg")) return ArtifactType.SVG;
    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")) return ArtifactType.IMAGE;
    if (lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".xml") || lower.endsWith(".sql")) return ArtifactType.CODE;
    return ArtifactType.UNKNOWN;
  }

  public static boolean shareArtifact(Context context, String title, String content) {
    if (context == null || content == null) return false;
    try {
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_SUBJECT, title);
      intent.putExtra(Intent.EXTRA_TEXT, content);
      Intent chooser = Intent.createChooser(intent, "Share " + (title != null ? title : "Artifact"));
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(chooser);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean saveToLocalStorage(Context context, String fileName, String content) {
    if (context == null || fileName == null || content == null) return false;
    try {
      File dir = new File(context.getFilesDir(), "artifacts");
      if (!dir.exists()) dir.mkdirs();
      File file = new File(dir, fileName);
      try (FileOutputStream fos = new FileOutputStream(file)) {
        fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
