package com.example.extensions.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.extensions.agents.AgentManager;
import com.example.extensions.core.ExtensionManager;
import com.example.extensions.models.AgentDefinition;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.SkillDefinition;
import com.example.extensions.skills.SkillManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExtensionDetailActivity extends Activity {

    public static final String EXTRA_EXTENSION_ID = "extension_id";

    private String extensionId;
    private ExtensionItem extension;

    private TextView tvDetailTitle;
    private TextView tvDetailMetadata;
    private EditText etDetailInstructions;
    private Button btnSaveInstructions;
    private ImageButton btnDetailBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.R.layout.activity_extension_detail);

        android.view.View root = findViewById(com.example.R.id.rootDetail);
        if (root != null) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.graphics.Insets sb = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                    v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
                } else {
                    v.setPadding(
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom()
                    );
                }
                return insets;
            });
        }

        extensionId = getIntent().getStringExtra(EXTRA_EXTENSION_ID);
        if (TextUtils.isEmpty(extensionId)) {
            Toast.makeText(this, "Extension ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailTitle = findViewById(com.example.R.id.tvDetailTitle);
        tvDetailMetadata = findViewById(com.example.R.id.tvDetailMetadata);
        etDetailInstructions = findViewById(com.example.R.id.etDetailInstructions);
        btnSaveInstructions = findViewById(com.example.R.id.btnSaveInstructions);
        btnDetailBack = findViewById(com.example.R.id.btnDetailBack);

        btnDetailBack.setOnClickListener(v -> finish());

        loadExtensionData();

        btnSaveInstructions.setOnClickListener(v -> saveInstructions());
    }

    private void loadExtensionData() {
        ExtensionManager manager = ExtensionManager.getInstance(this);
        extension = manager.getRegistry().getExtension(extensionId);
        if (extension == null) {
            extension = manager.getDbHelper().getExtensionById(extensionId);
        }

        if (extension == null) {
            Toast.makeText(this, "Extension not found: " + extensionId, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailTitle.setText(extension.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String installedDate = sdf.format(new Date(extension.getInstalledTimestamp()));

        StringBuilder meta = new StringBuilder();
        meta.append("ID: ").append(extension.getId()).append("\n");
        meta.append("Type: ").append(extension.getType()).append("\n");
        meta.append("Version: ").append(extension.getVersion()).append(" | Author: ").append(extension.getAuthor()).append("\n");
        meta.append("Status: ").append(extension.getValidationStatus()).append(" | Enabled: ").append(extension.isEnabled()).append("\n");
        meta.append("Installed: ").append(installedDate).append("\n");
        meta.append("Install Path: ").append(extension.getInstallDirectory()).append("\n");

        if (extension.getPermissions() != null && !extension.getPermissions().isEmpty()) {
            meta.append("Permissions: ").append(String.join(", ", extension.getPermissions())).append("\n");
        }
        if (extension.getDependencies() != null && !extension.getDependencies().isEmpty()) {
            meta.append("Dependencies: ").append(String.join(", ", extension.getDependencies())).append("\n");
        }

        tvDetailMetadata.setText(meta.toString());

        // Load instructions
        if (extension.getType() == ExtensionType.SKILL) {
            SkillDefinition skill = manager.getSkillManager().getSkillById(extensionId);
            if (skill != null && skill.getSkillMdContent() != null) {
                etDetailInstructions.setText(skill.getSkillMdContent());
            } else if (skill != null && skill.getInstructions() != null) {
                etDetailInstructions.setText(skill.getInstructions());
            } else {
                etDetailInstructions.setText(extension.getDescription());
            }
        } else {
            AgentDefinition agent = manager.getAgentManager().getAgentById(extensionId);
            if (agent != null && agent.getSystemInstructions() != null) {
                etDetailInstructions.setText(agent.getSystemInstructions());
            } else {
                etDetailInstructions.setText(extension.getDescription());
            }
        }
    }

    private void saveInstructions() {
        if (extension == null) return;

        String newContent = etDetailInstructions.getText().toString();
        ExtensionManager manager = ExtensionManager.getInstance(this);

        if (extension.getType() == ExtensionType.SKILL) {
            try {
                boolean success = manager.getSkillManager().updateSkillInstructions(extensionId, newContent);
                if (success) {
                    Toast.makeText(this, "Skill instructions updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update skill instructions", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error saving skill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            boolean success = manager.getAgentManager().updateAgentInstructions(extensionId, newContent);
            if (success) {
                Toast.makeText(this, "Agent instructions updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update agent instructions", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
