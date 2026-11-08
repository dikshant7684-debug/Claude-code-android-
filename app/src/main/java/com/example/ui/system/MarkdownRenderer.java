package com.example.ui.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced Markdown parsing and structured representation for AI messages.
 * Breaks down markdown into typed blocks for incremental, stable rendering.
 */
public class MarkdownRenderer {

  public enum BlockType {
    PARAGRAPH,
    HEADER_1,
    HEADER_2,
    HEADER_3,
    CODE_BLOCK,
    BLOCKQUOTE,
    UNORDERED_LIST_ITEM,
    ORDERED_LIST_ITEM,
    TASK_LIST_ITEM,
    TABLE,
    DIVIDER
  }

  public static class MarkdownBlock {
    private final BlockType type;
    private final String content;
    private final String metadata; // e.g. language for CODE_BLOCK, checked state for TASK_LIST_ITEM

    public MarkdownBlock(BlockType type, String content, String metadata) {
      this.type = type;
      this.content = content;
      this.metadata = metadata;
    }

    public BlockType getType() { return type; }
    public String getContent() { return content; }
    public String getMetadata() { return metadata; }
  }

  public static List<MarkdownBlock> parseMarkdown(String markdown) {
    List<MarkdownBlock> blocks = new ArrayList<>();
    if (markdown == null || markdown.trim().isEmpty()) {
      return blocks;
    }

    String[] lines = markdown.split("\n");
    boolean inCodeBlock = false;
    String codeLanguage = "";
    StringBuilder codeBuffer = new StringBuilder();

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String trimmed = line.trim();

      // Fenced Code Blocks
      if (trimmed.startsWith("```")) {
        if (!inCodeBlock) {
          inCodeBlock = true;
          codeLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim() : "text";
          codeBuffer.setLength(0);
        } else {
          inCodeBlock = false;
          blocks.add(new MarkdownBlock(BlockType.CODE_BLOCK, codeBuffer.toString(), codeLanguage));
        }
        continue;
      }

      if (inCodeBlock) {
        if (codeBuffer.length() > 0) codeBuffer.append("\n");
        codeBuffer.append(line);
        continue;
      }

      // Headers
      if (trimmed.startsWith("### ")) {
        blocks.add(new MarkdownBlock(BlockType.HEADER_3, trimmed.substring(4), ""));
      } else if (trimmed.startsWith("## ")) {
        blocks.add(new MarkdownBlock(BlockType.HEADER_2, trimmed.substring(3), ""));
      } else if (trimmed.startsWith("# ")) {
        blocks.add(new MarkdownBlock(BlockType.HEADER_1, trimmed.substring(2), ""));
      } else if (trimmed.equals("---") || trimmed.equals("***")) {
        blocks.add(new MarkdownBlock(BlockType.DIVIDER, "", ""));
      } else if (trimmed.startsWith("> ")) {
        blocks.add(new MarkdownBlock(BlockType.BLOCKQUOTE, trimmed.substring(2), ""));
      } else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ")) {
        blocks.add(new MarkdownBlock(BlockType.TASK_LIST_ITEM, trimmed.substring(6), "unchecked"));
      } else if (trimmed.startsWith("- [x] ") || trimmed.startsWith("* [x] ") || trimmed.startsWith("- [X] ")) {
        blocks.add(new MarkdownBlock(BlockType.TASK_LIST_ITEM, trimmed.substring(6), "checked"));
      } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
        blocks.add(new MarkdownBlock(BlockType.UNORDERED_LIST_ITEM, trimmed.substring(2), ""));
      } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
        int dotIdx = trimmed.indexOf('.');
        String num = trimmed.substring(0, dotIdx);
        String text = trimmed.substring(dotIdx + 1).trim();
        blocks.add(new MarkdownBlock(BlockType.ORDERED_LIST_ITEM, text, num));
      } else if (!trimmed.isEmpty()) {
        blocks.add(new MarkdownBlock(BlockType.PARAGRAPH, line, ""));
      }
    }

    if (inCodeBlock && codeBuffer.length() > 0) {
      blocks.add(new MarkdownBlock(BlockType.CODE_BLOCK, codeBuffer.toString(), codeLanguage));
    }

    return blocks;
  }
}
