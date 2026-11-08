package com.example.ui.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Palette Manager providing verified, implemented slash commands
 * accessible from the chat composer.
 */
public class CommandPaletteManager {

  public static class CommandItem {
    private final String command;
    private final String description;
    private final String category;
    private final String iconType;

    public CommandItem(String command, String description, String category, String iconType) {
      this.command = command;
      this.description = description;
      this.category = category;
      this.iconType = iconType;
    }

    public String getCommand() { return command; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIconType() { return iconType; }
  }

  private static final List<CommandItem> COMMANDS = new ArrayList<>();

  static {
    COMMANDS.add(new CommandItem("/help", "Show command palette assistance & keyboard shortcuts", "System", "help"));
    COMMANDS.add(new CommandItem("/clear", "Clear chat history and reset context", "Chat", "clear"));
    COMMANDS.add(new CommandItem("/model", "Open bottom sheet to select active AI model", "AI", "model"));
    COMMANDS.add(new CommandItem("/models", "List all configured providers and models", "AI", "models"));
    COMMANDS.add(new CommandItem("/compact", "Compact active conversation context memory", "Chat", "compact"));
    COMMANDS.add(new CommandItem("/plan", "Switch to autonomous planning mode", "Agent", "plan"));
    COMMANDS.add(new CommandItem("/think", "Toggle model reasoning effort (Low/Med/High)", "AI", "think"));
    COMMANDS.add(new CommandItem("/terminal", "Open terminal execution console", "Developer", "terminal"));
    COMMANDS.add(new CommandItem("/build", "Verify project compilation via Gradle", "Developer", "build"));
    COMMANDS.add(new CommandItem("/test", "Run Android unit & Robolectric tests", "Developer", "test"));
    COMMANDS.add(new CommandItem("/git", "Inspect git status and local file diffs", "Developer", "git"));
    COMMANDS.add(new CommandItem("/files", "Browse project directory and files", "Workspace", "files"));
    COMMANDS.add(new CommandItem("/search", "Search codebase for keywords or symbols", "Workspace", "search"));
    COMMANDS.add(new CommandItem("/mcp", "Manage MCP servers, tools and permissions", "Tools", "mcp"));
    COMMANDS.add(new CommandItem("/agents", "View autonomous agent loop status", "Agent", "agents"));
    COMMANDS.add(new CommandItem("/skills", "Manage installed marketplace skills & hooks", "Tools", "skills"));
    COMMANDS.add(new CommandItem("/settings", "Open application settings & theme preferences", "System", "settings"));
  }

  public static List<CommandItem> getAvailableCommands() {
    return new ArrayList<>(COMMANDS);
  }

  public static List<CommandItem> filterCommands(String query) {
    if (query == null || query.trim().isEmpty() || query.equals("/")) {
      return getAvailableCommands();
    }
    String clean = query.trim().toLowerCase();
    if (clean.startsWith("/")) {
      clean = clean.substring(1);
    }
    List<CommandItem> result = new ArrayList<>();
    for (CommandItem item : COMMANDS) {
      String cmdName = item.getCommand().toLowerCase();
      if (cmdName.contains(clean) || item.getDescription().toLowerCase().contains(clean)) {
        result.add(item);
      }
    }
    return result;
  }
}
