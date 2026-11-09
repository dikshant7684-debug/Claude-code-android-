package com.example.extensions.discovery;

import com.example.extensions.models.AgentDefinition;
import com.example.extensions.registry.ExtensionRegistry;
import com.example.extensions.security.ExtensionSecurityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutonomousAgentDiscoveryEngine {
    private final ExtensionRegistry registry;
    private final ExtensionSecurityManager securityManager;

    public AutonomousAgentDiscoveryEngine(ExtensionRegistry registry, ExtensionSecurityManager securityManager) {
        this.registry = registry;
        this.securityManager = securityManager;
    }

    /**
     * Discovers a specialized agent suited to handle the user's task.
     */
    public AgentDefinition findBestAgentForTask(String taskPrompt) {
        if (taskPrompt == null || taskPrompt.trim().isEmpty()) return null;
        String lower = taskPrompt.toLowerCase();
        List<AgentDefinition> agents = registry.getActiveAgents();

        for (AgentDefinition agent : agents) {
            String nameLower = agent.getName().toLowerCase();
            String descLower = agent.getDescription() != null ? agent.getDescription().toLowerCase() : "";

            if (lower.contains(nameLower)) return agent;

            // Capability matching
            for (String cap : agent.getCapabilities()) {
                if (lower.contains(cap.toLowerCase())) return agent;
            }

            // Keyword heuristics
            if (descLower.contains("debug") && (lower.contains("fix") || lower.contains("debug") || lower.contains("error"))) return agent;
            if (descLower.contains("test") && (lower.contains("test") || lower.contains("spec") || lower.contains("coverage"))) return agent;
            if (descLower.contains("security") && (lower.contains("audit") || lower.contains("vulnerability") || lower.contains("secret"))) return agent;
            if (descLower.contains("refactor") && (lower.contains("clean") || lower.contains("refactor") || lower.contains("optimize"))) return agent;
        }

        return null;
    }

    /**
     * Verifies which tools of the agent are authorized to execute.
     */
    public List<String> getAuthorizedTools(AgentDefinition agent) {
        if (agent == null) return Collections.emptyList();
        List<String> authorized = new ArrayList<>();
        for (String tool : agent.getAvailableTools()) {
            if (securityManager.isToolExecutionAllowed(tool, agent.getPermissionRequirements())) {
                authorized.add(tool);
            }
        }
        return authorized;
    }

    public List<AgentDefinition> findAgentsByCapability(String capability) {
        if (capability == null) return Collections.emptyList();
        List<AgentDefinition> matched = new ArrayList<>();
        for (AgentDefinition agent : registry.getActiveAgents()) {
            if (agent.getCapabilities() != null && agent.getCapabilities().contains(capability)) {
                matched.add(agent);
            }
        }
        return matched;
    }

    public List<String> getAuthorizedToolsForAgent(String agentId) {
        AgentDefinition agent = registry.getAgent(agentId);
        return getAuthorizedTools(agent);
    }
}
