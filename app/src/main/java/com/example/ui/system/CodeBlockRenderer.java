package com.example.ui.system;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Code Block Renderer and Syntax Highlighter.
 * Supports Java, Kotlin, Python, JavaScript, TypeScript, HTML, CSS, JSON, XML, YAML, Bash, SQL.
 */
public class CodeBlockRenderer {

  // Color tokens for syntax highlighting
  private static final int COLOR_KEYWORD = Color.parseColor("#A78BFA");      // Electric Violet
  private static final int COLOR_TYPE = Color.parseColor("#C4B5FD");         // Soft Lavender
  private static final int COLOR_STRING = Color.parseColor("#4ADE80");       // Neon Emerald
  private static final int COLOR_NUMBER = Color.parseColor("#FBBF24");       // Amber
  private static final int COLOR_COMMENT = Color.parseColor("#777181");      // Muted Purple
  private static final int COLOR_ANNOTATION = Color.parseColor("#60A5FA");   // Sky Blue

  private static final Set<String> COMMON_KEYWORDS = new HashSet<>(Arrays.asList(
      "public", "private", "protected", "static", "final", "abstract", "class",
      "interface", "enum", "extends", "implements", "fun", "val", "var", "package",
      "import", "return", "if", "else", "when", "switch", "case", "default",
      "for", "while", "do", "break", "continue", "try", "catch", "finally",
      "throw", "throws", "new", "this", "super", "null", "true", "false",
      "async", "await", "function", "const", "let", "def", "lambda", "from",
      "select", "where", "insert", "update", "delete", "from", "join"
  ));

  private static final Set<String> COMMON_TYPES = new HashSet<>(Arrays.asList(
      "int", "long", "float", "double", "boolean", "char", "byte", "short", "void",
      "String", "Object", "List", "Map", "Set", "ArrayList", "HashMap", "Int", "Boolean",
      "Long", "Float", "Double", "Any", "Unit", "Array", "Promise", "Observable"
  ));

  private static final Pattern STRING_PATTERN = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'");
  private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/|#.*");
  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFlL]?\\b");
  private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@[A-Za-z0-9_]+");
  private static final Pattern WORD_PATTERN = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b");

  public static SpannableStringBuilder highlightCode(String rawCode, String language) {
    if (rawCode == null) rawCode = "";
    SpannableStringBuilder ssb = new SpannableStringBuilder(rawCode);

    // 1. Strings
    Matcher strMatcher = STRING_PATTERN.matcher(rawCode);
    while (strMatcher.find()) {
      ssb.setSpan(new ForegroundColorSpan(COLOR_STRING), strMatcher.start(), strMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 2. Numbers
    Matcher numMatcher = NUMBER_PATTERN.matcher(rawCode);
    while (numMatcher.find()) {
      ssb.setSpan(new ForegroundColorSpan(COLOR_NUMBER), numMatcher.start(), numMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 3. Annotations
    Matcher annMatcher = ANNOTATION_PATTERN.matcher(rawCode);
    while (annMatcher.find()) {
      ssb.setSpan(new ForegroundColorSpan(COLOR_ANNOTATION), annMatcher.start(), annMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 4. Keywords and Types
    Matcher wordMatcher = WORD_PATTERN.matcher(rawCode);
    while (wordMatcher.find()) {
      String word = wordMatcher.group();
      if (COMMON_KEYWORDS.contains(word)) {
        ssb.setSpan(new ForegroundColorSpan(COLOR_KEYWORD), wordMatcher.start(), wordMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), wordMatcher.start(), wordMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      } else if (COMMON_TYPES.contains(word)) {
        ssb.setSpan(new ForegroundColorSpan(COLOR_TYPE), wordMatcher.start(), wordMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }

    // 5. Comments (applied last to override strings / keywords inside comments)
    Matcher cmtMatcher = COMMENT_PATTERN.matcher(rawCode);
    while (cmtMatcher.find()) {
      ssb.setSpan(new ForegroundColorSpan(COLOR_COMMENT), cmtMatcher.start(), cmtMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      ssb.setSpan(new StyleSpan(Typeface.ITALIC), cmtMatcher.start(), cmtMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    return ssb;
  }

  public static boolean copyToClipboard(Context context, String label, String text) {
    if (context == null || text == null) return false;
    try {
      ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null) {
        ClipData clip = ClipData.newPlainText(label != null ? label : "Code", text);
        cm.setPrimaryClip(clip);
        HapticManager.getInstance(context).triggerCopy();
        return true;
      }
    } catch (Exception ignored) {}
    return false;
  }
}
