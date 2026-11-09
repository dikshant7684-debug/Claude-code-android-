package com.example.extensions.core;

import android.content.Context;
import android.util.Log;

import com.example.extensions.agents.AgentManager;
import com.example.extensions.database.ExtensionDatabaseHelper;
import com.example.extensions.discovery.AutonomousAgentDiscoveryEngine;
import com.example.extensions.discovery.AutonomousSkillDiscoveryEngine;
import com.example.extensions.github.GitHubRepositoryClient;
import com.example.extensions.installer.ExtensionInstaller;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.registry.ExtensionRegistry;
import com.example.extensions.security.ExtensionSecurityManager;
import com.example.extensions.skills.SkillManager;
import com.example.extensions.validator.ExtensionValidator;

import java.io.File;
import java.util.List;

public class ExtensionManager {
    private static final String TAG = "ExtensionManager";
    private static volatile ExtensionManager instance;

    private final Context context;
    private final ExtensionDatabaseHelper dbHelper;
    private final ExtensionRegistry registry;
    private final ExtensionSecurityManager securityManager;
    private final ExtensionInstaller installer;
    private final SkillManager skillManager;
    private final AgentManager agentManager;
    private final GitHubRepositoryClient gitHubClient;
    private final AutonomousSkillDiscoveryEngine skillDiscoveryEngine;
    private final AutonomousAgentDiscoveryEngine agentDiscoveryEngine;

    public static synchronized ExtensionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExtensionManager(context.getApplicationContext());
        }
        return instance;
    }

    private ExtensionManager(Context context) {
        this.context = context;
        this.dbHelper = ExtensionDatabaseHelper.getInstance(context);
        this.registry = ExtensionRegistry.getInstance();
        this.securityManager = ExtensionSecurityManager.getInstance(context);
        this.installer = new ExtensionInstaller(context, dbHelper, registry, securityManager);
        this.skillManager = new SkillManager(context, dbHelper, registry, installer);
        this.agentManager = new AgentManager(context, dbHelper, registry, installer, securityManager);
        this.gitHubClient = new GitHubRepositoryClient();
        this.skillDiscoveryEngine = new AutonomousSkillDiscoveryEngine(registry);
        this.agentDiscoveryEngine = new AutonomousAgentDiscoveryEngine(registry, securityManager);

        // Load existing installed extensions from SQLite database and filesystem
        loadInstalledExtensions();
    }

    /**
     * Bootstraps persistent extensions on startup.
     */
    public synchronized void loadInstalledExtensions() {
        try {
            List<ExtensionItem> items = dbHelper.getAllExtensions();
            ExtensionValidator validator = new ExtensionValidator();

            for (ExtensionItem item : items) {
                registry.registerExtension(item);
                if (item.getInstallDirectory() != null) {
                    File dir = new File(item.getInstallDirectory());
                    if (dir.exists()) {
                        ExtensionValidator.ValidationResult val = validator.validateDirectory(dir);
                        if (val.isValid) {
                            if (val.skillDefinition != null) {
                                val.skillDefinition.setId(item.getId());
                                registry.registerSkill(val.skillDefinition);
                            } else if (val.agentDefinition != null) {
                                val.agentDefinition.setId(item.getId());
                                registry.registerAgent(val.agentDefinition);
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "Successfully bootstrapped " + items.size() + " extensions from database.");
        } catch (Exception e) {
            Log.e(TAG, "Error bootstrapping installed extensions", e);
        }
    }

    public Context getContext() { return context; }
    public ExtensionDatabaseHelper getDbHelper() { return dbHelper; }
    public ExtensionRegistry getRegistry() { return registry; }
    public ExtensionSecurityManager getSecurityManager() { return securityManager; }
    public ExtensionInstaller getInstaller() { return installer; }
    public SkillManager getSkillManager() { return skillManager; }
    public AgentManager getAgentManager() { return agentManager; }
    public GitHubRepositoryClient getGitHubClient() { return gitHubClient; }
    public AutonomousSkillDiscoveryEngine getSkillDiscoveryEngine() { return skillDiscoveryEngine; }
    public AutonomousAgentDiscoveryEngine getAgentDiscoveryEngine() { return agentDiscoveryEngine; }
}
