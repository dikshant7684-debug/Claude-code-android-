package com.example.extensions.agents;

import android.content.Context;

import com.example.extensions.database.ExtensionDatabaseHelper;
import com.example.extensions.installer.ExtensionInstaller;
import com.example.extensions.models.AgentDefinition;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationHistoryEntry;
import com.example.extensions.registry.ExtensionRegistry;
import com.example.extensions.security.ExtensionSecurityManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AgentManager {
    private final Context context;
    private final ExtensionDatabaseHelper dbHelper;
    private final ExtensionRegistry registry;
    private final ExtensionInstaller installer;
    private final ExtensionSecurityManager securityManager;
    private final File agentsDir;

    public AgentManager(Context context, ExtensionDatabaseHelper dbHelper,
                        ExtensionRegistry registry, ExtensionInstaller installer,
                        ExtensionSecurityManager securityManager) {
        this.context = context.getApplicationContext();
        this.dbHelper = dbHelper;
        this.registry = registry;
        this.installer = installer;
        this.securityManager = securityManager;
        this.agentsDir = new File(this.context.getFilesDir(), "extensions/agents");
        if (!agentsDir.exists()) agentsDir.mkdirs();
    }

    public List<AgentDefinition> getAllInstalledAgents() {
        return new ArrayList<>(registry.getActiveAgents());
    }

    public AgentDefinition getAgentById(String id) {
        return registry.getAgent(id);
    }

    /**
     * Creates a custom agent definition with specified capabilities, tools, and permissions.
     */
    public AgentDefinition createCustomAgent(String name, String description, String systemInstructions,
                                             List<String> capabilities, List<String> tools,
                                             List<String> permissions, String preferredModel,
                                             String author) throws IOException {
        String cleanName = name.trim();
        String id = cleanName.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
        File agentFolder = new File(agentsDir, id);
        if (agentFolder.exists()) {
            throw new IOException("An agent with ID '" + id + "' already exists.");
        }
        agentFolder.mkdirs();

        JSONObject json = new JSONObject();
        try {
            json.put("name", cleanName);
            json.put("description", description);
            json.put("version", "1.0.0");
            json.put("author", author != null ? author : "User");
            json.put("system_instructions", systemInstructions);
            json.put("preferred_model", preferredModel != null ? preferredModel : "");

            JSONArray capsArr = new JSONArray();
            if (capabilities != null) for (String c : capabilities) capsArr.put(c);
            json.put("capabilities", capsArr);

            JSONArray toolsArr = new JSONArray();
            if (tools != null) for (String t : tools) toolsArr.put(t);
            json.put("tools", toolsArr);

            JSONArray permsArr = new JSONArray();
            if (permissions != null) for (String p : permissions) permsArr.put(p);
            json.put("permissions", permsArr);
        } catch (Exception e) {
            throw new IOException("Failed to serialize agent JSON: " + e.getMessage());
        }

        File agentJsonFile = new File(agentFolder, "agent.json");
        try (FileWriter writer = new FileWriter(agentJsonFile)) {
            writer.write(json.toString(2));
        } catch (org.json.JSONException e) {
            throw new IOException("Failed to format agent JSON: " + e.getMessage());
        }

        AgentDefinition def = new AgentDefinition(id, cleanName, description, systemInstructions);
        def.setCapabilities(capabilities != null ? capabilities : new ArrayList<>());
        def.setAvailableTools(tools != null ? tools : new ArrayList<>());
        def.setPermissionRequirements(permissions != null ? permissions : new ArrayList<>());
        def.setPreferredModelId(preferredModel);
        def.setConfigurationJson(json.toString());
        def.setDirectoryPath(agentFolder.getAbsolutePath());

        ExtensionItem item = new ExtensionItem(
                id, cleanName, description, ExtensionType.AGENT, "1.0.0", author != null ? author : "User",
                "Custom Created", agentFolder.getAbsolutePath()
        );
        item.setPermissions(permissions);

        dbHelper.insertOrUpdateExtension(item);
        dbHelper.recordHistory(new InstallationHistoryEntry(
                id, cleanName, "CREATE", "SUCCESS", "1.0.0", "Created custom agent"
        ));

        registry.registerExtension(item);
        registry.registerAgent(def);

        return def;
    }

    public boolean setAgentEnabled(String id, boolean enabled) {
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

    public boolean deleteAgent(String id) {
        return installer.uninstallExtension(id);
    }

    /**
     * Updates an existing agent's system instructions.
     */
    public boolean updateAgentInstructions(String id, String newInstructions) {
        AgentDefinition agent = registry.getAgent(id);
        if (agent == null || agent.getDirectoryPath() == null) return false;

        File agentJson = new File(agent.getDirectoryPath(), "agent.json");
        if (!agentJson.exists()) return false;

        try {
            JSONObject json;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(agentJson))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                json = new JSONObject(sb.toString());
            }
            json.put("system_instructions", newInstructions);
            try (FileWriter writer = new FileWriter(agentJson)) {
                writer.write(json.toString(2));
            }

            agent.setSystemInstructions(newInstructions);

            ExtensionItem item = dbHelper.getExtensionById(id);
            if (item != null) {
                item.setLastUpdatedTimestamp(System.currentTimeMillis());
                dbHelper.insertOrUpdateExtension(item);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies if the agent is authorized to execute a specific tool based on its declared permissions.
     */
    public boolean verifyToolPermission(String agentId, String toolName) {
        AgentDefinition agent = registry.getAgent(agentId);
        if (agent == null) return false;
        return securityManager.isToolExecutionAllowed(toolName, agent.getPermissionRequirements());
    }
}
