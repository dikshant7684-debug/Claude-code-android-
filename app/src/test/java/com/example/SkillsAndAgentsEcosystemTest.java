package com.example;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.extensions.agents.AgentManager;
import com.example.extensions.chat.ChatExtensionCommandDetector;
import com.example.extensions.core.ExtensionManager;
import com.example.extensions.database.ExtensionDatabaseHelper;
import com.example.extensions.discovery.AutonomousAgentDiscoveryEngine;
import com.example.extensions.discovery.AutonomousSkillDiscoveryEngine;
import com.example.extensions.github.GitHubRepositoryClient;
import com.example.extensions.models.AgentDefinition;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionSource;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationHistoryEntry;
import com.example.extensions.models.SkillDefinition;
import com.example.extensions.security.ExtensionSecurityManager;
import com.example.extensions.skills.SkillManager;
import com.example.extensions.validator.ExtensionValidator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {36})
public class SkillsAndAgentsEcosystemTest {

    private Context context;
    private ExtensionManager extensionManager;
    private ExtensionDatabaseHelper dbHelper;
    private ExtensionSecurityManager securityManager;
    private ExtensionValidator validator;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        extensionManager = ExtensionManager.getInstance(context);
        dbHelper = extensionManager.getDbHelper();
        securityManager = extensionManager.getSecurityManager();
        validator = new ExtensionValidator();
    }

    @Test
    public void testGitHubRepositoryClientParsing() {
        GitHubRepositoryClient client = new GitHubRepositoryClient();

        GitHubRepositoryClient.RepoTarget parsed1 = client.parseGitHubUrl("https://github.com/google/ai-studio-skills");
        Assert.assertNotNull(parsed1);
        Assert.assertEquals("google", parsed1.owner);
        Assert.assertEquals("ai-studio-skills", parsed1.repo);
        Assert.assertEquals("main", parsed1.branch);
        Assert.assertEquals("", parsed1.subpath);

        GitHubRepositoryClient.RepoTarget parsed2 = client.parseGitHubUrl("https://github.com/octocat/Hello-World/tree/main/skills/debugging");
        Assert.assertNotNull(parsed2);
        Assert.assertEquals("octocat", parsed2.owner);
        Assert.assertEquals("Hello-World", parsed2.repo);
        Assert.assertEquals("main", parsed2.branch);
        Assert.assertEquals("skills/debugging", parsed2.subpath);

        GitHubRepositoryClient.RepoTarget invalid = client.parseGitHubUrl("invalid-url");
        Assert.assertNull(invalid);
    }

    @Test
    public void testExtensionSecurityZipSlipProtection() {
        File targetDir = new File(context.getFilesDir(), "test_sandbox");
        targetDir.mkdirs();

        // Zip Slip path traversal attempt
        boolean isSafe = securityManager.isSafeZipPath(targetDir, "../../etc/passwd");
        Assert.assertFalse("Zip slip path traversal must be rejected", isSafe);

        // Safe path inside target directory
        boolean isSafeValid = securityManager.isSafeZipPath(targetDir, "skills/android/SKILL.md");
        Assert.assertTrue("Legitimate relative path inside sandbox must be accepted", isSafeValid);

        // Relative path traversal test
        Assert.assertFalse(securityManager.isSafeRelativePath("../escape.txt"));
        Assert.assertTrue(securityManager.isSafeRelativePath("scripts/run.sh"));
    }

    @Test
    public void testExtensionSecurityToolPermissions() {
        List<String> declaredPerms = Arrays.asList("EXECUTE_TERMINAL_COMMANDS", "MODIFY_FILES");

        // Safe tools without special requirements
        Assert.assertTrue(securityManager.isToolExecutionAllowed("read_file", declaredPerms));
        Assert.assertTrue(securityManager.isToolExecutionAllowed("search_files", declaredPerms));

        // Privileged tools that match declared permissions
        Assert.assertTrue(securityManager.isToolExecutionAllowed("execute_command", declaredPerms));
        Assert.assertTrue(securityManager.isToolExecutionAllowed("write_file", declaredPerms));

        // Privileged tool not in declared permissions
        List<String> limitedPerms = Collections.singletonList("READ_FILES");
        Assert.assertFalse(securityManager.isToolExecutionAllowed("execute_command", limitedPerms));
        Assert.assertFalse(securityManager.isToolExecutionAllowed("write_file", limitedPerms));
    }

    @Test
    public void testYamlFrontmatterAndSkillValidation() throws IOException {
        File tempSkillDir = new File(context.getCacheDir(), "test_skill_" + System.currentTimeMillis());
        tempSkillDir.mkdirs();
        new File(tempSkillDir, "scripts").mkdirs();
        new File(tempSkillDir, "references").mkdirs();

        File skillMd = new File(tempSkillDir, "SKILL.md");
        try (FileWriter fw = new FileWriter(skillMd)) {
            fw.write("---\n");
            fw.write("name: \"Android Architecture Specialist\"\n");
            fw.write("description: \"Enforces modern clean architecture and M3 design in Compose\"\n");
            fw.write("category: \"Architecture\"\n");
            fw.write("author: \"NEU\\CODE Team\"\n");
            fw.write("version: \"1.2.0\"\n");
            fw.write("---\n\n");
            fw.write("# Android Architecture Specialist\n\n");
            fw.write("Use ViewModel, StateFlow, and Compose M3 standards.");
        }

        ExtensionValidator.ValidationResult result = validator.validateDirectory(tempSkillDir);
        Assert.assertTrue(result.isValid);
        Assert.assertEquals("SKILL", result.detectedType);
        Assert.assertNotNull(result.skillDefinition);
        Assert.assertEquals("Android Architecture Specialist", result.skillDefinition.getName());
        Assert.assertEquals("Architecture", result.skillDefinition.getCategory());
        Assert.assertTrue(result.skillDefinition.getInstructions().contains("ViewModel"));
    }

    @Test
    public void testAgentJsonValidation() throws IOException {
        File tempAgentDir = new File(context.getCacheDir(), "test_agent_" + System.currentTimeMillis());
        tempAgentDir.mkdirs();

        File agentJson = new File(tempAgentDir, "agent.json");
        try (FileWriter fw = new FileWriter(agentJson)) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", "Database Optimization Agent");
                obj.put("description", "Analyzes query plans and schema migrations");
                obj.put("version", "1.0.0");
                obj.put("author", "DBA Team");
                obj.put("system_instructions", "Always check SQLite indices and table schemas.");
                obj.put("capabilities", new JSONArray(Arrays.asList("database_optimization", "sql_analysis")));
                obj.put("tools", new JSONArray(Arrays.asList("read_file", "search_files", "execute_command")));
                obj.put("permissions", new JSONArray(Collections.singletonList("EXECUTE_TERMINAL_COMMANDS")));
                obj.put("preferred_model", "gemini-1.5-pro");
            } catch (Exception ignored) {}
            fw.write(obj.toString());
        }

        ExtensionValidator.ValidationResult result = validator.validateDirectory(tempAgentDir);
        Assert.assertTrue(result.isValid);
        Assert.assertEquals("AGENT", result.detectedType);
        Assert.assertNotNull(result.agentDefinition);
        Assert.assertEquals("Database Optimization Agent", result.agentDefinition.getName());
        Assert.assertTrue(result.agentDefinition.getAvailableTools().contains("execute_command"));
    }

    @Test
    public void testExtensionDatabaseCrud() {
        String testId = "test_ext_" + System.currentTimeMillis();
        ExtensionItem item = new ExtensionItem(
                testId,
                "Test Extension Engine",
                "Testing persistent database storage",
                ExtensionType.SKILL,
                "2.0.0",
                "Unit Tester",
                "https://github.com/test/repo",
                "/data/local/tmp/test"
        );
        item.setEnabled(true);
        item.setValidationStatus("VALID");

        // Insert
        boolean inserted = dbHelper.insertOrUpdateExtension(item);
        Assert.assertTrue(inserted);

        // Retrieve
        ExtensionItem fetched = dbHelper.getExtensionById(testId);
        Assert.assertNotNull(fetched);
        Assert.assertEquals("Test Extension Engine", fetched.getName());
        Assert.assertTrue(fetched.isEnabled());

        // Update enable state
        boolean toggled = dbHelper.setExtensionEnabled(testId, false);
        Assert.assertTrue(toggled);
        ExtensionItem updated = dbHelper.getExtensionById(testId);
        Assert.assertFalse(updated.isEnabled());

        // Record history
        InstallationHistoryEntry history = new InstallationHistoryEntry(
                testId, "Test Extension Engine", "INSTALL", "SUCCESS", "2.0.0", "Installed via test"
        );
        dbHelper.recordHistory(history);

        List<InstallationHistoryEntry> entries = dbHelper.getAllHistory();
        Assert.assertFalse(entries.isEmpty());
        Assert.assertTrue(entries.stream().anyMatch(e -> testId.equals(e.getExtensionId())));

        // Manage sources
        ExtensionSource source = new ExtensionSource(
                "src_test", "Custom Index", "https://skills.neucode.ai/index.json", "CUSTOM_INDEX", true
        );
        boolean srcAdded = dbHelper.insertSource(source);
        Assert.assertTrue(srcAdded);

        List<ExtensionSource> sources = dbHelper.getAllSources();
        Assert.assertTrue(sources.stream().anyMatch(s -> s.getId().equals("src_test")));

        // Cleanup
        dbHelper.deleteExtension(testId);
        Assert.assertNull(dbHelper.getExtensionById(testId));
    }

    @Test
    public void testSkillManagerCreateCustomSkill() throws IOException {
        SkillManager skillManager = extensionManager.getSkillManager();
        String skillName = "Compose M3 Expert " + System.currentTimeMillis();

        SkillDefinition skill = skillManager.createCustomSkill(
                skillName,
                "Creates elegant Material 3 components in Jetpack Compose",
                "UI",
                "Strictly use MaterialTheme.colorScheme and ensure 48dp minimum touch targets.",
                "Developer"
        );

        Assert.assertNotNull(skill);
        Assert.assertEquals(skillName, skill.getName());
        Assert.assertNotNull(skill.getDirectoryPath());

        File dir = new File(skill.getDirectoryPath());
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(new File(dir, "SKILL.md").exists());
        Assert.assertTrue(new File(dir, "scripts").exists());
        Assert.assertTrue(new File(dir, "references").exists());

        // Test update instructions
        boolean updated = skillManager.updateSkillInstructions(skill.getId(), "Updated instructions with new color rules.");
        Assert.assertTrue(updated);

        SkillDefinition reloaded = skillManager.getSkillById(skill.getId());
        Assert.assertNotNull(reloaded);
        Assert.assertTrue(reloaded.getInstructions().contains("Updated instructions"));
    }

    @Test
    public void testAgentManagerCreateCustomAgent() throws IOException {
        AgentManager agentManager = extensionManager.getAgentManager();
        String agentName = "Security Auditor Agent " + System.currentTimeMillis();

        AgentDefinition agent = agentManager.createCustomAgent(
                agentName,
                "Audits code for vulnerabilities and leaked secrets",
                "Scan source code for API keys, hardcoded credentials, and path traversals.",
                Arrays.asList("vulnerability_scanning", "security_audit"),
                Arrays.asList("read_file", "search_files"),
                Collections.singletonList("READ_FILES"),
                "gemini-1.5-pro",
                "Security Team"
        );

        Assert.assertNotNull(agent);
        Assert.assertEquals(agentName, agent.getName());
        Assert.assertNotNull(agent.getDirectoryPath());

        File dir = new File(agent.getDirectoryPath());
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(new File(dir, "agent.json").exists());

        // Verify permission enforcement
        Assert.assertTrue(agentManager.verifyToolPermission(agent.getId(), "read_file"));
        Assert.assertFalse(agentManager.verifyToolPermission(agent.getId(), "execute_command"));
    }

    @Test
    public void testAutonomousSkillDiscovery() {
        AutonomousSkillDiscoveryEngine engine = extensionManager.getSkillDiscoveryEngine();

        // Register dummy active skills
        SkillDefinition skill1 = new SkillDefinition("android_debug", "Android Debugger", "Diagnoses crashes and logcat errors", "# Android Debugger\nInspect logcat and crash traces.");
        skill1.setCategory("Debugging");
        extensionManager.getRegistry().registerSkill(skill1);

        SkillDefinition skill2 = new SkillDefinition("db_perf", "Database Tuning", "Optimizes SQL queries and Room DAOs", "# Database Tuning\nIndex foreign keys.");
        skill2.setCategory("Database");
        extensionManager.getRegistry().registerSkill(skill2);

        // Prompt asking for debugging
        List<SkillDefinition> matches = engine.discoverRelevantSkills("Please help me debug an Android crash in MainActivity");
        Assert.assertFalse(matches.isEmpty());
        Assert.assertTrue(matches.stream().anyMatch(s -> s.getId().equals("android_debug")));

        String promptInjection = AutonomousSkillDiscoveryEngine.buildSkillContextPrompt(matches);
        Assert.assertNotNull(promptInjection);
        Assert.assertTrue(promptInjection.contains("Android Debugger"));
        Assert.assertTrue(promptInjection.contains("Inspect logcat"));
    }

    @Test
    public void testAutonomousAgentDiscovery() {
        AutonomousAgentDiscoveryEngine engine = extensionManager.getAgentDiscoveryEngine();

        AgentDefinition agent1 = new AgentDefinition("code_reviewer", "Code Reviewer", "Reviews pull requests", "Look for clean code standards.");
        agent1.setCapabilities(Arrays.asList("code_review", "linting"));
        agent1.setAvailableTools(Arrays.asList("read_file", "search_files"));
        extensionManager.getRegistry().registerAgent(agent1);

        List<AgentDefinition> matched = engine.findAgentsByCapability("code_review");
        Assert.assertFalse(matched.isEmpty());
        Assert.assertEquals("code_reviewer", matched.get(0).getId());

        List<String> allowedTools = engine.getAuthorizedToolsForAgent("code_reviewer");
        Assert.assertTrue(allowedTools.contains("read_file"));
        Assert.assertFalse(allowedTools.contains("execute_command"));
    }

    @Test
    public void testChatExtensionCommandDetection() {
        // GitHub install prompts
        Assert.assertTrue(ChatExtensionCommandDetector.isExtensionCommand("Install this skill: https://github.com/google/skills-repo"));
        Assert.assertTrue(ChatExtensionCommandDetector.isExtensionCommand("Add agent from https://github.com/octocat/code-agent"));
        Assert.assertTrue(ChatExtensionCommandDetector.isExtensionCommand("Please install extension https://github.com/foo/bar"));

        // Custom creation prompts
        Assert.assertTrue(ChatExtensionCommandDetector.isExtensionCommand("Create a custom skill for React frontend development"));
        Assert.assertTrue(ChatExtensionCommandDetector.isExtensionCommand("Create an agent for database migration"));

        // Normal coding requests should not trigger extension command handler
        Assert.assertFalse(ChatExtensionCommandDetector.isExtensionCommand("Build a responsive website"));
        Assert.assertFalse(ChatExtensionCommandDetector.isExtensionCommand("Fix this bug in MainActivity.kt"));
        Assert.assertFalse(ChatExtensionCommandDetector.isExtensionCommand("Add a login screen with Google OAuth"));
    }
}
