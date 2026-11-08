package com.example.ui.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Real Code-Diff computation and model engine.
 * Computes line-by-line diffs between original and modified file contents.
 */
public class DiffViewer {

  public enum LineType {
    UNCHANGED,
    ADDED,
    DELETED,
    HEADER
  }

  public static class DiffLine {
    private final LineType type;
    private final String text;
    private final int oldLineNumber;
    private final int newLineNumber;

    public DiffLine(LineType type, String text, int oldLineNumber, int newLineNumber) {
      this.type = type;
      this.text = text;
      this.oldLineNumber = oldLineNumber;
      this.newLineNumber = newLineNumber;
    }

    public LineType getType() { return type; }
    public String getText() { return text; }
    public int getOldLineNumber() { return oldLineNumber; }
    public int getNewLineNumber() { return newLineNumber; }
  }

  public static class DiffResult {
    private final String filePath;
    private final List<DiffLine> lines;
    private final int additions;
    private final int deletions;

    public DiffResult(String filePath, List<DiffLine> lines, int additions, int deletions) {
      this.filePath = filePath;
      this.lines = lines;
      this.additions = additions;
      this.deletions = deletions;
    }

    public String getFilePath() { return filePath; }
    public List<DiffLine> getLines() { return lines; }
    public int getAdditions() { return additions; }
    public int getDeletions() { return deletions; }
  }

  public static DiffResult computeDiff(String filePath, String oldContent, String newContent) {
    if (oldContent == null) oldContent = "";
    if (newContent == null) newContent = "";

    String[] oldLines = oldContent.split("\n", -1);
    String[] newLines = newContent.split("\n", -1);

    List<DiffLine> diffLines = new ArrayList<>();
    int adds = 0;
    int dels = 0;

    int oldIdx = 0;
    int newIdx = 0;

    while (oldIdx < oldLines.length || newIdx < newLines.length) {
      if (oldIdx < oldLines.length && newIdx < newLines.length) {
        String ol = oldLines[oldIdx];
        String nl = newLines[newIdx];

        if (ol.equals(nl)) {
          diffLines.add(new DiffLine(LineType.UNCHANGED, ol, oldIdx + 1, newIdx + 1));
          oldIdx++;
          newIdx++;
        } else {
          // Look ahead to find matches
          boolean foundInNew = false;
          for (int k = newIdx + 1; k < Math.min(newIdx + 4, newLines.length); k++) {
            if (ol.equals(newLines[k])) {
              foundInNew = true;
              break;
            }
          }

          if (foundInNew) {
            diffLines.add(new DiffLine(LineType.ADDED, nl, -1, newIdx + 1));
            adds++;
            newIdx++;
          } else {
            diffLines.add(new DiffLine(LineType.DELETED, ol, oldIdx + 1, -1));
            dels++;
            oldIdx++;
          }
        }
      } else if (oldIdx < oldLines.length) {
        diffLines.add(new DiffLine(LineType.DELETED, oldLines[oldIdx], oldIdx + 1, -1));
        dels++;
        oldIdx++;
      } else {
        diffLines.add(new DiffLine(LineType.ADDED, newLines[newIdx], -1, newIdx + 1));
        adds++;
        newIdx++;
      }
    }

    return new DiffResult(filePath, diffLines, adds, dels);
  }
}
