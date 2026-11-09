package com.example.extensions.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.extensions.core.ExtensionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomExtensionCreatorActivity extends Activity {

    private ImageButton btnCreatorBack;
    private RadioGroup rgExtensionType;
    private RadioButton rbSkill;
    private RadioButton rbAgent;
    private EditText etCreatorName;
    private EditText etCreatorDescription;
    private TextView tvInstructionsLabel;
    private EditText etCreatorInstructions;
    private Button btnSubmitCreator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.R.layout.activity_custom_extension_creator);

        android.view.View root = findViewById(com.example.R.id.rootCreator);
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

        btnCreatorBack = findViewById(com.example.R.id.btnCreatorBack);
        rgExtensionType = findViewById(com.example.R.id.rgExtensionType);
        rbSkill = findViewById(com.example.R.id.rbSkill);
        rbAgent = findViewById(com.example.R.id.rbAgent);
        etCreatorName = findViewById(com.example.R.id.etCreatorName);
        etCreatorDescription = findViewById(com.example.R.id.etCreatorDescription);
        tvInstructionsLabel = findViewById(com.example.R.id.tvInstructionsLabel);
        etCreatorInstructions = findViewById(com.example.R.id.etCreatorInstructions);
        btnSubmitCreator = findViewById(com.example.R.id.btnSubmitCreator);

        btnCreatorBack.setOnClickListener(v -> finish());

        rgExtensionType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == com.example.R.id.rbSkill) {
                tvInstructionsLabel.setText("Instructions / SKILL.md Content");
                etCreatorInstructions.setHint("Write domain-specific prompts, rules, patterns...");
            } else {
                tvInstructionsLabel.setText("Agent System Instructions & Role");
                etCreatorInstructions.setHint("Define agent persona, responsibilities, and behavioral constraints...");
            }
        });

        btnSubmitCreator.setOnClickListener(v -> handleCreateExtension());
    }

    private void handleCreateExtension() {
        String name = etCreatorName.getText().toString().trim();
        String description = etCreatorDescription.getText().toString().trim();
        String instructions = etCreatorInstructions.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etCreatorName.setError("Extension name is required");
            etCreatorName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(instructions)) {
            etCreatorInstructions.setError("Instructions are required");
            etCreatorInstructions.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            description = name;
        }

        ExtensionManager manager = ExtensionManager.getInstance(this);

        try {
            if (rbSkill.isChecked()) {
                manager.getSkillManager().createCustomSkill(
                        name,
                        description,
                        "Custom",
                        instructions,
                        "User"
                );
                Toast.makeText(this, "Skill '" + name + "' created and activated!", Toast.LENGTH_SHORT).show();
            } else {
                List<String> capabilities = Arrays.asList("code_generation", "code_review", "debugging");
                List<String> tools = Arrays.asList("read_file", "write_file", "edit_file", "search_files", "list_directory");
                List<String> permissions = new ArrayList<>();
                manager.getAgentManager().createCustomAgent(
                        name,
                        description,
                        instructions,
                        capabilities,
                        tools,
                        permissions,
                        "gemini-1.5-pro",
                        "User"
                );
                Toast.makeText(this, "Agent '" + name + "' created and activated!", Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating extension: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
