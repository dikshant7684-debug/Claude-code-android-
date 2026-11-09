package com.example.extensions.registry;

import com.example.extensions.models.AgentDefinition;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.SkillDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExtensionRegistry {
    public interface RegistryListener {
        void onRegistryChanged();
    }

    private static volatile ExtensionRegistry instance;

    private final Map<String, ExtensionItem> extensionsMap = new ConcurrentHashMap<>();
    private final Map<String, SkillDefinition> skillsMap = new ConcurrentHashMap<>();
    private final Map<String, AgentDefinition> agentsMap = new ConcurrentHashMap<>();
    private final List<RegistryListener> listeners = new CopyOnWriteArrayList<>();

    public static synchronized ExtensionRegistry getInstance() {
        if (instance == null) {
            instance = new ExtensionRegistry();
        }
        return instance;
    }

    private ExtensionRegistry() {}

    public void registerExtension(ExtensionItem item) {
        if (item == null || item.getId() == null) return;
        extensionsMap.put(item.getId(), item);
        notifyListeners();
    }

    public void registerSkill(SkillDefinition skill) {
        if (skill == null || skill.getId() == null) return;
        skillsMap.put(skill.getId(), skill);
        notifyListeners();
    }

    public void registerAgent(AgentDefinition agent) {
        if (agent == null || agent.getId() == null) return;
        agentsMap.put(agent.getId(), agent);
        notifyListeners();
    }

    public void unregisterExtension(String id) {
        if (id == null) return;
        extensionsMap.remove(id);
        skillsMap.remove(id);
        agentsMap.remove(id);
        notifyListeners();
    }

    public ExtensionItem getExtension(String id) {
        return id != null ? extensionsMap.get(id) : null;
    }

    public SkillDefinition getSkill(String id) {
        return id != null ? skillsMap.get(id) : null;
    }

    public AgentDefinition getAgent(String id) {
        return id != null ? agentsMap.get(id) : null;
    }

    public List<ExtensionItem> getAllExtensions() {
        List<ExtensionItem> list = new ArrayList<>(extensionsMap.values());
        Collections.sort(list, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return list;
    }

    public List<SkillDefinition> getActiveSkills() {
        List<SkillDefinition> active = new ArrayList<>();
        for (SkillDefinition skill : skillsMap.values()) {
            ExtensionItem item = extensionsMap.get(skill.getId());
            if (item == null || item.isEnabled()) {
                active.add(skill);
            }
        }
        return active;
    }

    public List<AgentDefinition> getActiveAgents() {
        List<AgentDefinition> active = new ArrayList<>();
        for (AgentDefinition agent : agentsMap.values()) {
            ExtensionItem item = extensionsMap.get(agent.getId());
            if (item == null || item.isEnabled()) {
                active.add(agent);
            }
        }
        return active;
    }

    public void addListener(RegistryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RegistryListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (RegistryListener l : listeners) {
            try {
                l.onRegistryChanged();
            } catch (Exception ignored) {}
        }
    }

    public void clear() {
        extensionsMap.clear();
        skillsMap.clear();
        agentsMap.clear();
        notifyListeners();
    }
}
