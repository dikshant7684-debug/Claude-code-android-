package com.example.ui.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Selector Manager providing structured model metadata,
 * capabilities, context window dimensions, and search filters.
 */
public class ModelSelectorManager {

  public static class ModelInfo {
    private final String id;
    private final String displayName;
    private final String provider;
    private final boolean isFree;
    private final String contextWindow;
    private final List<String> capabilities;
    private final boolean isAvailable;

    public ModelInfo(String id, String displayName, String provider, boolean isFree, String contextWindow, List<String> capabilities, boolean isAvailable) {
      this.id = id;
      this.displayName = displayName;
      this.provider = provider;
      this.isFree = isFree;
      this.contextWindow = contextWindow;
      this.capabilities = capabilities;
      this.isAvailable = isAvailable;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getProvider() { return provider; }
    public boolean isFree() { return isFree; }
    public String getContextWindow() { return contextWindow; }
    public List<String> getCapabilities() { return capabilities; }
    public boolean isAvailable() { return isAvailable; }
  }

  private static final List<ModelInfo> MODELS = new ArrayList<>();

  static {
    List<String> sonnetCaps = new ArrayList<>();
    sonnetCaps.add("Reasoning");
    sonnetCaps.add("Coding");
    sonnetCaps.add("Vision");

    List<String> geminiCaps = new ArrayList<>();
    geminiCaps.add("Multimodal");
    geminiCaps.add("Coding");
    geminiCaps.add("Fast");

    List<String> gptCaps = new ArrayList<>();
    gptCaps.add("General");
    gptCaps.add("Coding");
    gptCaps.add("Vision");

    MODELS.add(new ModelInfo("claude-3-7-sonnet-20250219", "Claude 3.7 Sonnet", "Anthropic", false, "200k", sonnetCaps, true));
    MODELS.add(new ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Anthropic", false, "200k", sonnetCaps, true));
    MODELS.add(new ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", "Google", true, "1M", geminiCaps, true));
    MODELS.add(new ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "Google", false, "2M", geminiCaps, true));
    MODELS.add(new ModelInfo("gpt-4o", "GPT-4o", "OpenAI", false, "128k", gptCaps, true));
    MODELS.add(new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "OpenAI", true, "128k", gptCaps, true));
    MODELS.add(new ModelInfo("deepseek-chat", "DeepSeek-V3", "DeepSeek", true, "64k", sonnetCaps, true));
    MODELS.add(new ModelInfo("llama3.2", "Llama 3.2 3B", "Local Ollama", true, "128k", gptCaps, true));
  }

  public static List<ModelInfo> getAllModels() {
    return new ArrayList<>(MODELS);
  }

  public static List<ModelInfo> filterModels(String query, String providerFilter, Boolean freeOnly) {
    List<ModelInfo> results = new ArrayList<>();
    String q = (query != null) ? query.trim().toLowerCase() : "";

    for (ModelInfo m : MODELS) {
      if (!q.isEmpty() && !m.getDisplayName().toLowerCase().contains(q) && !m.getId().toLowerCase().contains(q)) {
        continue;
      }
      if (providerFilter != null && !providerFilter.isEmpty() && !providerFilter.equalsIgnoreCase("All") && !m.getProvider().equalsIgnoreCase(providerFilter)) {
        continue;
      }
      if (freeOnly != null && freeOnly && !m.isFree()) {
        continue;
      }
      results.add(m);
    }
    return results;
  }
}
