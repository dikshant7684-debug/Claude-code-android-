package com.example.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Real-time Context Window Monitor tracking model capacity, token usage,
 * and threshold triggers across various AI providers.
 */
public class ContextWindowMonitor {

  public static class ModelLimit {
    public final String modelId;
    public final int contextWindowTokens;
    public final int maxOutputTokens;

    public ModelLimit(String modelId, int contextWindowTokens, int maxOutputTokens) {
      this.modelId = modelId;
      this.contextWindowTokens = contextWindowTokens;
      this.maxOutputTokens = maxOutputTokens;
    }
  }

  private static final Map<String, ModelLimit> MODEL_LIMITS = new HashMap<>();

  static {
    // Anthropic Models
    MODEL_LIMITS.put("claude-3-7-sonnet-20250219", new ModelLimit("claude-3-7-sonnet-20250219", 200_000, 8_192));
    MODEL_LIMITS.put("claude-3-5-sonnet-20241022", new ModelLimit("claude-3-5-sonnet-20241022", 200_000, 8_192));
    MODEL_LIMITS.put("claude-3-5-haiku-20241022", new ModelLimit("claude-3-5-haiku-20241022", 200_000, 8_192));

    // Google Models
    MODEL_LIMITS.put("gemini-2.0-flash", new ModelLimit("gemini-2.0-flash", 1_048_576, 8_192));
    MODEL_LIMITS.put("gemini-1.5-pro", new ModelLimit("gemini-1.5-pro", 2_097_152, 8_192));
    MODEL_LIMITS.put("gemini-1.5-flash", new ModelLimit("gemini-1.5-flash", 1_048_576, 8_192));

    // OpenAI Models
    MODEL_LIMITS.put("gpt-4o", new ModelLimit("gpt-4o", 128_000, 4_096));
    MODEL_LIMITS.put("gpt-4o-mini", new ModelLimit("gpt-4o-mini", 128_000, 16_384));
    MODEL_LIMITS.put("o1", new ModelLimit("o1", 200_000, 100_000));
    MODEL_LIMITS.put("o3-mini", new ModelLimit("o3-mini", 200_000, 100_000));

    // DeepSeek & Open Models
    MODEL_LIMITS.put("deepseek-chat", new ModelLimit("deepseek-chat", 64_000, 8_000));
    MODEL_LIMITS.put("deepseek-coder", new ModelLimit("deepseek-coder", 64_000, 8_000));
    MODEL_LIMITS.put("llama3.2", new ModelLimit("llama3.2", 128_000, 4_096));
    MODEL_LIMITS.put("qwen-2.5-coder", new ModelLimit("qwen-2.5-coder", 128_000, 8_192));
  }

  private static final int DEFAULT_CONTEXT_WINDOW = 128_000;
  private static final int DEFAULT_MAX_OUTPUT = 4_096;

  /**
   * Retrieves context window limit for a model ID with fallback.
   */
  public static ModelLimit getModelLimit(String modelId) {
    if (modelId == null) {
      return new ModelLimit("default", DEFAULT_CONTEXT_WINDOW, DEFAULT_MAX_OUTPUT);
    }
    String clean = modelId.trim().toLowerCase();
    for (Map.Entry<String, ModelLimit> entry : MODEL_LIMITS.entrySet()) {
      if (clean.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return new ModelLimit(modelId, DEFAULT_CONTEXT_WINDOW, DEFAULT_MAX_OUTPUT);
  }

  /**
   * Estimates token count from text using character-to-token heuristics.
   * Approximately 3.8 characters per token for code/English text.
   */
  public static int estimateTokens(String text) {
    if (text == null || text.isEmpty()) return 0;
    // Standard approximation: 1 token ≈ 3.8 characters
    return (int) Math.ceil(text.length() / 3.8);
  }

  public enum CompactionThresholdState {
    NORMAL,           // < 60%
    MONITOR,          // 60% - 75%
    PREPARE_SUMMARY,  // 75% - 85%
    COMPACT_REQUIRED, // 85% - 90%
    CRITICAL_LIMIT    // > 90%
  }

  public static CompactionThresholdState evaluateThreshold(int currentTokens, int maxCapacityTokens) {
    if (maxCapacityTokens <= 0) return CompactionThresholdState.NORMAL;
    float ratio = (float) currentTokens / maxCapacityTokens;
    if (ratio >= 0.90f) return CompactionThresholdState.CRITICAL_LIMIT;
    if (ratio >= 0.85f) return CompactionThresholdState.COMPACT_REQUIRED;
    if (ratio >= 0.75f) return CompactionThresholdState.PREPARE_SUMMARY;
    if (ratio >= 0.60f) return CompactionThresholdState.MONITOR;
    return CompactionThresholdState.NORMAL;
  }
}
