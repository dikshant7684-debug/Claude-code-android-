package com.example.extensions.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.R;
import com.example.extensions.core.ExtensionManager;
import com.example.extensions.installer.ExtensionInstaller;
import com.example.extensions.models.ExtensionItem;
import com.example.extensions.models.ExtensionSource;
import com.example.extensions.models.ExtensionType;
import com.example.extensions.models.InstallationEvent;
import com.example.extensions.models.InstallationHistoryEntry;
import com.example.extensions.registry.ExtensionRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExtensionCenterActivity extends Activity implements ExtensionRegistry.RegistryListener {

    private static final int REQ_IMPORT_ZIP = 1001;

    private enum Tab { SKILLS, AGENTS, INSTALLED, DISCOVER, CUSTOM, SOURCES, IMPORT_EXPORT, HISTORY }
    private Tab currentTab = Tab.SKILLS;

    private ExtensionManager manager;
    private EditText etInstallUrl;
    private Button btnInstallRepo;
    private TextView tvInstallProgress;
    private TextView tvEmptyState;
    private ListView listView;

    private Button tabSkills, tabAgents, tabInstalled, tabDiscover, tabCustom, tabSources, tabImportExport, tabHistory;

    private ExtensionListAdapter extensionAdapter;
    private HistoryListAdapter historyAdapter;
    private SourcesListAdapter sourcesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extension_center);

        manager = ExtensionManager.getInstance(this);
        manager.getRegistry().addListener(this);

        initViews();
        setupTabs();
        setupActions();
        switchTab(Tab.SKILLS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.getRegistry().removeListener(this);
        }
    }

    @Override
    public void onRegistryChanged() {
        runOnUiThread(() -> refreshList());
    }

    private void initViews() {
        View root = findViewById(R.id.rootExtensionCenter);
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

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnCreate = findViewById(R.id.btnCreateCustom);
        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(ExtensionCenterActivity.this, CustomExtensionCreatorActivity.class);
            startActivity(intent);
        });

        etInstallUrl = findViewById(R.id.etInstallUrl);
        btnInstallRepo = findViewById(R.id.btnInstallRepo);
        tvInstallProgress = findViewById(R.id.tvInstallProgress);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        listView = findViewById(R.id.extensionsListView);

        tabSkills = findViewById(R.id.tabSkills);
        tabAgents = findViewById(R.id.tabAgents);
        tabInstalled = findViewById(R.id.tabInstalled);
        tabDiscover = findViewById(R.id.tabDiscover);
        tabCustom = findViewById(R.id.tabCustom);
        tabSources = findViewById(R.id.tabSources);
        tabImportExport = findViewById(R.id.tabImportExport);
        tabHistory = findViewById(R.id.tabHistory);
    }

    private void setupTabs() {
        tabSkills.setOnClickListener(v -> switchTab(Tab.SKILLS));
        tabAgents.setOnClickListener(v -> switchTab(Tab.AGENTS));
        tabInstalled.setOnClickListener(v -> switchTab(Tab.INSTALLED));
        tabDiscover.setOnClickListener(v -> switchTab(Tab.DISCOVER));
        tabCustom.setOnClickListener(v -> switchTab(Tab.CUSTOM));
        tabSources.setOnClickListener(v -> switchTab(Tab.SOURCES));
        tabImportExport.setOnClickListener(v -> switchTab(Tab.IMPORT_EXPORT));
        tabHistory.setOnClickListener(v -> switchTab(Tab.HISTORY));
    }

    private void setupActions() {
        btnInstallRepo.setOnClickListener(v -> {
            String url = etInstallUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a GitHub repository URL or owner/repo", Toast.LENGTH_SHORT).show();
                return;
            }

            tvInstallProgress.setVisibility(View.VISIBLE);
            tvInstallProgress.setText("Connecting to GitHub...");
            btnInstallRepo.setEnabled(false);

            manager.getInstaller().installFromGitHub(url, null, new ExtensionInstaller.InstallationListener() {
                @Override
                public void onProgress(InstallationEvent event) {
                    runOnUiThread(() -> {
                        tvInstallProgress.setText("[" + event.getStatus() + "] " + event.getCurrentOperation());
                    });
                }

                @Override
                public void onCompleted(ExtensionItem item) {
                    runOnUiThread(() -> {
                        tvInstallProgress.setVisibility(View.GONE);
                        btnInstallRepo.setEnabled(true);
                        etInstallUrl.setText("");
                        Toast.makeText(ExtensionCenterActivity.this, "Installed: " + item.getName(), Toast.LENGTH_LONG).show();
                        refreshList();
                    });
                }

                @Override
                public void onError(String extensionId, String errorMessage) {
                    runOnUiThread(() -> {
                        tvInstallProgress.setText("Error: " + errorMessage);
                        btnInstallRepo.setEnabled(true);
                        Toast.makeText(ExtensionCenterActivity.this, "Installation error: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        resetTabButtons();

        int activeBg = 0xFF8B5CF6;
        int activeText = 0xFFFFFFFF;

        switch (tab) {
            case SKILLS:
                tabSkills.setBackgroundColor(activeBg);
                tabSkills.setTextColor(activeText);
                break;
            case AGENTS:
                tabAgents.setBackgroundColor(activeBg);
                tabAgents.setTextColor(activeText);
                break;
            case INSTALLED:
                tabInstalled.setBackgroundColor(activeBg);
                tabInstalled.setTextColor(activeText);
                break;
            case DISCOVER:
                tabDiscover.setBackgroundColor(activeBg);
                tabDiscover.setTextColor(activeText);
                break;
            case CUSTOM:
                tabCustom.setBackgroundColor(activeBg);
                tabCustom.setTextColor(activeText);
                break;
            case SOURCES:
                tabSources.setBackgroundColor(activeBg);
                tabSources.setTextColor(activeText);
                break;
            case IMPORT_EXPORT:
                tabImportExport.setBackgroundColor(activeBg);
                tabImportExport.setTextColor(activeText);
                break;
            case HISTORY:
                tabHistory.setBackgroundColor(activeBg);
                tabHistory.setTextColor(activeText);
                break;
        }

        refreshList();
    }

    private void resetTabButtons() {
        int inactiveBg = 0xFF201D29;
        int inactiveText = 0xFFAAA4B8;

        tabSkills.setBackgroundColor(inactiveBg); tabSkills.setTextColor(inactiveText);
        tabAgents.setBackgroundColor(inactiveBg); tabAgents.setTextColor(inactiveText);
        tabInstalled.setBackgroundColor(inactiveBg); tabInstalled.setTextColor(inactiveText);
        tabDiscover.setBackgroundColor(inactiveBg); tabDiscover.setTextColor(inactiveText);
        tabCustom.setBackgroundColor(inactiveBg); tabCustom.setTextColor(inactiveText);
        tabSources.setBackgroundColor(inactiveBg); tabSources.setTextColor(inactiveText);
        tabImportExport.setBackgroundColor(inactiveBg); tabImportExport.setTextColor(inactiveText);
        tabHistory.setBackgroundColor(inactiveBg); tabHistory.setTextColor(inactiveText);
    }

    private void refreshList() {
        List<ExtensionItem> all = manager.getRegistry().getAllExtensions();

        if (currentTab == Tab.HISTORY) {
            List<InstallationHistoryEntry> history = manager.getDbHelper().getAllHistory();
            if (history.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No installation events recorded yet.");
                listView.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                historyAdapter = new HistoryListAdapter(history);
                listView.setAdapter(historyAdapter);
            }
            return;
        }

        if (currentTab == Tab.SOURCES) {
            List<ExtensionSource> sources = manager.getDbHelper().getAllSources();
            tvEmptyState.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            sourcesAdapter = new SourcesListAdapter(sources);
            listView.setAdapter(sourcesAdapter);
            return;
        }

        if (currentTab == Tab.IMPORT_EXPORT) {
            launchZipPicker();
            return;
        }

        List<ExtensionItem> filtered = new ArrayList<>();
        switch (currentTab) {
            case SKILLS:
                for (ExtensionItem item : all) if (item.getType() == ExtensionType.SKILL) filtered.add(item);
                break;
            case AGENTS:
                for (ExtensionItem item : all) if (item.getType() == ExtensionType.AGENT) filtered.add(item);
                break;
            case INSTALLED:
                filtered.addAll(all);
                break;
            case CUSTOM:
                for (ExtensionItem item : all) if ("Custom Created".equalsIgnoreCase(item.getSourceUrl())) filtered.add(item);
                break;
            case DISCOVER:
                // Seed sample discoverable skills if none present
                seedOfficialDiscoverableSkills();
                filtered.addAll(all);
                break;
        }

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No extensions found in this view.\nUse '+ Create' or enter a GitHub repository URL above.");
            listView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            extensionAdapter = new ExtensionListAdapter(filtered);
            listView.setAdapter(extensionAdapter);
        }
    }

    private void seedOfficialDiscoverableSkills() {
        if (manager.getRegistry().getAllExtensions().isEmpty()) {
            try {
                manager.getSkillManager().createCustomSkill(
                        "Android Architecture Skill",
                        "Guides clean architecture, Jetpack Compose, and ViewModel patterns.",
                        "Android",
                        "Always adhere strictly to Clean MVVM, Material 3 density guidelines, and proper Coroutine dispatchers.",
                        "Official AI Studio"
                );
                manager.getSkillManager().createCustomSkill(
                        "Python Code Specialist",
                        "Expert Python best practices, typing, and standard library architectures.",
                        "Python",
                        "Provide clean Python 3 with type annotations, docstrings, and standard library modules.",
                        "Official AI Studio"
                );
            } catch (Exception ignored) {}
        }
    }

    private void launchZipPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(intent, REQ_IMPORT_ZIP);
        } catch (Exception e) {
            Toast.makeText(this, "No file browser available to pick ZIP", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMPORT_ZIP && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importZipFromUri(uri);
            }
        }
    }

    private void importZipFromUri(Uri uri) {
        tvInstallProgress.setVisibility(View.VISIBLE);
        tvInstallProgress.setText("Importing local ZIP archive...");

        new Thread(() -> {
            try {
                File tempZip = new File(getCacheDir(), "import_" + System.currentTimeMillis() + ".zip");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(tempZip)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                }

                manager.getInstaller().installFromLocalZip(tempZip, new ExtensionInstaller.InstallationListener() {
                    @Override
                    public void onProgress(InstallationEvent event) {
                        runOnUiThread(() -> tvInstallProgress.setText(event.getCurrentOperation()));
                    }

                    @Override
                    public void onCompleted(ExtensionItem item) {
                        runOnUiThread(() -> {
                            tvInstallProgress.setVisibility(View.GONE);
                            Toast.makeText(ExtensionCenterActivity.this, "Imported: " + item.getName(), Toast.LENGTH_LONG).show();
                            switchTab(Tab.INSTALLED);
                        });
                    }

                    @Override
                    public void onError(String extensionId, String errorMessage) {
                        runOnUiThread(() -> {
                            tvInstallProgress.setText("Import error: " + errorMessage);
                            Toast.makeText(ExtensionCenterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvInstallProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed reading ZIP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== ADAPTERS ====================

    private class ExtensionListAdapter extends BaseAdapter {
        private final List<ExtensionItem> items;

        ExtensionListAdapter(List<ExtensionItem> items) {
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExtensionCenterActivity.this).inflate(R.layout.item_extension_card, parent, false);
            }

            ExtensionItem item = items.get(position);
            TextView tvName = convertView.findViewById(R.id.tvExtName);
            TextView tvBadge = convertView.findViewById(R.id.tvExtTypeBadge);
            TextView tvVersion = convertView.findViewById(R.id.tvExtVersion);
            TextView tvDesc = convertView.findViewById(R.id.tvExtDescription);
            TextView tvAuthor = convertView.findViewById(R.id.tvExtAuthor);
            TextView tvSource = convertView.findViewById(R.id.tvExtSource);
            Button btnToggle = convertView.findViewById(R.id.btnToggleEnable);
            Button btnInspect = convertView.findViewById(R.id.btnInspect);
            Button btnExport = convertView.findViewById(R.id.btnExport);
            Button btnDelete = convertView.findViewById(R.id.btnUninstall);

            tvName.setText(item.getName());
            tvBadge.setText(item.getType().name());
            tvVersion.setText("v" + (item.getVersion() != null ? item.getVersion() : "1.0.0"));
            tvDesc.setText(item.getDescription() != null ? item.getDescription() : "No description provided");
            tvAuthor.setText("Author: " + (item.getAuthor() != null ? item.getAuthor() : "Community"));
            tvSource.setText("Source: " + (item.getSourceUrl() != null ? item.getSourceUrl() : "Local"));

            btnToggle.setText(item.isEnabled() ? "Disable" : "Enable");
            btnToggle.setOnClickListener(v -> {
                boolean newStatus = !item.isEnabled();
                if (item.getType() == ExtensionType.SKILL) {
                    manager.getSkillManager().setSkillEnabled(item.getId(), newStatus);
                } else {
                    manager.getAgentManager().setAgentEnabled(item.getId(), newStatus);
                }
                notifyDataSetChanged();
            });

            btnInspect.setOnClickListener(v -> {
                Intent intent = new Intent(ExtensionCenterActivity.this, ExtensionDetailActivity.class);
                intent.putExtra("extension_id", item.getId());
                startActivity(intent);
            });

            btnExport.setOnClickListener(v -> {
                try {
                    File exportDir = new File(getFilesDir(), "exports");
                    if (!exportDir.exists()) exportDir.mkdirs();
                    File zip = new File(exportDir, item.getId() + ".zip");
                    manager.getSkillManager().exportSkillToZip(item.getId(), zip);
                    Toast.makeText(ExtensionCenterActivity.this, "Exported to: " + zip.getName(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(ExtensionCenterActivity.this, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            btnDelete.setOnClickListener(v -> {
                manager.getInstaller().uninstallExtension(item.getId());
                Toast.makeText(ExtensionCenterActivity.this, "Uninstalled " + item.getName(), Toast.LENGTH_SHORT).show();
                refreshList();
            });

            return convertView;
        }
    }

    private class HistoryListAdapter extends BaseAdapter {
        private final List<InstallationHistoryEntry> entries;

        HistoryListAdapter(List<InstallationHistoryEntry> entries) {
            this.entries = entries;
        }

        @Override public int getCount() { return entries.size(); }
        @Override public Object getItem(int position) { return entries.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExtensionCenterActivity.this).inflate(R.layout.item_installation_history, parent, false);
            }
            InstallationHistoryEntry e = entries.get(position);
            TextView tvName = convertView.findViewById(R.id.tvHistName);
            TextView tvStatus = convertView.findViewById(R.id.tvHistStatus);
            TextView tvDetails = convertView.findViewById(R.id.tvHistDetails);
            TextView tvTimestamp = convertView.findViewById(R.id.tvHistTimestamp);

            tvName.setText(e.getExtensionName() + " (" + e.getAction() + ")");
            tvStatus.setText(e.getStatus());
            tvStatus.setTextColor("SUCCESS".equalsIgnoreCase(e.getStatus()) ? 0xFF4ADE80 : 0xFFF87171);
            tvDetails.setText(e.getDetails());
            tvTimestamp.setText(new java.util.Date(e.getTimestamp()).toString());

            return convertView;
        }
    }

    private class SourcesListAdapter extends BaseAdapter {
        private final List<ExtensionSource> sources;

        SourcesListAdapter(List<ExtensionSource> sources) {
            this.sources = sources;
        }

        @Override public int getCount() { return sources.size(); }
        @Override public Object getItem(int position) { return sources.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExtensionCenterActivity.this).inflate(R.layout.item_extension_source, parent, false);
            }
            ExtensionSource s = sources.get(position);
            TextView tvName = convertView.findViewById(R.id.tvSourceName);
            TextView tvBadge = convertView.findViewById(R.id.tvSourceTrustedBadge);
            TextView tvUrl = convertView.findViewById(R.id.tvSourceUrl);

            tvName.setText(s.getName());
            tvBadge.setVisibility(s.isTrusted() ? View.VISIBLE : View.GONE);
            tvUrl.setText(s.getUrl());

            return convertView;
        }
    }
}
